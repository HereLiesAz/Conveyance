package com.hereliesaz.conveyance.demo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.hereliesaz.conveyance.compose.Motion
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.element
import com.hereliesaz.conveyance.compose.tell
import com.hereliesaz.conveyance.compose.yielding
import kotlinx.coroutines.delay

private val tray = ElementId("tray")

data class Person(val id: String, val initial: String) {
    val element: ElementId get() = ElementId("person:$id")
}

private val people = listOf(Person("mara", "M"), Person("ines", "I"), Person("otto", "O"))

/**
 * A tray of photographs and three people to send them to.
 *
 * The domain matters more than it looks like it should. The previous demo was a list of rows called
 * "document.1" carrying unlabelled grey squares — nothing a person could want, done to nothing a
 * person could recognise, by controls that named nothing. Conveyance cannot be demonstrated in a
 * world with no stakes and no objects, because there are no rules to teach.
 *
 * Here the destination is visible. That single fact is what makes Send mean anything: the photograph
 * leaves your hands and arrives somewhere you can point at, and the person it went to warms up. No
 * caption explains that, and none could explain it as well.
 */
@Composable
fun Gallery(initial: Int = 4) {
    ConveyanceHost(modifier = Modifier.background(Look.ground)) {
        val photographs = remember {
            mutableStateListOf(*(1..initial).map { SubjectId("photo.$it") }.toTypedArray())
        }
        var minted by remember { mutableStateOf(initial) }
        var chosen by remember { mutableStateOf<Person?>(null) }
        val received = remember { mutableStateMapOf<String, Int>() }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                people.forEach { person ->
                    Face(
                        person = person,
                        chosen = chosen == person,
                        heat = (received[person.id] ?: 0) / 4f,
                        onChoose = { chosen = person },
                    )
                }
            }

            val create = Act.create("photo.new", SubjectId("photo.${minted + 1}"), into = tray) {
                minted += 1
                photographs += SubjectId("photo.$minted")
                Outcome.Done
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Collection(
                    items = photographs.toList(),
                    creator = create,
                    key = { it },
                    modifier = Modifier.fillMaxSize(),
                    creatorContent = { Shutter(this) },
                ) { subject ->
                    val recipient = chosen
                    val send = Act.send(
                        id = "send.${subject.value}",
                        subject = subject,
                        to = recipient?.element ?: people.first().element,
                        scope = Scope.Item,
                        requires = listOf(
                            Gate("recipient.chosen", livesAt = people.first().element) { recipient != null },
                        ),
                    ) {
                        delay(260)
                        photographs -= subject
                        recipient?.let { received[it.id] = (received[it.id] ?: 0) + 1 }
                        Outcome.Done
                    }
                    Offer(send) { Photograph(subject, this) }
                }
            }
        }
    }
}

/**
 * A person, and the only destination a photograph has.
 *
 * Three jobs, which is what earns it its place: it identifies who this is, it invites you to choose
 * them, and its warmth reports how much has already gone their way. Choosing is a shape change,
 * because shape carries state.
 */
@Composable
private fun Face(person: Person, chosen: Boolean, heat: Float, onChoose: () -> Unit) {
    val engaged by animateFloatAsState(
        targetValue = if (chosen) 1f else 0f,
        animationSpec = Motion.spec(com.hereliesaz.conveyance.Weight.Light),
        label = "chosen",
    )
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(percent = (50 - engaged * 18f).toInt()))
            .background(if (heat > 0f) Look.heat(heat) else Look.rank(Rank.Secondary))
            .clickable { onChoose() }
            .element(person.element),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = person.initial,
            color = if (heat > 0.35f) Look.ground else Look.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * A photograph: the thing itself, and the act of sending it, in one element.
 *
 * There is no separate send button, no overflow menu and no grey square whose job you have to guess.
 * The photograph is what you touch, and touching it is what sends it — so the element does three
 * jobs at once, which is the only reason it is allowed on screen.
 */
@Composable
private fun Photograph(subject: SubjectId, scope: ActScope) {
    val seed = subject.value.substringAfterLast('.').toIntOrNull() ?: 0
    val blocked = scope.state is ActState.Blocked
    Box(
        modifier = Modifier
            .padding(vertical = 7.dp)
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .yielding(scope.yielding, scope.weight)
            .tell(scope.owesTell, scope.weight)
            .clip(RoundedCornerShape(if (blocked) 20.dp else 14.dp))
            .background(Look.photograph(seed))
            .clickable { scope.engage() },
    )
}

/**
 * The way new photographs arrive.
 *
 * It is a circle because it is an aperture, and it starts in the middle of an empty tray because
 * when there is nothing here, taking something is the only thing to do. Once there is something, it
 * moves to the corner and stays there.
 */
@Composable
private fun Shutter(scope: ActScope) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .tell(scope.owesTell, scope.weight)
            .clip(CircleShape)
            .background(Look.rank(Rank.Primary))
            .clickable { scope.engage() },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Look.ground.copy(alpha = 0.22f)))
    }
}
