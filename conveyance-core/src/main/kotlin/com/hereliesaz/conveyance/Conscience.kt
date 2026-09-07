package com.hereliesaz.conveyance

/** Which kind of design observation a finding is about. */
enum class Audit {
    IdleWorker,
    DeadEnd,
}

/**
 * Findings are advisory by default. Error is reserved for states the framework can prove are
 * internally incoherent or unsafe, not for breaking a preferred composition rule.
 */
enum class Severity { Error, Warning }

/**
 * Documentation attached to every lint finding.
 *
 * The log stays short and sends the designer to the rule's own section for examples, ideas, and the
 * precise semantic opt-out. Conveyance should convey without turning a build log into a manual.
 */
data class RuleGuide(
    val docs: String,
)

/**
 * One observation produced by the Conscience.
 *
 * [guide] is mandatory so every lint rule has a direct route to its explanation and opt-out.
 */
data class Finding(
    val audit: Audit,
    val severity: Severity,
    val where: String,
    val because: String,
    val instead: String,
    val guide: RuleGuide,
) {
    override fun toString(): String = buildString {
        append("[$severity] $audit at $where\n")
        append("Found: $because\n")
        append("Try: $instead\n")
        append("Examples, ideas, and opt-out: ${guide.docs}")
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

    val employmentGuide = RuleGuide(
        docs = "https://github.com/HereLiesAz/Conveyance/blob/main/docs/RULES-AND-OPTOUTS.md#employment",
    )

    val gateGuide = RuleGuide(
        docs = "https://github.com/HereLiesAz/Conveyance/blob/main/docs/RULES-AND-OPTOUTS.md#gates",
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
     *
     * This audit reasons across the whole surface before issuing findings. If several idle workers
     * are spatially and behaviourally related, it recommends one consolidation rather than ticketing
     * each fragment independently. When their combined jobs match a shipped SDK primitive, the
     * recommendation names that composable directly.
     */
    private fun idleWorkers(frame: AuditFrame): List<Finding> {
        val underEmployed = frame.elements.filter { !it.ambient && it.jobs.size < 4 }
        if (underEmployed.isEmpty()) return emptyList()

        val suggestions = ConsolidationAdvisor.suggest(frame)
        val covered = suggestions.flatMapTo(mutableSetOf()) { it.elements }

        val findings = suggestions.map { suggestion ->
            val because = suggestion.elements.joinToString(
                prefix = "related under-employed elements ",
                separator = ", ",
            ) { id ->
                val element = frame.elements.first { it.id == id }
                "${id.value} (${element.jobs.size})"
            }

            Finding(
                audit = Audit.IdleWorker,
                severity = Severity.Warning,
                where = frame.surface,
                because = because,
                instead = suggestion.message(),
                guide = employmentGuide,
            )
        }.toMutableList()

        val leftovers = underEmployed.filter { it.id !in covered }
        leftovers.forEach { element ->
            findings += Finding(
                audit = Audit.IdleWorker,
                severity = Severity.Warning,
                where = frame.surface,
                because = "${element.id.value} is doing ${element.jobs.size} jobs",
                instead = "Reimagine it until it honestly does four jobs. Enrich interface objects.",
                guide = employmentGuide,
            )
        }

        return findings
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
                instead = "Expose the resolver or rethink the blocker.",
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
                instead = "Expose the resolver or rethink the blocker.",
                guide = gateGuide,
            )
        }
    }

    /** Whether any finding represents a proven build-stopping problem. */
    fun blocks(findings: List<Finding>): Boolean = findings.any { it.severity == Severity.Error }
}
