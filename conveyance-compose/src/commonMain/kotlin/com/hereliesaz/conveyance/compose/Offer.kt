package com.hereliesaz.conveyance.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.Consequence
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Route
import com.hereliesaz.conveyance.Signature
import com.hereliesaz.conveyance.Step
import com.hereliesaz.conveyance.Weight
import kotlinx.coroutines.launch

/** Every act is addressable, because a refusal has to start somewhere and a result has to land. */
val Act.elementId: ElementId get() = ElementId("act:${id.value}")

/**
 * What a control knows about the act it is offering.
 *
 * All five states arrive through one scope, so it is not possible to write a control that paints
 * only the happy one. That is Law 1 enforced by the shape of the API rather than by review: the
 * separate spinner, the separate snackbar and the separate error banner have nowhere to attach.
 */
@Immutable
class ActScope internal constructor(
    val act: Act,
    val state: ActState,
    /** Practice-adjusted. Familiarity buys speed, never a reduction in stakes. */
    val weight: Weight,
    /** True exactly once, before first use: this control owes its half-rep. */
    val owesTell: Boolean,
    private val onEngage: () -> Unit,
) {
    val signature: Signature get() = act.signature

    /** Whether the person is being told to wait, and how far along if it is known. */
    val yielding: Float? get() = (state as? ActState.Yielding)?.extent

    fun engage() = onEngage()

    companion object {
        /**
         * An [ActScope] pinned to one state, for previews and for rendering a control's states side
         * by side.
         *
         * This is not a way around Law 1. A control still has to handle all five states — this hands
         * it one of them so a person can look at it, which is the opposite of letting a developer
         * ship only the happy path.
         */
        fun pinned(
            act: Act,
            state: ActState,
            weight: Weight = act.weight,
            owesTell: Boolean = false,
        ) = ActScope(act, state, weight, owesTell, onEngage = {})
    }
}

/**
 * Offer an act to the person.
 *
 * The control renders whichever of the five states the act is currently in, in one place, as one
 * element. Engaging a blocked act does not refuse in place and does not do nothing: it escorts, so
 * the person ends up at the thing they were missing rather than in front of a rule.
 *
 * Note what this function does not take. There is no `enabled`, because unavailability has an
 * address. There is no `onSuccess` or `onError`, because a result is a state of this element rather
 * than an event to be reported somewhere else. There is no `animationSpec`, because motion is the
 * grammar's business. What is absent here is as deliberate as what is present.
 */
