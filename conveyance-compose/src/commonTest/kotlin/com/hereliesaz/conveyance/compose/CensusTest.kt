package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
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

/**
 * How much is on screen, against how much can be done there.
 *
 * The framework does the counting because the framework already has both halves. Nothing in these
 * tests asks an application to describe itself.
 */
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

    /**
     * The point of the whole exercise: an element that backs an act invites, and nobody wrote that
     * down. A declaration would have been a label, and labels go stale the moment an element
     * changes what it does.
     */
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

    /** A gate's address is doing a job, and that is derivable from the act that names it. */
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
        assertTrue(jobs.size >= 2, "A gate's address is doing more than one job by definition.")
    }

    /** An element carrying a travelling token is identifying something particular. */
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

    /**
     * The measurement that matters. Content scales with data and must not count against a surface;
     * chrome does not scale and must.
     */
    @Test
    fun `chrome is counted per act, and content is not counted at all`() = runComposeUiTest {
        val registry = ElementRegistry()
        val send = Act.send("photo.send", subject, to = tray)

        setContent {
            host(registry) {
                Column {
                    Offer(send) { Box(Modifier.size(40.dp)) }
                    // Content: three subjects, each identifying something.
                    repeat(3) { n ->
                        Box(
                            Modifier.size(40.dp).element(
                                subjectElement(SubjectId("photo.$n")),
                                token = { Box(Modifier) },
                            ),
                        )
                    }
                    // Chrome: on screen, accounted for by nothing.
                    repeat(5) { n -> Box(Modifier.size(10.dp).element(ElementId("rule.$n"))) }
                    Box(Modifier.size(40.dp).element(tray))
                }
            }
        }
        waitForIdle()
        val census = registry.census()

        assertEquals(1, census.acts)
        assertEquals(3, census.content, "Subjects are the point of the screen, not clutter.")
        assertTrue(census.chrome >= 5, "Unaccounted elements are chrome: ${census.chrome}")
        assertTrue(census.chromePerAct >= 5f, "One act carrying six pieces of scaffolding.")
        assertTrue(census.chromePerAct > Census.CROWDED, "This surface owes an explanation.")
    }

    /** Two defects, not matters of taste: an act nobody can reach, and a promise behind nothing. */
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

        // The act's own element is composed, so it is reachable; its consequence target is not.
        assertEquals(1, registry.census().acts)
        assertTrue(registry.bounds(ElementId("absent")) == null)
    }
}
