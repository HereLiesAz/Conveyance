package com.hereliesaz.conveyance.demo

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Outcome
import com.hereliesaz.conveyance.Rank
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Motion
import com.hereliesaz.conveyance.compose.element
import com.hereliesaz.conveyance.compose.tell
import com.hereliesaz.conveyance.compose.yielding

/**
 * The creation control. One element, four jobs: it invites, it identifies the space, it locates
 * where new things come from, and after the Migration it marks where it will live from now on.
 */
@Composable
fun Creator(scope: ActScope) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .tell(scope.owesTell, scope.weight)
            .clip(RoundedCornerShape(percent = 44))
            .background(Palette.of(Rank.Primary))
            .clickable { scope.engage() },
        contentAlignment = Alignment.Center,
    ) {
        Text("New", color = Palette.ground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/**
 * A row, and its own destruction.
 *
 * The corner radius is the shape channel doing its assigned job: settled at rest, articulated while
 * engaged. It is not a style.
 */
@Composable
fun DocumentRow(subject: SubjectId, onDestroy: (Act) -> Unit) {
    val restore = Act.create("document.restore", subject, into = ElementId("documents"))
    val delete = Act.destroy(
        id = "document.delete.${subject.value}",
        subject = subject,
        target = ElementId("documents"),
        inverse = restore,
    ) { Outcome.Done }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.of(Rank.Tertiary))
            .element(ElementId(subject.value)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.padding(horizontal = 16.dp).size(18.dp)
                .clip(RoundedCornerShape(5.dp)).background(Palette.of(Rank.Secondary)),
        )
        Text(subject.value, color = Palette.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.of(Rank.Secondary))
                .clickable { onDestroy(delete) },
        )
    }
}

/**
 * The gate's address.
 *
 * When an escort arrives, the field articulates — the shape channel again, saying "engaged". Nothing
 * is written here explaining why the person was brought over; they were brought to the thing itself,
 * which is the explanation.
 */
@Composable
fun RecipientField(chosen: String?, articulating: Boolean, onChoose: () -> Unit) {
    val radius by animateDpAsState(
        targetValue = if (articulating) 22.dp else 10.dp,
        animationSpec = Motion.spec(com.hereliesaz.conveyance.Weight.Light),
        label = "articulate",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(radius))
            .background(if (articulating) Palette.heat(0.5f) else Palette.of(Rank.Tertiary))
            .clickable { onChoose() }
            .element(ElementId("recipient.field")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.padding(horizontal = 16.dp).size(20.dp).clip(RoundedCornerShape(10.dp)).background(Palette.of(Rank.Secondary)))
        Text(chosen ?: "Recipient", color = if (chosen == null) Palette.quiet else Palette.ink, fontSize = 14.sp)
    }
}

/**
 * The send control: one element through all five states.
 *
 * Ready invites. Blocked still invites — pressing it escorts rather than refusing. Yielding deforms
 * this element rather than summoning a spinner. Settled carries the result rather than announcing
 * it. There is no fifth widget anywhere on the screen for any of it.
 */
@Composable
fun Send(scope: ActScope) {
    val blocked = scope.state is ActState.Blocked
    val settled = scope.state is ActState.Settled
    val rank = if (blocked) Rank.Secondary else Rank.Primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .yielding(scope.yielding, scope.weight)
            .clip(RoundedCornerShape(if (settled) 28.dp else 16.dp))
            .background(if (settled) Palette.heat(0.7f) else Palette.of(rank))
            .clickable { scope.engage() }
            .element(ElementId("act:documents.send")),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (settled) "Sent" else "Send",
            color = if (blocked) Palette.quiet else Palette.ground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}
