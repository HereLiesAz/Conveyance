package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConscienceTest {

    private val send = ElementId("invoice.send")
    private val field = ElementId("recipient.field")

    private fun element(
        id: ElementId,
        rank: Rank = Rank.Tertiary,
        chrome: List<Label> = emptyList(),
    ) = DeclaredElement(
        id = id,
        employment = Employment.Working(Job.Invite, Job.Report, Job.Progress, Job.Interrupt),
        rank = rank,
        chrome = chrome,
    )

    /**
     * Every finding names the compliant construction. A build error that explains a philosophy is a
     * tooltip, and this framework does not ship tooltips — it escorts the developer to the fix the
     * same way a blocked act escorts a person to its gate.
     */
    @Test
    fun `every finding carries the fix, not just the complaint`() {
        val surface = Surface(
            name = "invoice",
            elements = (1..3).map { DeclaredElement(ElementId("a$it"), Employment.Ambient) },
            ambientBudget = 0,
        )
        val findings = Conscience.audit(surface)
        assertTrue(findings.isNotEmpty())
        findings.forEach {
            assertTrue(it.instead.isNotBlank(), "${it.audit} complained without naming the fix.")
            assertTrue(it.because.isNotBlank())
        }
    }

    @Test
    fun `a gate whose address is not on the surface is a dead end`() {
        val gate = Gate("recipient", livesAt = ElementId("nowhere")) { false }
        val findings = Conscience.audit(
            Surface("s", elements = listOf(element(send)), gates = listOf(gate)),
        )
        assertEquals(1, findings.count { it.audit == Audit.DeadEnd })
    }

    @Test
    fun `a clean surface is silent`() {
        val gate = Gate("recipient", livesAt = field) { true }
        val findings = Conscience.audit(
            Surface(
                "invoice",
                elements = listOf(
                    element(send, rank = Rank.Primary, chrome = listOf(Label("Send"))),
                    element(field, chrome = listOf(Label("Recipient"))),
                ),
                gates = listOf(gate),
                places = listOf(Place.from("invoice.detail", origin = send)),
            ),
        )
        assertTrue(findings.isEmpty(), "A compliant surface should produce nothing to read: $findings")
    }

    @Test
    fun `ambient elements are budgeted so the exemption cannot become the norm`() {
        fun surfaceWith(n: Int) = Surface(
            "s",
            elements = (1..n).map { DeclaredElement(ElementId("a$it"), Employment.Ambient) },
            ambientBudget = 2,
        )
        assertTrue(Conscience.audit(surfaceWith(2)).none { it.audit == Audit.IdleWorker })
        val over = Conscience.audit(surfaceWith(5)).filter { it.audit == Audit.IdleWorker }
        assertEquals(1, over.size)
        assertEquals(Severity.Warning, over.single().severity, "What cannot be proven reports, not blocks.")
    }

    @Test
    fun `a product may have one beginning`() {
        val two = Surface("s", places = listOf(Place.root("a"), Place.root("b")))
        assertTrue(Conscience.audit(two).any { it.audit == Audit.Teleport })

        val one = Surface("s", places = listOf(Place.root("a"), Place.from("b", origin = send)))
        assertTrue(Conscience.audit(one).none { it.audit == Audit.Teleport })
    }

    @Test
    fun `warnings report and errors block`() {
        val warningOnly = listOf(
            Finding(Audit.DeadEnd, Severity.Warning, "s", "because", "instead"),
        )
        assertFalse(Conscience.blocks(warningOnly))
        assertTrue(Conscience.blocks(warningOnly + Finding(Audit.Teleport, Severity.Error, "s", "b", "i")))
    }
}
