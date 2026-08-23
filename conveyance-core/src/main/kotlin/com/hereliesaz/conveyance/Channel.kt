package com.hereliesaz.conveyance

/**
 * What a channel is allowed to say.
 *
 * Every meaning is claimed by exactly one channel, and every channel carries exactly one meaning.
 * Anything left over is not decorated — it goes plain. The pay-off is that nothing on screen varies
 * without saying something, so everything that varies is information, which is what lets an
 * interface be read without labels.
 */
enum class Meaning {
    /** Where a thing came from and where it belongs. */
    OriginAndRelation,

    /** How much this matters *right now* — not its permanent rank. */
    MomentaryImportance,

    /** Settled, engaged, or pending. */
    State,

    /** One primary per surface, secondary alternatives, tertiary ambient. */
    SemanticRank,

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

    /** The nine verbs, and no borrowed signatures. */
    MotionGrammar,

    /** How much this cost, felt rather than seen. */
    ConsequenceMagnitude,

    /** Reserved for the product's one to three keystones. */
    KeystoneOnly,
}

/**
 * The channel assignment. This table is the product's entire visual vocabulary.
 *
 * Two entries are unusual enough to be worth defending. **Elevation carries reversibility**: once it
 * is consistent, a person knows at a glance which things they can back out of, and shadow stops
 * being lighting and becomes a safety map. **Opacity is transition-only**: half-opacity is the
 * universal signal for "disabled", which is the construct this framework does not have, so there is
 * no purgatory of ghosted controls to be tested by tapping.
 */
enum class Channel(val carries: Meaning) {
    Position(Meaning.OriginAndRelation),
    Size(Meaning.MomentaryImportance),
    Shape(Meaning.State),
    Hue(Meaning.SemanticRank),
    Chroma(Meaning.Heat),
    Elevation(Meaning.Reversibility),
    Opacity(Meaning.TransitionOnly),
    TypeScale(Meaning.ReadingOrder),
    Density(Meaning.Relatedness),
    Motion(Meaning.MotionGrammar),
    Haptics(Meaning.ConsequenceMagnitude),
    Sound(Meaning.KeystoneOnly);

    companion object {
        /** The channel that owns a meaning. Total, by construction. */
        fun carrying(meaning: Meaning): Channel = entries.first { it.carries == meaning }
    }
}

/**
 * The haptic channel has two voices and no more.
 *
 * Distinguishing "you did a thing that took effect" from "the world you are operating in is now a
 * different world" is the only distinction a person can reliably feel, so it is the only one made.
 * Nothing fires per drag frame; a cycler speaks once, when it commits, not on each step through its
 * options.
 */
enum class HapticVoice {
    /** An act took effect. */
    Commit,

    /** The rules in force just changed. */
    ModeChange;

    /** Intensity is [Weight], because the hand should be told the cost, not the event. */
    fun intensity(weight: Weight): Weight = weight
}
