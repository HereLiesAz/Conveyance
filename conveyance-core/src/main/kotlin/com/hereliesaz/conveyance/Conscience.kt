package com.hereliesaz.conveyance

/** Which rule a finding is about. */
enum class Audit {
    IdleWorker,
    Teleport,
    DeadEnd,
}

/**
 * Blocking or not.
 *
 * The split is not about how much a violation matters; it is about how sure the tool can be.
 * Conservative analysis cannot see every dynamic case, so anything it cannot prove is reported
 * rather than enforced — but it is still reported, because silence would be the more expensive lie.
 */
enum class Severity { Error, Warning }

/**
 * One thing the Conscience found.
 *
 * [instead] is required, and it is the whole reason this type is not just a string. A build error
 * that explains a philosophy is a tooltip, and this framework does not ship tooltips. A finding
 * names the compliant construction and gets out of the way — it escorts the developer to the fix,
 * exactly as a blocked act escorts a person to its gate.
 */
data class Finding(
    val audit: Audit,
    val severity: Severity,
    val where: String,
    val because: String,
    val instead: String,
) {
    override fun toString() = "[$severity] $audit at $where: $because -> $instead"
}

/**
 * The verification layer.
 *
 * Nine of the framework's rules cannot be violated at all, because they are enforced by types
 * rather than checked here: a destruction has no inverse-less constructor, a gate has no
 * address-less constructor, a place has no origin-less constructor outside [Place.root], product
 * code cannot name a spring, an element cannot claim fewer than four jobs, chrome text cannot become
 * a [Label] if it reads as an instruction, a surface cannot hold two primaries, a product cannot
 * declare zero or more than three keystones, and a channel cannot be declared against the wrong
 * meaning because there is no longer anywhere to write the wrong one down. This class covers what
 * is left — judgements about a whole surface that no single constructor can see.
 */
object Conscience {

    fun audit(product: Product): List<Finding> = product.surfaces.flatMap { audit(it) }

    fun audit(surface: Surface): List<Finding> = buildList {
        addAll(idleWorkers(surface))
        addAll(deadEnds(surface))
        addAll(teleports(surface))
    }

    /**
     * The live counterpart: the two checks a hand-declared [Surface] gets, run against a real
     * running app's own [AuditFrame] instead of a fixture nobody wired up.
     *
     * [teleports] has no live counterpart here -- it is a property of a surface's whole set of
     * possible entry places, not something any one running snapshot could ever show more than one
     * of at a time. That check stays exclusively static.
     */
    fun audit(frame: AuditFrame, ambientBudget: Int = 2): List<Finding> = buildList {
        addAll(idleWorkers(frame, ambientBudget))
        addAll(deadEnds(frame))
    }

    /**
     * Elements that are on screen doing nothing.
     *
     * [Employment.Working] already refuses fewer than four jobs at construction, so what is left
     * to check is the exemption: how many things a surface has declared Ambient.
     */
    private fun idleWorkers(surface: Surface): List<Finding> {
        val ambient = surface.elements.filter { it.employment == Employment.Ambient }
        if (ambient.size <= surface.ambientBudget) return emptyList()
        return listOf(
            Finding(
                audit = Audit.IdleWorker,
                severity = Severity.Warning,
                where = surface.name,
                because = "${ambient.size} ambient elements against a budget of ${surface.ambientBudget}: " +
                    ambient.joinToString { it.id.value },
                instead = "Give each one four jobs, merge it with a neighbour, or delete it.",
            ),
        )
    }

    /** A gate whose address is nowhere on the surface leaves the person with no way out. */
    private fun deadEnds(surface: Surface): List<Finding> {
        val present = surface.elements.map { it.id }.toSet()
        return surface.gates.filter { it.livesAt !in present }.map { gate ->
            Finding(
                audit = Audit.DeadEnd,
                severity = Severity.Warning,
                where = "${surface.name}/${gate.id}",
                because = "its address ${gate.livesAt.value} is not on this surface",
                instead = "Point livesAt at an element the escort can actually reach, or move the " +
                    "gate to the surface that owns its condition.",
            )
        }
    }

    /**
     * Elements on screen doing nothing, caught live rather than only at construction.
     *
     * A hand-declared [Surface] can never contain the second finding this reports --
     * [Employment.Working]'s own constructor already refuses fewer than four jobs, so an
     * under-resourced element simply cannot be built. A live [AuditFrame] carries no such
     * guarantee: it reports what actually rendered, and an element with fewer than four jobs and
     * no declared [AuditElement.ambient] exemption is exactly the violation [Audit.IdleWorker]
     * exists to name -- reachable here for the first time, against a real screen, rather than
     * only against a fixture someone remembered to write by hand.
     */
    private fun idleWorkers(frame: AuditFrame, ambientBudget: Int): List<Finding> = buildList {
        val ambient = frame.elements.filter { it.ambient }
        if (ambient.size > ambientBudget) {
            add(
                Finding(
                    audit = Audit.IdleWorker,
                    severity = Severity.Warning,
                    where = frame.surface,
                    because = "${ambient.size} ambient elements against a budget of $ambientBudget: " +
                        ambient.joinToString { it.id.value },
                    instead = "Give each one four jobs, merge it with a neighbour, or delete it.",
                ),
            )
        }
        val underResourced = frame.elements.filter { !it.ambient && it.jobs.size < 4 }
        if (underResourced.isNotEmpty()) {
            add(
                Finding(
                    audit = Audit.IdleWorker,
                    severity = Severity.Warning,
                    where = frame.surface,
                    because = "${underResourced.size} elements doing fewer than four jobs and not " +
                        "declared ambient: " +
                        underResourced.joinToString { "${it.id.value} (${it.jobs.size})" },
                    instead = "Give each one four jobs, declare it Employment.Ambient on purpose, " +
                        "or delete it.",
                ),
            )
        }
    }

    /** A gate whose address never actually composed leaves the person with no way out -- live. */
    private fun deadEnds(frame: AuditFrame): List<Finding> {
        val present = frame.elements.map { it.id }.toSet()
        return frame.gateAddresses.filter { it !in present }.map { address ->
            Finding(
                audit = Audit.DeadEnd,
                severity = Severity.Warning,
                where = "${frame.surface}/${address.value}",
                because = "its address never actually composed on this surface",
                instead = "Point livesAt at an element the escort can actually reach, or move the " +
                    "gate to the surface that owns its condition.",
            )
        }
    }

    /** More than one way in means the product has more than one beginning. */
    private fun teleports(surface: Surface): List<Finding> {
        val roots = surface.places.filter { it.isRoot }
        if (roots.size <= 1) return emptyList()
        return listOf(
            Finding(
                audit = Audit.Teleport,
                severity = Severity.Error,
                where = surface.name,
                because = "${roots.size} root places: ${roots.joinToString { it.id.value }}",
                instead = "Give all but the genuine entry point an origin element to grow out of.",
            ),
        )
    }

    /** Whether a set of findings should stop a build. Warnings report; errors block. */
    fun blocks(findings: List<Finding>): Boolean = findings.any { it.severity == Severity.Error }
}
