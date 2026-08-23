package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Gate
import com.hereliesaz.conveyance.Practice
import com.hereliesaz.conveyance.SubjectId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OfferTest {

    private val invoice = SubjectId("invoice.41")
    private val avatar = ElementId("recipient.avatar")
    private val field = ElementId("recipient.field")

    /**
     * The framework's entire replacement for the disabled state, end to end.
     *
     * A greyed-out button announces a rule and abandons the person. This asserts the opposite
     * happens: the gate is off-screen, engaging the blocked act brings it into view, and it is the
     * thing being emphasised when they arrive.
     */
    @Test
    fun `a blocked act escorts the person to a gate below the fold`() = runComposeUiTest {
        val registry = ElementRegistry()
        val practice = Practice()
        val gate = Gate("recipient.chosen", livesAt = field) { false }
        val send = Act.send("invoice.send", invoice, avatar, requires = listOf(gate))
        var scope: ActScope? = null

        setContent {
            CompositionLocalProvider(
                LocalElements provides registry,
                LocalPractice provides practice,
            ) {
                Column(Modifier.height(120.dp).verticalScroll(rememberScrollState())) {
                    Offer(send) {
                        scope = this
                        Box(Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(400.dp))
                    Box(Modifier.size(40.dp).element(field))
                }
            }
        }
        waitForIdle()

        assertIs<ActState.Blocked>(scope!!.state, "An unmet gate must read as Blocked, not disabled.")
        assertFalse(registry.visible(field), "Precondition: the gate starts below the fold.")

        runOnUiThread { scope!!.engage() }
        waitForIdle()

        assertTrue(registry.visible(field), "The escort must bring the gate into view.")
        assertEquals(field, registry.articulating, "The gate must be what is emphasised on arrival.")
    }

    @Test
    fun `a blocked act is never recorded as practice`() = runComposeUiTest {
        val registry = ElementRegistry()
        val practice = Practice()
        val gate = Gate("never", livesAt = field) { false }
        val send = Act.send("invoice.send", invoice, avatar, requires = listOf(gate))
        var scope: ActScope? = null

        setContent {
            CompositionLocalProvider(LocalElements provides registry, LocalPractice provides practice) {
                Column {
                    Offer(send) { scope = this; Box(Modifier.size(40.dp)) }
                    Box(Modifier.size(40.dp).element(field))
                }
            }
        }
        waitForIdle()
        runOnUiThread { scope!!.engage() }
        waitForIdle()

        assertEquals(0, practice.count(send.id), "Reaching for a thing is not doing it.")
        assertTrue(scope!!.owesTell, "An act never performed still owes its Tell.")
    }

    /** A gate satisfied elsewhere unblocks this control with nothing for the person to dismiss. */
    @Test
    fun `satisfying a gate elsewhere quietly unblocks the act`() = runComposeUiTest {
        val registry = ElementRegistry()
        val chosen = mutableStateOf(false)
        val gate = Gate("recipient.chosen", livesAt = field) { chosen.value }
        val send = Act.send("invoice.send", invoice, avatar, requires = listOf(gate))
        var scope: ActScope? = null

        setContent {
            CompositionLocalProvider(LocalElements provides registry, LocalPractice provides Practice()) {
                Offer(send) { scope = this; Box(Modifier.size(40.dp)) }
            }
        }
        waitForIdle()
        assertIs<ActState.Blocked>(scope!!.state)

        chosen.value = true
        waitForIdle()

        assertEquals(ActState.Ready, scope!!.state, "The world changed; the control should just be live.")
    }

    @Test
    fun `an engaged act settles and earns its practice`() = runComposeUiTest {
        val registry = ElementRegistry()
        val practice = Practice()
        val send = Act.send("invoice.send", invoice, avatar)
        var scope: ActScope? = null

        setContent {
            CompositionLocalProvider(LocalElements provides registry, LocalPractice provides practice) {
                Offer(send) { scope = this; Box(Modifier.size(40.dp)) }
            }
        }
        waitForIdle()
        assertTrue(scope!!.owesTell, "Before first use, the control owes its half-rep.")

        runOnUiThread { scope!!.engage() }
        waitForIdle()

        assertEquals(ActState.Settled, scope!!.state)
        assertEquals(1, practice.count(send.id))
        assertFalse(scope!!.owesTell, "Once done, the Tell is spent and must not repeat.")
    }
}
