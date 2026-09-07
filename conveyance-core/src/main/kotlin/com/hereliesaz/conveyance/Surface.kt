package com.hereliesaz.conveyance

/**
 * How prominent an element is, semantically rather than decoratively.
 *
 * Carried by hue, and by hue alone. One primary per surface is enforced where the primaries are
 * actually gathered — at [Surface] construction — because two primaries is two answers to "what
 * should I do here", and a rule an audit merely reports is a rule a build can ship anyway.
 */
enum class Rank { Primary, Secondary, Tertiary }

/**
 * Chrome text, refused at construction if it reads as an instruction rather than a name.
 *
 * The operational test: four words or fewer, no closing sentence punctuation, none of a small set
 * of words that address the reader directly rather than naming a thing. A string that fails this is
 * not a style problem to flag later — it cannot become a [Label], the same way a string cannot
 * become a [Gate] without an address.
 */
data class Label(val text: String) {
    init {
        // \p{Zs} is the Unicode "space separator" category -- U+00A0 (non-breaking space) and its
        // relatives among them. A label built from pasted text is exactly where one can appear.
        val words = text.trim().split(Regex("[\\s\\p{Zs}]+")).filter { it.isNotBlank() }
        require(words.isNotEmpty()) { "A label cannot be blank." }
        require(words.size <= 4) {
            "\"$text\" is ${words.size} words. Name the thing or the act; if behaviour cannot carry " +
                "the meaning, the design needs changing rather than captioning."
        }
        require(text.none { it in SENTENCE_ENDS }) {
            "\"$text\" reads as a sentence. A label names; it does not narrate."
        }
        val tells = words.any { it.trim(',', '.', '!', '?').lowercase() in TELLS }
        require(!tells) { "\"$text\" tells the person what to do. Name the thing or the act instead." }
    }

    override fun toString() = text

    private companion object {
        val SENTENCE_ENDS = setOf('.', '!', '?')
        val TELLS = setOf(
            "tap", "click", "press", "swipe", "drag", "select", "choose", "enter",
            "please", "simply", "just", "now", "here", "your", "you",
        )
    }
}

/** An element as declared to the Conscience. */
data class DeclaredElement(
    val id: ElementId,
    val employment: Employment,
    val rank: Rank = Rank.Tertiary,
    val chrome: List<Label> = emptyList(),
    val channels: Set<Channel> = emptySet(),
)

/** One screen's worth of claims. */
data class Surface(
    val name: String,
    val elements: List<DeclaredElement> = emptyList(),
    val gates: List<Gate> = emptyList(),
    val places: List<Place> = emptyList(),
    val ambientBudget: Int = 2,
) {
    init {
        val primaries = elements.filter { it.rank == Rank.Primary }
        require(primaries.size <= 1) {
            "${primaries.size} primary elements on \"$name\": ${primaries.joinToString { it.id.value }}. " +
                "Keep one primary; the rest are secondary alternatives or tertiary ambient."
        }
    }
}

/** Everything, for the audits that are only meaningful across a whole product. */
data class Product(
    val name: String,
    val keystones: List<ActId>,
    val surfaces: List<Surface> = emptyList(),
) {
    init {
        require(keystones.size in 1..3) {
            val count = keystones.size
            "$name declares ${if (count == 0) "no keystone" else "$count keystones"}. Declare " +
                "between one and three; the scarcity is what makes them land."
        }
    }
}
