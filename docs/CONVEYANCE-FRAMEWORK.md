# Conveyance

**A UI/UX framework in which the interface teaches its own rules.**

Status: design specification, v0.1. Platform-neutral. Reference binding: Jetpack Compose.

---

## 0. What this is, and the one rule that generates the rest

The manifesto says three things:

1. **Conveyance** — the design teaches by example, never by instruction.
2. **Resourceful minimalism** — if a thing is on screen, it works, and it works at more than one job.
3. **Compassionate design** — dignity, empowerment, security; give people the benefit of the doubt.

A *style guide* would restate those as advice and hope. A *framework* has to make them structural: the compliant thing must be the easy thing, and the non-compliant thing must be hard to say. Everything below is derived from that requirement.

There is one rule the whole framework hangs from:

> **The Two-Sided Rule.** Conveyance applies to the developer as much as the user. If a developer needs a manual to use this framework, the framework is a construction zone. The API is a user interface. It must convey.

This is not a slogan; it is a constraint with teeth, and it shows up throughout as concrete API decisions. The vocabulary is small enough to fit on one page (Appendix A) because a vocabulary you have to look up is a tooltip.

### The design premise

Conventional UI toolkits model **state** and render **appearance**. You say *what things look like*, and separately, elsewhere, you write what happens. The gap between those two is exactly where instruction has to be inserted to patch things up — the tooltip, the empty-state paragraph, the "Are you sure?", the snackbar that reports on something that already happened somewhere off-screen.

Conveyance models **affordance** and renders **consequence**. You declare what a person can do, what stops them, and what will visibly change when they do it. Appearance is derived. There is no gap to patch, so there is nothing to explain.

That inversion is the whole framework. The rest is bookkeeping.

---

# Part I — The Five Laws

Every mechanism in this document is an enforcement of one of these. They are ordered by how much they cost to violate.

### Law 1 — One Element

**An action's invitation, its progress, its result, and its failure are the same pixels.**

Not a button plus a spinner plus a toast plus a red banner. One element, four states, continuous identity throughout. The user learns the mapping *action → this thing → outcome* because it never breaks. Every separate feedback surface you add is a second construction worker standing next to the hole.

*Consequence for the API:* an `Act` is a single declaration carrying all four states. There is no way to declare a control without declaring what it looks like while working and what it looks like when it fails.

### Law 2 — Continuity

**Nothing appears from nowhere and nothing goes nowhere.**

If tapping a row opens a detail view, the row *becomes* the detail view. If a menu opens from a button, the button *is* the menu, expanded. Continuity is not decoration — it is the entire mechanism by which a person builds a mental map of a system they have never seen. Cross-fades and teleports destroy that map and then require a breadcrumb trail to rebuild it in words.

*Consequence for the API:* a destination cannot be declared without naming the element it grows out of. There is no `navigate(route)`. There is `Enter(place, from = element)`.

### Law 3 — Grammar

**Each kind of change has exactly one motion signature, used everywhere and used for nothing else.**

Not "motion should feel alive." A grammar: eight verbs, eight signatures, applied with total consistency across the entire product. After three encounters a person can predict the behavior of a control they have never touched, because they have learned the world's physics rather than a list of features. That predictive competence *is* the feeling of "it just clicks."

Consistency is load-bearing. A motion signature reused for decoration is a lie in the language and does more damage than no motion at all.

*Consequence for the API:* you cannot specify a duration or an easing curve. Those words are not in the vocabulary. You specify a verb and a weight; the framework owns the rest.

### Law 4 — Employment

**Every element does at least two jobs. Elements with one job get merged; elements with none get deleted.**

This is resourceful minimalism made checkable. Jobs are enumerable (§4.2), and an element that cannot name two of them is standing around watching one guy dig.

*Consequence for the API:* elements declare their jobs; the Idle Worker audit fails the build on unemployment.

### Law 5 — Benefit of the Doubt

**Never warn where you can reverse. Never instruct where you can demonstrate. Never block where you can escort.**

