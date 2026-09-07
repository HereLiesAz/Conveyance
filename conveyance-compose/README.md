# conveyance-compose

The Compose Multiplatform binding for [`conveyance-core`](../conveyance-core/README.md).

Core models what the interface means. This module turns those relationships into geometry, motion, routing, and live audit evidence.

For a first screen, start with [Getting started](../docs/GETTING-STARTED.md). For the full model, read the [framework spec](../docs/CONVEYANCE-FRAMEWORK.md).

---

## ConveyanceHost

A surface lives inside `ConveyanceHost`:

```kotlin
ConveyanceHost {
    // application UI
}
```

The host owns cross-surface machinery such as the Element registry, practice state, Ghosts, and the Stage used to render consequence motion beyond an individual composable's clip bounds.

---

## Offer

`Offer` makes an Act available to the person.

```kotlin
Offer(send) {
    Box(
        Modifier
            .tell(owesTell, weight)
            .yielding(yielding, weight)
            .clickable { engage() },
    ) {
        Text("Send")
    }
}
```

`ActScope` keeps the action lifecycle together:

```text
Ready
Blocked
Yielding
Settled
Refused
```

The application does not need a separate spinner, snackbar, or detached success object just to report the lifecycle of this Act.

### Act emphasis in Compose

The core Act carries semantic expressive importance:

```kotlin
ActEmphasis.Heroic
ActEmphasis.Primary
ActEmphasis.Secondary
ActEmphasis.Supporting
```

Inside `Offer`, use `act.emphasis` or the `ActScope.emphasis` convenience property.

```kotlin
Offer(publish) {
    val treatment = MyTheme.actTreatment(emphasis)
    PublishControl(treatment)
}
```

Conveyance intentionally does not create a parallel styling system. Compose already has themes, CompositionLocals, design tokens, and product-specific component tokens. Conveyance supplies the semantic token; the product's theme decides how that token becomes shape, type, color, motion, space, haptics, sound, or surrounding response.

A Heroic Act is therefore not a special Conveyance component. It is an Act whose theme is allowed to engineer a hero moment.

---

## Gates and Escort

A blocked Act carries a `Gate` that already names where resolution lives.

The binding can therefore:

1. respond at the point of contact;
2. route through prerequisite Gates;
3. bring the useful resolver into view;
4. articulate it.

That is the Escort.

The important semantic distinction is that Gates are **resolvable** blockers. An unavailable fact with no action behind it should not be modelled as a fake Gate with a fake destination.

---

## ElementRegistry

`ElementRegistry` turns semantic addresses into current geometry.

It knows:

- which Elements are composed;
- current bounds and visibility;
- which Act is offered at which Element;
- which Elements resolve Gates;
- which Elements carry travelling identity tokens;
- declared Employment where the framework cannot infer it.

From those facts it derives observed jobs and live audit evidence.

### Census

```kotlin
registry.census()
```

reports what is present and what can be done there.

### AuditFrame

```kotlin
registry.auditFrame("invoice")
```

captures the semantic truth of a running surface:

- geometry;
- offered Acts;
- consequence verbs and targets;
- reversibility;
- Gates;
- jobs;
- Act emphasis;
- Ambient declarations.

That frame is what lets Conscience reason about relationships instead of only source declarations.

---

## Dynamic consolidation

Conscience uses `AuditFrame` together with `ConsolidationAdvisor` to find under-employed clusters.

Instead of:

```text
button: 2 jobs
spinner: 1 job
success badge: 1 job
```

becoming three unrelated warnings, the advisor can recognize a fragmented action lifecycle and recommend one richer construction.

When a cluster matches a known SDK recipe, the recommendation can name it directly:

```text
Action source + progress + completion
→ Offer
```

The intended catalog grows from actual SDK capability, not from names like `Button` or `Spinner` alone.

---

## Collection

`Collection` is a behavioral construction for repeating identified Subjects with creation and recovery behavior.

It is not merely a styled list.

Because the binding knows the collection, its Subjects, and the Acts affecting them, it can render creation travel, destruction residue, restoration, and live audit evidence from semantic facts rather than application-authored animation wiring.

---

## Form

`Form` groups related fields and completion behavior into one coherent structure.

This matters to resourceful minimalism: several weak field/status/validation fragments are often better understood as one form object doing several jobs.

The recommendation engine can eventually use this same capability signature to suggest `Form` when a live surface has recreated that structure by hand.

---

## Places

`Places` hosts continuity between a Place and the Element it came from.

```kotlin
Places(root = Place.root("tray")) { place ->
    if (place.isRoot) Tray() else Detail(place.subject!!)
}
```

`Place.from(...)` gives an entered Place an antecedent. `Place.root(...)` names a genuine beginning.

The binding can use that relationship for Enter and Return without requiring the application to describe a separate animation graph.

---

## Motion

Consequence motion comes from the Act's semantic `Signature`.

The binding owns reference motion because application-supplied durations and easing would let the same consequence teach contradictory physics across call sites.

That does **not** mean all motion in a Compose product must be consequence grammar. Ambient motion, identity motion, role personality, data animation, simulation, and visual atmosphere may coexist with it because they are saying something else.

The useful rule is semantic: motion used to teach a consequence should remain learnable and truthful when retargeted or interrupted.

---

## Employment

Most jobs should be derived from behavior where the registry can see them.

Use `Modifier.element(..., employment = ...)` for facts the framework genuinely cannot infer.

```kotlin
Modifier.element(
    id = backgroundTexture,
    employment = Employment.Ambient,
)
```

`Ambient` is the explicit opt-out for intentionally non-operational composition.

---

## Suppression

`Modifier.suppressEscort` temporarily holds Escort travel while another gesture or transition is actively occupying the person's hand.

It does not suppress the semantic Gate, disable the Act, or turn off Conveyance. It only prevents a relocation from occurring underneath an active gesture.

---

## What this binding does not decide

`conveyance-compose` does not prescribe:

- one Material component set;
- one palette;
- one corner-radius system;
- one density;
- one amount of visual chaos;
- one interpretation of Heroic;
- monochrome hierarchy;
- hue-as-rank;
- a ban on decorative/personality motion.

Those belong to product themes and companion design systems.

The binding's job is to preserve semantic relationships strongly enough that whatever aesthetic you choose can still teach through use.

---

## Related docs

- [Manifesto](../README.md)
- [Framework spec](../docs/CONVEYANCE-FRAMEWORK.md)
- [Rules and opt-outs](../docs/RULES-AND-OPTOUTS.md)
- [Core module](../conveyance-core/README.md)
