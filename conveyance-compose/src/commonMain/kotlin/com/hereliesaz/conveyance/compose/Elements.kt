package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActId
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.AuditElement
import com.hereliesaz.conveyance.AuditFrame
import com.hereliesaz.conveyance.Census
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Employment
import com.hereliesaz.conveyance.Job
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.Weight

/**
 * Where a named element is, and whether the person can currently see it.
 *
 * [bounds] is deliberately **unclipped**, and reflects any scaling applied to the element or its
 * ancestors: both corners are mapped through the transform, so the rect is always coherent.
 *
 * Unclipped: it is where the element actually is, even when it has
 * scrolled out of view. Clipped bounds collapse to zero the moment an element leaves the viewport,
 * and an escort aimed at a zero rect travels to the window's top-left corner instead of to the field
 * the person forgot to fill in. Since a gate is very often below the fold, that is the common case
 * rather than the edge case.
 *
 * [visible] carries the other half of the answer. An escort to something off-screen has to bring it
 * into view first and articulate second; without this flag it would articulate something nobody is
 * looking at.
 */
@Immutable
data class Placement(
    val bounds: Rect,
    val visible: Boolean,
)

/**
 * Where every named element currently is.
 *
 * This registry is what turns the core's addresses into geometry, and it is the load-bearing piece
 * of the whole binding. A consequence names the element that changes; a gate names the element where
 * it is resolved; a place names the element it grows out of. None of that means anything on screen
 * until something can answer "and where is that, right now".
 *
 * Placements are held in observable state, so a motion that begins while the world is still settling
 * picks up the corrected position rather than animating toward where a thing used to be.
 */
@Stable
class ElementRegistry {

    private val placements = mutableStateMapOf<ElementId, Placement>()

    @OptIn(ExperimentalFoundationApi::class)
    private val requesters = mutableMapOf<ElementId, BringIntoViewRequester>()

    /**
     * How to draw each element somewhere else.
     *
     * A verb that travels has to render the thing that is travelling, and only the element itself
     * knows what it looks like. Registering it here is what lets the framework fly a row to an
     * avatar without the app writing a single line of animation.
     */
    private val tokens = mutableMapOf<ElementId, @Composable () -> Unit>()

    /**
     * What an element claims to be for, in the rare case it has to say.
     *
     * Declaration is the exception here, not the rule. A declared job is a label, and labels
     * ossify: the element gains or loses behaviour, the label does not move, and the count quietly
     * starts measuring the declaration instead of the thing. Worse, a two-job minimum invites
     * everyone to write exactly two and stop thinking, which is how a parameter becomes a formality.
     *
     * So [jobsOf] derives what it can from what the framework already knows, and this map only
     * carries what it cannot see.
     */
    private val employments = mutableStateMapOf<ElementId, Employment>()

    /** Elements that are some gate's address. Where a person is carried when something is missing. */
    private val gateAddresses = mutableStateMapOf<ElementId, Boolean>()

    /**
     * The act each element is offering, kept whole rather than by identity.
     *
     * Holding the act itself is what makes an audit possible at all: its verb, its weight, whether
     * it can be taken back. An id would have been enough to count with and useless to judge by.
     */
    private val offered = mutableStateMapOf<ActId, Pair<Act, ElementId>>()

    /**
     * The element an escort has just delivered someone to, which that element renders as
     * articulation until it is dismissed.
     *
     * Held centrally rather than per element because only one thing can be the answer to "here is
     * what you were missing" at a time. Two simultaneous articulations would be two signs competing
     * for attention, which is the construction zone again.
     */
    var articulating: ElementId? by mutableStateOf(null)
        private set

    operator fun get(id: ElementId): Placement? = placements[id]

    /** Unclipped bounds, present whether or not the element is currently on screen. */
    fun bounds(id: ElementId): Rect? = placements[id]?.bounds

    /** Whether an address currently resolves to a composed element. */
    fun resolves(id: ElementId): Boolean = placements.containsKey(id)

    /** Whether the person can actually see it right now. An escort must check this first. */
    fun visible(id: ElementId): Boolean = placements[id]?.visible == true

    internal fun place(id: ElementId, placement: Placement) {
        placements[id] = placement
    }