The three most common patronizing constructs — the confirmation dialog, the tooltip, the greyed-out control — each have a compassionate mechanical replacement (the Ghost, the Tell, the Escort). All three are in the framework; none of the originals are.

*Consequence for the API:* a destructive act will not compile without an inverse. There is no tooltip primitive. There is no `enabled: Boolean` — there is `requires(gate)`, and a gate knows where it lives.

---

# Part II — The Lexicon

Six nouns. That is the entire semantic model.

```
Subject       a thing in the product that a person cares about (a document, a track, a friend)
Place         somewhere a person can be
Act           something a person can do
Gate          a condition that stands between a person and an Act
Consequence   what visibly changes when an Act completes
Keystone      an Act designated as the emotional core of the product
```

### 2.1 Act

```
Act {
  identity     : Id                     // stable across time and place
  subject      : Subject?               // what it acts on
  requires     : List<Gate>             // what must be true first
  consequence  : Consequence            // REQUIRED — what changes, and where
  inverse      : Act?                   // REQUIRED if consequence is destructive
  weight       : derived                // from consequence magnitude, not chosen
}
```

An `Act` renders itself through five states, in the same location, as the same element:

```
Ready      → Blocked(gate)     the world is not ready; the element leans toward what is missing
Ready      → Yielding(extent)  the system is working; the element itself deforms under load
Yielding   → Settled(result)   done; the element carries the result rather than announcing it
Yielding   → Refused(reason)   failed; the element holds the reason and the retry, in place
```

Three things about this shape are deliberate and are where most of the philosophy lives:

**`consequence` is required and must name a target.** You cannot declare an action whose result is invisible or unlocated. If you cannot say what changes and where, you have a hidden consequence, which is the root cause of every toast ever written. Naming the target is also what lets the framework animate from the control to the outcome for free — Continuity falls out of a field you were forced to fill in.

**`inverse` is required for destruction, and there is no confirmation primitive.** The framework will not ask "Are you sure?" on your behalf because it cannot. Reversibility is the only offered safety mechanism, which means the cheap patronizing option is unavailable and the respectful one is the default path.

**`weight` is derived, never chosen.** Weight is the inertia of the element's motion, and it is computed from the magnitude of the consequence. Deleting a character is light. Deleting an account is heavy — slow to start, slow to stop, resistant. The user *feels the stakes in their thumb* before they read anything. This is the security pillar of compassionate design, implemented as physics. It is not available as a styling knob, because a designer choosing weight for aesthetic reasons would be lying to a person's hands.

### 2.2 Gate

```
Gate {
  satisfied    : Boolean
  livesAt      : Element        // REQUIRED — where the person goes to satisfy it
}
```

There is no disabled state in this framework. A greyed-out control is a sign nailed to a post: it announces a rule and abandons you. A `Blocked` Act is a live element that knows where its own precondition lives, and pressing it **escorts you there** (§5.2).

`livesAt` is required for the same reason `consequence` is: if you cannot name the place where the blocker can be resolved, you have built a dead end, and the person will need a paragraph to get out of it.

### 2.3 Place

```
Place {
  identity     : Id
  origin       : Element        // REQUIRED — the element this place grows out of
  subject      : Subject?
}
```

`Place.Root` exists for genuine entry points and is budgeted (default: 1). Everything else has an antecedent. A place with many possible origins declares the morph per origin, or accepts a single canonical one; either way there is no teleport.

### 2.4 Consequence

Consequence is not free text. It is one of six classes, each bound to a motion signature in Part III:

```
Reveal(target)          more of what is already here becomes visible
Enter(place)            the person goes somewhere
Create(subject, into)   a new thing exists, in a named collection
Destroy(subject)        a thing ceases (requires inverse)
Alter(subject, field)   a thing changes in place
Send(subject, to)       a thing leaves the person's control
```

Choosing the class is the only expressive decision. Everything visual follows from it. This is the mechanism that keeps a product's grammar consistent across a team of twelve people and four years — nobody is choosing animations, they are choosing verbs.

### 2.5 Keystone

