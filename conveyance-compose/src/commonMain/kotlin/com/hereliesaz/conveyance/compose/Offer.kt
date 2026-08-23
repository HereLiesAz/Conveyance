package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Signature
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
    content: @Composable ActScope.() -> Unit,
) {
    val registry = LocalElements.current
    val practice = LocalPractice.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember(act.id) { mutableStateOf<ActState>(ActState.Ready) }

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
                    is ActState.Blocked -> registry.escortTo(terminal.gate.livesAt)
                    // Practice is earned by doing the thing, not by reaching for it.
                    else -> practice.record(act.id)
                }
            }
        },
    )

    Box(modifier.element(act.elementId)) {
        scope.content()
    }
}
