package com.secretsanta.app.draw

/** Motivos pelos quais um sorteio não pode ser realizado. A UI traduz cada um. */
enum class DrawError {
    /** Menos participantes que [SecretSantaEngine.MIN_PARTICIPANTS]. */
    TOO_FEW_PARTICIPANTS,

    /** Mais participantes que [SecretSantaEngine.MAX_PARTICIPANTS]. */
    TOO_MANY_PARTICIPANTS,

    /**
     * Dois participantes com o mesmo nome. Não dá para revelar "você tirou Ana"
     * quando existem duas Anas, então o organizador precisa desambiguar.
     * [DrawOutcome.Failure.offendingName] traz o nome repetido.
     */
    DUPLICATE_NAME,

    /**
     * As restrições eliminam todas as combinações possíveis. Isto é uma prova,
     * não um palpite: só é devolvido depois que a busca exata não encontrou
     * nenhum emparelhamento completo.
     */
    IMPOSSIBLE_RESTRICTIONS,
}

/**
 * Par ordenado proibido, por índice na lista de participantes: [giver] não pode
 * tirar [receiver].
 *
 * A engine trabalha só com pares ordenados. "Ana e Bruno são casados, não podem
 * se tirar" é uma decisão de produto que a UI traduz nos dois pares ordenados
 * correspondentes — assim uma restrição de mão única continua possível.
 */
data class BlockedPair(val giver: Int, val receiver: Int)

/** Quem presenteia quem. */
data class Assignment(val giver: String, val receiver: String)

sealed interface DrawOutcome {
    /** Em ordem de participante: `assignments[i].giver == names[i]`. */
    data class Success(val assignments: List<Assignment>) : DrawOutcome

    data class Failure(val error: DrawError, val offendingName: String? = null) : DrawOutcome
}
