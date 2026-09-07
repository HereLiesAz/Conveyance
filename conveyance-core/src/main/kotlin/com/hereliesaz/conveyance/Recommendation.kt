package com.hereliesaz.conveyance

import kotlin.math.hypot

/**
 * A composable-shaped replacement the linter knows how to recommend.
 *
 * This is metadata, not a rendering dependency: core can reason about the behaviour a shipped SDK
 * primitive absorbs without depending on Compose itself. The names intentionally match the public
 * composables offered by conveyance-compose.
 */
data class ComposableRecipe(
    val name: String,
    val absorbs: Set<Job>,
    val minFragments: Int = 2,
    val maxFragments: Int = 4,
    val docsAnchor: String,
)

/** The standard replacement vocabulary shipped by the Conveyance SDK. */
object ConveyanceRecipes {
    val Offer = ComposableRecipe(
        name = "Offer",
        absorbs = setOf(Job.Invite, Job.Progress, Job.Confirm, Job.Interrupt),
        docsAnchor = "offer",
    )

    val Form = ComposableRecipe(
        name = "Form",
        absorbs = setOf(Job.Group, Job.Report, Job.Progress, Job.Confirm),
        docsAnchor = "form",
    )

    val Collection = ComposableRecipe(
        name = "Collection",
        absorbs = setOf(Job.Group, Job.Identify, Job.Locate, Job.Report),
        docsAnchor = "collection",
    )

    val Places = ComposableRecipe(
        name = "Places",
        absorbs = setOf(Job.Locate, Job.Navigate, Job.Group, Job.Identify),
        docsAnchor = "places",
    )

    val all: List<ComposableRecipe> = listOf(Offer, Form, Collection, Places)
}

/** A surface-level suggestion derived from several elements together. */
data class ConsolidationSuggestion(
    val elements: List<ElementId>,
    val combinedJobs: Set<Job>,
    val replacement: ComposableRecipe? = null,
) {
    fun message(): String = if (replacement != null) {
        "Replace ${elements.joinToString { it.value }} with a single Conveyance ${replacement.name}; " +
            "together they already describe ${combinedJobs.joinToString()}."
    } else {
        "Combine ${elements.joinToString { it.value }} into one richer interface object; together they already do " +
            "${combinedJobs.size} jobs: ${combinedJobs.joinToString()}."
    }
}

/**
 * Looks across the whole surface instead of ticketing each under-employed element in isolation.
 *
 * Candidates must be spatially related and collectively reach the four-job threshold. Exact SDK
 * recipe matches win over generic consolidation, then smaller/tighter clusters win so advice stays
 * local and actionable.
 */
object ConsolidationAdvisor {
    fun suggest(
        frame: AuditFrame,
        recipes: List<ComposableRecipe> = ConveyanceRecipes.all,
    ): List<ConsolidationSuggestion> {
        val candidates = frame.elements.filter { !it.ambient && it.jobs.isNotEmpty() && it.jobs.size < 4 }
        if (candidates.size < 2) return emptyList()

        val groups = mutableListOf<List<AuditElement>>()
        for (size in 2..minOf(4, candidates.size)) {
            combinations(candidates, size).forEach { group ->
                val jobs = group.flatMapTo(mutableSetOf()) { it.jobs }
                if (jobs.size >= 4 && spatiallyRelated(group)) groups += group
            }
        }

        val suggestions = groups.map { group ->
            val jobs = group.flatMapTo(mutableSetOf()) { it.jobs }
            val recipe = recipes
                .filter { group.size in it.minFragments..it.maxFragments && jobs.containsAll(it.absorbs) }
                .maxByOrNull { it.absorbs.size }
            ConsolidationSuggestion(group.map { it.id }, jobs, recipe)
        }.sortedWith(
            compareByDescending<ConsolidationSuggestion> { it.replacement != null }
                .thenBy { it.elements.size },
        )

        val used = mutableSetOf<ElementId>()
        return buildList {
            suggestions.forEach { suggestion ->
                if (suggestion.elements.none { it in used }) {
                    add(suggestion)
                    used += suggestion.elements
                }
            }
        }
    }

    private fun spatiallyRelated(elements: List<AuditElement>): Boolean {
        val centers = elements.map { (it.left + it.width / 2f) to (it.top + it.height / 2f) }
        val scale = elements.map { maxOf(it.width, it.height, 1f) }.average().toFloat()
        val maxDistance = scale * 8f
        return centers.indices.all { a ->
            centers.indices.any { b ->
                a != b && hypot(
                    (centers[a].first - centers[b].first).toDouble(),
                    (centers[a].second - centers[b].second).toDouble(),
                ) <= maxDistance
            }
        }
    }

    private fun <T> combinations(items: List<T>, size: Int): List<List<T>> {
        val out = mutableListOf<List<T>>()
        fun walk(start: Int, current: MutableList<T>) {
            if (current.size == size) {
                out += current.toList()
                return
            }
            for (index in start until items.size) {
                current += items[index]
                walk(index + 1, current)
                current.removeAt(current.lastIndex)
            }
        }
        walk(0, mutableListOf())
        return out
    }
}
