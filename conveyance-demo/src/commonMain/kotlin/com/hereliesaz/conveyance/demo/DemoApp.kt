package com.hereliesaz.conveyance.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The two independent demos this binary carries, and the switch between them.
 *
 * Which one is showing is app-shell chrome, not part of either surface's own conveyed content --
 * closer to which window is frontmost than to anything [Gallery] or [StyleShowcase] itself is
 * accountable for modeling. That is why the tab strip below is a plain click, not an [Act]: it
 * picks which single-surface demonstration runs, it does not act within one.
 */
@Composable
fun DemoApp() {
    var showcase by remember { mutableStateOf(false) }
    // Switching tabs unmounts whichever demo isn't showing -- without this, Gallery's own created
    // photographs, recipients, and scroll position (and Styles' own scroll position) would reset
    // every time a person switched away and back, which is real state loss a person would notice
    // and blame on the demo rather than on this app shell's own choice of navigation mechanism.
    val tabState = rememberSaveableStateHolder()
    Column(modifier = Modifier.fillMaxSize().background(Look.ground)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Tab(text = "Photos", selected = !showcase) { showcase = false }
            Tab(text = "Styles", selected = showcase) { showcase = true }
        }
        Box(Modifier.weight(1f)) {
            if (showcase) {
                tabState.SaveableStateProvider("styles") { StyleShowcase() }
            } else {
                tabState.SaveableStateProvider("photos") { Gallery() }
            }
        }
    }
}

@Composable
private fun Tab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Look.ink else Look.quiet,
        fontSize = 13.sp,
        modifier = Modifier
            .semantics { role = Role.Tab; this.selected = selected }
            .clickable(onClick = onClick),
    )
}