    internal fun token(id: ElementId): (@Composable () -> Unit)? = tokens[id]

    internal fun employ(id: ElementId, employment: Employment) {
        employments[id] = employment
    }

    internal fun offer(act: Act, at: ElementId) {
        offered[act.id] = act to at
    }

    internal fun markGate(id: ElementId) {
        gateAddresses[id] = true
    }

    /**
     * What an element is actually doing, worked out rather than asked for.
     *
     * Everything here is already known: an element backing an act invites; a gate's address is
     * where an unmet condition gets resolved; an element with a travelling token is one a verb can
     * carry. None of that should be typed by anyone, and an element is free to pick up and drop
     * jobs as its surroundings change without a stale label contradicting it.
     *
     * The union with a declaration is for what the framework genuinely cannot see -- a value it did
     * not compute, a grouping it did not impose.
     */
    fun jobsOf(id: ElementId): Set<Job> = buildSet {
        if (offered.values.any { it.second == id }) add(Job.Invite)
        if (gateAddresses.containsKey(id)) {
            add(Job.Invite)
            add(Job.Locate)
        }
        if (tokens.containsKey(id)) add(Job.Identify)
        when (val declared = employments[id]) {
            is Employment.Working -> addAll(declared.jobs)
            else -> Unit
        }
    }

    internal fun withdraw(act: ActId) {
        offered.remove(act)
    }

    /**
     * Count what is on screen against what can be done with it.
     *
     * Live, and free: the registry already knew every addressed element, and Offer already knew
     * every act. Nothing here is new information -- the two halves simply had never been asked to
     * compare notes.
     *
     * An element that has not declared what it is for counts as chrome. That is deliberate rather
     * than punitive: undeclared is exactly the state of an element nobody has had to justify, and
     * this measurement exists to find those.
     */
    fun census(): Census {
        val composed = placements.keys.toSet()
        val offering = offered.filterValues { it.second in composed }
        val invitingIds = offering.values.map { it.second }.toSet()

        var content = 0
        var ambient = 0
        composed.forEach { id ->
            if (id in invitingIds) return@forEach
            if (employments[id] == Employment.Ambient) {
                ambient++
                return@forEach
            }
            val jobs = jobsOf(id)
            if (jobs.any { it == Job.Identify || it == Job.Report }) content++
        }

        return Census(
            acts = offered.size,
            reachable = offering.count { placements[it.value.second]?.visible == true },
            elements = composed.size,
            inviting = invitingIds.size,
            content = content,
            ambient = ambient,
            unreachable = offered.filterValues { it.second !in composed }.keys.toList(),
            mute = composed.filter { id ->
                id !in invitingIds && Job.Invite in jobsOf(id)
            },
        )
    }

    /**
     * Everything the framework knows about this surface, for something that will be shown only the
     * pixels.
     *
     * This is the half a screenshot cannot contain, and the reason grading a naive viewer is
     * possible at all: the framework holds the answers, so the gap between what a first-time
     * observer predicts and what is actually true can be measured rather than guessed at.
     */
    fun auditFrame(surface: String): AuditFrame {
        val byElement = offered.values.associateBy { it.second }
        val elements = placements.map { (id, placement) ->
            val act = byElement[id]?.first
            AuditElement(
                id = id,
                left = placement.bounds.left,
                top = placement.bounds.top,
                width = placement.bounds.width,
                height = placement.bounds.height,
                visible = placement.visible,
                act = act?.id,
                verb = act?.verb,
                consequence = act?.consequence?.let { "${act.verb} -> ${it.target}" },
                weight = act?.weight,
                reversible = act?.reversible == true,
                blocked = act?.state() is ActState.Blocked,
                jobs = jobsOf(id),
            )
        }
        return AuditFrame(surface = surface, census = census(), elements = elements)
    }

