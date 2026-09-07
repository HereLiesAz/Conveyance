package com.hereliesaz.conveyance

/** Which kind of design observation a finding is about. */
enum class Audit {
    IdleWorker,
    DeadEnd,
    HeroicSaturation,
}

/**
 * Findings are advisory by default. Error is reserved for states the framework can prove are
 * internally incoherent or unsafe, not for breaking a preferred composition rule.
 */
enum class Severity { Error, Warning }

/** Documentation attached to every lint finding. */
data class RuleGuide(
    val docs: String,
)

/** One observation produced by the Conscience. */
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
 * Conscience should prefer useful relationships over isolated scolding. It can still point at one
 * broken promise, but when several weak objects are fragments of one richer object, or several
 * emphasis tokens only make sense in contrast with one another, the finding should describe the
 * system the developer actually needs to rethink.
 */
object Conscience {

    val employmentGuide = RuleGuide(
        docs = "https://github.com/HereLiesAz/Conveyance/blob/main/docs/RULES-AND-OPTOUTS.md#employment",
    )

    val gateGuide = RuleGuide(
        docs = "https://github.com/HereLiesAz/Conveyance/blob/main/docs/RULES-AND-OPTOUTS.md#gates",
    )

    val emphasisGuide = RuleGuide(
        docs = "https://github.com/HereLiesAz/Conveyance/blob/main/docs/RULES-AND-OPTOUTS.md#act-emphasis",
    )

    fun audit(product: Product): List<Finding> = product.surfaces.flatMap { audit(it) }

    fun audit(surface: Surface): List<Finding> = buildList {
        addAll(deadEnds(surface))
    }

    fun audit(frame: AuditFrame): List<Finding> = buildList {
        addAll(idleWorkers(frame))
        addAll(heroicSaturation(frame))
        addAll(deadEnds(frame))
    }

    /**
     * Find under-employed fragments, then reason across the surface before issuing findings.
     *
     * If several idle workers are spatially and behaviourally related, recommend one consolidation
     * rather than ticketing each fragment independently. When their combined jobs match a shipped
     * SDK primitive, name that composable directly.
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

        underEmployed.filter { it.id !in covered }.forEach { element ->
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
     * Heroic is meaningful through contrast, not through a global numeric budget.
     *
     * Two heroic acts can be exactly right. Ten can be exactly right. The only state this audit can
     * prove has erased the distinction is a live surface where every visible offered act claims the
     * maximum emphasis. That is worth a question, not a constructor failure.
     */
    private fun heroicSaturation(frame: AuditFrame): List<Finding> {
        val offered = frame.elements.filter { it.visible && it.act != null && it.emphasis != null }
        if (offered.size < 2 || offered.any { it.emphasis != ActEmphasis.Heroic }) return emptyList()

        return listOf(
            Finding(
                audit = Audit.HeroicSaturation,
                severity = Severity.Warning,
                where = frame.surface,
                because = "every visible act is Heroic, so no act is allowed to recede",
                instead = "Keep Heroic where you want to engineer the strongest moment; let surrounding acts use quieter emphasis tokens so the distinction can be learned.",
                guide = emphasisGuide,
            ),
        )
    }

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

    fun blocks(findings: List<Finding>): Boolean = findings.any { it.severity == Severity.Error }
}
