package com.hereliesaz.conveyance.demo

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hereliesaz.conveyance.Rank

/**
 * The channel assignment, written out.
 *
 * One clarification the framework needed and did not state plainly enough: Channel Economy governs
 * the interface's own vocabulary, not the content it holds. A photograph may be any colour it likes,
 * because its colour is the subject rather than a signal. What may not vary freely is chrome — the
 * parts of the screen that are the product talking.
 */
object Look {
    val ground = Color(0xFF0B0C10)
    val ink = Color(0xFFF4F5F8)
    val quiet = Color(0xFF6E7482)

    /** Hue carries semantic rank, and rank alone. One primary per surface. */
    fun rank(rank: Rank): Color = when (rank) {
        Rank.Primary -> Color(0xFFFFC24B)
        Rank.Secondary -> Color(0xFF2A2F3A)
        Rank.Tertiary -> Color(0xFF171A21)
    }

    /** Chroma carries heat: how much has recently gone this way. */
    fun heat(fraction: Float): Color =
        Color(0xFFFFC24B).copy(alpha = (0.10f + 0.75f * fraction).coerceIn(0f, 1f))

    /**
     * Content, not chrome. Each photograph gets its own light so the tray reads as a set of
     * particular things rather than a column of placeholders — which was the previous demo's real
     * failure: rows nobody could care about, carrying squares nobody could name.
     */
    fun photograph(seed: Int): Brush {
        val palettes = listOf(
            listOf(Color(0xFFEF6C5A), Color(0xFF8E2E58), Color(0xFF2B1B3D)),
            listOf(Color(0xFF6FD3C7), Color(0xFF2B7A9B), Color(0xFF16304A)),
            listOf(Color(0xFFF3C969), Color(0xFFCC7A2B), Color(0xFF4A2418)),
            listOf(Color(0xFFA8D06B), Color(0xFF3E8B5A), Color(0xFF13301F)),
            listOf(Color(0xFFB79BE8), Color(0xFF5C4A9B), Color(0xFF1E1A33)),
            listOf(Color(0xFF8FB4FF), Color(0xFF2F5BA8), Color(0xFF121C33)),
        )
        return Brush.linearGradient(palettes[seed.mod(palettes.size)])
    }
}
