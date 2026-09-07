package com.hereliesaz.conveyance

/**
 * What an element is for.
 *
 * Jobs are a vocabulary for describing useful work an element may do. They are not a quota. A
 * beautifully specific element may do one job; a dense H2G2-style record may do six. Conveyance can
 * expose that difference to tools and audits without pretending arithmetic decides whether the
 * design deserves to exist.
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
     * The element is doing one or more identifiable jobs.
     *
     * There is deliberately no minimum count. The jobs exist so a product can reason about its
     * elements, not so the framework can issue citations for insufficient multitasking.
     */
    class Working(val jobs: Set<Job>) : Employment {
        constructor(vararg jobs: Job) : this(jobs.toSet())

        init {
            require(jobs.isNotEmpty()) { "Working employment needs at least one actual job." }
        }

        override fun toString() = "Working(${jobs.joinToString(", ")})"
    }

    /**
     * Deliberately present without an operational job: ground, texture, breathing room, ornament,
     * atmosphere, or anything else the product wants there.
     *
     * Ambient elements are not budgeted by the framework.
     */
    data object Ambient : Employment
}
