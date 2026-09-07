# Conveyance Rules and Their Opt-Outs

Conveyance is deliberately demanding. Its rules exist to make a designer reconsider the interface, combine responsibilities, trust the person using it, and make consequences legible without explanatory scaffolding.

A strong rule is not weakened because a legitimate exception exists.

The rule and its exception belong together.

> **Rule → named semantic opt-out.**
>
> The opt-out must say what the thing **is instead**. `ignore = true`, suppression annotations, and vague "custom" switches are not Conveyance vocabulary.

The question is not "can this rule ever be broken?". The useful questions are:

1. Does the rule provoke a more inventive or more self-explanatory interface?
2. When the rule genuinely does not describe the thing, can the designer say why in the model itself?
3. Can Conscience distinguish that deliberate exception from an accidental violation?

---

## Employment

### Rule — a working element does at least four jobs

`Employment.Working` requires at least four distinct `Job`s.

The point is not efficiency arithmetic. The pressure is creative: if a button only submits, a label only labels, and a status chip only reports, the designer is encouraged to ask whether those fragments could become one richer thing that invites, reports, progresses, confirms, locates, groups, identifies, interrupts, or otherwise earns its space in several ways.

```kotlin
Employment.Working(
    Job.Invite,
    Job.Report,
    Job.Progress,
    Job.Interrupt,
)
```

### Opt-out — `Employment.Ambient`

Use `Employment.Ambient` when the element is **not attempting to be operational**: ground, texture, breathing room, atmosphere, ornament, a rule, or another presence whose purpose would be distorted by inventing four fake jobs.

```kotlin
Employment.Ambient
```

`Ambient` is not "ignore the employment rule". It says: *this is not a working element*.

---

## Trusting chrome

### Rule — chrome does not narrate obvious mechanics

`Label` rejects instructional filler such as `tap`, `click`, `press`, `swipe`, `drag`, `select`, `choose`, `please`, `simply`, and `just`.

The rule is about trusting the person. If an affordance only works after the interface says "tap here", the extra sentence is usually repairing a design failure in words.

The rule is **not** a general vocabulary ban and is not a prose style guide.

### Opt-out — it is content, not `Label`

Actual prose, documentation, user-authored text, narrative copy, help, warnings whose content is genuinely necessary, and other reading material are not chrome labels. Do not model them as `Label` merely because they are text on a screen.

The opt-out is therefore semantic and structural: **content remains content**.

---

## Continuity of place

### Rule — entered places have an antecedent

A navigated `Place` is made with `Place.from(...)`. Its origin is the element it grows out of and returns toward.

```kotlin
Place.from("invoice.detail", origin = invoiceRow)
```

This creates continuity instead of teleporting the person and then rebuilding the lost relationship with breadcrumbs.

### Opt-out — `Place.root(...)`

Use `Place.root(...)` when there genuinely is no visual antecedent: an application entry point, restored/deep-linked entry, externally launched destination, or another true beginning.

```kotlin
Place.root("home")
```

A root is not an entered place with continuity disabled. It declares: *this is where this journey begins*.

There is no universal quota on roots in core Conveyance. A product may have several legitimate entry points.

---

## Gates

### Rule — a resolvable blocker knows where resolution lives

A `Gate` carries `livesAt`. When an Act is blocked, the interface can escort the person toward something they can actually do instead of greying out the control and abandoning them.

```kotlin
Gate("recipient.chosen", livesAt = recipientField) { recipient != null }
```

### Opt-out — do not model an unresolvable state as a Gate

If there is nothing the person can currently do to satisfy the condition, it is not a resolvable Gate. Represent the state as a non-inviting/status element, or as ordinary content explaining an external fact when explanation is genuinely necessary.

The exception is not a Gate with a fake address. It is the declaration that **there is no available act to escort to**.

Conscience may still report a Gate whose declared resolver is absent because that is a contradiction between what the model promises and what composed.

---

## Destruction

### Rule — destruction is reversible whenever reality permits it

