package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConscienceTest {

    private val send = ElementId("invoice.send")
    private val field = ElementId("recipient.field")

    private fun element(id: ElementId) = DeclaredElement(
        id = id,
        employment = Employment.Working(Job.Invite, Job.Report, Job.Progress, Job.Interrupt),
    )

    @Test
    fun `a gate whose resolver is absent is reported`() {
        val gate = Gate("recipient", livesAt = ElementId("nowhere")) { false }
        val findings = Conscience.audit(
            Surface("s", elements = listOf(element(send)), gates = listOf(gate)),
        )
        assertEquals(1, findings.count { it.audit == Audit.DeadEnd })
    }

    @Test
    fun `a reachable gate produces no finding`() {
        val gate = Gate("recipient", livesAt = field) { true }
        val findings = Conscience.audit(
            Surface(
                "invoice",
                elements = listOf(element(send), element(field)),
                gates = listOf(gate),
                places = listOf(Place.from("invoice.detail", origin = send)),
            ),
        )
        assertTrue(findings.isEmpty(), "A coherent surface should produce nothing to read: $findings")
    }

    private fun auditElement(
        id: ElementId,
        jobs: Set<Job> = setOf(Job.Invite, Job.Report, Job.Progress, Job.Interrupt),
        ambient: Boolean = false,
    ) = AuditElement(
        id = id,
        left = 0f,
        top = 0f,
        width = 0f,
        height = 0f,
        visible = true,
        jobs = jobs,
        ambient = ambient,
    )

    @Test
    fun `a live working element doing fewer than four jobs is an idle worker`() {
        val frame = AuditFrame(
            surface = "invoice",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(auditElement(send, jobs = setOf(Job.Invite, Job.Report))),
        )
        assertEquals(1, Conscience.audit(frame).count { it.audit == Audit.IdleWorker })
    }

    @Test
    fun `ambient explicitly opts out of the four job rule`() {
        val frame = AuditFrame(
            surface = "invoice",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(auditElement(send, jobs = emptySet(), ambient = true)),
        )
        assertTrue(Conscience.audit(frame).none { it.audit == Audit.IdleWorker })
    }

    @Test
    fun `a live gate resolver that did not compose is reported`() {
        val frame = AuditFrame(
            surface = "invoice",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(auditElement(send)),
            gateAddresses = setOf(ElementId("nowhere")),
        )
        assertEquals(1, Conscience.audit(frame).count { it.audit == Audit.DeadEnd })
    }

    @Test
    fun `lint finding teaches rule opt-out and links directly to docs`() {
        val frame = AuditFrame(
            surface = "invoice",
            census = Census(0, 0, 0, 0, 0, 0, emptyList(), emptyList(), emptyList()),
            elements = listOf(auditElement(send, jobs = setOf(Job.Invite, Job.Report))),
        )

        val finding = Conscience.audit(frame).single { it.audit == Audit.IdleWorker }
        val log = finding.toString()

        assertTrue(log.contains("Rule:"), log)
        assertTrue(log.contains("Why:"), log)
        assertTrue(log.contains("Opt-out:"), log)
        assertTrue(log.contains("Docs:"), log)
        assertTrue(log.contains("Employment.Ambient"), log)
        assertTrue(log.contains("RULES-AND-OPTOUTS.md#employment"), log)
        assertFalse(log.contains("Examples:"), log)
    }

    @Test
    fun `gate lint finding links to its rule and opt-out`() {
        val gate = Gate("recipient", livesAt = ElementId("nowhere")) { false }
        val finding = Conscience.audit(
            Surface("s", elements = listOf(element(send)), gates = listOf(gate)),
        ).single()

        val log = finding.toString()
        assertTrue(log.contains("Rule:"), log)
        assertTrue(log.contains("Opt-out:"), log)
        assertTrue(log.contains("Docs:"), log)
        assertTrue(log.contains("do not model it as a Gate"), log)
        assertTrue(log.contains("RULES-AND-OPTOUTS.md#gates"), log)
        assertFalse(log.contains("Examples:"), log)
    }

    @Test
    fun `warnings inform but do not block`() {
        val warningOnly = listOf(
            Finding(
                audit = Audit.DeadEnd,
                severity = Severity.Warning,
                where = "s",
                because = "because",
                instead = "instead",
                guide = Conscience.gateGuide,
            ),
        )
        assertFalse(Conscience.blocks(warningOnly))
    }
}