An Act marked `Keystone` receives the expressive motion budget: the extra articulation, the overshoot, the sound, the haptic, the moment. Between one and three per product, enforced.

The scarcity is the point. The manifesto's "hero moment" fails the instant it is applied generously — an app where everything sings is an app where nothing is emphasized, which is an app that has spent its most expensive channel on decoration. The Keystone budget is resourceful minimalism applied to delight itself.

---

# Part III — The Motion Grammar

Eight verbs. Each has exactly one signature. Each signature means only its verb.

| Verb | Occurs when | Signature | What it teaches |
|---|---|---|---|
| **Reveal** | `Reveal` | The container grows from the edge the person touched. Nothing translates. Nothing else moves. | "This was always here. You are in the same place." |
| **Enter** | `Enter` | The touched element expands to become the new place. Its content is the new content, morphing. | "That thing and this screen are the same thing." |
| **Return** | back | The place contracts back into the element it came from, which is still where it was. | "You did not lose your place. It is here." |
| **Create** | `Create` | The subject precipitates *out of the creating control*, then travels to and settles into its position in the collection. | "New things come from here and live there." |
| **Destroy** | `Destroy` | The subject collapses in place, leaving a Ghost (§5.3) that can be pulled back. | "It is gone, and gone is not final." |
| **Alter** | `Alter` | Only the changed property moves. The subject does not translate, lift, or flash. | "That, and only that, is different now." |
| **Send** | `Send` | The subject travels toward the on-screen representation of its destination and diminishes into it. | "It went *there*, to *them*." |
| **Refuse** | blocked | The element resists at the point of contact, then escorts toward its gate (§5.2). | "Not yet — and here is why, and it is over there." |

Plus one non-verb, always available:

| **Yield** | working | The engaged element itself deforms under load — compresses, stretches, fills, thickens. Never a separate spinner. | "*This* is what is busy, and it is what you touched." |

### 3.1 Exclusivity

A signature may be used for its verb and for nothing else. A decorative flourish that borrows the `Send` signature is a false statement in the language; the audit flags it (§7.9). This is stricter than it sounds and it is the single highest-value rule in the framework, because a grammar with exceptions is not a grammar and cannot be learned without being taught.

### 3.2 Physics, not timing

One spring family, three weights, derived from consequence magnitude:

```
Light    responsive, slight overshoot     chips, toggles, selections, Alter on trivia
Medium   crisp, settles clean             cards, sheets, Create, Send
Heavy    high inertia, no overshoot       Places, Destroy, anything with a real cost
```

Weight is inertia, and inertia reads as consequence. A heavy element is slow to start under the finger and slow to stop — it *feels* like it matters, and it gives the hand a beat in which to change its mind. That beat is the confirmation dialog, replaced by physics, without the interruption or the insult.

**Durations and easing curves are not in the API.** There is no `animationSpec` parameter to pass. This is not a limitation the framework apologizes for; it is the mechanism that makes an entire product internally consistent by construction rather than by review.

### 3.3 The reduced register

Motion carries meaning here, so "disable animations" cannot mean "disable meaning." Every verb has a reduced expression that preserves **continuity and location** while removing **traversal**:

- `Enter` still morphs, instantly — the new place appears at the origin element's bounds and expands to fill in one frame.
- `Create` still precipitates at the control's position, then is instantly at its collection position — the intermediate flight is dropped, the endpoints are kept.
- `Destroy` still leaves the Ghost.
- `Yield` still deforms the engaged element, statically.

What is removed is duration, not identity. This is a hard requirement of the design, not an accessibility afterthought: if the reduced register loses the grammar, the grammar was carried by ornament rather than by structure.

---

# Part IV — Channel Economy

Resourceful minimalism applied to the design vocabulary itself.

A visual channel is a dimension you can vary: position, size, shape, hue, chroma, elevation, opacity, type scale, density, motion, haptics, sound. Conventional design systems assign meaning to a few and spend the rest on brand. Conveyance assigns **exactly one global meaning to every channel**, and anything unassigned goes unused — plain, flat, quiet — rather than being decorated.

