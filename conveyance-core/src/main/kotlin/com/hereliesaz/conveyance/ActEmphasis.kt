package com.hereliesaz.conveyance

/**
 * How much expressive attention an [Act] is allowed to command.
 *
 * This is a semantic token, not an appearance. Core Conveyance never decides that Heroic means
 * "large", "bright", "bouncy", or any other particular treatment. A binding or product theme maps
 * the token onto shape, motion, type, space, colour, haptics, sound, surrounding response, or any
 * other channels it owns.
 *
 * The distinction exists so a product can intentionally concentrate expressive effort where it has
 * the most to teach. A Heroic act is not "the biggest button"; it is an act whose consequence is
 * important enough that the interface may spend more of its expressive vocabulary making that
 * consequence unusually legible and memorable.
 *
 * There is deliberately no quota. If everything is Heroic, the token has stopped conveying
 * emphasis; that is a relational design problem for Conscience to point out, not a numeric law for
 * the constructor to police.
 */
enum class ActEmphasis {
    /** The product's engineered hero moment: maximum expressive license. */
    Heroic,

    /** A leading act in the current product or surface. */
    Primary,

    /** Important, but not the leading act. */
    Secondary,

    /** Present and useful without asking to dominate the composition. */
    Supporting,
}
