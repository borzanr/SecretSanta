package com.secretsanta.app.data

import kotlinx.serialization.Serializable

/**
 * Um participante do grupo.
 *
 * O [id] existe porque as restrições apontam para pessoas, e a posição na lista
 * não serve para isso: remover alguém do meio deslocaria todo mundo e faria uma
 * restrição passar a valer para outra pessoa, silenciosamente.
 */
@Serializable
data class Participant(
    val id: String,
    val name: String,
    val wishes: List<String> = emptyList(),
) {
    companion object {
        /** Sugestões de presente por pessoa. Três cabem na tela e no link. */
        const val MAX_WISHES = 3
    }
}

/** [giverId] não pode tirar [receiverId]. */
@Serializable
data class Restriction(val giverId: String, val receiverId: String)

/**
 * Um grupo de amigo secreto, salvo para poder ser reusado no ano seguinte.
 *
 * [budget] é texto livre de propósito: "R$ 50", "$20", "até 30 euros". Modelar
 * moeda e valor obrigaria a escolher formato por região e a validar entrada,
 * para um campo que só é lido por humanos na hora de revelar.
 */
@Serializable
data class Group(
    val id: String,
    val name: String,
    val budget: String = "",
    val participants: List<Participant> = emptyList(),
    val restrictions: List<Restriction> = emptyList(),
    val createdAt: Long = 0L,
    val drawnAt: Long? = null,
) {
    val names: List<String> get() = participants.map { it.name }

    fun participantById(id: String): Participant? = participants.firstOrNull { it.id == id }

    /**
     * Remove um participante **e** toda restrição que o envolvia. Deixar as
     * restrições órfãs para trás faria o sorteio ser recusado por uma regra que
     * não existe mais em tela.
     */
    fun withoutParticipant(participantId: String): Group = copy(
        participants = participants.filterNot { it.id == participantId },
        restrictions = restrictions.filterNot {
            it.giverId == participantId || it.receiverId == participantId
        },
    )

    fun withParticipant(participant: Participant): Group {
        val index = participants.indexOfFirst { it.id == participant.id }
        return copy(
            participants = if (index >= 0) {
                participants.toMutableList().apply { this[index] = participant }
            } else {
                participants + participant
            },
        )
    }

    /** Adiciona a restrição nos dois sentidos — casais e irmãos são o caso comum. */
    fun withMutualRestriction(firstId: String, secondId: String): Group {
        val pair = listOf(
            Restriction(firstId, secondId),
            Restriction(secondId, firstId),
        )
        return copy(restrictions = (restrictions + pair).distinct())
    }

    fun withoutRestrictionsBetween(firstId: String, secondId: String): Group = copy(
        restrictions = restrictions.filterNot {
            (it.giverId == firstId && it.receiverId == secondId) ||
                (it.giverId == secondId && it.receiverId == firstId)
        },
    )

    /** Pares distintos de pessoas que não podem se tirar, para listar em tela. */
    fun restrictionPairs(): List<Pair<Participant, Participant>> =
        restrictions
            .mapNotNull { restriction ->
                val giver = participantById(restriction.giverId) ?: return@mapNotNull null
                val receiver = participantById(restriction.receiverId) ?: return@mapNotNull null
                if (giver.id < receiver.id) giver to receiver else receiver to giver
            }
            .distinctBy { it.first.id to it.second.id }
}
