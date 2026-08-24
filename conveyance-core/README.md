# conveyance-core

The type system. Pure Kotlin, zero runtime dependencies, no UI toolkit — the model a screen has to
agree with, not a library for drawing one. If you're looking for something to put on screen, you
want [`conveyance-compose`](../conveyance-compose/README.md); this module is what that binding
renders.

The governing idea, explained at length in [the manifesto](../README.md) and specified precisely in
[the framework spec](../docs/CONVEYANCE-FRAMEWORK.md), is **conveyance**: an interface should let a
person predict what a control does before they touch it, and never omit what it costs. This module
is where that idea stops being an essay and starts being a constructor that refuses bad input.

## The shape of it

Everything a person can do is an **[`Act`](src/main/kotlin/com/hereliesaz/conveyance/Act.kt)**, made
through one of six factories — `reveal`, `enter`, `create`, `destroy`, `alter`, `send` — never
constructed directly. Each factory takes exactly what its verb needs and nothing else:

```kotlin
val send = Act.send(
    id = "invoice.send",
    subject = SubjectId("invoice.41"),
    to = recipientAvatar,
    requires = listOf(
        Gate("recipient.chosen", livesAt = recipientField) { recipient != null },
    ),
)
```

From that one declaration, everything else is *derived*, never separately configured:

- **`act.verb`** and **`act.signature`** — which of the nine motions this is, and how it moves.
- **`act.weight`** — Light, Medium, or Heavy, from the consequence, the scope, and whether it's
  reversible. There is no `weight = Weight.Heavy` parameter to set wrong.
- **`act.reversible`** — whether an `inverse` was supplied (destruction requires one; nothing else
  is allowed to fake one).
- **`act.state()`** — one of exactly five states (`Ready`, `Blocked`, `Yielding`, `Settled`,
  `Refused`), derived from whether every [`Gate`](src/main/kotlin/com/hereliesaz/conveyance/Gate.kt)
  in `requires` is currently satisfied.

A **`Gate`** is not a boolean flag. It names `livesAt`: the element where the missing condition gets
resolved. A blocked act isn't a dead end — it's a pointer to where a person can actually go, which is
the entire mechanism [`conveyance-compose`](../conveyance-compose/README.md)'s Escort runs on. Given
a set of acts, [`Route`](src/main/kotlin/com/hereliesaz/conveyance/Route.kt) will breadth-first search
that graph for the nearest thing a person can actually do, skipping a gate that is itself blocked —
nobody declares this graph; it falls out of every act already naming its own gates.

A **[`Place`](src/main/kotlin/com/hereliesaz/conveyance/Place.kt)** is somewhere a person can be. It
has no root-less constructor except `Place.root(id)` for a product's genuine entry point — every
other place is `Place.from(id, origin)`, and `Act.enter` refuses a root place outright: a teleport
with no antecedent is exactly the thing a "you are here" breadcrumb exists to prevent, so the type
makes it unconstructable rather than merely discouraged.

## What's enforced, and how

Illegal states are refused at construction wherever the type system can carry the whole rule:

| Rule | Enforced by |
|---|---|
| A destruction has no inverse-less constructor | `Act.destroy` requires `inverse` |
| A gate has no address-less constructor | `Gate(id, livesAt, condition)` — `livesAt` is not optional |
| Entering a root place is refused | `Consequence.Enter`'s own `init` |
| Chrome text that reads as an instruction can't compile | [`Label`](src/main/kotlin/com/hereliesaz/conveyance/Surface.kt)'s `init` |
| A surface can't hold two primaries | `Surface`'s `init` |
| A product can't declare 0 or >3 keystones | `Product`'s `init` |
| A channel can't be paired with the wrong meaning | `DeclaredElement.channels` is a `Set<Channel>`, not a `Map<Channel, Meaning>` — there's nowhere left to write the wrong one |

What's left — judgements that genuinely need a whole surface gathered together rather than one
value — is [`Conscience`](src/main/kotlin/com/hereliesaz/conveyance/Conscience.kt): idle workers,
teleports (more than one entry point), and dead ends (a gate whose address isn't on the surface). It
returns `Finding`s, never throws; a `Finding` always names the compliant construction, the same way a
blocked act escorts a person to its gate rather than refusing them in place.

The two rules no structural check can settle — *can a person predict what this does*, and *does it
omit something they needed to know* — are the job of
[`conveyance-auditor`](../conveyance-auditor/README.md), not this module.

## Also here

- **[`Weight`](src/main/kotlin/com/hereliesaz/conveyance/Weight.kt)** / **[`Grammar`](src/main/kotlin/com/hereliesaz/conveyance/Grammar.kt)** — the derivation tables from consequence + scope to physical weight and motion signature.
- **[`Practice`](src/main/kotlin/com/hereliesaz/conveyance/Practice.kt)** — per-act familiarity, so ceremony decays with use instead of everything staying equally verbose forever.
- **[`Reversal`](src/main/kotlin/com/hereliesaz/conveyance/Reversal.kt)** — how long a destruction stays recoverable, scaled by what it cost.
- **[`AuditReport`](src/main/kotlin/com/hereliesaz/conveyance/AuditReport.kt)** / **`Census`** — the data models the live registry and the AI judge report through; nothing here talks to a screen or a network.

## Using it

```kotlin
dependencies {
    implementation("com.hereliesaz.conveyance:conveyance-core:0.1.0")
}
```

Not yet on Maven Central — see the [root quickstart](../docs/GETTING-STARTED.md) for what's actually
available today (`publishToMavenLocal`, or building against source directly).
