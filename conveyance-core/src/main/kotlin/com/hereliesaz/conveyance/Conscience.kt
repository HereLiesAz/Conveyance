package com.hereliesaz.conveyance

/** Which kind of design observation a finding is about. */
enum class Audit {
    IdleWorker,
    Teleport,
    DeadEnd,
}

/**
 * Findings are advisory by default. Error is reserved for states the framework can prove are
 * internally incoherent or unsafe, not for breaking a preferred composition rule.
 */
enum class Severity { Error, Warning }

/**
 * The compact teaching material that accompanies a lint finding.
 *
 * Conveyance should convey to its designer too. A lint message therefore never stops at "wrong";
 * it states the generative rule, gives concrete examples where useful, and names the semantic
 * opt-out that applies when the rule genuinely does not describe the element.
 */
data class RuleGuide(
    val rule: String,
    val why: String,
    val examples: List<String> = emptyList(),
    val optOut: String,
)

/** One observation produced by the Conscience. */
data class Finding(
    val audit: Audit,
    val severity: Severity,
    val where: String,
    val because: String,
    val instead: String,
    val guide: RuleGuide? = null,
) {
    override fun toString(): String = buildString {
        append("[$severity] $audit at $where\n")
        append("  Found: $because\n")
        append("  Try: $instead")
        guide?.let { guide ->
            append("\n  Rule: ${guide.rule}")
            append("\n  Why: ${guide.why}")
            if (guide.examples.isNotEmpty()) {
                append("\n  Examples:")
                guide.examples.take(2).forEach { append("\n    - $it") }
            }
            append("\n  Opt-out: ${guide.optOut}")
        }
    }
}

/**
 * The verification layer.
 *
 * Conscience is a critic, not a cop. It points out contradictions and violations of generative
 * constraints, while leaving aesthetic order, density, prominence, repetition, ornament, and
 * controlled chaos to the product.
 */
object Conscience {

    private val employmentGuide = RuleGuide(
        rule = "A working element does at least four distinct jobs.",
        why = "The constraint forces one-purpose chrome to be reimagined as richer, more useful interface objects.",
        examples = listOf(
            "A submit control can Invite, Report, Progress, and Interrupt from the same element.",
            "A record can Identify, Locate, Navigate, and Report instead of splitting those jobs across extra chrome.",
        ),
        optOut = "Declare Employment.Ambient when the element is intentionally non-operational: ground, texture, breathing room, ornament, or atmosphere.",
    )

    private val gateGuide = RuleGuide(
        rule = "A resolvable blocker names where the person can resolve it.",
        why = "A blocked act should escort toward something useful instead of becoming an inert disabled control or a dead end.",
        examples = listOf(
            "Gate(\"recipient.chosen\", livesAt = recipientField) { recipient != null }",
            "A permissions gate can live at the permission control that can actually satisfy it.",
        ),
        optOut = "If nothing the person can currently do can satisfy the condition, do not model it as a Gate; render it as status/content or another non-inviting element instead.",
    )

    fun audit(product: Product): List<Finding> = product.surfaces.flatMap { audit(it) }

    fun audit(surface: Surface): List<Finding> = buildList {
        addAll(deadEnds(surface))
    }

    fun audit(frame: AuditFrame): List<Finding> = buildList {
        addAll(idleWorkers(frame))
        addAll(deadEnds(frame))
    }

    /**
     * The static model cannot contain an under-employed Working element because Employment.Working
     * enforces the four-job creative constraint at construction. A live frame can still expose a
     * custom-rendered element that bypassed that declaration, so the auditor keeps watch there.
     */
    private fun idleWorkers(frame: AuditFrame): List<Finding> {
        val underEmployed = frame.elements.filter { !it.ambient && it.jobs.size < 4 }
        if (underEmployed.isEmpty()) return emptyList()
        return listOf(
            Finding(
                audit = Audit.IdleWorker,
                severity = Severity.Warning,
                where = frame.surface,
                because = "${underEmployed.size} working element(s) are doing fewer than four jobs: " +
                    underEmployed.joinToString { "${it.id.value} (${it.jobs.size})" },
                instead = "Reimagine each element until it honestly does four jobs, or declare it Ambient if it is not operational chrome.",
                guide = employmentGuide,
            ),
        )
    }

    /**
     * A gate whose advertised resolver is absent is worth pointing out because the escort cannot
     * complete as described. This remains a warning: dynamic products may intentionally resolve the
     * condition elsewhere or compose the target later.
     */
    private fun deadEnds(surface: Surface): List<Finding> {
        val present = surface.elements.map { it.id }.toSet()
        return surface.gates.filter { it.livesAt !in present }.map { gate ->
            Finding(
                audit = Audit.DeadEnd,
                severity = Severity.Warning,
                where = "${surface.name}/${gate.id}",
                because = "its declared resolver ${gate.livesAt.value} is not on this surface",
                instead = "Expose the declared resolver, point the gate at something reachable, or stop modelling an unresolvable condition as a Gate.",
                guide = gateGuide,
            )
        }
    }

    private fun deadEnds(frame: AuditFrame): List<Finding> {
        val present = frame.elements.map { it.id }.toSet()
        return frame.gateAddresses.filter { it !in present }.map { address ->
            Finding(
                audit = Audit.DeadEnd,
                severity = Severity.Warning,
                where = "${frame.surface}/${address.value}",
                because = "its declared resolver did not compose in this frame",
                instead = "Expose the declared resolver, defer the Gate until it exists, or represent the condition as non-inviting status if it cannot be resolved here.",
                guide = gateGuide,
            )
        }
    }

    /** Whether any finding represents a proven build-stopping problem. */
    fun blocks(findings: List<Finding>): Boolean = findings.any { it.severity == Severity.Error }
}
