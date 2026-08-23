package com.hereliesaz.conveyance

/**
 * Something a person can do.
 *
 * An act is not a button. A button is an appearance with a callback attached, and the gap between
 * that appearance and what actually happens is exactly where instruction has to be inserted to patch
 * things up — the tooltip, the "Are you sure?", the toast reporting on something that happened
 * somewhere off-screen. An act closes the gap by carrying its own consequence, its own conditions
 * and its own reversal, so there is nothing left to narrate.
 *
 * There is no public constructor. Acts are made through one of the six verb factories below, each of
 * which takes exactly what its verb needs and nothing else. That is deliberate, and it is the
 * framework applying its own rules to itself: a single constructor covering all six cases would have
 * grown a parameter per special case until it needed a manual, which is how component libraries die.
 */
class Act private constructor(
    val id: ActId,
    val consequence: Consequence,
    val scope: Scope,
    val requires: List<Gate>,
    /** The act that undoes this one. Present for every [Consequence.Destroy], by construction. */
    val inverse: Act?,
    /** Marked as one of the product's one-to-three emotional cores. Budgeted, not sprinkled. */
    val keystone: Boolean,
    private val perform: suspend () -> Outcome,
) {
    /** Which of the nine verbs this act speaks. Derived; there is no routing decision to make. */
    val verb: Verb get() = Verb.of(consequence)

    /** The motion this act will produce. Derived. */
    val signature: Signature get() = Grammar.of(consequence)

    val reversible: Boolean get() = inverse != null

    /**
     * The inertia this act's motion carries, and therefore how costly it feels in the hand.
     * Derived from consequence, scope and reversibility — never chosen, never a parameter.
     */
    val weight: Weight get() = Weight.of(consequence, scope, reversible)

    /** The first unmet condition, or null when the world is ready. */
    fun blockingGate(): Gate? = requires.firstOrNull { !it.satisfied }

    /** [ActState.Ready], or [ActState.Blocked] naming the gate and therefore its address. */
    fun state(): ActState = blockingGate()?.let(ActState::Blocked) ?: ActState.Ready

    /**
     * Engage the act, reporting each state to [emit] as the same element passes through them.
     *
     * A blocked act does **not** fail and does not do nothing: it returns [ActState.Blocked], which
     * is the binding's cue to escort the person to the gate's address. Refusing in place, or greying
     * out, would be the framework abandoning someone at exactly the moment they needed carrying.
     *
     * @return the terminal state — Blocked, Settled, or Refused.
     */
    suspend fun engage(emit: (ActState) -> Unit = {}): ActState {
        blockingGate()?.let { gate ->
            val blocked = ActState.Blocked(gate)
            emit(blocked)
            return blocked
        }
        emit(ActState.Yielding())
        val terminal = when (val outcome = runCatching { perform() }.getOrElse {
            Outcome.Failed(Refusal.Interrupted)
        }) {
            Outcome.Done -> ActState.Settled
            is Outcome.Failed -> ActState.Refused(outcome.refusal)
        }
        emit(terminal)
        return terminal
    }

    override fun toString() = "Act($id, $verb, $weight${if (keystone) ", keystone" else ""})"

    companion object {

        /** More of what is already here becomes visible. */
        fun reveal(
            id: String,
            target: ElementId,
            requires: List<Gate> = emptyList(),
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Reveal(target), Scope.Detail, requires,
            inverse = null, keystone = false, perform = perform,
        )

        /**
         * The person goes somewhere, and [from] is the element that becomes it.
         *
         * There is no overload without an origin. A place with no antecedent is a teleport, and a
         * person who has been teleported needs a breadcrumb trail — in words — to rebuild the map
         * they just lost.
         */
        fun enter(
            id: String,
            place: PlaceId,
            from: ElementId,
            requires: List<Gate> = emptyList(),
            keystone: Boolean = false,
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Enter(place, from), Scope.Item, requires,
            inverse = null, keystone = keystone, perform = perform,
        )

        /** A new subject exists, in a named collection, having come out of this control. */
        fun create(
            id: String,
            subject: SubjectId,
            into: ElementId,
            scope: Scope = Scope.Item,
            requires: List<Gate> = emptyList(),
            keystone: Boolean = false,
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Create(subject, into), scope, requires,
            inverse = null, keystone = keystone, perform = perform,
        )

        /**
         * A subject ceases — and [inverse] is not optional.
         *
         * This framework has no confirmation dialog and cannot grow one, so reversibility is the
         * only safety mechanism on offer. That is the point: the cheap patronising option is
         * unavailable, and the respectful one is the only path. What [inverse] buys the person is
         * the Ghost — a recoverable residue left exactly where the subject was, rather than an
         * interruption before the fact or an undo bar in a different postcode afterwards.
         */
        fun destroy(
            id: String,
            subject: SubjectId,
            target: ElementId,
            inverse: Act,
            scope: Scope = Scope.Item,
            requires: List<Gate> = emptyList(),
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Destroy(subject, target), scope, requires,
            inverse = inverse, keystone = false, perform = perform,
        )

        /** A subject changes in place. Only the changed property moves. */
        fun alter(
            id: String,
            subject: SubjectId,
            property: String,
            target: ElementId,
            scope: Scope = Scope.Detail,
            requires: List<Gate> = emptyList(),
            inverse: Act? = null,
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Alter(subject, property, target), scope, requires,
            inverse = inverse, keystone = false, perform = perform,
        )

        /** A subject leaves the person's control, toward something they can see. */
        fun send(
            id: String,
            subject: SubjectId,
            to: ElementId,
            scope: Scope = Scope.Item,
            requires: List<Gate> = emptyList(),
            keystone: Boolean = false,
            inverse: Act? = null,
            perform: suspend () -> Outcome = { Outcome.Done },
        ) = Act(
            ActId(id), Consequence.Send(subject, to), scope, requires,
            inverse = inverse, keystone = keystone, perform = perform,
        )
    }
}
