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
        chrome: List<String> = emptyList(),
        channels: Map<Channel, Meaning> = emptyMap(),
    ) = DeclaredElement(
        id = id,
        employment = Employment.Working(Job.Invite, Job.Report),
        rank = rank,
        chrome = chrome,
        channels = channels,
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
            elements = listOf(
                element(send, rank = Rank.Primary, chrome = listOf("Tap here to send your invoice.")),
                element(field, rank = Rank.Primary),
            ),
        )
        val findings = Conscience.audit(surface)
        assertTrue(findings.isNotEmpty())
        findings.forEach {
            assertTrue(it.instead.isNotBlank(), "${it.audit} complained without naming the fix.")
            assertTrue(it.because.isNotBlank())
        }
    }

    @Test
    fun `a sentence in the chrome is an error and a name is not`() {
        fun audit(text: String) = Conscience.audit(
            Surface("s", elements = listOf(element(send, chrome = listOf(text)))),
        ).filter { it.audit == Audit.Instruction }

        assertTrue(audit("Tap here to send your invoice.").any { it.severity == Severity.Error })
        assertTrue(audit("Your file is being uploaded").isNotEmpty())
        assertTrue(audit("Send").isEmpty(), "A verb is an invitation, not an instruction.")
        assertTrue(audit("Invoices").isEmpty(), "A noun names a thing.")
        assertTrue(audit("Overdue invoices").isEmpty())
    }

    @Test
    fun `two primaries on a surface is two answers to one question`() {
        val findings = Conscience.audit(
            Surface(
                "s",
                elements = listOf(
                    element(send, rank = Rank.Primary),
                    element(field, rank = Rank.Primary),
                ),
            ),
        )
        assertEquals(1, findings.count { it.audit == Audit.PrimaryContention })
        assertTrue(Conscience.blocks(findings))
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
    fun `a channel used for a meaning it does not carry is refused, and told where to go`() {
        val findings = Conscience.audit(
            Surface(
                "s",
                elements = listOf(element(send, channels = mapOf(Channel.Opacity to Meaning.State))),
            ),
        ).filter { it.audit == Audit.Channel }

        assertEquals(1, findings.size)
        assertEquals(Severity.Error, findings.single().severity)
        assertTrue(
            findings.single().instead.contains(Channel.Shape.name),
            "The fix must name the channel that actually carries State.",
        )
    }

    @Test
    fun `a clean surface is silent`() {
        val gate = Gate("recipient", livesAt = field) { true }
        val findings = Conscience.audit(
            Surface(
                "invoice",
                elements = listOf(
                    element(send, rank = Rank.Primary, chrome = listOf("Send")),
                    element(field, chrome = listOf("Recipient")),
                ),
                gates = listOf(gate),
                places = listOf(Place.from("invoice.detail", origin = send)),
            ),
        )
        assertTrue(findings.isEmpty(), "A compliant surface should produce nothing to read: $findings")
    }

    @Test
    fun `keystones are budgeted, and both too few and too many are errors`() {
        fun audit(n: Int) = Conscience.audit(
            Product("p", surfaces = emptyList(), keystones = (1..n).map { ActId("k$it") }),
        )
        assertTrue(audit(0).any { it.audit == Audit.KeystoneBudget })
        assertTrue(audit(1).none { it.audit == Audit.KeystoneBudget })
        assertTrue(audit(3).none { it.audit == Audit.KeystoneBudget })
        assertTrue(audit(4).any { it.audit == Audit.KeystoneBudget })
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
        assertTrue(Conscience.blocks(warningOnly + Finding(Audit.Channel, Severity.Error, "s", "b", "i")))
    }
}
