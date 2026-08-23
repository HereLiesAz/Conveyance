package com.hereliesaz.conveyance

/**
 * Somewhere a person can be.
 *
 * [origin] is not optional and has no default. A place with no antecedent is a teleport, and a
 * person who has been teleported has lost the map they were building; the usual remedy is a
 * breadcrumb trail, which is instruction, which is the thing this framework exists to avoid.
 *
 * There is exactly one exception, [Root], for a product's genuine entry point. It is budgeted rather
 * than merely discouraged, because "this one is special" is how every rule dies.
 */
class Place private constructor(
    val id: PlaceId,
    /** The element this place grows out of, and shrinks back into on the way out. */
    val origin: ElementId?,
    val subject: SubjectId? = null,
) {
    val isRoot: Boolean get() = origin == null

    override fun toString() =
        if (isRoot) "Place(${id}, root)" else "Place($id, from $origin)"

    companion object {
        /** A place reached from an element, which becomes it. */
        fun from(id: String, origin: ElementId, subject: SubjectId? = null) =
            Place(PlaceId(id), origin, subject)

        /** A product's entry point. Budgeted; everything else has an antecedent. */
        fun root(id: String) = Place(PlaceId(id), origin = null)
    }
}
