package com.secretsanta.app.draw

import com.secretsanta.app.data.Group
import com.secretsanta.app.data.Participant
import com.secretsanta.app.data.Restriction
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDrawTest {

    private fun group(vararg names: String) = Group(
        id = "g",
        name = "Grupo",
        participants = names.map { Participant(id = it.lowercase(), name = it) },
    )

    @Test
    fun `restricoes viram os indices certos`() {
        val subject = group("Ana", "Bruno", "Carla", "Diego")
            .withMutualRestriction("bruno", "diego")

        // Bruno é o índice 1, Diego o 3.
        assertEquals(
            setOf(BlockedPair(1, 3), BlockedPair(3, 1)),
            subject.blockedPairs(),
        )
    }

    @Test
    fun `restricao de mao unica nao vira mutua na traducao`() {
        val subject = group("Ana", "Bruno", "Carla")
            .copy(restrictions = listOf(Restriction("ana", "carla")))

        assertEquals(setOf(BlockedPair(0, 2)), subject.blockedPairs())
    }

    @Test
    fun `restricao orfa e descartada em vez de apontar para outra pessoa`() {
        val subject = group("Ana", "Bruno", "Carla")
            .copy(restrictions = listOf(Restriction("ana", "fantasma")))

        assertTrue(subject.blockedPairs().isEmpty())
    }

    /**
     * O bug que este teste existe para impedir: reordenar ou remover gente muda
     * os índices, e uma tradução feita por posição passaria a bloquear o par
     * errado sem nenhum sintoma visível.
     */
    @Test
    fun `restricao continua valendo para as mesmas pessoas depois de remover alguem`() {
        // Cinco pessoas para que sobrem quatro: com três e um par bloqueado, os
        // dois bloqueados só teriam a mesma pessoa para tirar, e o sorteio seria
        // legitimamente impossível.
        val subject = group("Ana", "Bruno", "Carla", "Diego", "Elza")
            .withMutualRestriction("carla", "diego")
            .withoutParticipant("ana")

        // Sem Ana, Carla passa a ser o índice 1 e Diego o 2.
        assertEquals(
            setOf(BlockedPair(1, 2), BlockedPair(2, 1)),
            subject.blockedPairs(),
        )

        repeat(30) { seed ->
            val outcome = subject.drawAssignments(Random(seed)) as DrawOutcome.Success
            assertTrue(
                "Carla e Diego se tiraram: ${outcome.assignments}",
                outcome.assignments.none {
                    (it.giver == "Carla" && it.receiver == "Diego") ||
                        (it.giver == "Diego" && it.receiver == "Carla")
                },
            )
        }
    }
}
