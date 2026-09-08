package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendationTest {

    private fun element(
        id: String,
        jobs: Set<Job>,
        left: Float,
        act: ActId? = null,
        verb: Verb? = null,
    ) = AuditElement(
        id = ElementId(id),
        left = left,
        top = 0f,
        width = 20f,
        height = 20f,
        visible = true,
        act = act,
        verb = verb,
        jobs = jobs,
    )

    @Test
    fun `observed facts derive behavioral roles without component names`() {
        val source = element(
            id = "anything",
            jobs = setOf(Job.Invite, Job.Interrupt),
            left = 0f,
            act = ActId("save"),
        )

        val roles = source.behavioralRoles()

        assertTrue(BehavioralRole.ActionSource in roles)
        assertTrue(BehavioralRole.Interruptible in roles)
    }

    @Test
    fun `gate address derives gate resolver role`() {
        val id = ElementId("recipient")
        val resolver = element(
            id = id.value,
            jobs = setOf(Job.Locate),
            left = 0f,
        )

        val roles = resolver.behavioralRoles(setOf(id))

        assertTrue(BehavioralRole.GateResolver in roles)
        assertTrue(BehavioralRole.Locator in roles)
    }

    @Test
    fun `fragmented action lifecycle maps through roles to Offer`() {
        val source = element(
            id = "save.source",
            jobs = setOf(Job.Invite, Job.Interrupt),
            left = 0f,
            act = ActId("save"),
        )
        val progress = element(
            id = "save.progress",
            jobs = setOf(Job.Progress),
            left = 24f,
        )
        val completion = element(
            id = "save.complete",
            jobs = setOf(Job.Confirm),
            left = 48f,
        )
        val frame = AuditFrame(
            surface = "editor",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(source, progress, completion),
        )

        val suggestion = ConsolidationAdvisor.suggest(frame).single()

        assertEquals(ConveyanceRecipes.Offer, suggestion.replacement)
        assertTrue(BehavioralRole.ActionSource in suggestion.combinedRoles)
        assertTrue(BehavioralRole.ProgressReporter in suggestion.combinedRoles)
        assertTrue(BehavioralRole.CompletionReporter in suggestion.combinedRoles)
        assertTrue(suggestion.message().contains("Conveyance Offer"))
    }

    @Test
    fun `custom legacy recipe can still match raw jobs`() {
        val legacy = ComposableRecipe(
            name = "LegacyThing",
            absorbs = setOf(Job.Invite, Job.Report, Job.Progress, Job.Interrupt),
            docsAnchor = "legacy",
        )
        val first = element(
            id = "a",
            jobs = setOf(Job.Invite, Job.Report),
            left = 0f,
        )
        val second = element(
            id = "b",
            jobs = setOf(Job.Progress, Job.Interrupt),
            left = 24f,
        )
        val frame = AuditFrame(
            surface = "legacy",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(first, second),
        )

        assertEquals(legacy, ConsolidationAdvisor.suggest(frame, recipes = listOf(legacy)).single().replacement)
    }
}