@Composable
fun Offer(
    act: Act,
    modifier: Modifier = Modifier,
    /**
     * The address this act is offered at.
     *
     * Defaults to the act's own, which is right when the control is whatever this composable draws.
     * Pass the visible element's address when the act is offered *by* something that already has
     * one -- a row, a face, a card. Otherwise the act registers at a wrapper nobody can see, the
     * thing a person actually touches is accounted for by nothing, and the census reports an
     * invitation with no act behind it. That is exactly how this parameter was discovered.
     */
    element: ElementId = act.elementId,
    content: @Composable ActScope.() -> Unit,
) {
    val registry = LocalElements.current
    val practice = LocalPractice.current
    val stage = LocalStage.current
    val places = LocalPlaces.current
    val reduced = LocalReducedMotion.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember(act.id) { mutableStateOf<ActState>(ActState.Ready) }

    // Registering the act against its element is what lets the surface be counted: how much is on
    // screen, against how much can be done there.
    DisposableEffect(registry, act.id, element) {
        registry.offer(act, element)
        // A gate's address is doing a job -- it is where a person is carried when something is
        // missing -- and nobody should have to write that down.
        act.requires.forEach { registry.markGate(it.livesAt) }
        onDispose { registry.withdraw(act.id) }
    }

    // The Refuse signature: resist at the point of contact, leaning toward the gate, before the
    // escort carries the person over. Without this the refusal is silent and the escort looks like
    // the screen moving on its own.
    val resist = remember(act.id) { Animatable(Offset.Zero, Offset.VectorConverter) }

    // While at rest, track the world: a gate satisfied elsewhere unblocks this control with no
    // notification, no refresh, and nothing for the person to dismiss.
    val atRest = state is ActState.Ready || state is ActState.Blocked
    val live = act.state()
    LaunchedEffect(atRest, live) {
        if (atRest) state = live
    }

    val scope = ActScope(
        act = act,
        state = state,
        weight = practice.weightFor(act),
        owesTell = practice.owesTell(act.id),
        onEngage = {
            coroutineScope.launch {
                when (val terminal = act.engage { state = it }) {
                    is ActState.Blocked -> {
                        // Not "where is the first thing that is missing" but "where is the first
                        // thing they can actually do". When the missing thing is itself blocked,
                        // those are different addresses, and only the second one is any use.
                        val destination = when (val step = Route.from(act, registry::offering)) {
                            is Step.Do -> step.opens.livesAt
                            is Step.Stranded -> step.gate.livesAt
                            Step.Ready -> terminal.gate.livesAt
                        }
                        registry.lean(resist, act, destination)
                        registry.escortTo(destination)
                    }
                    ActState.Settled -> {
                        // Practice is earned by doing the thing, not by reaching for it.
                        practice.record(act.id)
                        when (val consequence = act.consequence) {
                            // Entering is the one verb whose destination is not another element --
                            // it is the whole window -- so it is the places host that renders it,
                            // not the stage.
                            is Consequence.Enter -> places?.enter(consequence.place, act.weight)
                            else -> registry.carry(stage, act, reduced)
                        }
                    }
                    else -> Unit
                }
            }
        },
    )

    Box(
        modifier
            .graphicsLayer {
                translationX = resist.value.x
                translationY = resist.value.y
            }
            .element(element, token = { ActScope.pinned(act, ActState.Ready).content() }),
    ) {
        scope.content()
    }
}

/**
 * Render the act's consequence as motion, from the model alone.
 *
 * This is what the required [Consequence.target] field was always for. The act knows what changes
 * and where; the registry knows where everything is; the grammar knows what that verb looks like.
 * Nothing else has to be supplied, and in particular the application supplies no animation — which
 * is the difference between a framework that conveys and a framework that merely permits conveying.
 *
 * A journey with no resolvable endpoint is silently skipped. A verb aimed at something that is not
 * on screen has nothing truthful to say, and inventing a destination would teach a rule that is not
 * real.
 */
internal fun ElementRegistry.carry(stage: Stage, act: Act, reduced: Boolean) {
    val signature = act.signature.let { if (reduced) it.reduced() else it }
    if (!signature.translates) return

    val origin: ElementId = when (val consequence = act.consequence) {
        // Entering is rendered by the places host; a flight from an element to itself would be a
        // motion that says nothing.
        is Consequence.Enter -> return
        is Consequence.Send -> subjectElement(consequence.subject)
        is Consequence.Destroy -> subjectElement(consequence.subject)
        is Consequence.Alter -> subjectElement(consequence.subject)
        else -> act.elementId
    }

    val from: Rect = bounds(origin) ?: bounds(act.elementId) ?: return
    val to: Rect = bounds(act.consequence.target) ?: return
    val token = token(origin) ?: token(act.elementId) ?: return

    stage.launch(signature, from, to, act.weight, token)
}

/**
 * Lean toward the gate and settle back.
 *
 * The direction is real: it is computed from where this act is to where its unmet condition lives,
 * so a person's hand is told which way they are about to be taken before they are taken there. A
 * refusal that recoils in a fixed direction would be decoration; this one is information.
 */
internal suspend fun ElementRegistry.lean(
    resist: Animatable<Offset, *>,
    act: Act,
    gate: ElementId,
) {
    val here = bounds(act.elementId)?.center ?: return
    val there = bounds(gate)?.center ?: return
    val delta = there - here
    val distance = kotlin.math.hypot(delta.x, delta.y)
    if (distance < 1f) return

    val reach = 26f
    val toward = Offset(delta.x / distance * reach, delta.y / distance * reach)
    resist.animateTo(toward, Motion.spec(Weight.Light))
    resist.animateTo(Offset.Zero, Motion.spec(Weight.Medium))
}
