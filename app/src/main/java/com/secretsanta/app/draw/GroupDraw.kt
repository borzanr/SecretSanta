package com.secretsanta.app.draw

import com.secretsanta.app.data.Group
import kotlin.random.Random

/**
 * Ponte entre o grupo salvo e a engine.
 *
 * O grupo guarda restrições por id de participante; a engine trabalha por
 * índice. A tradução mora aqui, fora da UI, para poder ser testada — e porque
 * um erro nela viraria exatamente o bug que o app promete não ter: uma
 * restrição aplicada à pessoa errada.
 */
fun Group.blockedPairs(): Set<BlockedPair> {
    val indexOf = participants.withIndex().associate { (index, p) -> p.id to index }
    return restrictions.mapNotNullTo(mutableSetOf()) { restriction ->
        val giver = indexOf[restriction.giverId] ?: return@mapNotNullTo null
        val receiver = indexOf[restriction.receiverId] ?: return@mapNotNullTo null
        BlockedPair(giver, receiver)
    }
}

fun Group.drawAssignments(random: Random = Random.Default): DrawOutcome =
    SecretSantaEngine.draw(names, blockedPairs(), random)
