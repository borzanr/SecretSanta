package com.secretsanta.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta tirada da arte-chave (`playstore/art/`): o mago sorteando papéis do
 * caldeirão. Roxo profundo de manto, dourado de brasão e o azul do cristal do
 * cajado, sobre um fundo violeta-tinta em vez de preto.
 *
 * Deliberadamente diferente do dark neon do LuckyDraw, e deliberadamente **não**
 * vermelho-e-verde de Natal: o registro é "sorteio mágico", o que também livra o
 * app de parecer descartável fora de dezembro — amigo secreto de empresa e de
 * confraternização acontece o ano todo.
 */
val Ink = Color(0xFF140E20)
val Surface1 = Color(0xFF1E1630)
val Surface2 = Color(0xFF2A2042)
val Outline = Color(0xFF3D3157)

/** Dourado é reservado para a ação principal ("Sortear"). */
val Gold = Color(0xFFE0B354)
val Amethyst = Color(0xFF7C5BC7)
val Crystal = Color(0xFF6BD6F0)

val TextPrimary = Color(0xFFF2E8D5)
val TextSecondary = Color(0xFFA99BC0)
val Danger = Color(0xFFE5687B)

/** Fundo de avisos (links desatualizados): vinho escuro, legível sobre o violeta. */
val DangerContainer = Color(0xFF4A1F33)

/** Cores dos confetes (ARGB int, formato exigido pelo Konfetti). */
val ConfettiColors = listOf(0xE0B354, 0x7C5BC7, 0x6BD6F0, 0xF2E8D5, 0xC08A3E)