The result: nothing on screen varies without saying something, so everything that varies is information. That is what makes an interface readable without labels.

### 4.1 The assignment

| Channel | Carries | Never carries |
|---|---|---|
| Position | Relationship and origin — where a thing came from and belongs | Balance, rhythm |
| Size | Current importance in this moment, not permanent rank | Brand presence |
| Shape / radius | State — settled shapes at rest, articulated shapes when engaged | Brand personality |
| Hue | Semantic rank — one primary per surface, secondary alternatives, tertiary ambient | Category, decoration, mood |
| Chroma | Heat — how live, recent, or urgent this is | Brand saturation |
| Elevation | **Reversibility.** Things that float can be dismissed; things that are flush cannot | Depth aesthetics, hierarchy |
| Opacity | Transition only. Never a resting state | Disabled, de-emphasis |
| Type scale | Reading order | Emphasis, personality |
| Density | Relatedness — proximity is grouping and nothing else | Fitting more in |
| Motion | The grammar (Part III) | Delight, polish |
| Haptics | Consequence magnitude — the tactile echo of weight | Confirmation of taps |
| Sound | Keystones only | Everything else |

Two of these are worth dwelling on because they are unusual and they pay off enormously:

**Elevation means reversibility.** Once it is consistent, a person knows at a glance and without ever being told which things they can back out of. Shadows stop being lighting and become a safety map. The security pillar, delivered in a channel most systems spend on taste.

**Opacity is never a resting state.** Half-opacity is the universal signal for "disabled," which is the construct this framework does not have. A thing is present and live, or it is not there. There is no purgatory of ghosted controls that a person has to test by tapping.

### 4.2 The Job enum

Employment (Law 4) needs jobs to be countable. Every element declares from:

```
Invite     offers an act              Report     shows current state
Locate     tells you where you are    Identify   distinguishes one subject from another
Group      binds things together      Separate   marks a boundary
Progress   shows work happening       Confirm    shows work completed
Warn       shows risk                 Navigate   moves you
```

Two minimum. One means "merge this with its neighbor." Zero means "delete it." A small number of `Ambient` elements (a background, a rule) may be declared exempt, and the exemption is budgeted per surface so that the exemption cannot quietly become the norm.

---

# Part V — The Named Behaviors

These are the framework's replacements for the constructs it refuses to ship. Each one is a single mechanism doing what two or three conventional constructs did badly.

### 5.1 The Tell

*Replaces: coach marks, tooltips, onboarding overlays, "swipe to continue" hints.*

An element that has never been operated performs, once, an abbreviated version of its own interaction. The drawer eases out twelve units and settles back. The card lifts a fraction and drops. The dial rotates a few degrees and returns.

Not an arrow pointing at it. Not a caption. The element does a half-rep of the thing it wants you to do, the way a poker player's hands give them away before they act. It is over in a third of a second, it never repeats after the person has done the thing once, and it teaches the gesture rather than describing it.

Budget: at most one Tell per surface per session, on the surface's most consequential unpracticed act. Elements track their own practice count; there is no separate onboarding state machine to maintain, and no onboarding flow to skip.

### 5.2 The Escort

*Replaces: disabled states, validation error summaries, "please complete all required fields."*

Pressing a `Blocked` act does not do nothing, and does not show a message. The element resists at the point of contact — the `Refuse` signature — and then **carries the person to the gate**: it travels to, scrolls to, or opens the place where the unmet condition lives, and the gate element receives focus already articulated.

Three conventional constructs collapse into one mechanism: the disabled control, the error text, and the "jump to first error" affordance. And the emotional register changes completely. A greyed-out button says *you failed to read the rules*. An escort says *come on, it's this way*. Same information, opposite treatment of the person's dignity.

### 5.3 The Ghost

*Replaces: confirmation dialogs, undo snackbars, trash/archive round-trips.*

A destroyed subject collapses in place and leaves a Ghost — a compressed residue occupying the space the subject held, in the position it held, for a window proportional to the act's weight. Pulling the Ghost restores the subject. Letting it be lets it go.

