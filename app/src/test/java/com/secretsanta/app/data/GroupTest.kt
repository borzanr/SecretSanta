package com.secretsanta.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupTest {

    private fun group(vararg names: String) = Group(
        id = "g",
        name = "Grupo",
        participants = names.map { Participant(id = it.lowercase(), name = it) },
    )

    @Test
    fun `restricao mutua vale nos dois sentidos e nao duplica`() {
        val subject = group("Ana", "Bruno", "Carla")
            .withMutualRestriction("ana", "bruno")
            .withMutualRestriction("ana", "bruno")

        assertEquals(
            setOf(Restriction("ana", "bruno"), Restriction("bruno", "ana")),
            subject.restrictions.toSet(),
        )
    }

    @Test
    fun `remover participante leva junto as restricoes dele`() {
        val subject = group("Ana", "Bruno", "Carla")
            .withMutualRestriction("ana", "bruno")
            .withMutualRestriction("bruno", "carla")
            .withoutParticipant("bruno")

        assertEquals(listOf("Ana", "Carla"), subject.names)
        assertTrue(
            "restrição órfã sobrou: ${subject.restrictions}",
            subject.restrictions.isEmpty(),
        )
    }

    @Test
    fun `remover restricao apaga os dois sentidos`() {
        val subject = group("Ana", "Bruno")
            .withMutualRestriction("ana", "bruno")
            .withoutRestrictionsBetween("bruno", "ana")

        assertTrue(subject.restrictions.isEmpty())
    }

    @Test
    fun `pares para exibicao aparecem uma vez so`() {
        val subject = group("Ana", "Bruno", "Carla")
            .withMutualRestriction("ana", "bruno")
            .withMutualRestriction("bruno", "carla")

        val pairs = subject.restrictionPairs().map { (a, b) -> a.name to b.name }
        assertEquals(2, pairs.size)
        assertEquals(setOf("Ana" to "Bruno", "Bruno" to "Carla"), pairs.toSet())
    }

    @Test
    fun `restricao apontando para quem nao existe mais e ignorada na exibicao`() {
        val subject = group("Ana", "Bruno")
            .copy(restrictions = listOf(Restriction("ana", "fantasma")))

        assertTrue(subject.restrictionPairs().isEmpty())
    }

    @Test
    fun `editar participante substitui em vez de acrescentar`() {
        val subject = group("Ana", "Bruno")
            .withParticipant(Participant(id = "ana", name = "Ana Maria", wishes = listOf("livro")))

        assertEquals(listOf("Ana Maria", "Bruno"), subject.names)
        assertEquals(listOf("livro"), subject.participantById("ana")?.wishes)
    }
}
