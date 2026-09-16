package com.secretsanta.app.draw

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretSantaEngineTest {

    // region utilidades

    private fun names(n: Int) = (1..n).map { "P$it" }

    /**
     * A promessa do app, verificada por inteiro: todo mundo presenteia uma vez,
     * todo mundo é presenteado uma vez, ninguém tira a si mesmo e nenhum par
     * bloqueado aparece.
     */
    private fun assertValid(
        names: List<String>,
        blocked: Set<BlockedPair>,
        outcome: DrawOutcome,
    ) {
        val success = outcome as? DrawOutcome.Success
            ?: error("esperava sorteio válido, veio $outcome")
        val assignments = success.assignments

        assertEquals("todo mundo presenteia", names.size, assignments.size)
        assertEquals("na ordem dos participantes", names, assignments.map { it.giver })
        assertEquals(
            "cada pessoa é presenteada exatamente uma vez",
            names.toSet(),
            assignments.map { it.receiver }.toSet(),
        )
        assertEquals(
            "sem receptor repetido",
            assignments.size,
            assignments.map { it.receiver }.distinct().size,
        )
        assignments.forEach {
            assertTrue("${it.giver} tirou a si mesmo", it.giver != it.receiver)
        }
        blocked.forEach { pair ->
            val giver = names[pair.giver]
            val forbidden = names[pair.receiver]
            assertTrue(
                "$giver tirou $forbidden, que estava bloqueado",
                assignments.none { it.giver == giver && it.receiver == forbidden },
            )
        }
    }

    /** Existe alguma permutação válida? Resposta exata, por força bruta. */
    private fun solvableByBruteForce(n: Int, blocked: Set<BlockedPair>): Boolean {
        val indices = (0 until n).toList()
        return permutations(indices).any { perm ->
            perm.withIndex().all { (giver, receiver) ->
                giver != receiver && BlockedPair(giver, receiver) !in blocked
            }
        }
    }

    private fun permutations(items: List<Int>): Sequence<List<Int>> =
        if (items.size <= 1) sequenceOf(items)
        else items.asSequence().flatMap { head ->
            permutations(items - head).map { tail -> listOf(head) + tail }
        }

    // endregion

    @Test
    fun `sorteio simples respeita todas as garantias`() {
        val people = names(8)
        assertValid(people, emptySet(), SecretSantaEngine.draw(people, random = Random(1)))
    }

    @Test
    fun `sem restricoes o resultado e uma corrente unica`() {
        val people = names(10)
        val success = SecretSantaEngine.draw(people, random = Random(7)) as DrawOutcome.Success
        val receiverOf = success.assignments
            .associate { people.indexOf(it.giver) to people.indexOf(it.receiver) }

        // Partindo de qualquer um, só voltamos ao início depois de passar por todos.
        var current = 0
        var steps = 0
        do {
            current = receiverOf.getValue(current)
            steps++
        } while (current != 0)
        assertEquals("deveria ser um ciclo único cobrindo o grupo", people.size, steps)
    }

    @Test
    fun `casais nao se tiram`() {
        val people = listOf("Ana", "Bruno", "Carla", "Diego", "Elza", "Fabio")
        // Ana-Bruno e Carla-Diego são casais: bloqueio nos dois sentidos.
        val blocked = setOf(
            BlockedPair(0, 1), BlockedPair(1, 0),
            BlockedPair(2, 3), BlockedPair(3, 2),
        )
        repeat(50) { seed ->
            assertValid(people, blocked, SecretSantaEngine.draw(people, blocked, Random(seed)))
        }
    }

    @Test
    fun `restricoes densas ainda encontram solucao quando ela existe`() {
        val people = names(6)
        // Cada um proibido de tirar os dois seguintes: sobra pouca folga, o
        // suficiente para as tentativas cíclicas falharem e a busca exata entrar.
        val blocked = (0 until 6).flatMap { g ->
            listOf(BlockedPair(g, (g + 1) % 6), BlockedPair(g, (g + 2) % 6))
        }.toSet()
        repeat(30) { seed ->
            assertValid(people, blocked, SecretSantaEngine.draw(people, blocked, Random(seed)))
        }
    }

    @Test
    fun `restricoes impossiveis devolvem erro em vez de travar`() {
        val people = names(3)
        // P1 e P2 só poderiam tirar P3 — os dois ao mesmo tempo, o que não existe.
        val blocked = setOf(BlockedPair(0, 1), BlockedPair(1, 0))
        assertEquals(
            DrawOutcome.Failure(DrawError.IMPOSSIBLE_RESTRICTIONS),
            SecretSantaEngine.draw(people, blocked, Random(0)),
        )
    }

    @Test
    fun `participante sem nenhum destino possivel devolve erro`() {
        val people = names(4)
        val blocked = (1 until 4).map { BlockedPair(0, it) }.toSet()
        assertEquals(
            DrawOutcome.Failure(DrawError.IMPOSSIBLE_RESTRICTIONS),
            SecretSantaEngine.draw(people, blocked, Random(0)),
        )
    }

    /**
     * O teste que fecha a questão: para grupos pequenos, o veredito da engine é
     * conferido contra a enumeração de todas as permutações. Ela nunca pode
     * devolver um sorteio inválido, nem alegar "impossível" quando existe saída.
     */
    @Test
    fun `veredito bate com a forca bruta em todos os casos pequenos`() {
        val random = Random(20261015)
        var impossibleSeen = 0
        var solvedSeen = 0

        repeat(400) {
            val n = random.nextInt(SecretSantaEngine.MIN_PARTICIPANTS, 7)
            val people = names(n)
            val blocked = buildSet {
                repeat(random.nextInt(0, n * 2)) {
                    val giver = random.nextInt(n)
                    val receiver = random.nextInt(n)
                    if (giver != receiver) add(BlockedPair(giver, receiver))
                }
            }

            val outcome = SecretSantaEngine.draw(people, blocked, random)
            if (solvableByBruteForce(n, blocked)) {
                solvedSeen++
                assertValid(people, blocked, outcome)
            } else {
                impossibleSeen++
                assertEquals(
                    "não existia solução, mas a engine devolveu $outcome",
                    DrawOutcome.Failure(DrawError.IMPOSSIBLE_RESTRICTIONS),
                    outcome,
                )
            }
        }

        // Garante que o teste exercitou os dois lados, e não só o caminho fácil.
        assertTrue("nenhum caso solúvel no sorteio do teste", solvedSeen > 0)
        assertTrue("nenhum caso impossível no sorteio do teste", impossibleSeen > 0)
    }

    @Test
    fun `grupos grandes continuam validos`() {
        val people = names(SecretSantaEngine.MAX_PARTICIPANTS)
        assertValid(people, emptySet(), SecretSantaEngine.draw(people, random = Random(3)))
    }

    @Test
    fun `nome repetido falha e aponta o culpado`() {
        val people = listOf("Ana", "Bruno", "ana ")
        assertEquals(
            DrawOutcome.Failure(DrawError.DUPLICATE_NAME, offendingName = "ana "),
            SecretSantaEngine.draw(people, random = Random(0)),
        )
    }

    @Test
    fun `acentos distinguem pessoas diferentes`() {
        assertNull(SecretSantaEngine.findDuplicate(listOf("Jose", "José")))
        assertNotNull(SecretSantaEngine.findDuplicate(listOf("José", "josé")))
    }

    @Test
    fun `grupo pequeno demais falha`() {
        assertEquals(
            DrawOutcome.Failure(DrawError.TOO_FEW_PARTICIPANTS),
            SecretSantaEngine.draw(names(2), random = Random(0)),
        )
    }

    @Test
    fun `grupo grande demais falha`() {
        assertEquals(
            DrawOutcome.Failure(DrawError.TOO_MANY_PARTICIPANTS),
            SecretSantaEngine.draw(names(SecretSantaEngine.MAX_PARTICIPANTS + 1), random = Random(0)),
        )
    }

    @Test
    fun `mesma semente gera o mesmo sorteio`() {
        val people = names(12)
        assertEquals(
            SecretSantaEngine.draw(people, random = Random(42)),
            SecretSantaEngine.draw(people, random = Random(42)),
        )
    }

    @Test
    fun `parse limpa lista colada`() {
        val raw = "Ana, Bruno\n Carla ;;\n\nDiego,"
        assertEquals(listOf("Ana", "Bruno", "Carla", "Diego"), SecretSantaEngine.parse(raw))
    }
}