The undo is **where the thing was**, not in a bar at the bottom of the screen that steals the space and then leaves. There is no modal interruption before the fact, and no orphaned report after it. The reversal is located in the world, which is where a person's hand already is.

This is why `inverse` is a required field for destructive acts. The Ghost is not a courtesy the framework offers; it is the only destruction mechanism the framework has.

### 5.4 The Migration

*Replaces: empty-state illustrations and their explanatory paragraphs.*

An empty collection does not display a message about being empty. It displays **its creation control, at full size, in the center of the space the collection will occupy.** When the first subject is created, that control performs `Create` — and then travels to the corner position where it will live from now on, and shrinks into it.

In one motion, with no words, a person learns: what this space is for, how to fill it, and where the button will be for the rest of their life with this product. One element, four jobs (Invite, Locate, Navigate, Identify), zero instructions. This is the framework's canonical demonstration of what resourceful minimalism actually buys you.

### 5.5 The Yield

*Replaces: spinners, progress bars, loading skeletons, "please wait."*

The engaged element deforms under load. A button being pressed into service compresses and fills. A list being fetched thickens its own rows. A field being validated tightens.

Progress is never a separate object, because a separate object severs the link between what you touched and what is happening — which is precisely the link the person is trying to learn. Indeterminate work deforms rhythmically; determinate work deforms proportionally. Same channel, same element, no vocabulary to learn.

### 5.6 The First Move

*Replaces: tutorials, product tours, sample-data-with-a-dismiss-button.*

The product's initial state is **arranged** so that the most sensible available action is the one that teaches the core loop. Not gated, not forced, not narrated — arranged, the way Mega Man X puts you in a corridor where the only thing to do is the thing that teaches you the thing.

Framework support is a `Rehearsal` state: a declared arrangement of the product at zero data that is *composed*, not empty, and that dissolves into ordinary use the moment the person acts. It is the single highest-effort thing in this framework and the only place where a real budget of design time is justified before launch, because a first minute that clicks buys a permanent trust that no amount of later polish recovers.

---

# Part VI — Teaching Without Instruction

The framework's position on text, stated precisely, because "no text" is a slogan and slogans get products into trouble.

### 6.1 Text is not banned. Instruction is.

```
ALLOWED     names of subjects            "Invoices"          nouns identify
            names of acts                "Send"              verbs invite
            values                       "$412.00"           data is data
            user-generated content       anything            not yours to police
ABSENT      sentences describing the UI  "Tap here to..."    the UI describes itself
            explanations of state        "Your file is..."   the element shows it
            warnings before the fact     "Are you sure?"     reverse instead
            reports after the fact       "Saved!"            the element settled
            help text, tooltips, hints, empty-state copy, feature tours
```

The operational test, and the one the audit implements: **user-facing strings in chrome are nouns or verbs, not sentences.** Four words is the practical threshold. If you need a fifth, you have a design problem you are papering over, and the paper is the tell.

### 6.2 The accessibility layer is exactly the opposite, and this is not a contradiction

A person using a screen reader cannot perceive a morph, a Ghost, or an Escort. For them, the framework's entire pedagogical apparatus is invisible. **Text is removed from the visual surface, and is mandatory in the semantic layer.**

So the `Act` model — which already knows its identity, its consequence, its gate, and its inverse — generates the accessibility tree automatically and richly:

- The consequence class becomes the action description.
- The gate's identity becomes the reason a blocked act is unavailable, plus a direct move to it (the Escort has a non-visual form: focus travel).
- The Ghost becomes an announced, focusable undo affordance in the collection's position.
- Yield becomes a live region tied to the engaged element.
- Continuity becomes focus continuity: focus lands on the morphed element, never at the top of a new screen.

This is a real advantage of modelling affordance rather than appearance, and it is worth stating plainly: **because the framework knows what every act means, it can describe every act in words — it simply declines to write those words on the screen.** A conventional toolkit knows only that there is a rectangle, which is why its accessibility labels are always someone's afterthought.

The same structure serves automated testing (stable identities, declared consequences) and any surface that genuinely requires prose, such as search-indexable web content.

