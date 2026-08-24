package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Claims a [Surface] and a [Product] cannot make, because the illegal state no longer has a
 * constructor to reach. These used to be Conscience findings, caught whenever an audit happened to
 * run; they are refused at the call site now.
 */
class SurfaceTest {

    private fun element(id: String, rank: Rank) =
        DeclaredElement(ElementId(id), Employment.Working(Job.Invite, Job.Report), rank = rank)

    @Test
    fun `two primaries on a surface cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> {
            Surface(
                "s",
                elements = listOf(element("a", Rank.Primary), element("b", Rank.Primary)),
            )
        }
    }

    @Test
    fun `one primary among secondaries and tertiaries is fine`() {
        Surface(
            "s",
            elements = listOf(
                element("a", Rank.Primary),
                element("b", Rank.Secondary),
                element("c", Rank.Tertiary),
            ),
        )
        assertTrue(true, "Construction did not throw.")
    }

    @Test
    fun `a product with no keystone cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> { Product("p", keystones = emptyList()) }
    }

    @Test
    fun `a product with more than three keystones cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> {
            Product("p", keystones = (1..4).map { ActId("k$it") })
        }
    }

    @Test
    fun `between one and three keystones is fine`() {
        Product("p", keystones = listOf(ActId("k1")))
        Product("p", keystones = (1..3).map { ActId("k$it") })
        assertTrue(true, "Construction did not throw.")
    }

    @Test
    fun `a channel can only ever mean what it carries, because there is nowhere left to say otherwise`() {
        // The old defect was a Map<Channel, Meaning> that could pair a channel with the wrong
        // meaning. A Set<Channel> has no second value to get wrong -- Opacity in the set means
        // exactly Meaning.TransitionOnly, because that is the only meaning Opacity carries.
        val element = DeclaredElement(
            ElementId("a"),
            Employment.Working(Job.Invite, Job.Report),
            channels = setOf(Channel.Opacity, Channel.Elevation),
        )
        assertTrue(Channel.Opacity in element.channels)
        assertTrue(element.channels.all { it.carries == it.carries }, "Tautological, and that is the point.")
    }
}
