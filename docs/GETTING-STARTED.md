## Getting started

A working screen in about ten minutes: one `Act`, one blocked precondition, one Escort, rendered
with no animation code of your own. If you want the philosophy first, read
[the manifesto](../README.md) or [the full spec](CONVEYANCE-FRAMEWORK.md) — this page assumes you
already want to see it work. For the generated, per-class/per-function reference (every public
signature and KDoc comment across `conveyance-core`/`conveyance-compose`/`conveyance-auditor`,
kept current automatically on every push to `main`), see the
[wiki's API reference](../../../wiki/api-reference/conveyance-core/index).

## `convey` — a second, independent implementation

`convey/` in this repo is a git submodule pointing at [`HereLiesAz/convey`](https://github.com/HereLiesAz/convey)
— its own repository, with its own history, issues, and CI. It is not a copy of `conveyance-core`/
`conveyance-compose`'s code, and it does not share their API (`ConveyGrammar`/`ConveyWeight`/
`ConveyOffer`/... rather than `Act`/`Gate`/`ActScope`/...). It is a separately-developed Compose
Multiplatform implementation of the same manifesto, linked here so this repo can act as the single
place to find every SDK offered on top of the Conveyance Manifesto, without merging its code or
history into this one. Cloning this repo does not fetch it automatically:

```bash
git submodule update --init convey
```

See [`convey`'s own `AGENTS.md`](../convey/AGENTS.md) for its module shape, build instructions, and
current composable inventory — it is not covered by the rest of this document, which describes
`conveyance-core`/`conveyance-compose` only.

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
  with three tabs. **Photos** exercises Create, Enter, Return, Send, Alter, and Refuse together
  against this repo's own `conveyance-core`/`conveyance-compose`. **Styles** showcases the five
  separate composable-set libraries (`conveyance-h2g2`, `conveyance-expressive`,
  `conveyance-liquid`, `conveyance-bacterium`, `conveyance-space`) alongside the photo gallery.
  **Convey** showcases `conveyance-convey` — a second, standalone Compose Multiplatform design
  system with no dependency on `conveyance-core`/`conveyance-compose` — including its weight
  hierarchy and morph controls, kinetic text, subject-verb-object scene animation driven by
  WordNet/VerbNet lexicons, topographical layout, and an attention grid.
- **[The framework spec](CONVEYANCE-FRAMEWORK.md)** — every law, every audit, every named behavior,
  precisely.

## What this SDK offers today

An honest accounting, not a sales pitch:

**Solid:** the type system (`conveyance-core`), the Compose binding for Android and desktop
(`conveyance-compose`), the keyless-capable AI judge (`conveyance-auditor`), one real demo app
exercising most of the grammar, 100+ deterministic tests. `Job.Interrupt` has a live binding now
too — `ActScope.interrupt()` cancels an act's own in-flight work and it settles as
`ActState.Refused(Refusal.Interrupted)`, the same vocabulary any other failure reports through.
`Conscience` also runs against a real running app now: `Conscience.audit(AuditFrame)` carries the
idle-worker and dead-end checks live, off `ElementRegistry.auditFrame()` directly, rather than only
against a hand-declared `Surface` fixture — including one finding a hand-declared `Surface` could
never even produce, since `Employment.Working`'s own constructor refuses to let an under-resourced,
non-`Ambient` element exist in the first place. `Teleport` stays exclusively static: it names a
surface's whole set of *possible* entry places, not something one running snapshot could ever show
more than one of at once. `Form` (`conveyance-core`) is the new composite primitive: a group of
`FormField`s conveyed as one element rather than N unrelated ones, reporting `percentComplete` and
gating a submit act's `Gate` on `isComplete` — the honest reading of "a plain field almost never
appears alone" (a real GLEE-audit finding, not a guess). `conveyance-compose`'s `rememberFormState`
wraps a `rememberSaveable` value directly, so "form recovery" is exactly what already recovers a
saveable value across a configuration change or process death, not a second bespoke mechanism.

**Not built by this framework, deliberately:** platform autofill. `FormField.kind` names a closed
vocabulary (`Name`, `Email`, `Phone`, ...) precisely so a host *can* map it onto its own platform's
autofill API, but wiring a specific platform's autofill semantics is a host's job, not something a
cross-platform primitive can honestly claim to do uniformly across Android, desktop, and whatever
comes after — the framework earned that caution the hard way once already this cycle, tripping over
a Compose API that looked cross-platform and silently wasn't.

**Not yet built:**
- **Publishing.** No Maven Central coordinates yet — `publishToMavenLocal` or `includeBuild` only.
- **iOS / web targets.** `conveyance-compose` currently builds for Android and desktop JVM only.
- **"First Move"** — the manifesto's zero-data onboarding behavior (§5.6, a `Rehearsal` state) is
  specified but has no code yet.
- **The platform-boundary compiler plugin.** The spec calls for two more audits — no platform
  Toast/Snackbar/Dialog construction, no literal animation duration in product code — enforced by a
  Kotlin compiler plugin. That plugin doesn't exist; today nothing stops a call straight past the
  binding into a platform API.
