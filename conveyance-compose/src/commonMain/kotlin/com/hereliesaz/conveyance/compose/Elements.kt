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

        /** How to draw this element somewhere else when a verb carries it across the window. */
        var token: (@Composable () -> Unit)? = null

        @OptIn(ExperimentalFoundationApi::class)
        var requester: BringIntoViewRequester? = null
    }

    private fun tenant(id: ElementId): Tenant? = tenancy[id]?.lastOrNull()

    private fun claim(id: ElementId, owner: Any): Tenant {
        val held = tenancy[id].orEmpty()
        held.firstOrNull { it.owner === owner }?.let { return it }
        val fresh = Tenant(owner)
        tenancy[id] = held + fresh
        return fresh
    }

    private fun composed(): Set<ElementId> =
        tenancy.keys.filterTo(mutableSetOf()) { tenant(it)?.placement != null }

    private val gateFlags = mutableStateMapOf<ElementId, Boolean>()

    val gateAddresses: Set<ElementId> get() = gateFlags.keys

    private class OfferClaim(val owner: Any, val act: Act, val at: ElementId)

    private val offered = mutableStateMapOf<ActId, List<OfferClaim>>()

    private val currentOffers: Map<ActId, Pair<Act, ElementId>>
        get() = offered.mapNotNull { (id, claims) -> claims.lastOrNull()?.let { id to (it.act to it.at) } }.toMap()

    var articulating: ElementId? by mutableStateOf(null)
        private set

    operator fun get(id: ElementId): Placement? = tenant(id)?.placement

    fun bounds(id: ElementId): Rect? = tenant(id)?.placement?.bounds

    fun resolves(id: ElementId): Boolean = tenant(id)?.placement != null

    fun visible(id: ElementId): Boolean = tenant(id)?.placement?.visible == true

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
        gateFlags[id] = true
    }

    /**
     * What an element is actually doing, worked out rather than asked for.
     */
    fun jobsOf(id: ElementId): Set<Job> = buildSet {
        if (currentOffers.values.any { it.second == id }) add(Job.Invite)
        if (gateFlags.containsKey(id)) {
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

    fun offering(id: ElementId): Act? = currentOffers.values.firstOrNull { it.second == id }?.first

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

    /** Everything the framework knows about this live surface. */
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
                emphasis = act?.emphasis,
                ambient = tenant(id)?.employment == Employment.Ambient,
            )
        }
        return AuditFrame(surface = surface, census = census(), elements = elements, gateAddresses = gateAddresses)
    }

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

    @OptIn(ExperimentalFoundationApi::class)
    suspend fun escortTo(id: ElementId) {
        tenant(id)?.requester?.bringIntoView()
        articulating = id
    }

    fun settleArticulation() {
        articulating = null
    }

    val placed: Set<ElementId> get() = composed()
}

private val NoRegistry = ElementRegistry()

val LocalElements = staticCompositionLocalOf { NoRegistry }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.element(
    id: ElementId,
    token: (@Composable () -> Unit)? = null,
    employment: Employment? = null,
): Modifier {
    val registry = LocalElements.current
    val requester = remember(id) { BringIntoViewRequester() }
    val claim = remember(id) { Any() }
    DisposableEffect(registry, id, token, employment) {
        registry.attach(id, claim, requester)
        if (token != null) registry.attachToken(id, claim, token)
        if (employment != null) registry.employ(id, claim, employment)
        onDispose { registry.forget(id, claim) }
    }

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
            val lift = settle.value
            scaleX = 1f + lift * 0.12f
            scaleY = 1f + lift * 0.12f
        }
        .onGloballyPositioned { coordinates ->
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

fun subjectElement(subject: SubjectId): ElementId = ElementId("subject:${subject.value}")
