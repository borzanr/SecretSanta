package com.secretsanta.app.ui.reveal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.data.Group
import com.secretsanta.app.draw.Assignment
import com.secretsanta.app.ui.DrawPhase
import com.secretsanta.app.ui.theme.ConfettiColors
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

/**
 * Revelação presencial: o telefone circula e cada pessoa vê só o próprio par.
 *
 * O aparelho volta para a lista assim que a pessoa fecha o cartão, e quem já viu
 * some da lista — sem tela intermediária onde o par anterior ainda esteja visível
 * quando o celular já está na mão do próximo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassThePhoneScreen(
    group: Group,
    ready: DrawPhase.Ready,
    onRevealed: (String) -> Unit,
    onExit: () -> Unit,
) {
    var confirming by remember { mutableStateOf<Assignment?>(null) }
    var showing by remember { mutableStateOf<Assignment?>(null) }
    var confirmingExit by remember { mutableStateOf(false) }

    // Sair descarta o sorteio, que só existe em memória. Com gente no meio da
    // rodada isso significa recomeçar do zero, então o voltar pergunta antes —
    // um toque distraído aqui custaria a paciência do grupo inteiro.
    BackHandler {
        if (ready.revealed.isEmpty()) onExit() else confirmingExit = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reveal_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.reveal_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            items(ready.pending, key = { it.giver }) { assignment ->
                Card(
                    onClick = { confirming = assignment },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    ListItem(
                        headlineContent = {
                            Text(assignment.giver, style = MaterialTheme.typography.titleMedium)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (ready.isComplete) {
                        Text(
                            stringResource(R.string.reveal_all_done),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.reveal_finish))
                        }
                    } else {
                        Text(
                            pluralStringResource(
                                R.plurals.reveal_remaining,
                                ready.pending.size,
                                ready.pending.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Confirmação antes de revelar: impede que o organizador, segurando o
    // aparelho, abra o par de outra pessoa por um toque distraído.
    confirming?.let { assignment ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(R.string.reveal_confirm_title, assignment.giver)) },
            text = { Text(stringResource(R.string.reveal_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    showing = assignment
                }) { Text(stringResource(R.string.reveal_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) {
                    Text(stringResource(R.string.reveal_confirm_no))
                }
            },
        )
    }

    if (confirmingExit) {
        AlertDialog(
            onDismissRequest = { confirmingExit = false },
            title = { Text(stringResource(R.string.reveal_discard_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.reveal_discard_body,
                        ready.revealed.size,
                        ready.revealed.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingExit = false
                    onExit()
                }) { Text(stringResource(R.string.reveal_discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingExit = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    showing?.let { assignment ->
        RevealCard(
            assignment = assignment,
            budget = group.budget,
            wishes = group.participants.firstOrNull { it.name == assignment.receiver }?.wishes.orEmpty(),
            onHide = {
                showing = null
                onRevealed(assignment.giver)
            },
        )
    }
}

@Composable
private fun RevealCard(
    assignment: Assignment,
    budget: String,
    wishes: List<String>,
    onHide: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(assignment) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "revealScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Cobre a tela por inteiro: nada da lista pode aparecer atrás.
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp).scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.reveal_you_got),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                assignment.receiver,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            if (budget.isNotBlank()) {
                Text(
                    stringResource(R.string.reveal_budget, budget),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Text(
                stringResource(R.string.reveal_wishes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (wishes.isEmpty()) {
                Text(
                    stringResource(R.string.reveal_no_wishes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                wishes.forEach { wish ->
                    Text(
                        "• $wish",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onHide,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Text(
                    stringResource(R.string.reveal_hide),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        KonfettiView(modifier = Modifier.fillMaxSize(), parties = remember { confettiParties() })
    }
}

private fun confettiParties(): List<Party> {
    val emitter = Emitter(duration = 220, TimeUnit.MILLISECONDS).max(180)
    return listOf(
        Party(
            speed = 12f,
            maxSpeed = 42f,
            damping = 0.9f,
            angle = 270,
            spread = 90,
            colors = ConfettiColors,
            position = Position.Relative(0.5, 0.0),
            emitter = emitter,
        ),
    )
}
