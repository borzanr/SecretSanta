package com.secretsanta.app.share

import com.secretsanta.app.data.Group
import com.secretsanta.app.data.LinkDraw
import com.secretsanta.app.data.SealedLink
import com.secretsanta.app.draw.Assignment

/**
 * Transforma um sorteio em um link por pessoa.
 *
 * Cada link carrega só o que **aquela** pessoa pode ver: quem ela tirou e as
 * sugestões de presente de quem ela tirou. As sugestões dela mesma não vão no
 * próprio link — iriam no link de quem a tirou.
 *
 * @param language idioma da página de revelação ("en", "pt" ou "es"). Vem do
 *   aparelho do organizador: quem organiza em português manda para um grupo que,
 *   na imensa maioria dos casos, também lê português.
 */
fun Group.buildLinkDraw(
    assignments: List<Assignment>,
    language: String,
    now: Long,
): LinkDraw {
    val byName = participants.associateBy { it.name }
    val links = assignments.map { assignment ->
        val giver = byName.getValue(assignment.giver)
        val receiver = byName.getValue(assignment.receiver)
        val payload = RevealPayload(
            version = RevealLink.VERSION,
            event = name.trim(),
            giver = giver.name,
            receiver = receiver.name,
            budget = budget.trim(),
            wishes = receiver.wishes,
            language = language,
        )
        SealedLink(giverId = giver.id, giverName = giver.name, url = RevealLink.url(payload))
    }
    return LinkDraw(createdAt = now, fingerprint = contentFingerprint(), links = links)
}

/** Reduz um locale do Android aos idiomas que a página conhece. */
fun revealLanguage(languageTag: String): String = when (languageTag.take(2).lowercase()) {
    "pt" -> "pt"
    "es" -> "es"
    else -> "en"
}
