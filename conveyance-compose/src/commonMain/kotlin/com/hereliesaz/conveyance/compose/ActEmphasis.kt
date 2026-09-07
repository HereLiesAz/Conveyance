package com.hereliesaz.conveyance.compose

import com.hereliesaz.conveyance.ActEmphasis

/**
 * The semantic emphasis token of the act this scope is rendering.
 *
 * Compose already has the theming machinery. Conveyance therefore does not invent another visual
 * token system here; it exposes the semantic token directly so a product's existing theme can map
 * Heroic, Primary, Secondary, and Supporting onto whatever combination of shape, typography,
 * colour, motion, space, haptics, sound, or surrounding response belongs to that design language.
 */
val ActScope.emphasis: ActEmphasis get() = act.emphasis
