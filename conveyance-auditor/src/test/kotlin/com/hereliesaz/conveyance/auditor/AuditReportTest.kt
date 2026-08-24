package com.hereliesaz.conveyance.auditor

import com.hereliesaz.conveyance.AuditReport
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Grade
import com.hereliesaz.conveyance.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditReportTest {

    private fun verdict(grade: Grade) = Verdict(ElementId("e"), grade, "predicted", "actual", "")

    /**
     * The asymmetry is the whole point of the grading scheme. Someone who cannot tell what a
     * control does proceeds carefully. Someone confidently wrong proceeds, and the interface put
     * them there.
     */
    @Test
    fun `wrong is counted separately from no idea, because it is worse`() {
        val report = AuditReport(
            surface = "gallery",
            verdicts = listOf(
                verdict(Grade.Right),
                verdict(Grade.Right),
                verdict(Grade.NoIdea),
                verdict(Grade.Wrong),
            ),
            omissions = emptyList(),
        )

        assertEquals(2, report.right)
        assertEquals(1, report.noIdea)
        assertEquals(1, report.wrong)
        assertEquals(1, report.misleading, "Only wrong answers count as misleading.")
        assertEquals(0.5f, report.predictable)
    }

    @Test
    fun `a surface nobody could read is not a surface with no verdicts`() {
        val silent = AuditReport("empty", emptyList(), emptyList())
        assertEquals(1f, silent.predictable, "Nothing to predict is not a failure to predict.")
        assertEquals(0, silent.misleading)
    }

    @Test
    fun `omissions are carried separately from predictions`() {
        val report = AuditReport(
            surface = "gallery",
            verdicts = listOf(verdict(Grade.Right)),
            omissions = listOf("Deleting is permanent and nothing on screen says so."),
        )
        assertEquals(1f, report.predictable, "A perfectly predictable screen can still hide the stakes.")
        assertTrue(report.omissions.isNotEmpty())
    }
}
