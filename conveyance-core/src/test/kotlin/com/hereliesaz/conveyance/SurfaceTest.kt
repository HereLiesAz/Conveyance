package com.hereliesaz.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals

class SurfaceTest {

    private fun element(id: String, rank: Rank) =
        DeclaredElement(ElementId(id), Employment.Working(Job.Report), rank = rank)

    @Test
    fun `a surface may have several prominent elements`() {
        val surface = Surface(
            "s",
            elements = listOf(element("a", Rank.Primary), element("b", Rank.Primary)),
        )
        assertEquals(2, surface.elements.count { it.rank == Rank.Primary })
    }

    @Test
    fun `a surface may have no primary`() {
        val surface = Surface(
            "s",
            elements = listOf(element("a", Rank.Secondary), element("b", Rank.Tertiary)),
        )
        assertEquals(0, surface.elements.count { it.rank == Rank.Primary })
    }

    @Test
    fun `keystones are optional and unbudgeted`() {
        assertEquals(0, Product("p").keystones.size)
        assertEquals(8, Product("p", keystones = (1..8).map { ActId("k$it") }).keystones.size)
    }

    @Test
    fun `reference channels remain readable from declarations`() {
        val element = DeclaredElement(
            ElementId("a"),
            Employment.Working(Job.Report),
            channels = setOf(Channel.Hue, Channel.Elevation),
        )
        assertEquals(
            setOf(Meaning.VisualIdentity, Meaning.Reversibility),
            element.channels.map { it.carries }.toSet(),
        )
    }
}
