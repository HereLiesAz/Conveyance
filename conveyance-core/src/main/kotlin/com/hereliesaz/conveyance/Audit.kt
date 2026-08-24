package com.hereliesaz.conveyance

/**
 * The framework describing a surface to something that will judge it.
 *
 * Two of the framework's rules cannot be settled by any structural check. *Not omit necessary
 * details* requires knowing which detail was necessary, and *the reader can tell beforehand what
 * each will do* requires a reader. Both were written off as human-only work — which was too quick.
 * A viewer given nothing but the pixels can attempt both, and the gap between what that viewer
 * predicts and what is actually true is the finding.
 *
 * The whole exercise depends on the viewer being **naive**: it must see the rendered surface and
 * nothing else. No element names, no act identifiers, no source. The moment it can read the code it
 * stops predicting and starts reciting, and the test measures nothing.
 *
 * This is the other half — the truth, held back from the viewer and used only to grade it. No
 * screenshot tool could produce it, and that is exactly what the semantic model was for.
 */
data class AuditFrame(
    val surface: String,
    val census: Census,
    val elements: List<AuditElement>,
)

/** One element as the framework knows it: where it is, and what it will actually do. */
data class AuditElement(
    val id: ElementId,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val visible: Boolean,
    /** Present when this element offers an act. Absent means it does nothing when touched. */
    val act: ActId? = null,
    val verb: Verb? = null,
    /** What the act will change, and where. The thing a person should be able to predict. */
    val consequence: String? = null,
    val weight: Weight? = null,
    /** Whether the act can be taken back. A person deserves to know this before acting. */
    val reversible: Boolean = false,
    /** Whether the act is currently gated. */
    val blocked: Boolean = false,
    val jobs: Set<Job> = emptySet(),
) {
    /**
     * Whether this carries a cost a person cannot take back.
     *
     * Irreversibility alone, not weight. Weight is how an act *feels* — how much inertia it has
     * under the finger — and a heavy act that can be undone is not a trap. Entering a place is the
     * clearest case: it is deliberately weighty, because leaving what you were doing is
     * consequential, and it is completely recoverable. Treating those as the same thing had the
     * auditor demanding a warning before every tap.
     */
    val staked: Boolean get() = act != null && !reversible
}
