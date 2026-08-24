package com.hereliesaz.conveyance.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import com.hereliesaz.conveyance.Place
import com.hereliesaz.conveyance.Weight

/**
 * Where a person is, and how they got there.
 *
 * Enter and Return are the two verbs the framework declared and never rendered, which meant the law
 * they serve had no code behind it. They are also the two that matter most: continuity between
 * screens is the whole of Law 2, and a person who has been teleported has lost the map they were
 * building and needs words to rebuild it.
 *
 * So a place is not swapped in. It **grows out of the element that was touched**, and on the way
 * back it **shrinks into that element where it now sits** — not where it used to be, because the
 * screen underneath may have moved while the person was away. That is why the origin is resolved
 * from the registry at the moment of return rather than captured on the way out.
 */
@Stable
class PlacesState internal constructor(root: Place) {

    internal val stack = mutableStateListOf(root)

    /** 0 = collapsed into the origin element, 1 = filling the window. */
    internal val extent = Animatable(1f)

    /** The place on its way out, still drawn until it has finished shrinking. */
    internal var receding by mutableStateOf<Place?>(null)
        private set

    val current: Place get() = receding ?: stack.last()

    val depth: Int get() = stack.size

    /** The place underneath, which stays put while the one above it moves. */
    internal val beneath: Place?
        get() = if (receding != null) stack.lastOrNull() else stack.getOrNull(stack.lastIndex - 1)

    internal suspend fun enter(place: Place, weight: Weight) {
        stack += place
        extent.snapTo(0f)
        extent.animateTo(1f, Motion.spec(weight))
    }

    /**
     * Go back, and report whether there was anywhere to go.
     *
     * Returning false rather than throwing lets a host decide what a back gesture means at the
     * root, which is a question about the product rather than about this framework.
     */
    suspend fun back(weight: Weight = Weight.Heavy): Boolean {
        if (stack.size <= 1) return false
        receding = stack.removeAt(stack.lastIndex)
        extent.snapTo(1f)
        extent.animateTo(0f, Motion.spec(weight))
        receding = null
        extent.snapTo(1f)
        return true
    }
}

val LocalPlaces = staticCompositionLocalOf<PlacesState?> { null }

/**
 * Render wherever the person currently is.
 *
 * The place beneath is drawn first and stays still. The place in motion is drawn on top, laid out
 * between its origin element's bounds and the whole window, so entering and returning are the same
 * geometry run in opposite directions — which is exactly what the grammar says they are.
 */
@Composable
fun Places(
    root: Place,
    modifier: Modifier = Modifier,
    content: @Composable (Place) -> Unit,
) {
    val registry = LocalElements.current
    val places = remember(root.id) { PlacesState(root) }
    var window by remember { mutableStateOf(Rect.Zero) }

    CompositionLocalProvider(LocalPlaces provides places) {
        Box(
            modifier
                .fillMaxSize()
                .onGloballyPositioned { window = it.boundsInRoot() },
        ) {
            places.beneath?.let { content(it) }

            val moving = places.current
            // The claim *underneath* the moving place, never the moving place's own. A detail
            // place that shows the same subject legitimately answers to the same address, and
            // asking the ordinary question would have the geometry chasing itself.
            val origin = moving.origin?.let { registry.anchor(it) }
            val extent = places.extent.value

            if (origin == null || extent >= 1f || window.isEmpty) {
                // Nothing to grow from, or already arrived. A root place is always simply here.
                Box(Modifier.fillMaxSize()) { content(moving) }
            } else {
                Box(
                    Modifier.layout { measurable, _ ->
                        val left = lerp(origin.left, window.left, extent)
                        val top = lerp(origin.top, window.top, extent)
                        val width = lerp(origin.width, window.width, extent)
                        val height = lerp(origin.height, window.height, extent)
                        val placeable = measurable.measure(
                            Constraints.fixed(
                                width.toInt().coerceAtLeast(1),
                                height.toInt().coerceAtLeast(1),
                            ),
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(left.toInt(), top.toInt())
                        }
                    },
                ) {
                    content(moving)
                }
            }
        }
    }
}

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
