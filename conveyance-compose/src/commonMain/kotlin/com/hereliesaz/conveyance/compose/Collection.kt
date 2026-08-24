package com.hereliesaz.conveyance.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ElementId
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
 * The collection does four things the application no longer has to, and each one is a behaviour the
 * framework was previously only describing:
 *
 * 1. **It gives every subject an address**, so a Send can find the card it is sending and a Destroy
 *    can find the row it is destroying, with nothing wired up by hand.
 * 2. **It registers each subject's appearance**, so a verb that carries something across the window
 *    can draw the actual thing rather than an abstract marker.
 * 3. **It registers its own address**, taken from the creator's consequence, so a Create has
 *    somewhere to fly to.
 * 4. **It draws the Ghost itself.** There is no slot for the application to fill, because a residue
 *    the application draws is a residue the framework did not provide.
 *
 * The Migration and the Ghost both live here because both are facts about collections rather than
 * about controls: one is where new things come from, the other is that a destroyed thing's place is
 * kept for a while.
 */
@Composable
fun <T> Collection(
    items: List<T>,
    creator: Act,
    key: (T) -> SubjectId,
    modifier: Modifier = Modifier,
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

    // The collection answers to the address the creator's consequence names, so Create has a
    // destination without anyone declaring one twice.
    Box(modifier.element(creator.consequence.target)) {
        Column {
            slots.forEach { slot ->
                when (slot) {
                    is Slot.Present -> {
                        val subject = key(slot.item)
                        Box(
                            Modifier.element(
                                id = subjectElement(subject),
                                token = { item(slot.item) },
                                // No employment declared: registering a token is already the
                                // evidence that this element identifies a particular subject.
                            ),
                        ) {
                            item(slot.item)
                        }
                    }

                    is Slot.Gone -> ghosts[slot.subject]?.let { residue ->
                        GhostSlot(residue) { ghosts.recover(residue.subject) }
                    }
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
 * The residue, drawn by the framework, in the slot its subject held.
 *
 * It is the subject itself, compressed — not an icon, not a bar, not a word. That is the whole
 * argument for the Ghost over an undo snackbar: the person is looking at the gap the thing left, so
 * the thing is what should be sitting in it, flattened and waiting. Pulling it back restores it.
 *
 * Geometry only. Nothing here spends a colour, because every channel already carries an assigned
 * meaning and "recently deleted" is not one of them.
 */
@Composable
private fun GhostSlot(residue: Residue, onRecover: () -> Unit) {
    val open by animateFloatAsState(
        targetValue = 0.34f,
        animationSpec = Motion.spec(residue.weight),
        label = "ghost",
    )
    Box(
        modifier = Modifier
            .element(ghostElement(residue.subject))
            .clickable { onRecover() }
            .graphicsLayer {
                scaleY = open
                // Held down rather than lifted: it is not gone, it is pressed flat.
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
            .clipToBounds(),
    ) {
        LocalElements.current.token(subjectElement(residue.subject))?.invoke()
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

/**
 * A residue's address.
 *
 * Derived like a subject's, so a recovery can be aimed at the gap the subject left rather than at
 * wherever a notification happened to appear.
 */
fun ghostElement(subject: SubjectId): ElementId = ElementId("ghost:${subject.value}")
