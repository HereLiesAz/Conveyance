package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.hereliesaz.conveyance.ElementId

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

    internal fun forget(id: ElementId) {
        placements.remove(id)
        requesters.remove(id)
        if (articulating == id) articulating = null
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun attach(id: ElementId, requester: BringIntoViewRequester) {
        requesters[id] = requester
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
fun Modifier.element(id: ElementId): Modifier {
    val registry = LocalElements.current
    val requester = remember(id) { BringIntoViewRequester() }
    DisposableEffect(registry, id) {
        registry.attach(id, requester)
        onDispose { registry.forget(id) }
    }
    return bringIntoViewRequester(requester).onGloballyPositioned { coordinates ->
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
