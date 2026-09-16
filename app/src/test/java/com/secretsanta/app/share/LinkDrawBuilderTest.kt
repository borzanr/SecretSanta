package com.secretsanta.app.share

import com.secretsanta.app.data.Group
import com.secretsanta.app.data.Participant
import com.secretsanta.app.draw.DrawOutcome
import com.secretsanta.app.draw.drawAssignments
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDrawBuilderTest {

    private val group = Group(
        id = "g",
        name = "Natal",
        budget = "R$ 50",
        participants = listOf(
            Participant("a", "Ana", listOf("livro")),
            Participant("b", "Bruno", listOf("caneca", "meia")),
            Participant("c", "Carla"),
            Participant("d", "Diego", listOf("vinho")),
        ),
    )

    private fun linkDraw() = group.buildLinkDraw(
        assignments = (group.drawAssignments(Random(1)) as DrawOutcome.Success).assignments,
        language = "pt",
        now = 123L,
    )

    @Test
    fun `cada link mostra a pessoa certa, quem ela tirou e as sugestoes de quem ela tirou`() {
        val assignments = (group.drawAssignments(Random(1)) as DrawOutcome.Success).assignments
        val draw = group.buildLinkDraw(assignments, "pt", 123L)

        assertEquals(group.participants.map { it.id }, draw.links.map { it.giverId })
        draw.links.forEach { link ->
            val payload = RevealLink.decode(link.url.substringAfter('#'))!!
            val expected = assignments.first { it.giver == link.giverName }
            val receiver = group.participants.first { it.name == expected.receiver }

            assertEquals(link.giverName, payload.giver)
            assertEquals(expected.receiver, payload.receiver)
            assertEquals("as sugestões são de quem foi tirado", receiver.wishes, payload.wishes)
            assertEquals("Natal", payload.event)
            assertEquals("R$ 50", payload.budget)
            assertEquals("pt", payload.language)
        }
    }

    @Test
    fun `links saem todos como nao enviados e apontam para a pagina`() {
        val draw = linkDraw()
        assertTrue(draw.links.none { it.sent })
        assertTrue(draw.links.all { it.url.startsWith(RevealLink.BASE_URL + "#") })
        assertEquals(0, draw.sentCount)
        assertFalse(draw.isComplete)
    }

    @Test
    fun `link fica desatualizado quando algo que vai nele muda`() {
        val drawn = group.copy(linkDraw = linkDraw())
        assertFalse(drawn.isLinkDrawStale)

        val edits = listOf(
            drawn.copy(name = "Natal 2026"),
            drawn.copy(budget = "R$ 80"),
            drawn.withParticipant(Participant("e", "Elza")),
            drawn.withParticipant(Participant("c", "Carla", listOf("chocolate"))),
            drawn.withMutualRestriction("a", "b"),
        )
        edits.forEach { assertTrue("não detectou mudança: $it", it.isLinkDrawStale) }
    }

    @Test
    fun `marcar como enviado nao desatualiza o sorteio`() {
        val draw = linkDraw()
        val marked = draw.copy(links = draw.links.map { it.copy(sent = true) })
        val drawn = group.copy(linkDraw = marked)
        assertFalse(drawn.isLinkDrawStale)
        assertTrue(marked.isComplete)
    }

    @Test
    fun `idioma do aparelho vira um dos tres idiomas da pagina`() {
        assertEquals("pt", revealLanguage("pt-BR"))
        assertEquals("pt", revealLanguage("pt"))
        assertEquals("es", revealLanguage("es-419"))
        assertEquals("en", revealLanguage("en-US"))
        assertEquals("en", revealLanguage("fr-FR"))
    }
}