### 6.3 Novices and experts want opposite things

Conveyance optimizes for the person who has never seen this before. The person who has seen it four thousand times wants speed, not pedagogy, and will come to experience the Tell as a stutter and the heavy weights as molasses.

The framework's answer is **practice-decay**: elements track their own operation count. Tells stop. Weight is permitted to lighten toward its floor on high-frequency acts. Keystone articulation shortens. The world's grammar never changes — a `Send` is always a `Send` — but its ceremony attenuates with familiarity, in the same way a skilled musician's motions get smaller.

Practice-decay is per-element and automatic. There is no "expert mode" toggle, because a toggle is a preference someone has to be told about.

---

# Part VII — The Conscience

The framework's verification layer: static audits at build time, runtime assertions in debug. This is what makes it a framework rather than a manifesto with a component library attached.

| # | Audit | Fails when | Enforces |
|---|---|---|---|
| 1 | **Idle Worker** | An element declares fewer than two jobs and is not budgeted `Ambient` | Law 4 |
| 2 | **Instruction** | A chrome string exceeds four words, contains terminal punctuation, or is imperative-with-object | Law 5, §6.1 |
| 3 | **Teleport** | A `Place` has no `origin`, or a transition resolves to a cross-fade | Law 2 |
| 4 | **Orphan Feedback** | A platform Toast, Snackbar, Dialog, Spinner, or ProgressBar is constructed | Law 1 |
| 5 | **Duration** | A literal duration, easing curve, or animation spec appears in product code | Law 3 |
| 6 | **Primary Contention** | More than one primary-rank element is live on a surface | §4.1 |
| 7 | **Keystone Budget** | More or fewer than 1–3 Keystones, or expressive articulation outside one | §2.5 |
| 8 | **Reversibility** | A destructive `Consequence` has no `inverse` | Law 5, §5.3 |
| 9 | **Channel** | A channel varies without carrying its assigned meaning — decorative color, resting opacity, borrowed motion signature | Part IV, §3.1 |
| 10 | **Dead End** | A `Gate` has no `livesAt`, or `livesAt` is unreachable from the blocked element | §2.2, §5.2 |

Audits 3, 4, 5, 8 and 10 are structural — they are consequences of required fields and of vocabulary that does not exist, so they mostly cannot be violated in the first place. Audits 1, 6, 7 and 9 are judgment calls the tool can only flag, not decide; they report and require an explicit, reviewed waiver rather than blocking.

**The Conscience obeys the Two-Sided Rule.** It never lectures. A failure names the element, names the law, and offers the compliant construction as a diff — it escorts the developer to the fix, exactly as §5.2 escorts the user to the gate. A build error that explains a philosophy is a tooltip, and this framework does not ship tooltips.

---

# Part VIII — The Absent Vocabulary

What a framework refuses to provide is a design decision equal in weight to what it provides. These do not exist in the API, and their platform equivalents are flagged on sight:

```
Toast · Snackbar          reports about elsewhere            → the element settles (Law 1)
Dialog · AlertDialog      interruption before the fact       → weight, then the Ghost (§5.3)
Spinner · ProgressBar     progress detached from cause       → Yield (§5.5)
Tooltip · coach mark      instruction attached to a thing    → the Tell (§5.1)
enabled: Boolean          a rule with no address             → requires(Gate) (§2.2)
duration · easing         local timing decisions             → the grammar (§3.2)
navigate(route)           teleportation                      → Enter(place, from) (Law 2)
EmptyState(text)          a paragraph where a control goes   → the Migration (§5.4)
onSuccess / onError toasts feedback in a different postcode  → Settled / Refused (§2.1)
```

Nine constructs removed. Every one of them exists in conventional toolkits to compensate for the same underlying flaw: appearance and behavior were modelled separately, so the connection between them has to be narrated after the fact. Model affordance instead and the narration has nothing to say.

This list is also the framework's own resourceful minimalism. The API is small not because it is unfinished but because each remaining piece is doing several jobs.

---

# Part IX — Applying This to Any App