Use `Act.destroy(...)`. The inverse is mandatory.

```kotlin
Act.destroy(
    id = "document.delete",
    subject = document,
    target = collection,
    inverse = restore,
)
```

This pressure is intentional. It forces the designer to look for undo, recovery, staging, a Ghost, or another respectful alternative before reaching for friction and confirmation.

### Opt-out — `Act.destroyIrreversibly(...)`

When the actual consequence has no meaningful inverse, say so explicitly.

```kotlin
Act.destroyIrreversibly(
    id = "submission.finalise",
    subject = submission,
    target = destination,
)
```

Examples include external irreversible side effects, legal submissions, physical actions, or remote operations the product cannot restore.

This factory exists so the exception does not weaken `Act.destroy`. It also keeps irreversibility visible to weight, audits, and bindings.

---

## Consequence motion

### Rule — consequence motion teaches consequence

A Conveyance consequence has a grammar-derived `Signature`. Motion used to communicate Reveal, Enter, Create, Destroy, Alter, Send, Refuse, Yield, or Return should remain recognisable enough that the product can be learned by watching it.

### Opt-out — motion that is not consequence grammar

Ambient movement, stable identity motion, role personality, decorative life, data animation, simulation, and other motion whose job is **not to describe a consequence** is outside the consequence grammar.

Do not lie by assigning it a consequence verb merely to obtain an animation.

Bindings and companion libraries may expose named motion vocabularies such as `Ambient`, `Identity`, or `Personality`; those motions coexist with consequence grammar because they are saying something else.

The rule is therefore not "only nine animations may exist". It is "do not borrow a consequence's learned motion to mean an unrelated consequence".

---

## Visual channels

### Rule — semantic visual language should be learnable

When a product uses hue, chroma, shape, size, elevation, opacity, typography, density, motion, haptics, or sound **semantically**, repeated use should not contradict itself casually.

Conveyance's reference `Channel` mapping is a useful vocabulary and a test bed, not a universal tailoring specification.

### Opt-out — identity, content, atmosphere, and product grammar

A visual property may instead be carrying stable identity, content, atmosphere, illustration, brand language, or another product-defined grammar.

H2G2-style identity hues are the canonical example: hue distinguishes *who/what this is*, while some other channel can carry rank or state.

The opt-out is not "random color is allowed". It is: **this channel is intentionally carrying a different named job in this product**.

Accessibility still applies regardless of which visual grammar is chosen.

---

## One Element

### Rule — invitation, progress, result, and failure keep one identity

An Act should not become a button plus unrelated spinner plus unrelated toast plus unrelated error banner. The person should be able to follow the same subject through engagement and consequence.

### Opt-out — the process genuinely outlives the originating element

Some work becomes a durable process of its own: a build, import, render, workflow, swarm, background sync, deployment, or long-running external operation.

In that case the new process must receive its **own stable subject identity** and the transition from origin to process must be visible. The opt-out is not "show a global spinner"; it is "this action created a new thing whose state now belongs to that thing".

---

## Keystone expression

### Rule — keystone treatment means something

If an Act is marked as a keystone, the product is claiming that the act deserves unusually expressive treatment. That distinction should remain intentional rather than becoming a decorative flag sprinkled everywhere.

### Opt-out — do not mark a keystone

A product is not required to manufacture an emotional centre. `Product.keystones` may be empty, and a product may define its own expressive hierarchy.

The core no longer imposes a numeric one-to-three quota. The useful rule is semantic: if everything is called exceptional, the word has stopped carrying information.

---

## The meta-rule

Every hard Conveyance requirement should satisfy one of two conditions:

1. **No legitimate exception exists because violating it makes the model internally incoherent**, or
2. **A named semantic opt-out is documented immediately beside it.**

When a new rule is proposed, its opt-out should be designed at the same time. If the only available escape hatch is "ignore Conveyance here", the vocabulary is unfinished.

When an opt-out is proposed, it should answer **what the thing is instead**. If it merely turns off enforcement, it is a suppression switch and should be treated with suspicion.
