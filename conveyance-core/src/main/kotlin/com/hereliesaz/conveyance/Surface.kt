package com.hereliesaz.conveyance

/**
 * How prominent an element is in the current composition.
 *
 * Rank is descriptive, not a dress code. A surface may have several prominent things, no obvious
 * primary, or a deliberately chaotic hierarchy if that better serves the product. Conveyance should
 * help a design communicate what it is doing, not force every screen into the same composition.
 */
enum class Rank { Primary, Secondary, Tertiary }

/**
 * User-facing chrome text.
 *
 * Conveyance does not impose arbitrary word counts, punctuation bans, or vocabulary blacklists.
 * Labels may be terse, conversational, strange, funny, instructional, or deliberately verbose when
 * the product calls for it. The useful question is whether the language helps the interface teach
 * itself in context, not whether it passes a tailoring specification.
 */
data class Label(val text: String) {
    init {
        require(text.isNotBlank()) { "A label cannot be blank." }
    }

    override fun toString() = text
}

/** An element as declared to the Conscience. */
data class DeclaredElement(
    val id: ElementId,
    val employment: Employment,
    val rank: Rank = Rank.Tertiary,
    val chrome: List<Label> = emptyList(),
    val channels: Set<Channel> = emptySet(),
)

/**
 * One surface's worth of claims.
 *
 * Budgets belong in product-specific design systems when they are useful. The core framework does
 * not limit how many prominent or ambient elements a surface may contain.
 */
data class Surface(
    val name: String,
    val elements: List<DeclaredElement> = emptyList(),
    val gates: List<Gate> = emptyList(),
    val places: List<Place> = emptyList(),
)

/**
 * Everything, for audits that are meaningful across a whole product.
 *
 * Keystones are optional expressive anchors. Products may have none, one, several, or many; scarcity
 * is a design choice, not a framework law.
 */
data class Product(
    val name: String,
    val keystones: List<ActId> = emptyList(),
    val surfaces: List<Surface> = emptyList(),
)
