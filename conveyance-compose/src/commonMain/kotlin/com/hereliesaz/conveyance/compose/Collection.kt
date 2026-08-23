package com.hereliesaz.conveyance.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.Weight

/** A place in a collection: something that is there, or the residue of something that was. */
private sealed interface Slot<out T> {
    data class Present<T>(val item: T) : Slot<T>
    data class Gone(val subject: SubjectId) : Slot<Nothing>
}

/**
 * A collection of subjects, and the control that creates them.
 *
 * Two of the framework's named behaviours live here because both are really facts about collections
 * rather than about controls.
 *
 * **The Migration.** An empty collection does not display a message about being empty. It displays
 * its creation control, full size, in the centre of the space the collection will occupy. When the
 * first subject arrives, that control travels to the corner where it will live from now on and
 * shrinks into it. In one motion, with no words, a person learns what the space is for, how to fill
 * it, and where the button will be for the rest of their life with the product. One element, four
 * jobs, zero instructions.
 *
 * **The Ghost.** A destroyed subject's slot is held open for the recovery window, so the residue sits
 * exactly where the subject was rather than in a bar at the bottom of the screen. That is what the
 * collection contributes: position. Without it, "undo in its own place" is a slogan.
 */
@Composable
fun <T> Collection(
    items: List<T>,
    creator: Act,
    key: (T) -> SubjectId,
    modifier: Modifier = Modifier,
    ghost: @Composable (com.hereliesaz.conveyance.compose.Residue) -> Unit = {},
    creatorContent: @Composable ActScope.() -> Unit,
    item: @Composable (T) -> Unit,
) {
    val ghosts = LocalGhosts.current
    val order = remember { mutableStateListOf<SubjectId>() }
    val slots = resolveSlots(items, key, ghosts, order)

    // Bias runs from the centre (0) to the corner (near 1). The creation control does not jump
    // between two positions; it travels, because the travel is the part that teaches where it went.
    val settled by animateFloatAsState(
        targetValue = if (slots.isEmpty()) 0f else 1f,
        animationSpec = Motion.spec(Weight.Medium),
        label = "migration",
    )

    Box(modifier) {
        Column {
            slots.forEach { slot ->
                when (slot) {
                    is Slot.Present -> item(slot.item)
                    is Slot.Gone -> ghosts[slot.subject]?.let { ghost(it) }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias = settled * 0.92f, verticalBias = settled * 0.92f))
                .graphicsLayer {
                    val scale = 1f - settled * 0.45f
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Offer(creator, content = creatorContent)
        }
    }
}

/**
 * Merge the live items with the residues of the dead, preserving position.
 *
 * The remembered order is what makes a Ghost appear where its subject was rather than appended at
 * the end. A residue that has been recovered or released simply stops resolving and its slot closes.
 */
@Composable
private fun <T> resolveSlots(
    items: List<T>,
    key: (T) -> SubjectId,
    ghosts: Ghosts,
    order: SnapshotStateList<SubjectId>,
): List<Slot<T>> {
    val byKey = items.associateBy(key)
    val slots = mutableListOf<Slot<T>>()
    val seen = mutableSetOf<SubjectId>()

    order.forEach { subject ->
        val live = byKey[subject]
        when {
            live != null -> {
                slots += Slot.Present(live)
                seen += subject
            }
            // Gone from the list but still recoverable: hold the slot open where it was.
            ghosts.holds(subject) -> {
                slots += Slot.Gone(subject)
                seen += subject
            }
            // Gone and released. The slot closes and the collection forgets it ever existed.
            else -> Unit
        }
    }
    items.forEach { candidate ->
        val subject = key(candidate)
        if (subject !in seen) {
            slots += Slot.Present(candidate)
            seen += subject
        }
    }

    val resolved = slots.map {
        when (it) {
            is Slot.Present -> key(it.item)
            is Slot.Gone -> it.subject
        }
    }
    if (resolved != order.toList()) {
        order.clear()
        order.addAll(resolved)
    }
    return slots
}
