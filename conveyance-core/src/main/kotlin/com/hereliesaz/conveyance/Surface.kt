package com.hereliesaz.conveyance

/**
 * How prominent an element is, semantically rather than decoratively.
 *
 * Carried by hue, and by hue alone. One primary per surface, and that is a rule with a test behind
 * it: two primaries is two answers to "what should I do here", which is the question the interface
 * is supposed to be answering for the person rather than asking them.
 */
enum class Rank { Primary, Secondary, Tertiary }

/**
 * An element as declared to the Conscience.
 *
 * This is not a rendering. It is what a surface claims about itself, so the claims can be checked
 * before anyone has to look at a screen and form an opinion.
 */
data class DeclaredElement(
    val id: ElementId,
    val employment: Employment,
    val rank: Rank = Rank.Tertiary,
    /** User-facing strings this element puts on screen. Not user-generated content. */
    val chrome: List<String> = emptyList(),
    /** Channels this element varies, and what it varies them to say. */
    val channels: Map<Channel, Meaning> = emptyMap(),
)

/** One screen's worth of claims. */
data class Surface(
    val name: String,
    val elements: List<DeclaredElement> = emptyList(),
    val gates: List<Gate> = emptyList(),
    val places: List<Place> = emptyList(),
    /**
     * How many elements may be [Employment.Ambient] here.
     *
     * An exemption that is not counted becomes the norm, and then the rule it exempts was
     * decorative all along.
     */
    val ambientBudget: Int = 2,
)

/** Everything, for the audits that are only meaningful across a whole product. */
data class Product(
    val name: String,
    val surfaces: List<Surface> = emptyList(),
    val keystones: List<ActId> = emptyList(),
)
