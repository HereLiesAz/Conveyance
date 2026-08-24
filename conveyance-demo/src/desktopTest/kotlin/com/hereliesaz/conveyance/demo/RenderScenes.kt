package com.hereliesaz.conveyance.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Gate
import com.hereliesaz.conveyance.Outcome
import com.hereliesaz.conveyance.Rank
import com.hereliesaz.conveyance.Scope
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.ConveyanceHost
import com.hereliesaz.conveyance.compose.Ghosts
import com.hereliesaz.conveyance.compose.LocalGhosts
import com.hereliesaz.conveyance.compose.Offer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the framework's behaviours off-screen, from the real components.
 *
 * The spec is explicit that the only test that finally matters cannot be automated: hand it to
 * someone who has never seen it, say nothing, and watch. This is the next best thing — it makes the
 * behaviours lookable-at rather than merely asserted, and it runs without a display.
 */
class RenderScenes {

    private val out = File("build/scenes").apply { mkdirs() }
    private val list = ElementId("documents")
    private val recipientField = ElementId("recipient.field")
    private val creator = Act.create("document.new", SubjectId("document.next"), into = list) { Outcome.Done }

    private fun docs(n: Int) = (1..n).map { SubjectId("document.$it") }

    private fun render(name: String, frames: Int = 1, between: () -> Unit = {}, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = 920, height = 1440, density = Density(2f), content = content)
        try {
            var nanos = 0L
            repeat(frames) { frame ->
                if (frame > 0) between()
                // Advance well past any spring so what is captured is the settled result rather
                // than an arbitrary point on the way to it.
                nanos += 2_000_000_000L
                val image = scene.render(nanos)
                if (frame == frames - 1) {
                    File(out, "$name.png").writeBytes(image.encodeToData()!!.bytes)
                }
            }
        } finally {
            scene.close()
        }
        assertTrue(File(out, "$name.png").length() > 0, "$name rendered empty")
    }

    /**
     * The residue, drawn in the slot its subject held. Elevated and warm, because elevation means
     * reversible and this is the most reversible thing on the screen.
     */
    @Composable
    private fun GhostRow(subject: SubjectId) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.heat(0.35f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.padding(horizontal = 16.dp).size(18.dp)
                    .clip(RoundedCornerShape(9.dp)).background(Palette.of(Rank.Primary)),
            )
            Text(subject.value, color = Palette.quiet, fontSize = 14.sp)
        }
    }

    @Composable
    private fun Frame(content: @Composable ColumnScope.() -> Unit) {
        ConveyanceHost {
            Column(
                modifier = Modifier.fillMaxSize().background(Palette.ground).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("Documents", color = Palette.ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                content()
            }
        }
    }

    @Test
    fun `the migration, empty and filled`() {
        render("01-migration-empty") {
            Frame {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Collection(
                        items = emptyList<SubjectId>(),
                        creator = creator,
                        key = { it },
                        modifier = Modifier.fillMaxSize(),
                        creatorContent = { Creator(this) },
                    ) { }
                }
            }
        }

        // The same collection with content: the creation control has travelled to its home.
        render("02-migration-filled") {
            Frame {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Collection(
                        items = docs(3),
                        creator = creator,
                        key = { it },
                        modifier = Modifier.fillMaxSize(),
                        creatorContent = { Creator(this) },
                    ) { DocumentRow(it) {} }
                }
            }
        }
    }

    /**
     * Two frames, because a Ghost only holds a slot the collection has already seen occupied — which
     * is the whole point of it. Faking the residue into an empty collection would prove nothing.
     */
    @Test
    fun `the ghost holds the slot its subject had`() {
        val items = mutableStateListOf(*docs(3).toTypedArray())
        val ghosts = Ghosts()
        val doomed = SubjectId("document.2")
        val delete = Act.destroy(
            id = "document.delete",
            subject = doomed,
            target = list,
            inverse = Act.create("document.restore", doomed, into = list),
        )

        render(
            name = "03-ghost",
            frames = 2,
            between = {
                ghosts.leave(delete, at = list)
                items.remove(doomed)
            },
        ) {
            CompositionLocalProvider(LocalGhosts provides ghosts) {
                Frame {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Collection(
                            items = items.toList(),
                            creator = creator,
                            key = { it },
                            modifier = Modifier.fillMaxSize(),
                            ghost = { GhostRow(it.subject) },
                            creatorContent = { Creator(this) },
                        ) { DocumentRow(it) {} }
                    }
                }
            }
        }
    }

    @Test
    fun `blocked, then escorted`() {
        val chosen = mutableStateOf(false)
        val articulating = mutableStateOf(false)
        val send = Act.send(
            id = "documents.send",
            subject = SubjectId("documents"),
            to = ElementId("outbox"),
            scope = Scope.Collection,
            requires = listOf(Gate("recipient.chosen", livesAt = recipientField) { chosen.value }),
        )

        @Composable
        fun scene() = Frame {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Collection(
                    items = docs(3),
                    creator = creator,
                    key = { it },
                    modifier = Modifier.fillMaxSize(),
                    creatorContent = { Creator(this) },
                ) { DocumentRow(it) {} }
            }
            RecipientField(chosen = null, articulating = articulating.value) {}
            Offer(send, modifier = Modifier.fillMaxWidth()) { Send(this) }
        }

        render("04-blocked") { scene() }
        articulating.value = true
        render("05-escorted") { scene() }
    }

    @Test
    fun `yielding and settled, in the same element`() {
        val act = Act.send("documents.send", SubjectId("documents"), to = ElementId("outbox"), scope = Scope.Collection)

        @Composable
        fun scene(state: ActState) = Frame {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Collection(
                    items = docs(3),
                    creator = creator,
                    key = { it },
                    modifier = Modifier.fillMaxSize(),
                    creatorContent = { Creator(this) },
                ) { DocumentRow(it) {} }
            }
            RecipientField(chosen = "someone", articulating = false) {}
            Send(ActScope.pinned(act, state))
        }

        render("06-yielding") { scene(ActState.Yielding(0.35f)) }
        render("07-settled") { scene(ActState.Settled) }
    }
}
