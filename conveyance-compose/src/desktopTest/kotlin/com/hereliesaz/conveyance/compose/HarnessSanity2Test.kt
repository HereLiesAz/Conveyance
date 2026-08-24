package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import java.lang.ref.WeakReference
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HarnessSanity2Test {
    private class Thing(val id: String)

    @Test
    fun `does a mutableStateOf payload list get released after reassignment`() = runComposeUiTest {
        var gone: Thing? = Thing("gone")
        val weak = WeakReference(gone)
        val state = mutableStateOf(listOf(Thing("keep"), gone!!))

        setContent {
            Box(Modifier) { state.value.forEach { it.id.length } }
        }
        waitForIdle()

        state.value = state.value.filter { it.id != "gone" }
        gone = null
        waitForIdle()

        repeat(10) { System.gc(); Thread.sleep(30) }
        println("DEBUG plain-state weak.get() = " + weak.get())
    }
}
