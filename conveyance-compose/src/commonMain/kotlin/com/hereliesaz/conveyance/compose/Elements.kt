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

    /**
     * Everyone currently claiming each address, oldest first.
     *
     * One address, more than one claimant, is not a mistake to be forbidden — it is what a place
     * transition *is*. While a detail place is growing out of a thumbnail, the photograph exists
     * twice: small underneath, large on top. Both are the same subject and must answer to the same
     * name, or a Send from the detail would fly out of a thumbnail hidden behind it.
     *
     * So an address is tenanted rather than owned. The newest claimant answers for it, and when
     * that claimant leaves the answer reverts to whoever was there before instead of vanishing.
     * That single rule is what makes Return land correctly: the way out resolves the origin at the
     * moment of return, and by then the tenancy has already handed the name back to the tray.
     */
    private val tenancy = mutableStateMapOf<ElementId, List<Tenant>>()

    /**
     * One claimant's hold on an address, and everything it knows about itself.
     *
     * Kept together rather than as parallel maps because a claim is a single fact — this element,
     * here, drawable like this — and splitting it across four maps is how the halves get out of
     * step when one of them is handed back and the others are not.
     */
    private class Tenant(val owner: Any) {
        var placement: Placement? by mutableStateOf(null)
        var employment: Employment? by mutableStateOf(null)

        /**
         * How to draw this element somewhere else.
         *
         * A verb that travels has to render the thing that is travelling, and only the element
         * itself knows what it looks like. Holding it here is what lets the framework fly a row to
         * an avatar without the app writing a single line of animation.
         */
        var token: (@Composable () -> Unit)? = null

        @OptIn(ExperimentalFoundationApi::class)
        var requester: BringIntoViewRequester? = null
    }

    /** Whoever currently answers for this address. */
    private fun tenant(id: ElementId): Tenant? = tenancy[id]?.lastOrNull()

    private fun claim(id: ElementId, owner: Any): Tenant {
        val held = tenancy[id].orEmpty()
        held.firstOrNull { it.owner === owner }?.let { return it }
        val fresh = Tenant(owner)
        tenancy[id] = held + fresh
        return fresh
    }

    /** Addresses that have actually been laid out, as opposed to merely spoken for. */
    private fun composed(): Set<ElementId> =
        tenancy.keys.filterTo(mutableSetOf()) { tenant(it)?.placement != null }

    /** Elements that are some gate's address. Where a person is carried when something is missing. */
    private val gateAddresses = mutableStateMapOf<ElementId, Boolean>()

    /** One claimant's hold on an [ActId] -- the same shape [Tenant] gives an [ElementId], and for
     *  the same reason: the offering composable's own identity, so a departing claimant can hand
     *  the id back to whoever else still holds it instead of erasing it outright. */
    private class OfferClaim(val owner: Any, val act: Act, val at: ElementId)

    /**
     * The act each element is offering, kept whole rather than by identity.
     *
     * Holding the act itself is what makes an audit possible at all: its verb, its weight, whether
     * it can be taken back. An id would have been enough to count with and useless to judge by.
     *
     * Tenanted, like [tenancy]: the same [ActId] can legitimately be offered from more than one
     * composable at once -- the same act rendered in both a list row and the detail place growing
     * out of it, mid-transition, the identical reason [ElementId] addresses are tenanted rather
     * than owned. A flat single-claim map made one of those composables leaving the composition
     * silently erase a still-mounted sibling's registration; see [currentOffers] for what a read
     * actually gets.
     */
    private val offered = mutableStateMapOf<ActId, List<OfferClaim>>()

    /** The newest claimant for each currently-offered [ActId] -- what every read in this class
     *  other than [offer]/[withdraw] means by "the" offer, the [tenant] of [offer] cases. */
    private val currentOffers: Map<ActId, Pair<Act, ElementId>>
        get() = offered.mapNotNull { (id, claims) -> claims.lastOrNull()?.let { id to (it.act to it.at) } }.toMap()

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

    operator fun get(id: ElementId): Placement? = tenant(id)?.placement

    /** Unclipped bounds, present whether or not the element is currently on screen. */
    fun bounds(id: ElementId): Rect? = tenant(id)?.placement?.bounds

    /** Whether an address currently resolves to a composed element. */
    fun resolves(id: ElementId): Boolean = tenant(id)?.placement != null

    /** Whether the person can actually see it right now. An escort must check this first. */
    fun visible(id: ElementId): Boolean = tenant(id)?.placement?.visible == true

    /**
     * Where an address is according to whoever held it *before* its current holder.
     *
     * This is the one query a place transition must ask, and asking the ordinary one would be
     * circular: while a detail place is growing out of a photograph, the large photograph inside it
     * answers to the same name, so "where is the photograph" would return the moving place itself
     * and the geometry would chase its own tail. The thing a place grows out of is by definition the
     * claim underneath its own, which is exactly what this returns — falling back to the only
     * claimant when the place has not composed yet, or does not re-use the name at all.
     */
    internal fun anchor(id: ElementId): Rect? {
        val held = tenancy[id].orEmpty()
        val below = held.getOrNull(held.lastIndex - 1) ?: held.lastOrNull()
        return below?.placement?.bounds
    }

    internal fun place(id: ElementId, owner: Any, placement: Placement) {
        claim(id, owner).placement = placement
    }

    internal fun token(id: ElementId): (@Composable () -> Unit)? = tenant(id)?.token

    internal fun employ(id: ElementId, owner: Any, employment: Employment) {
        claim(id, owner).employment = employment
    }

    internal fun offer(act: Act, at: ElementId, owner: Any) {
        val held = offered[act.id].orEmpty().filterNot { it.owner === owner }
        offered[act.id] = held + OfferClaim(owner, act, at)
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
        if (currentOffers.values.any { it.second == id }) add(Job.Invite)
        if (gateAddresses.containsKey(id)) {
            add(Job.Invite)
            add(Job.Locate)
        }
        if (tenant(id)?.token != null) add(Job.Identify)
        when (val declared = tenant(id)?.employment) {
            is Employment.Working -> addAll(declared.jobs)
            else -> Unit
        }
    }

    internal fun withdraw(id: ActId, owner: Any) {
        val remaining = offered[id].orEmpty().filterNot { it.owner === owner }
        if (remaining.isEmpty()) offered.remove(id) else offered[id] = remaining
    }

    /**
     * The act offered at an address.
     *
     * This is the edge of the prerequisite graph, and nobody wrote it down. A gate names the element
     * where it is resolved; that element is offering an act; therefore that act is what resolves the
     * gate. Both halves were already required for other reasons, so the whole flowchart of a
     * product's preconditions is derivable and cannot drift out of step with the acts it describes.
     */
    fun offering(id: ElementId): Act? = currentOffers.values.firstOrNull { it.second == id }?.first

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
        val composed = composed()
        val current = currentOffers
        val offering = current.filterValues { it.second in composed }
        val invitingIds = offering.values.map { it.second }.toSet()

        var content = 0
        var ambient = 0
        composed.forEach { id ->
            if (id in invitingIds) return@forEach
            if (tenant(id)?.employment == Employment.Ambient) {
                ambient++
                return@forEach
            }
            val jobs = jobsOf(id)
            if (jobs.any { it == Job.Identify || it == Job.Report }) content++
        }

        return Census(
            acts = current.size,
            reachable = offering.count { visible(it.value.second) },
            elements = composed.size,
            inviting = invitingIds.size,
            content = content,
            ambient = ambient,
            unreachable = current.filterValues { it.second !in composed }.keys.toList(),
            mute = composed.filter { id ->
                id !in invitingIds && Job.Invite in jobsOf(id)
            },
            contested = composed.filter { id -> (tenancy[id]?.size ?: 0) > 1 },
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
        val byElement = currentOffers.values.associateBy { it.second }
        val elements = composed().map { id ->
            val placement = requireNotNull(tenant(id)?.placement)
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

    /**
     * Give up one claim on an address.
     *
     * The address itself only disappears when the last claimant has gone. A departing tenant that
     * was merely the most recent hands the name back rather than deleting it, which is what stops a
     * place transition from erasing the element it is transitioning out of.
     */
    internal fun forget(id: ElementId, owner: Any) {
        val remaining = tenancy[id].orEmpty().filterNot { it.owner === owner }
        if (remaining.isEmpty()) {
            tenancy.remove(id)
            if (articulating == id) articulating = null
        } else {
            tenancy[id] = remaining
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun attach(id: ElementId, owner: Any, requester: BringIntoViewRequester) {
        claim(id, owner).requester = requester
    }

    internal fun attachToken(id: ElementId, owner: Any, token: @Composable () -> Unit) {
        claim(id, owner).token = token
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
        tenant(id)?.requester?.bringIntoView()
        articulating = id
    }

    /** The person has arrived and acted; the emphasis has done its job and stops. */
    fun settleArticulation() {
        articulating = null
    }

    /** Every address currently composed. Used by the audits, not by product code. */
    val placed: Set<ElementId> get() = composed()
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
    // This call site's identity, which is what the registry tenants an address to. Two composables
    // may legitimately answer to one name at once -- a thumbnail and the place growing out of it --
    // and telling them apart is what lets the second hand the name back when it leaves.
    val claim = remember(id) { Any() }
    DisposableEffect(registry, id, token, employment) {
        registry.attach(id, claim, requester)
        if (token != null) registry.attachToken(id, claim, token)
        if (employment != null) registry.employ(id, claim, employment)
        onDispose { registry.forget(id, claim) }
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
            claim,
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
