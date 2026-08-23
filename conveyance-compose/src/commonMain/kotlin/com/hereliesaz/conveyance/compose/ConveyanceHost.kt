package com.hereliesaz.conveyance.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.hereliesaz.conveyance.Practice

/**
 * The root every Conveyance surface sits inside.
 *
 * It holds the two things that must be shared across an entire product rather than per screen: the
 * element registry, because an escort may need to travel to somewhere the current screen has never
 * heard of, and practice counts, because familiarity belongs to the person and not to a composition
 * that is thrown away on rotation.
 */
@Composable
fun ConveyanceHost(
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val elements = remember { ElementRegistry() }
    val practice = remember { Practice() }
    CompositionLocalProvider(
        LocalElements provides elements,
        LocalPractice provides practice,
        LocalReducedMotion provides reducedMotion,
        content = content,
    )
}

val LocalPractice = staticCompositionLocalOf { Practice() }

/**
 * Whether the person has asked for reduced motion.
 *
 * Read by the binding to pick a verb's reduced register, which drops traversal and keeps identity.
 * It is never read to decide *whether* to convey — only how.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
