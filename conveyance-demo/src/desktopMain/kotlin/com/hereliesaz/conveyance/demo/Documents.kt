package com.hereliesaz.conveyance.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.Text
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
import com.hereliesaz.conveyance.compose.LocalElements
import com.hereliesaz.conveyance.compose.LocalGhosts
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.element
import com.hereliesaz.conveyance.compose.tell
import com.hereliesaz.conveyance.compose.yielding
import kotlinx.coroutines.delay

private val list = ElementId("documents")
private val recipientField = ElementId("recipient.field")
private val outbox = ElementId("outbox")

/**
 * Every string on this screen is a noun or a verb. There is no sentence anywhere, no tooltip, no
 * empty-state paragraph, no confirmation, and no toast — not because they were removed in a tidying
 * pass, but because the framework provides nothing to build them out of.
 */
@Composable
fun Documents() {
    ConveyanceHost {
        val documents = remember { mutableStateListOf<SubjectId>() }
        var minted by remember { mutableStateOf(0) }
        var recipient by remember { mutableStateOf<String?>(null) }

        val ghosts = LocalGhosts.current
        val elements = LocalElements.current

        Column(
            modifier = Modifier.fillMaxSize().background(Palette.ground).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Documents", color = Palette.ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)

            val create = Act.create(
                id = "document.new",
                subject = SubjectId("document.${minted + 1}"),
                into = list,
                scope = Scope.Item,
            ) {
                minted += 1
                documents += SubjectId("document.$minted")
                Outcome.Done
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Collection(
                    items = documents.toList(),
                    creator = create,
                    key = { it },
                    modifier = Modifier.fillMaxSize(),
                    ghost = { residue ->
                        // The Ghost sits in the slot its subject held. Elevated, because elevation
                        // means reversible and this is the most reversible thing on the screen.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Palette.heat(0.35f))
                                .clickable { ghosts.recover(residue.subject)?.let { documents += residue.subject } }
                                .element(ElementId("ghost:${residue.subject.value}")),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.padding(horizontal = 16.dp).size(18.dp).clip(RoundedCornerShape(9.dp)).background(Palette.of(Rank.Primary)))
                            Text(residue.subject.value, color = Palette.quiet, fontSize = 14.sp)
                        }
                    },
                    creatorContent = { Creator(this) },
                ) { subject ->
                    DocumentRow(
                        subject = subject,
                        onDestroy = { act ->
                            ghosts.leave(act, at = list)
                            documents -= subject
                        },
                    )
                }
            }

            // The gate's address. It sits where it always sits; the escort comes to it.
            RecipientField(chosen = recipient, articulating = elements.articulating == recipientField) {
                recipient = "someone"
            }

            val send = Act.send(
                id = "documents.send",
                subject = SubjectId("documents"),
                to = outbox,
                scope = Scope.Collection,
                requires = listOf(Gate("recipient.chosen", livesAt = recipientField) { recipient != null }),
            ) {
                delay(900)
                Outcome.Done
            }
            Offer(send, modifier = Modifier.fillMaxWidth()) { Send(this) }
        }
    }
}
