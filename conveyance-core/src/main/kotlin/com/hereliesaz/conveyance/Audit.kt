package com.hereliesaz.conveyance

/**
 * The framework describing a surface to something that will judge it.
 *
 * Some of Conveyance's most important questions are relational rather than structural: can a person
 * tell what an act will do before taking it, are several under-employed fragments really one missing
 * object, is expressive emphasis concentrated where it teaches something, and does a rendered
 * hierarchy agree with the product's declared intent? [AuditFrame] is the semantic half of that
 * evidence. A visual evaluator may see only pixels; Conscience gets the truth afterwards.
 */
data class AuditFrame(
    val surface: String,
    val census: Census,
    val elements: List<AuditElement>,
    /** Every address currently a gate's own. */
    val gateAddresses: Set<ElementId> = emptySet(),
)

/** One element as the framework knows it: where it is, what it does, and what it offers. */
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
    /**
     * The semantic expressive importance of the offered act.
     *
     * This is not a style measurement. The binding decides how [ActEmphasis] manifests; Conscience
     * keeps the token so it can reason about relationships such as every visible act being Heroic,
     * or a pivotal act being marked Supporting while surrounding incidental acts dominate.
     */
    val emphasis: ActEmphasis? = null,
    /**
     * Whether this element was explicitly declared [Employment.Ambient].
     *
     * Kept separate from [jobs] because the two answer different questions: jobs are what the
     * element is observed doing; Ambient says it intentionally is not a working element and therefore
     * opts out of the four-job rule.
     */
    val ambient: Boolean = false,
) {
    /** Whether this carries a cost a person cannot take back. */
    val staked: Boolean get() = act != null && !reversible
}
