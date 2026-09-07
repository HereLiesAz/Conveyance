package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChannelTest {

    @Test
    fun `reference channels remain internally coherent`() {
        val assigned = Channel.entries.map { it.carries }
        assertEquals(assigned.size, assigned.toSet().size)
        Meaning.entries.forEach { assertEquals(it, Channel.carrying(it).carries) }
    }

    @Test
    fun `hue is available for stable visual identity`() {
        assertEquals(Channel.Hue, Channel.carrying(Meaning.VisualIdentity))
    }

    @Test
    fun `reference haptic intensity follows weight`() {
        assertEquals(Weight.Heavy, HapticVoice.Commit.intensity(Weight.Heavy))
    }

    @Test
    fun `an element may honestly do one job`() {
        val employed = Employment.Working(Job.Report)
        assertEquals(setOf(Job.Report), employed.jobs)
    }

    @Test
    fun `working employment must still name something real`() {
        assertFailsWith<IllegalArgumentException> { Employment.Working() }
    }

    @Test
    fun `duplicate job declarations collapse to the underlying job set`() {
        val employed = Employment.Working(Job.Invite, Job.Invite)
        assertEquals(setOf(Job.Invite), employed.jobs)
    }
}
