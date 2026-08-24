package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Chrome text that cannot become instruction, because there is no longer anywhere to write an
 * instruction down and have it compile. This used to be a linter finding, discovered whenever
 * someone next ran the audit; it is a constructor now, discovered the moment the string is written.
 */
class LabelTest {

    @Test
    fun `a sentence cannot become a label`() {
        assertFailsWith<IllegalArgumentException> { Label("Tap here to send your invoice.") }
    }

    @Test
    fun `a long phrase cannot become a label even without punctuation`() {
        assertFailsWith<IllegalArgumentException> { Label("Your file is being uploaded") }
    }

    @Test
    fun `a word that tells the reader what to do cannot become a label`() {
        assertFailsWith<IllegalArgumentException> { Label("Tap") }
        assertFailsWith<IllegalArgumentException> { Label("Choose a recipient") }
    }

    @Test
    fun `a blank label cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> { Label("   ") }
    }

    @Test
    fun `a verb is an invitation and a noun names a thing`() {
        Label("Send")
        Label("Invoices")
        Label("Overdue invoices")
        assertTrue(true, "None of the above threw.")
    }

    @Test
    fun `a label reads back as its own text`() {
        assertTrue(Label("Send").toString() == "Send")
    }
}
