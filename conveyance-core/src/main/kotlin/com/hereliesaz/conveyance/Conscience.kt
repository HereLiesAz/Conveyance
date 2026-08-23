package com.hereliesaz.conveyance

/** Which rule a finding is about. */
enum class Audit {
    IdleWorker,
    Instruction,
    Teleport,
    PrimaryContention,
    KeystoneBudget,
    Channel,
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
 * Five of the framework's ten rules cannot be violated at all, because they are enforced by types
 * rather than checked here: a destruction has no inverse-less constructor, a gate has no
 * address-less constructor, a place has no origin-less constructor outside [Place.root], product
 * code cannot name a spring, and an element cannot claim fewer than two jobs. This class covers the
 * rest — the ones that are judgements about a whole surface, which no single constructor can see.
 */
object Conscience {

    /** Words that mark a string as telling a person what to do rather than naming a thing. */
    private val instructionMarkers = setOf(
        "tap", "click", "press", "swipe", "drag", "select", "choose", "enter",
        "please", "simply", "just", "now", "here", "your", "you",
    )

    fun audit(product: Product): List<Finding> =
        keystoneBudget(product) + product.surfaces.flatMap { audit(it) }

    fun audit(surface: Surface): List<Finding> = buildList {
        addAll(idleWorkers(surface))
        addAll(instruction(surface))
        addAll(primaryContention(surface))
        addAll(deadEnds(surface))
        addAll(channels(surface))
        addAll(teleports(surface))
    }

    /**
     * Elements that are on screen doing nothing.
     *
     * [Employment.Working] already refuses fewer than two jobs at construction, so what is left to
     * check is the exemption: how many things a surface has declared Ambient.
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
                instead = "Give each one two jobs, merge it with a neighbour, or delete it.",
            ),
        )
    }

    /**
     * Text that explains the interface instead of naming things in it.
     *
     * The operational test is that chrome strings are nouns or verbs, not sentences. Four words is
     * the practical threshold; needing a fifth is a design problem being papered over, and the paper
     * is the tell.
     */
    private fun instruction(surface: Surface): List<Finding> =
        surface.elements.flatMap { element ->
            element.chrome.mapNotNull { text -> instructionFinding(surface, element, text) }
        }

    private fun instructionFinding(surface: Surface, element: DeclaredElement, text: String): Finding? {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val tooLong = words.size > 4
        val sentence = text.trim().lastOrNull() in setOf('.', '!', '?')
        val tells = words.any { it.trim(',', '.', '!', '?').lowercase() in instructionMarkers }
        if (!tooLong && !sentence && !tells) return null

        val because = buildList {
            if (tooLong) add("${words.size} words")
            if (sentence) add("ends as a sentence")
            if (tells) add("tells the person what to do")
        }.joinToString(", ")

        return Finding(
            audit = Audit.Instruction,
            severity = if (tooLong || sentence) Severity.Error else Severity.Warning,
            where = "${surface.name}/${element.id.value}",
            because = "\"$text\" — $because",
            instead = "Name the thing or the act. If behaviour cannot carry the meaning, the design " +
                "needs changing rather than captioning.",
        )
    }

    /** Two primaries is two answers to "what should I do here". */
    private fun primaryContention(surface: Surface): List<Finding> {
        val primaries = surface.elements.filter { it.rank == Rank.Primary }
        if (primaries.size <= 1) return emptyList()
        return listOf(
            Finding(
                audit = Audit.PrimaryContention,
                severity = Severity.Error,
                where = surface.name,
                because = "${primaries.size} primary elements: ${primaries.joinToString { it.id.value }}",
                instead = "Keep one primary. The rest are secondary alternatives or tertiary ambient.",
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

    /** A channel saying something it was not assigned to say. */
    private fun channels(surface: Surface): List<Finding> =
        surface.elements.flatMap { element ->
            element.channels.mapNotNull { (channel, meaning) ->
                if (channel.carries == meaning) return@mapNotNull null
                Finding(
                    audit = Audit.Channel,
                    severity = Severity.Error,
                    where = "${surface.name}/${element.id.value}",
                    because = "$channel is being used for $meaning, but it carries ${channel.carries}",
                    instead = "Say $meaning with ${Channel.carrying(meaning)}, or say nothing with " +
                        "$channel and leave it plain.",
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

    /**
     * Scarcity is the mechanism, not a side effect.
     *
     * A product where everything sings is a product where nothing is emphasised, which is a product
     * that has spent its most expensive channel on decoration.
     */
    private fun keystoneBudget(product: Product): List<Finding> {
        val count = product.keystones.size
        if (count in 1..3) return emptyList()
        return listOf(
            Finding(
                audit = Audit.KeystoneBudget,
                severity = Severity.Error,
                where = product.name,
                because = if (count == 0) "no keystone declared" else "$count keystones declared",
                instead = "Declare between one and three. The scarcity is what makes them land.",
            ),
        )
    }

    /** Whether a set of findings should stop a build. Warnings report; errors block. */
    fun blocks(findings: List<Finding>): Boolean = findings.any { it.severity == Severity.Error }
}
