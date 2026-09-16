package com.secretsanta.app.draw

import kotlin.random.Random

/**
 * Sorteio de amigo secreto.
 *
 * Um sorteio válido é uma permutação **sem pontos fixos** (ninguém tira a si
 * mesmo) que também respeita os pares bloqueados pelo organizador. Disso
 * decorre a garantia que define o produto: toda pessoa presenteia exatamente
 * uma vez e é presenteada exatamente uma vez. Ninguém fica de fora e ninguém é
 * sorteado duas vezes.
 *
 * Lógica pura, sem dependência de Android, para poder ser testada em JVM.
 */
object SecretSantaEngine {

    /**
     * Com duas pessoas só existe um resultado possível e as duas já sabem qual
     * é — não há segredo a preservar. O sorteio começa a fazer sentido em três.
     */
    const val MIN_PARTICIPANTS = 3

    /** Teto generoso: acima disso a tela de envio deixa de ser utilizável. */
    const val MAX_PARTICIPANTS = 100

    /** Aceita nomes separados por quebra de linha, vírgula ou ponto e vírgula. */
    private val SEPARATORS = Regex("[\r\n,;]+")

    /**
     * Quantas permutações cíclicas aleatórias tentar antes de partir para a
     * busca exata. Sem restrições a primeira já serve; o limite só existe para
     * o caso de restrições densas, em que sortear às cegas é improdutivo.
     */
    private const val CYCLE_ATTEMPTS = 200

    /**
     * Quebra um texto colado (da lista do grupo do WhatsApp, por exemplo) em
     * nomes. Espaços nas pontas somem e entradas vazias são descartadas, de modo
     * que vírgulas sobrando não virem "participantes fantasma".
     */
    fun parse(raw: String): List<String> =
        raw.split(SEPARATORS)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Primeiro nome repetido na lista, ou `null` se todos forem distintos.
     * A comparação ignora caixa e espaços nas pontas, mas **não** acentos:
     * "Ana" e "Ana" são a mesma pessoa, "Jose" e "José" podem não ser.
     */
    fun findDuplicate(names: List<String>): String? {
        val seen = HashSet<String>(names.size)
        for (name in names) {
            if (!seen.add(name.trim().lowercase())) return name
        }
        return null
    }

    /**
     * Sorteia. [blocked] são pares ordenados proibidos; o auto-sorteio já é
     * proibido implicitamente e não precisa ser informado.
     *
     * A estratégia tem duas fases porque os dois cenários são muito diferentes:
     *
     * 1. **Ciclo aleatório.** Uma permutação cíclica única nunca tem ponto fixo,
     *    então já nasce válida quando não há restrições — que é o caso da
     *    esmagadora maioria dos sorteios. De quebra rende a propriedade que as
     *    pessoas gostam: uma corrente só, sem pares que se tiram mutuamente.
     * 2. **Busca exata.** Se as restrições derrubarem as tentativas acima, cai
     *    para um emparelhamento bipartido (algoritmo de Kuhn) com ordens
     *    embaralhadas. Ele é polinomial e **completo**: se existe solução, ele
     *    acha; se ele não acha, é porque não existe. Daí
     *    [DrawError.IMPOSSIBLE_RESTRICTIONS] ser uma afirmação e não um chute —
     *    nunca devolvemos "não deu" por ter desistido de tentar.
     */
    fun draw(
        names: List<String>,
        blocked: Set<BlockedPair> = emptySet(),
        random: Random = Random.Default,
    ): DrawOutcome {
        val n = names.size
        if (n < MIN_PARTICIPANTS) return DrawOutcome.Failure(DrawError.TOO_FEW_PARTICIPANTS)
        if (n > MAX_PARTICIPANTS) return DrawOutcome.Failure(DrawError.TOO_MANY_PARTICIPANTS)
        findDuplicate(names)?.let {
            return DrawOutcome.Failure(DrawError.DUPLICATE_NAME, offendingName = it)
        }

        // Matriz de permissão: allowed[g][r] = g pode tirar r.
        val allowed = Array(n) { g ->
            BooleanArray(n) { r -> r != g && BlockedPair(g, r) !in blocked }
        }
        val candidates = Array(n) { g -> (0 until n).filter { allowed[g][it] } }

        // Atalho barato: alguém sem nenhum destino possível já condena o sorteio.
        if (candidates.any { it.isEmpty() }) {
            return DrawOutcome.Failure(DrawError.IMPOSSIBLE_RESTRICTIONS)
        }

        repeat(CYCLE_ATTEMPTS) {
            val attempt = randomCycle(n, random)
            if ((0 until n).all { allowed[it][attempt[it]] }) {
                return success(names, attempt)
            }
        }

        val matched = perfectMatching(candidates, random)
            ?: return DrawOutcome.Failure(DrawError.IMPOSSIBLE_RESTRICTIONS)
        return success(names, matched)
    }

    private fun success(names: List<String>, receiverOf: IntArray) =
        DrawOutcome.Success(names.indices.map { Assignment(names[it], names[receiverOf[it]]) })

    /**
     * Permutação formada por um único ciclo cobrindo todo mundo. Embaralha a
     * ordem e faz cada um apontar para o seguinte, fechando no primeiro — o que
     * garante, por construção, que ninguém aponta para si mesmo.
     */
    private fun randomCycle(n: Int, random: Random): IntArray {
        val order = (0 until n).shuffled(random)
        val receiverOf = IntArray(n)
        for (i in 0 until n) {
            receiverOf[order[i]] = order[(i + 1) % n]
        }
        return receiverOf
    }

    /**
     * Emparelhamento bipartido completo entre quem dá e quem recebe (Kuhn, com
     * caminhos aumentantes). Devolve `null` quando não existe nenhum — ou seja,
     * quando as restrições realmente tornam o sorteio impossível.
     *
     * As ordens são embaralhadas para que sorteios sob restrição continuem
     * variando de uma vez para outra em vez de devolverem sempre o mesmo par.
     */
    private fun perfectMatching(candidates: Array<List<Int>>, random: Random): IntArray? {
        val n = candidates.size
        val receiverOf = IntArray(n) { -1 }
        val giverOf = IntArray(n) { -1 }

        for (giver in (0 until n).shuffled(random)) {
            val visited = BooleanArray(n)
            if (!augment(giver, candidates, random, visited, receiverOf, giverOf)) return null
        }
        return receiverOf
    }

    private fun augment(
        giver: Int,
        candidates: Array<List<Int>>,
        random: Random,
        visited: BooleanArray,
        receiverOf: IntArray,
        giverOf: IntArray,
    ): Boolean {
        for (receiver in candidates[giver].shuffled(random)) {
            if (visited[receiver]) continue
            visited[receiver] = true

            val incumbent = giverOf[receiver]
            if (incumbent == -1 ||
                augment(incumbent, candidates, random, visited, receiverOf, giverOf)
            ) {
                giverOf[receiver] = giver
                receiverOf[giver] = receiver
                return true
            }
        }
        return false
    }
}
