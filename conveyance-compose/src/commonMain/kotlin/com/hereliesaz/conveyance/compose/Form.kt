package com.hereliesaz.conveyance.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Form

/**
 * A [Form]'s live fill state -- which [com.hereliesaz.conveyance.FormField]s currently hold a
 * value, from any source: a person's own typing, a platform autofill, or a draft this same state
 * is restoring right now. [Form] itself can't hold this: it's the same fixed value for every
 * person who ever fills it out, while this is one person's progress through it.
 *
 * Wraps the [MutableState] [rememberFormState] gets from `rememberSaveable` rather than copying
 * its value into a state of its own -- [mark] has to write through to the exact instance that
 * gets saved, or "form recovery" would restore whatever the form looked like when this state was
 * first remembered and silently ignore everything filled in since.
 */
class FormState internal constructor(private val form: Form, private val filledState: MutableState<Set<ElementId>>) {
    val filled: Set<ElementId> get() = filledState.value

    val percentComplete: Float get() = form.percentComplete(filled)
    val isComplete: Boolean get() = form.isComplete(filled)

    fun mark(field: ElementId, hasValue: Boolean = true) {
        filledState.value = if (hasValue) filledState.value + field else filledState.value - field
    }

    fun clear() {
        filledState.value = emptySet()
    }
}

/**
 * Remembers a [FormState] for [form], recovered automatically across whatever this platform
 * already recovers a plain [rememberSaveable] value across -- a configuration change, a process
 * death, a closed and reopened tab. That is the whole of what "form recovery" needs to mean:
 * the filled set is the only thing that was ever mutable, so restoring it restores everything a
 * person would recognise as their progress. No separate draft-persistence mechanism is layered on
 * top, because one already exists and reinventing it here would just be a second place for the
 * two copies to disagree.
 */
@Composable
fun rememberFormState(form: Form): FormState {
    val filledState = rememberSaveable(form.id.value, saver = filledFieldsSaver) {
        mutableStateOf(emptySet())
    }
    return remember(form, filledState) { FormState(form, filledState) }
}

private val filledFieldsSaver: Saver<MutableState<Set<ElementId>>, List<String>> =
    Saver(
        save = { it.value.map(ElementId::value) },
        restore = { saved -> mutableStateOf(saved.map(::ElementId).toSet()) },
    )
