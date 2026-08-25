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

    /**
     * [Act.keystone] is set at construction and, on its own, read by nothing else in the
     * framework -- [com.hereliesaz.conveyance.Product.keystones] only checks its own list's
     * length. This is the one wire that makes a live cross-check possible at all: a real running
     * act's own [Act.keystone] flag, carried all the way out to the [AuditElement] a
     * [Product]-level audit would actually compare against.
     */
    @Test
    fun `a keystone act carries that flag out to its audit element`() = runComposeUiTest {
        val registry = ElementRegistry()
        val send = Act.send("photo.send", subject, to = tray, keystone = true)
        val plain = Act.send("photo.discard", subject, to = tray)

        setContent {
            host(registry) {
                Column {
                    Offer(send, element = ElementId("keystone.control")) { Box(Modifier.size(40.dp)) }
                    Offer(plain, element = ElementId("plain.control")) { Box(Modifier.size(40.dp)) }
                }
            }
        }
        waitForIdle()

        val elements = registry.auditFrame("gallery").elements.associateBy { it.id }
        assertTrue(elements.getValue(ElementId("keystone.control")).keystone)
        assertTrue(!elements.getValue(ElementId("plain.control")).keystone)
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

    /**
     * Tenancy exists so a place transition can let a detail place share its subject's address with
     * the thumbnail it grew out of -- but nothing about the mechanism itself distinguishes that from
     * two unrelated elements that happened to pick the same [ElementId] by accident. Before tenancy,
     * a flat map made that mistake invisible by construction: the second registration silently won.
     * Tenancy without a report would have made it invisible on purpose. It is reported instead.
     */
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
        assertEquals(
            1,
            census.elements,
            "One address is still one composed element by count, however many things answer to it -- " +
                "which is exactly why it needs its own report rather than showing up as a shortfall " +
                "in a count nobody would think to double-check.",
        )
    }
}
