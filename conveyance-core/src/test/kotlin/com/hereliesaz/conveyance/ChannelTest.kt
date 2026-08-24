package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChannelTest {

    /**
     * Channel Economy, as a test rather than as advice. If a meaning has two channels the product
     * says the same thing twice; if a channel has two meanings a person cannot tell which is being
     * said. Either way the interface stops being readable without labels.
     */
    @Test
    fun `every channel carries exactly one meaning and every meaning has exactly one channel`() {
        val assigned = Channel.entries.map { it.carries }
        assertEquals(assigned.size, assigned.toSet().size, "Two channels claim the same meaning.")
        assertEquals(Meaning.entries.toSet(), assigned.toSet(), "A meaning has no channel, or a channel invents one.")
        Meaning.entries.forEach { assertEquals(it, Channel.carrying(it).carries) }
    }

    @Test
    fun `elevation means reversibility and opacity is never a resting state`() {
        assertEquals(Channel.Elevation, Channel.carrying(Meaning.Reversibility))
        assertEquals(Meaning.TransitionOnly, Channel.Opacity.carries)
    }

    @Test
    fun `the haptic channel has two voices and intensity follows weight`() {
        assertEquals(2, HapticVoice.entries.size)
        assertEquals(Weight.Heavy, HapticVoice.Commit.intensity(Weight.Heavy))
    }

    @Test
    fun `an element with fewer than four jobs cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> { Employment.Working(Job.Report) }
        assertFailsWith<IllegalArgumentException> { Employment.Working() }
        // Three, even three real ones including Invite, is still a job short: nothing here is
        // inferred on the developer's behalf, however close to automatic Progress and Interrupt
        // are for an inviting element.
        assertFailsWith<IllegalArgumentException> { Employment.Working(Job.Invite, Job.Progress, Job.Interrupt) }

        val employed = Employment.Working(Job.Invite, Job.Progress, Job.Interrupt, Job.Report)
        assertEquals(setOf(Job.Invite, Job.Progress, Job.Interrupt, Job.Report), employed.jobs)
    }

    @Test
    fun `duplicate jobs do not count toward employment`() {
        assertFailsWith<IllegalArgumentException> { Employment.Working(Job.Invite, Job.Invite) }
    }
}
