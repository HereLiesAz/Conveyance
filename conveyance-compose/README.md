# conveyance-compose

The Compose Multiplatform binding — Android and desktop today (see
[targets](build.gradle.kts)). This is where [`conveyance-core`](../conveyance-core/README.md)'s
model becomes pixels: the framework draws every verb's motion itself, so the application never
writes an `AnimationSpec` or decides what a blocked control looks like.

New here? The [root quickstart](../docs/GETTING-STARTED.md) gets a screen rendering in a few
minutes. This document is the reference once you're past that.

## `ConveyanceHost` and `Offer`

Every surface sits inside one **[`ConveyanceHost`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/ConveyanceHost.kt)**,
which holds what has to be shared across an entire product rather than reset per screen: the element
registry, practice counts, and the `Stage` that draws motion above your content (a verb travelling
from a row to an avatar has to be able to leave the row's own clip bounds).

```kotlin
ConveyanceHost {
    // your screen
}
```

The one control is **[`Offer`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/Offer.kt)**.
It takes an `Act` and paints all five of its states through a single `ActScope` — there is no way to
write a control that only handles the happy path:

```kotlin
Offer(send) {
    // `this` is an ActScope
    Box(
        Modifier
            .tell(owesTell, weight)      // the half-rep a control owes before first use
            .yielding(yielding, weight)  // deforms while state is Yielding
            .clickable { engage() },
    ) {
        Text(if (state is ActState.Blocked) "…" else "Send")
    }
}
```

Engaging a **Blocked** act doesn't grey out or refuse in place — it leans toward the gate (the
Refuse signature) and then carries the person there (the Escort), routed through
[`Route`](../conveyance-core/README.md) so a blocked prerequisite behind another blocked
prerequisite still lands somewhere they can actually act. A **Settled** act renders its own
consequence as motion, resolved purely from what the model already knows: the act's target, and
where that element currently is.

## Named behaviors

- **The Escort** — above. A disabled state that carries you to what's missing instead of refusing you in place.
- **[`Collection`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/Collection.kt)** — the Migration (a creation control travels from the centre of an empty space to its corner once used) and the Ghost (a destroyed subject leaves a recoverable, flattened residue in the slot it held, rather than a snackbar in a different postcode) both live here, because both are facts about collections rather than about controls.
- **[`Places`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/Places.kt)** — Enter and Return. A place isn't swapped in; it grows out of the element that was touched, and shrinks back into wherever that element sits *now* on the way out — not where it used to be, in case the ground moved while the person was away.
  ```kotlin
  Places(root = Place.root("tray")) { place ->
      if (place.isRoot) Tray() else Detail(place.subject!!)
  }
  ```
- **`Modifier.suppressEscort`** ([`Suppression.kt`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/Suppression.kt)) — holds an escort's carry back while a gesture is registered active elsewhere on the surface (a drag, a sheet mid-settle), so arriving somewhere new never happens out from under a person's finger. The felt resistance at the point of contact is never held back — only the travel.

## The registry, for anything that needs to look

**[`ElementRegistry`](src/commonMain/kotlin/com/hereliesaz/conveyance/compose/Elements.kt)** is what
turns the core's addresses into geometry — a consequence names the element that changes, a gate names
where it's resolved, a place names what it grows from, and this is what answers *"and where is that,
right now."* An address can have more than one simultaneous claimant (**tenancy**) — deliberately,
because that's what a place transition *is*: a detail place legitimately shares its subject's
address with the thumbnail it grew out of, for exactly as long as both are on screen.

Two things read the registry without touching product code:

- **`registry.census()`** → a live [`Census`](../conveyance-core/README.md) — acts on screen against
  how many can actually be reached, computed continuously, for free, from data the registry already
  had.
- **`registry.auditFrame(surfaceName)`** → what [`conveyance-auditor`](../conveyance-auditor/README.md)
  shows its judge alongside a screenshot: the truth a naive viewer never sees.

## Using it

```kotlin
// commonMain
dependencies {
    implementation("com.hereliesaz.conveyance:conveyance-compose:0.1.0")
}
```

Gradle resolves the right platform artifact (`conveyance-compose-android` or
`conveyance-compose-desktop`) automatically via the published Kotlin Multiplatform metadata. Not yet
on Maven Central — see the [root quickstart](../docs/GETTING-STARTED.md) for what works today.
