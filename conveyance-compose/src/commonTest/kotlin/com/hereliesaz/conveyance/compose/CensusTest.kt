package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActEmphasis
import com.hereliesaz.conveyance.Census
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Gate
import com.hereliesaz.conveyance.Job
import com.hereliesaz.conveyance.Practice
import com.hereliesaz.conveyance.SubjectId
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CensusTest {

    private val tray = ElementId("tray")
    private val field = ElementId("recipient.field")
    private val subject = SubjectId("photo.1")

    @Composable
    private fun host(registry: ElementRegistry, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalElements provides registry,
            LocalPractice provides Practice(),
            LocalGhosts provides Ghosts(),
            LocalStage provides Stage(),
            content = content,
        )
    }

    @Test
    fun `an element that offers an act is known to invite, undeclared`() = runComposeUiTest {
        val registry = ElementRegistry()
        val send = Act.send("photo.send", subject, to = tray)

        setContent {
            host(registry) {
                Column {
                    Offer(send) { Box(Modifier.size(40.dp)) }
                    Box(Modifier.size(40.dp).element(tray))
                }
            }
        }
        waitForIdle()

        assertContains(registry.jobsOf(send.elementId), Job.Invite)
        assertEquals(1, registry.census().acts)
        assertEquals(1, registry.census().inviting)
    }

    @Test
    fun `act emphasis carries through to the live audit element`() = runComposeUiTest {
        val registry = ElementRegistry()
        val heroic = Act.send(
            "photo.send",
            subject,
            to = tray,
            emphasis = ActEmphasis.Heroic,
        )
        val supporting = Act.send("photo.discard", subject, to = tray)

        setContent {
            host(registry) {
                Column {
                    Offer(heroic, element = ElementId("hero.control")) { Box(Modifier.size(40.dp)) }
                    Offer(supporting, element = ElementId("support.control")) { Box(Modifier.size(40.dp)) }
                }
            }
        }
        waitForIdle()

        val elements = registry.auditFrame("gallery").elements.associateBy { it.id }
        assertEquals(ActEmphasis.Heroic, elements.getValue(ElementId("hero.control")).emphasis)
        assertEquals(ActEmphasis.Supporting, elements.getValue(ElementId("support.control")).emphasis)
    }

    @Test
    fun `a gate's address is known to be one, undeclared`() = runComposeUiTest {
        val registry = ElementRegistry()
        val send = Act.send(
            id = "photo.send",
            subject = subject,
            to = tray,
            requires = listOf(Gate("recipient", livesAt = field) { false }),
        )

        setContent {
            host(registry) {
                Column {
                    Offer(send) { Box(Modifier.size(40.dp)) }
                    Box(Modifier.size(40.dp).element(field))
                    Box(Modifier.size(40.dp).element(tray))
                }
            }
        }
        waitForIdle()

        val jobs = registry.jobsOf(field)
        assertContains(jobs, Job.Invite)
        assertContains(jobs, Job.Locate)
        assertTrue(jobs.size >= 2)
    }

    @Test
    fun `an element with a token counts as content, not chrome`() = runComposeUiTest {
        val registry = ElementRegistry()
        setContent {
            host(registry) {
                Box(Modifier.size(40.dp).element(subjectElement(subject), token = { Box(Modifier) }))
            }
        }
        waitForIdle()

        assertContains(registry.jobsOf(subjectElement(subject)), Job.Identify)
        assertEquals(1, registry.census().content)
        assertEquals(0, registry.census().chrome)
    }

    @Test
    fun `chrome is counted per act, and content is not counted at all`() = runComposeUiTest {
        val registry = ElementRegistry()
        val send = Act.send("photo.send", subject, to = tray)

        setContent {
            host(registry) {
                Column {
                    Offer(send) { Box(Modifier.size(40.dp)) }
                    repeat(3) { n ->
                        Box(
                            Modifier.size(40.dp).element(
                                subjectElement(SubjectId("photo.$n")),
                                token = { Box(Modifier) },
                            ),
                        )
                    }
                    repeat(5) { n -> Box(Modifier.size(10.dp).element(ElementId("rule.$n"))) }
                    Box(Modifier.size(40.dp).element(tray))
                }
            }
        }
        waitForIdle()
        val census = registry.census()

        assertEquals(1, census.acts)
        assertEquals(3, census.content)
        assertTrue(census.chrome >= 5)
        assertTrue(census.chromePerAct >= 5f)
        assertTrue(census.chromePerAct > Census.CROWDED)
    }

    @Test
    fun `an act whose element is not composed reports as unreachable`() = runComposeUiTest {
        val registry = ElementRegistry()
        val ghostAct = Act.send("nowhere", subject, to = ElementId("absent"))

        setContent {
            host(registry) {
                Offer(ghostAct) { Box(Modifier.size(40.dp)) }
            }
        }
        waitForIdle()

        assertEquals(1, registry.census().acts)
        assertTrue(registry.bounds(ElementId("absent")) == null)
    }

    @Test
    fun `two unrelated elements sharing one address are reported as contested`() = runComposeUiTest {
        val registry = ElementRegistry()
        val collided = ElementId("card")

        setContent {
            host(registry) {
                Column {
                    Box(Modifier.size(40.dp).element(collided))
                    Box(Modifier.size(40.dp).element(collided))
                }
            }
        }
        waitForIdle()

        val census = registry.census()
        assertTrue(census.hasContestedAddresses)
        assertContains(census.contested, collided)
        assertEquals(1, census.elements)
    }
}
