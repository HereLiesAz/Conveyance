package com.hereliesaz.conveyance

/**
 * What an element is for.
 *
 * Resourceful minimalism is unenforceable as advice and trivial to enforce as arithmetic, so jobs
 * are enumerable and elements declare them. An element that cannot name four is standing around
 * watching one guy dig.
 */
enum class Job {
    /** Offers an act. */
    Invite,

    /** Shows current state. */
    Report,

    /** Tells you where you are. */
    Locate,

    /** Distinguishes one subject from another. */
    Identify,

    /** Binds things together. */
    Group,

    /** Marks a boundary. */
    Separate,

    /** Shows work happening. */
    Progress,

    /** Shows work completed. */
    Confirm,

    /** Shows risk. */
    Warn,

    /** Moves you. */
    Navigate,

    /** Stops what it started. */
    Interrupt,
}

/** Why an element is on screen at all. */
sealed interface Employment {

    /**
     * Doing real work. Four jobs is the minimum. For an inviting element, three of the four
     * cost nothing to justify -- [Job.Invite] is the declaration itself, [Job.Progress] is true
     * the moment it exists (Offer renders every act's states, Yielding included, from the same
     * pixels), and [Job.Interrupt] is owed for the same reason Law 4 already names it. But
     * nothing here is inferred: the type does not add jobs on an element's behalf, because a
     * claimed job with no code standing behind it is exactly the failure this law exists to
     * catch, not a shortcut around declaring it. A developer who has internalised that the first
     * three are close to automatic still has to write all four down -- what that buys is one
     * real job of friction, not zero.
     *
     * The count is checked at construction rather than by a later audit, because an unemployed
     * element that reaches an audit has already been designed, reviewed and probably shipped.
     */
    class Working(val jobs: Set<Job>) : Employment {
        constructor(vararg jobs: Job) : this(jobs.toSet())

        init {
            require(jobs.size >= 4) {
                "An element with ${jobs.size} job(s) is standing around. " +
                    "Merge it with its neighbour, or delete it: $jobs"
            }
        }

        override fun toString() = "Working(${jobs.joinToString(", ")})"
    }

    /**
     * Deliberately doing nothing: a ground, a rule, a field of space.
     *
     * Budgeted per surface, because an exemption that is not counted quietly becomes the norm and
     * then the whole rule was decorative.
     */
    data object Ambient : Employment
}