The framework is a **spec** (this document), a set of **bindings** (per platform), and the **Conscience** (a lint/CI tool). Adoption is incremental and does not require a rewrite.

### 9.1 The five steps

1. **Inventory the verbs.** Walk every screen and write down every action as one of the six `Consequence` classes. This alone typically finds the product's real problems: the actions nobody can classify are the ones users cannot predict either.
2. **Assign the channels.** Fill in the Part IV table for your product, once. Then remove every variation that is not carrying its assigned meaning. This is the largest single visual change and it usually subtracts.
3. **Collapse the feedback.** For each action, find its spinner, its toast, and its error banner, and fold all three back into the control. Delete the orphans. Law 1 first, because it is the most mechanical and produces the most immediate relief.
4. **Wire continuity.** Give every destination an origin. Replace cross-fades with morphs. Focus follows the morph.
5. **Name the Keystone.** Pick one. Spend the budget there and nowhere else.

Steps 1–3 can be done on a live product a screen at a time. Step 4 is the one that needs coordination. Step 5 is a decision, not work.

### 9.2 A worked example — "Send invoice"

**Before.** A `Send` button. Pressing it opens a confirmation dialog. Confirming shows a modal spinner. On success, a green snackbar says "Invoice sent successfully." On failure, a red snackbar says "Something went wrong. Please try again." The recipient's name is in a field above. Six elements, four of them existing purely to report on the other two, and two text strings apologizing for the design.

**After.**

```
Act(
  identity    = "invoice.send",
  subject     = invoice,
  requires    = [ Gate(recipient != null, livesAt = recipientField) ],
  consequence = Send(invoice, to = recipientAvatar),
)
```

What the person experiences: with no recipient, the button resists and carries them to the recipient field, which articulates as they arrive. With a recipient, the press compresses the button and fills it as the work runs — and the invoice's card lifts, travels toward the recipient's avatar, and diminishes into it. The avatar takes on heat. The button settles. Nothing announces anything.

Six elements became two, four strings became zero, and the person now knows — because they watched it — that this product sends things *to people*, and that the little avatar is where sent things go. The next time they meet an unfamiliar control that flies something toward an avatar, they will already know what it did.

That transfer is conveyance. It is also the entire return on the framework: **the cost is paid once, in grammar; the benefit compounds across every feature you ever ship.**

### 9.3 Grading an existing product

Score 0–2 on each: One Element, Continuity, Grammar consistency, Employment, Reversibility, Instruction load, Channel discipline, Keystone discipline. Sixteen points. Most shipping products score 3–6, and almost all of the recoverable ground is in Law 1 and Law 5, which are also the cheapest to fix.

---

# Part X — Where This Is Hard

Stated plainly, because a framework that only lists its strengths is doing a strip tease.

**Discoverability of rare features.** Conveyance teaches what is in front of you. A feature used twice a year has no ambient teaching surface, and the Tell budget will never reach it. The honest answer is that such features belong in a deliberate, searchable index — a command surface — and that the framework should stop pretending everything can be conveyed. Some things are looked up. Making that surface excellent is better than sprinkling hints.

**Density-first professional tools.** A trading terminal or a DAW is operated by experts at speed on a grid of thousands of live values. Continuity morphs cost time those users will not spend. Practice-decay (§6.3) handles part of it; the rest is a real limit. The grammar and Channel Economy survive at high density. The ceremony does not.

**Text-first and indexed surfaces.** Marketing pages, documentation, anything that lives or dies by search: prose is the product. The framework applies to the application, not to the essay.

**Cross-platform grammar drift.** A verb's signature must be recognizably the same on phone, desktop, and web, while respecting each platform's conventions. This is the hardest engineering problem in the framework, and the resolution is that **spatial relationships are normative and rendering details are not**: `Enter` must always grow from its origin, but how far and how fast is platform-owned.

**Legacy integration.** A screen half-converted is worse than either extreme, because a grammar with holes teaches nothing and the user learns to distrust it. Convert by whole surfaces, never by individual controls.

