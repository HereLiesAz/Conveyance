package com.hereliesaz.conveyance

/**
 * What a channel is allowed to say.
 *
 * A channel is useful when its variation is deliberate and learnable. Conveyance does not require
 * every product to assign the same meaning to a visual property, and it does not reserve colour for
 * hierarchy. A product may use colour for stable identity, grouping, rank, state, or another coherent
 * grammar as long as that grammar stays consistent and does not carry critical meaning by colour
 * alone.
 *
 * The meanings below are the reference vocabulary used by the framework and reference binding. They
 * are not a mandate that every product flatten its visual language into the same palette.
 */
enum class Meaning {
    /** Where a thing came from and where it belongs. */
    OriginAndRelation,

    /** How much this matters *right now* — not its permanent rank. */
    MomentaryImportance,

    /** Settled, engaged, or pending. */
    State,

    /** Stable product-defined identity or grouping expressed through colour. */
    VisualIdentity,

    /** How live, recent or urgent. */
    Heat,

    /** Whether this can be backed out of. */
    Reversibility,

    /** Mid-transition and nothing else. Never a resting value. */
    TransitionOnly,

    /** What to read first. */
    ReadingOrder,

    /** What belongs with what. */
    Relatedness,

    /** The motion grammar used by the product. */
    MotionGrammar,

    /** How much this cost, felt rather than seen. */
    ConsequenceMagnitude,

    /** Reserved for deliberately scarce expressive moments. */
    KeystoneOnly,
}

/**
 * The reference channel assignment.
 *
 * This is a useful default vocabulary, not a universal aesthetic constitution. In particular,
 * **Hue carries product-defined visual identity**, not semantic rank. A product may therefore use a
 * broad identity palette — for roles, subjects, workstreams, people, collections, or other stable
 * concepts — while hierarchy is conveyed through the product's composition, type, size, placement,
 * emphasis, and interaction grammar.
 *
 * Critical state or safety information must remain legible without colour, so hue may reinforce a
 * distinction but must not be its only carrier.
 */
enum class Channel(val carries: Meaning) {
    Position(Meaning.OriginAndRelation),
    Size(Meaning.MomentaryImportance),
    Shape(Meaning.State),
    Hue(Meaning.VisualIdentity),
    Chroma(Meaning.Heat),
    Elevation(Meaning.Reversibility),
    Opacity(Meaning.TransitionOnly),
    TypeScale(Meaning.ReadingOrder),
    Density(Meaning.Relatedness),
    Motion(Meaning.MotionGrammar),
    Haptics(Meaning.ConsequenceMagnitude),
    Sound(Meaning.KeystoneOnly);

    companion object {
        /** The reference channel that carries a framework meaning. */
        fun carrying(meaning: Meaning): Channel = entries.first { it.carries == meaning }
    }
}

/**
 * The reference haptic vocabulary.
 *
 * Products may extend this where the hardware and interaction genuinely support another learnable
 * distinction. The important rule is semantic consistency, not an arbitrary global count.
 */
enum class HapticVoice {
    /** An act took effect. */
    Commit,

    /** The rules in force just changed. */
    ModeChange;

    /** Intensity follows consequence weight in the reference binding. */
    fun intensity(weight: Weight): Weight = weight
}
