package com.hereliesaz.conveyance

/** What a naive viewer thought would happen, before being told anything. */
data class Prediction(
    /** Where on the surface, in the viewer's own words. It has no element names to use. */
    val where: String,
    /** What they expect touching it to do. */
    val expectation: String,
    /** How sure they were. Certainty about a wrong answer is the worst outcome, not the best. */
    val certain: Boolean,
)

/**
 * How a prediction fared against the truth.
 *
 * **Wrong is worse than NoIdea.** A person with no idea proceeds carefully; a person who is
 * confidently wrong proceeds, and the interface put them there. Any scoring that treats these as
 * equally bad has misunderstood what the test is for.
 */
enum class Grade { Right, Wrong, NoIdea }

data class Verdict(
    val element: ElementId?,
    val grade: Grade,
    val predicted: String,
    val actual: String,
    val note: String,
)

/**
 * The result of showing a surface to someone who has never seen it.
 *
 * [omissions] is the other rule this exists to test: things a person would need to know before
 * acting that the surface does not show. Irreversibility and cost are the usual answers, and the
 * usual answer is that nothing on screen mentions either.
 */
data class AuditReport(
    val surface: String,
    val verdicts: List<Verdict>,
    val omissions: List<String>,
) {
    val right: Int get() = verdicts.count { it.grade == Grade.Right }
    val wrong: Int get() = verdicts.count { it.grade == Grade.Wrong }
    val noIdea: Int get() = verdicts.count { it.grade == Grade.NoIdea }

    /** Right answers as a fraction. Reported, never a gate — see [misleading]. */
    val predictable: Float
        get() = if (verdicts.isEmpty()) 1f else right.toFloat() / verdicts.size

    /**
     * The number worth acting on.
     *
     * A surface that leaves people uncertain is unfinished. A surface that makes them confidently
     * wrong is actively lying, and no amount of correct predictions elsewhere makes up for it.
     */
    val misleading: Int get() = verdicts.count { it.grade == Grade.Wrong }

    override fun toString(): String =
        "$surface: $right right, $wrong wrong, $noIdea no idea; ${omissions.size} omissions"
}
