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

        /**
         * Limites de digitação. Tudo isto pode ir parar dentro de um link colado
         * no WhatsApp; os tetos mantêm o pior caso bem abaixo do tamanho em que
         * apps de mensagem começam a cortar URLs.
         */
        const val MAX_NAME = 40
        const val MAX_WISH = 60
    }
}

/**
 * O link de uma pessoa, já codificado. Guardado para que o organizador possa
 * enviar aos poucos, ao longo de dias, sem precisar sortear de novo.
 *
 * Só o link é guardado — nunca um par legível. Nenhuma tela do app decodifica
 * [url]; o organizador vê apenas [giverName] e se já marcou como enviado.
 */
@Serializable
data class SealedLink(
    val giverId: String,
    val giverName: String,
    val url: String,
    val sent: Boolean = false,
)

/**
 * Um sorteio feito para revelação por link.
 *
 * [fingerprint] é o [Group.contentFingerprint] do grupo no momento do sorteio.
 * Se o grupo muda depois — alguém entra, uma sugestão é editada — os links
 * já gerados ficam desatualizados, e a tela avisa em vez de enviar resultado
 * velho em silêncio.
 */
@Serializable
data class LinkDraw(
    val createdAt: Long,
    val fingerprint: String,
    val links: List<SealedLink>,
) {
    val sentCount: Int get() = links.count { it.sent }
    val isComplete: Boolean get() = links.isNotEmpty() && links.all { it.sent }
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
    val linkDraw: LinkDraw? = null,
) {
    val names: List<String> get() = participants.map { it.name }

    /**
     * Resumo de tudo que vai dentro dos links: nome do grupo, valor, pessoas,
     * sugestões e regras. Serve só para detectar mudança, não para segurança.
     */
    fun contentFingerprint(): String = buildString {
        // Separadores entre campos, para que nome "Ana" + valor "Bo" não gere o
        // mesmo resumo que nome "An" + valor "aBo".
        append(name).append("\u241F").append(budget)
        participants.forEach { p ->
            append("\u241E").append(p.id).append("\u241F").append(p.name)
            p.wishes.forEach { append("\u241D").append(it) }
        }
        restrictions.sortedBy { it.giverId + it.receiverId }.forEach {
            append("\u241C").append(it.giverId).append('>').append(it.receiverId)
        }
    }.hashCode().toUInt().toString(16)

    /** Os links existem mas o grupo mudou desde o sorteio. */
    val isLinkDrawStale: Boolean
        get() = linkDraw != null && linkDraw.fingerprint != contentFingerprint()

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
