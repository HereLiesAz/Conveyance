package com.hereliesaz.conveyance

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ActTest {

    private val list = ElementId("invoices")
    private val recipientField = ElementId("recipient.field")
    private val recipientAvatar = ElementId("recipient.avatar")
    private val invoice = SubjectId("invoice.41")

    @Test
    fun `an act with no unmet gate is ready`() {
        val act = Act.send("invoice.send", invoice, recipientAvatar)
        assertEquals(ActState.Ready, act.state())
        assertEquals(null, act.blockingGate())
    }

    @Test
    fun `a gate names the element where it is resolved`() {
        var recipient: String? = null
        val gate = Gate("recipient.chosen", livesAt = recipientField) { recipient != null }
        val act = Act.send("invoice.send", invoice, recipientAvatar, requires = listOf(gate))

        val blocked = assertIs<ActState.Blocked>(act.state())
        assertEquals(recipientField, blocked.gate.livesAt)

        recipient = "someone"
        assertEquals(ActState.Ready, act.state())
    }

    /**
     * The framework's replacement for the disabled state. A blocked act is not inert and does not
     * fail: it reports where to go, and the binding carries the person there.
     */
    @Test
    fun `a blocked act escorts instead of performing`() = runTest {
        var performed = false
        val gate = Gate("never", livesAt = recipientField) { false }
        val act = Act.send("invoice.send", invoice, recipientAvatar, requires = listOf(gate)) {
            performed = true
            Outcome.Done
        }

        val seen = mutableListOf<ActState>()
        val terminal = act.engage(seen::add)

        assertIs<ActState.Blocked>(terminal)
        assertFalse(performed, "A blocked act must not run its work.")
        assertEquals(1, seen.size, "A blocked act does not pass through Yielding.")
    }

    @Test
    fun `an engaged act yields, then settles, in that order`() = runTest {
        val act = Act.create("invoice.new", invoice, into = list)
        val seen = mutableListOf<ActState>()

        assertEquals(ActState.Settled, act.engage(seen::add))
        assertContentEquals(listOf(ActState.Yielding(), ActState.Settled), seen)
    }

    @Test
    fun `failure becomes a state of the element, carrying a derived retry`() = runTest {
        val act = Act.send("invoice.send", invoice, recipientAvatar) { Outcome.Failed(Refusal.Unreachable) }
        val refused = assertIs<ActState.Refused>(act.engage())
        assertEquals(Refusal.Unreachable, refused.refusal)
        assertTrue(refused.retryable)

        val denied = Act.send("x", invoice, recipientAvatar) { Outcome.Failed(Refusal.Denied) }
        assertFalse(assertIs<ActState.Refused>(denied.engage()).retryable, "Retrying a refusal invites repeated failure.")
    }

    @Test
    fun `work that dies part-way is interrupted, not silently settled`() = runTest {
        val act = Act.create("boom", invoice, into = list) { error("connection dropped") }
        assertEquals(ActState.Refused(Refusal.Interrupted), act.engage())
    }

    /**
     * There is no way to write a destruction without a reversal — [Act.destroy] takes a non-null
     * inverse. This test records the consequence of that: every destructive act is reversible, so
     * the Ghost is always available and a confirmation dialog is never needed.
     */
    @Test
    fun `every destruction is reversible by construction`() {
        val restore = Act.create("invoice.restore", invoice, into = list)
        val delete = Act.destroy("invoice.delete", invoice, target = list, inverse = restore)

        assertTrue(delete.reversible)
        assertEquals(restore, delete.inverse)
        assertTrue(delete.signature.leavesResidue, "A destruction with no Ghost has nowhere to be undone from.")
    }

    @Test
    fun `an act cannot be declared without a consequence that names its target`() {
        val send = Act.send("invoice.send", invoice, recipientAvatar)
        assertEquals(recipientAvatar, send.consequence.target)

        val create = Act.create("invoice.new", invoice, into = list)
        assertEquals(list, create.consequence.target)

        val enter = Act.enter("invoice.open", Place.from("invoice.detail", origin = list))
        assertEquals(list, enter.consequence.target)
    }

    /**
     * A root place has no antecedent to grow out of, so entering one is not a smaller version of
     * Enter -- it is not Enter at all. This used to fail only when something later read
     * [Consequence.target] (an audit, a render), which meant the bad act could be constructed,
     * passed around and even offered on screen before anything noticed. Refusing at construction
     * is what makes the framework's own claim -- that this cannot be represented -- actually true.
     */
    @Test
    fun `entering a root place is refused where the act is made, not wherever it is later read`() {
        assertFailsWith<IllegalArgumentException> { Act.enter("go.home", Place.root("home")) }
        assertFailsWith<IllegalArgumentException> { Consequence.Enter(Place.root("home")) }
    }
}
