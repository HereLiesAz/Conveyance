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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Gate
import com.hereliesaz.conveyance.Outcome
import com.hereliesaz.conveyance.Place
import com.hereliesaz.conveyance.Rank
import com.hereliesaz.conveyance.Scope
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.ConveyanceHost
import com.hereliesaz.conveyance.compose.LocalPlaces
import com.hereliesaz.conveyance.compose.Motion
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.Places
import com.hereliesaz.conveyance.compose.subjectElement
import com.hereliesaz.conveyance.compose.tell
import com.hereliesaz.conveyance.compose.yielding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
fun Gallery(
    initial: Int = 4,
    /** Passed only by the audit harness, so a screen can be graded from outside. */
    registry: com.hereliesaz.conveyance.compose.ElementRegistry? = null,
) {
    ConveyanceHost(modifier = Modifier.background(Look.ground), registry = registry) {
        val photographs = remember {
            mutableStateListOf(*(1..initial).map { SubjectId("photo.$it") }.toTypedArray())
        }
        var minted by remember { mutableStateOf(initial) }
        var chosen by remember { mutableStateOf<Person?>(null) }
        val received = remember { mutableStateMapOf<String, Int>() }

        Places(root = Place.root("tray")) { place ->
            val viewing = place.subject
            if (viewing != null) {
                Detail(
                    subject = viewing,
                    chosen = chosen,
                    received = received,
                    onChoose = { chosen = it },
                    onSent = { person ->
                        photographs -= viewing
                        received[person.id] = (received[person.id] ?: 0) + 1
                    },
                )
            } else {
                Tray(
                    photographs = photographs,
                    onCreate = {
                        minted += 1
                        photographs += SubjectId("photo.$minted")
                    },
                    nextName = { SubjectId("photo.${minted + 1}") },
                )
            }
        }
    }
}

/**
 * Everything a person has, and the way to get more.
 *
 * There is nothing else here. No faces, no toolbar, no header saying "Gallery" — the photographs
 * are the gallery, and taking one is the only other thing there is to do. Sending lives one touch
 * away, in the place where its cost can be seen before it is paid.
 */
@Composable
private fun Tray(
    photographs: List<SubjectId>,
    onCreate: () -> Unit,
    nextName: () -> SubjectId,
) {
    val create = Act.create("photo.new", nextName(), into = tray) {
        onCreate()
        Outcome.Done
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp)) {
        Collection(
            items = photographs,
            creator = create,
            key = { it },
            modifier = Modifier.fillMaxSize(),
            creatorContent = { Shutter(this) },
        ) { subject ->
            // Touching a photograph goes to it. Sending happens there, where the stakes can be
            // shown before they are taken -- which is the whole difference between an interface
            // that omits a necessary detail and one that does not.
            val open = Act.enter(
                id = "open.${subject.value}",
                place = Place.from(
                    id = subject.value,
                    origin = subjectElement(subject),
                    subject = subject,
                ),
            )
            Offer(open, element = subjectElement(subject)) { Photograph(subject, this) }
        }
    }
}

/**
 * One photograph, and the people it can go to.
 *
 * The place a person arrives at after touching a photograph, and the only place sending exists. It
 * is here rather than in the tray because the tray showed no sign that sending is permanent — the
 * omission the framework's own audit flagged — and a cost has to be visible before it is taken.
 *
 * The photograph is still the thing you touch to send it: there is no send button, because the
 * subject and the act on it are one element. Touching it before choosing anyone does not refuse in
 * place; it leans toward the people and carries you to them.
 *
 * Returning is the surrounding ground. Whether that is discoverable is exactly the sort of question
 * the Prediction Test is for, and it is an honest place to find out.
 */
@Composable
private fun Detail(
    subject: SubjectId,
    chosen: Person?,
    received: Map<String, Int>,
    onChoose: (Person) -> Unit,
    onSent: (Person) -> Unit,
) {
    val places = LocalPlaces.current
    val leaving = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Look.ground)
            .clickable { leaving.launch { places?.back() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val send = Act.send(
                id = "send.${subject.value}",
                subject = subject,
                to = (chosen ?: people.first()).element,
                scope = Scope.Item,
                requires = listOf(
                    // The gate knows where it is answered, which is what lets a refusal become a
                    // journey instead of a sign.
                    Gate("recipient.chosen", livesAt = people.first().element) { chosen != null },
                ),
            ) {
                delay(240)
                chosen?.let(onSent)
                Outcome.Done
            }

            Offer(send, element = subjectElement(subject)) { Portrait(subject, this) }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                people.forEach { person ->
                    // Choosing a recipient is an act, and has to be declared as one. The framework
                    // caught this: the faces were tappable but modelled as nothing, so the census
                    // reported an invitation with no act behind it and faces accounted for by
                    // nobody. A control the model does not know about cannot be conveyed, graded,
                    // or undone.
                    val choose = Act.alter(
                        id = "recipient.choose.${person.id}",
                        subject = SubjectId("recipient"),
                        property = "recipient",
                        target = person.element,
                        scope = Scope.Detail,
                        // Choosing someone else puts it back, so this costs nothing and should
                        // feel like it costs nothing.
                        inverse = Act.alter(
                            id = "recipient.clear.${person.id}",
                            subject = SubjectId("recipient"),
                            property = "recipient",
                            target = person.element,
                        ),
                    ) {
                        onChoose(person)
                        Outcome.Done
                    }
                    Offer(choose, element = person.element) {
                        Face(
                            person = person,
                            chosen = chosen == person,
                            heat = (received[person.id] ?: 0) / 4f,
                            scope = this,
                        )
                    }
                }
            }

            // The stakes, shown before they are taken rather than discovered afterwards. Nothing
            // here explains the interface; it states what the act costs, which is the necessary
            // detail the tray omitted.
            Text("Once", color = Look.quiet, fontSize = 13.sp)
        }
    }
}

/**
 * The photograph at the size it deserves, and the act of sending it.
 *
 * It takes over the subject's address from the thumbnail it grew out of, so a Send leaves from the
 * picture the person is actually looking at, and Return still finds its way back to the tray.
 */
@Composable
private fun Portrait(subject: SubjectId, scope: ActScope) {
    val seed = subject.value.substringAfterLast('.').toIntOrNull() ?: 0
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .yielding(scope.yielding, scope.weight)
            .tell(scope.owesTell, scope.weight)
            .clip(RoundedCornerShape(18.dp))
            .background(Look.photograph(seed))
            .clickable { scope.engage() },
    )
}

/**
 * A person, and the only destination a photograph has.
 *
 * Three jobs, which is what earns it its place: it identifies who this is, it invites you to choose
 * them, and its warmth reports how much has already gone their way. Choosing is a shape change,
 * because shape carries state.
 */
@Composable
private fun Face(person: Person, chosen: Boolean, heat: Float, scope: ActScope) {
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
            .tell(scope.owesTell, scope.weight)
            .clickable { scope.engage() },
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
 * A photograph in the tray: the thing itself, and the way into it.
 *
 * There is no overflow menu, no chevron and no grey square whose job you have to guess. The
 * photograph is what you touch, and touching it becomes the place you are in — so the element does
 * three jobs at once, which is the only reason it is allowed on screen.
 */
@Composable
private fun Photograph(subject: SubjectId, scope: ActScope) {
    val seed = subject.value.substringAfterLast('.').toIntOrNull() ?: 0
    Box(
        modifier = Modifier
            .padding(vertical = 7.dp)
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .tell(scope.owesTell, scope.weight)
            .clip(RoundedCornerShape(14.dp))
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