    internal fun forget(id: ElementId) {
        placements.remove(id)
        requesters.remove(id)
        tokens.remove(id)
        employments.remove(id)
        if (articulating == id) articulating = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun attach(id: ElementId, requester: BringIntoViewRequester) {
        requesters[id] = requester
    }

    internal fun attachToken(id: ElementId, token: @Composable () -> Unit) {
        tokens[id] = token
    }

    /**
     * Carry the person to [id]: bring it into view if it is not, then articulate it.
     *
     * This is the Escort, and it is the whole of the framework's answer to the disabled state. A
     * greyed-out control announces a rule and abandons you; this arrives at the thing you were
     * missing. The order matters — articulating something off-screen would be emphasising a thing
     * nobody is looking at.
     */
    @OptIn(ExperimentalFoundationApi::class)
    suspend fun escortTo(id: ElementId) {
        requesters[id]?.bringIntoView()
        articulating = id
    }

    /** The person has arrived and acted; the emphasis has done its job and stops. */
    fun settleArticulation() {
        articulating = null
    }

    /** Every address currently composed. Used by the audits, not by product code. */
    val placed: Set<ElementId> get() = placements.keys.toSet()
}

private val NoRegistry = ElementRegistry()

val LocalElements = staticCompositionLocalOf { NoRegistry }

/**
 * Give this element its address.
 *
 * Registration follows the element through recomposition and scrolling because it is driven by
 * layout rather than by composition: `onGloballyPositioned` fires whenever the element actually
 * moves, which is exactly when a motion aimed at it would otherwise be aiming at the wrong place.
 * Deregistration is tied to leaving the composition, so an address never outlives the thing it names
 * and an escort can never travel to a control that has since been removed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.element(
    id: ElementId,
    /** How to draw this element elsewhere, when a verb carries it across the window. */
    token: (@Composable () -> Unit)? = null,
    /**
     * What this element is for, *only* where the framework cannot work it out.
     *
     * Most elements should leave this null: backing an act, being a gate's address and carrying a
     * travelling token are all derived. Declare only what is genuinely invisible from the model.
     */
    employment: Employment? = null,
): Modifier {
    val registry = LocalElements.current
    val requester = remember(id) { BringIntoViewRequester() }
    DisposableEffect(registry, id, token, employment) {
        registry.attach(id, requester)
        if (token != null) registry.attachToken(id, token)
        if (employment != null) registry.employ(id, employment)
        onDispose { registry.forget(id) }
    }

    // Articulation, rendered by the framework rather than left to the app.
    //
    // An escort that only sets a flag has not escorted anyone -- it has told the application to
    // draw something, which is the application conveying, not the framework. So the arrival is
    // physical and it is geometry only: the element settles under the person's attention the way a
    // thing does when it is put down in front of you. No colour is involved, because every channel
    // already carries an assigned meaning and "look here" is not one of them.
    val arriving = registry.articulating == id
    val settle = remember(id) { Animatable(0f) }
    LaunchedEffect(arriving) {
        if (arriving) {
            settle.animateTo(1f, Motion.spec(Weight.Light))
            settle.animateTo(0f, Motion.spec(Weight.Medium))
        }
    }

    return bringIntoViewRequester(requester)
        .graphicsLayer {
            // Perceptible on purpose. An emphasis nobody can see is an emphasis that did not
            // happen, and the first version of this was a four-percent scale -- technically a
            // settle, practically nothing.
            val lift = settle.value
            scaleX = 1f + lift * 0.12f
            scaleY = 1f + lift * 0.12f
        }
        .onGloballyPositioned { coordinates ->
        // Both corners are mapped through the ancestor transforms, never just the origin. Mapping
        // only the origin and pairing it with the raw layout size yields an incoherent rect the
        // moment anything is scaled -- a transformed position wearing an untransformed size -- and
        // a morph aimed at it would arrive in the right place at the wrong size.
        val size = coordinates.size
        registry.place(
            id,
            Placement(
                bounds = Rect(
                    coordinates.localToRoot(Offset.Zero),
                    coordinates.localToRoot(Offset(size.width.toFloat(), size.height.toFloat())),
                ),
                visible = !coordinates.boundsInRoot().isEmpty,
            ),
        )
    }
}

/**
 * Every subject has an address, derived rather than declared.
 *
 * This is what lets a Send find the card it is sending and a Destroy find the row it is destroying,
 * without the app wiring anything up. A collection registers its items here automatically.
 */
fun subjectElement(subject: SubjectId): ElementId = ElementId("subject:${subject.value}")