**The framework can be complied with and still be bad.** Every audit can pass on a product that is confusing, because the audits check structure and conveyance is ultimately about whether a person's prediction matches the world. The Conscience prevents the known failure modes; it does not manufacture insight. The test that matters has never changed and cannot be automated: **hand it to someone who has never seen it, say nothing, and watch.**

---

# Appendix A — The Whole Framework, One Page

*If this page is not enough to use the framework, the framework has failed its own Two-Sided Rule.*

```
LAWS       1 One Element      invitation, progress, result, failure = same pixels
           2 Continuity       nothing appears from nowhere; nothing goes nowhere
           3 Grammar          one signature per verb, everywhere, for nothing else
           4 Employment       two jobs minimum, or merge; zero jobs, delete
           5 Benefit          reverse not warn · demonstrate not instruct · escort not block

NOUNS      Subject  Place  Act  Gate  Consequence  Keystone

ACT        identity · subject · requires[Gate] · consequence! · inverse(if destructive)!
STATES     Ready → Blocked(gate) | Yielding(extent) → Settled(result) | Refused(reason)

VERBS      Reveal   grow from the touched edge, nothing translates
           Enter    the element becomes the place
           Return   the place becomes the element again, where it was
           Create   precipitate from the control, travel to the collection
           Destroy  collapse in place, leave a Ghost
           Alter    only the changed property moves
           Send     travel toward the destination, diminish into it
           Refuse   resist, then escort to the gate
           Yield    the engaged element deforms under load

WEIGHT     Light / Medium / Heavy — derived from consequence, never chosen. No durations.

CHANNELS   position=origin  size=momentary importance  shape=state  hue=rank
           chroma=heat  elevation=REVERSIBILITY  opacity=transition only
           type=reading order  density=relatedness  motion=grammar
           haptics=magnitude  sound=Keystone only

BEHAVIORS  Tell       an unpracticed element does a half-rep of itself, once
           Escort     a blocked act carries you to its gate
           Ghost      a destroyed thing leaves a recoverable residue in its own place
           Migration  the empty state is the creation control, which then moves home
           Yield      progress is a deformation of what you touched
           First Move the zero state is arranged to teach the loop

ABSENT     Toast · Dialog · Spinner · Tooltip · enabled: Boolean · duration
           navigate(route) · EmptyState(text) · success & error callbacks-to-nowhere

AUDITS     Idle Worker · Instruction · Teleport · Orphan Feedback · Duration
           Primary Contention · Keystone Budget · Reversibility · Channel · Dead End

TEXT       nouns and verbs on screen; full sentences in the accessibility tree; never both
```

---

# Appendix B — Reference Binding Sketch (Jetpack Compose)

Illustrative, not final. Shown to demonstrate that the model survives contact with a real toolkit.

```kotlin
// The only control constructor. There is no Button.
@Composable
fun Act(
    identity: ActId,
    consequence: Consequence,          // required, and names its target
    requires: List<Gate> = emptyList(),
    inverse: Act? = null,              // compile-checked against Consequence.Destroy
    content: ActScope.() -> Unit,      // renders all five states; scope exposes state
)

// Places are entered, never navigated to.
@Composable
fun Place(identity: PlaceId, origin: ElementRef, content: @Composable () -> Unit)

// Gates know where they live.
fun Gate(satisfied: Boolean, livesAt: ElementRef): Gate

// Collections know how to be empty.
@Composable
fun <T> Collection(items: List<T>, creator: Act, item: @Composable (T) -> Unit)
// empty  → creator centered, full size          (the Migration)
// filled → creator at its home position
```

Notes on the binding:

- `ActScope` exposes the current state, so a single lambda paints Ready, Blocked, Yielding, Settled and Refused. It is not possible to write a control that handles only one of them.
- `ElementRef` is produced by a modifier, so origins and gate locations are real measured positions — Escort and Enter are geometry, not configuration.
- The grammar lives in the runtime. Product code never names a spring.
- The Conscience is a Kotlin compiler plugin plus a lint ruleset; audits 3, 4, 5, 8 and 10 are compile-time, the rest are lint.
