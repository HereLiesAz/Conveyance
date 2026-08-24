package com.hereliesaz.conveyance.demo

import androidx.compose.ui.graphics.Color
import com.hereliesaz.conveyance.Rank

/**
 * The demo's channel assignment, written out because the framework requires every channel to carry
 * exactly one meaning and this is what that looks like in practice.
 *
 * Hue carries semantic rank and nothing else. There is no brand colour here, no accent chosen
 * because it looked well, and no second use of colour for category or mood — if a thing is coloured,
 * it is because of what it is for.
 */
object Palette {
    val ground = Color(0xFF14161A)
    val surface = Color(0xFF1D2026)
    val ink = Color(0xFFE7E9EE)
    val quiet = Color(0xFF8A909C)

    /** Rank, and only rank. One primary per surface. */
    fun of(rank: Rank): Color = when (rank) {
        Rank.Primary -> Color(0xFF6FA8FF)
        Rank.Secondary -> Color(0xFF3A414F)
        Rank.Tertiary -> Color(0xFF262B34)
    }

    /** Heat: how live or recent. Chroma, never hue. */
    fun heat(fraction: Float): Color =
        Color(0xFF6FA8FF).copy(alpha = 0.12f + 0.5f * fraction.coerceIn(0f, 1f))
}
