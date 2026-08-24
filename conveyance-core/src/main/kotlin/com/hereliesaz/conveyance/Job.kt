package com.hereliesaz.conveyance

/**
 * What an element is for.
 *
 * Resourceful minimalism is unenforceable as advice and trivial to enforce as arithmetic, so jobs
 * are enumerable and elements declare them. An element that cannot name three is standing around
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
     * Doing real work. Three jobs is the minimum: one job means "merge this with its
     * neighbour", and two is still a job short, because anything that starts something owes a
     * way to stop it -- a control with no [Job.Interrupt] and no third job in its place is
     * asking to be trusted rather than offering a way out.
     *
     * The count is checked at construction rather than by a later audit, because an unemployed
     * element that reaches an audit has already been designed, reviewed and probably shipped.
     */
    class Working(val jobs: Set<Job>) : Employment {
        constructor(vararg jobs: Job) : this(jobs.toSet())

        init {
            require(jobs.size >= 3) {
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
