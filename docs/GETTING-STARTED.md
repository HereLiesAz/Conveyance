# Getting started

A working screen in about ten minutes: one `Act`, one blocked precondition, one Escort, rendered
with no animation code of your own. If you want the philosophy first, read
[the manifesto](../README.md) or [the full spec](CONVEYANCE-FRAMEWORK.md) — this page assumes you
already want to see it work.

## What's actually available today

This project isn't on Maven Central yet — see [the SDK inventory below](#what-this-sdk-offers-today)
for the honest state of publishing. Right now, the fastest path is building against source:

```kotlin
// settings.gradle.kts, in the app that wants to use it
includeBuild("../Conveyance")
```

or, to depend on it like a normal published library from your local machine:

```bash
git clone https://github.com/HereLiesAz/Conveyance
cd Conveyance
./gradlew publishToMavenLocal
```

```kotlin
// your app's settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
```

```kotlin
// your app's build.gradle.kts
dependencies {
    implementation("com.hereliesaz.conveyance:conveyance-core:0.1.0")
    implementation("com.hereliesaz.conveyance:conveyance-compose:0.1.0")
}
```

## Your first Act

An `Act` is never constructed directly — one of six verb factories, each taking exactly what its
verb needs:

```kotlin
// Recipient is your own type -- the framework only ever needs to know whether one is chosen.
val chosen = mutableStateOf<Recipient?>(null)
val recipientField = ElementId("recipient.field")
val recipientAvatar = ElementId("recipient.avatar")

val send = Act.send(
    id = "invoice.send",
    subject = SubjectId("invoice.41"),
    to = recipientAvatar,
    requires = listOf(
        // The gate names *where* the missing condition gets resolved -- not just that it's missing.
        Gate("recipient.chosen", livesAt = recipientField) { chosen.value != null },
    ),
)
```

Nothing here is UI. `send.weight`, `send.reversible`, `send.state()` are all already derived —
there is no separate place to declare how heavy this feels or whether it can be undone.

## Render it

Every surface sits inside one `ConveyanceHost`. The one control is `Offer`, which paints all five of
an act's states through a single scope:

```kotlin
setContent {
    ConveyanceHost {
        Column {
            Offer(send) {
                // No branch for Blocked here: the framework renders the felt refusal and the
                // Escort itself. The label doesn't need to change for a person to feel the state.
                Box(
                    Modifier
                        .tell(owesTell, weight)
                        .clickable { engage() },
                ) {
                    Text("Send")
                }
            }

            // The gate's address. Nothing wires this to the act above by hand --
            // the Gate already named it.
            Offer(
                Act.alter("recipient.choose", SubjectId("recipient"), "recipient", recipientField) {
                    chosen.value = Recipient("Mara")
                    Outcome.Done
                },
                element = recipientField,
            ) {
                Box(Modifier.clickable { engage() }) { Text("Mara") }
            }
        }
    }
}
```

Run it. Tap **Send** before choosing anyone: instead of doing nothing, it leans toward the gate (felt
resistance at the point of contact) and then carries you there — the whole framework's answer to the
disabled state. Tap **Mara**, then **Send** again: it settles, no animation you wrote anywhere.

## Where to go from here

- **[conveyance-core](../conveyance-core/README.md)** — the full type system: `Place`, `Route`,
  `Weight`, what's refused at construction and why.
- **[conveyance-compose](../conveyance-compose/README.md)** — `Collection` (Migration + Ghost),
  `Places` (Enter/Return), suppression, the live registry.
- **[conveyance-auditor](../conveyance-auditor/README.md)** — show your screen to a naive viewer and
  grade what they predicted, no API key required.
- **[conveyance-demo](../conveyance-demo)** — a full working app (`./gradlew :conveyance-demo:run`)
  exercising Create, Enter, Return, Send, Alter, and Refuse together.
- **[The framework spec](CONVEYANCE-FRAMEWORK.md)** — every law, every audit, every named behavior,
  precisely.

## What this SDK offers today

An honest accounting, not a sales pitch:

**Solid:** the type system (`conveyance-core`), the Compose binding for Android and desktop
(`conveyance-compose`), the keyless-capable AI judge (`conveyance-auditor`), one real demo app
exercising most of the grammar, 100+ deterministic tests.

**Not yet built:**
- **Publishing.** No Maven Central coordinates yet — `publishToMavenLocal` or `includeBuild` only.
- **iOS / web targets.** `conveyance-compose` currently builds for Android and desktop JVM only.
- **The static `Surface`/`Conscience` audit path is disconnected from real apps.** `Surface`,
  `DeclaredElement`, and `Product` exist and are fully tested, but nothing in `conveyance-compose` or
  the demo actually constructs one from a live screen — today they're only exercised by their own
  unit tests. Bridging a live `AuditFrame` into a `Surface` automatically, so `Conscience`'s
  whole-surface audits (idle workers, teleports, dead ends) run against a real running app without
  hand-declaring one, is open work.
- **"First Move"** — the manifesto's zero-data onboarding behavior (§5.6, a `Rehearsal` state) is
  specified but has no code yet.
- **The platform-boundary compiler plugin.** The spec calls for two more audits — no platform
  Toast/Snackbar/Dialog construction, no literal animation duration in product code — enforced by a
  Kotlin compiler plugin. That plugin doesn't exist; today nothing stops a call straight past the
  binding into a platform API.
- **`Job.Interrupt` has no live binding yet.** Law 4 now requires three jobs, and an element that
  starts something is expected to own stopping it — but `Offer` has no way to cancel an act already
  in flight. The vocabulary exists in `conveyance-core`; nothing in `conveyance-compose` implements
  it, so `Job.Interrupt` is only reachable from the static `Surface` declarations today, the same gap
  as the audit path above.
