package com.secretsanta.app.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.data.Group
import com.secretsanta.app.data.Participant
import com.secretsanta.app.draw.SecretSantaEngine
import com.secretsanta.app.ui.components.DrawButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    group: Group,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onAddParticipants: (String) -> Unit,
    onEditParticipant: (Participant) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onOpenRestrictions: () -> Unit,
    onDraw: () -> Unit,
) {
    var editing by remember { mutableStateOf<Participant?>(null) }
    val canDraw = group.participants.size >= SecretSantaEngine.MIN_PARTICIPANTS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        // Sortear fica fixo no rodapé. No fim da lista, um grupo de 15 pessoas
        // obrigava a rolar até o final só para achar a ação principal da tela.
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DrawButton(
                        text = stringResource(R.string.group_draw),
                        onClick = onDraw,
                        enabled = canDraw,
                    )
                    if (!canDraw) {
                        Text(
                            stringResource(
                                R.string.group_needs_more,
                                SecretSantaEngine.MIN_PARTICIPANTS,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = group.name,
                    onValueChange = onRename,
                    singleLine = true,
                    label = { Text(stringResource(R.string.group_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = group.budget,
                    onValueChange = onBudgetChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.group_budget_label)) },
                    placeholder = { Text(stringResource(R.string.group_budget_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { RestrictionsRow(group, onOpenRestrictions) }

            item {
                Text(
                    stringResource(R.string.group_participants),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            item { AddParticipantField(onAdd = onAddParticipants) }

            items(group.participants, key = { it.id }) { participant ->
                ParticipantRow(participant = participant, onClick = { editing = participant })
            }
        }
    }

    editing?.let { participant ->
        ParticipantSheet(
            participant = participant,
            onDismiss = { editing = null },
            onSave = {
                editing = null
                onEditParticipant(it)
            },
            onRemove = {
                editing = null
                onRemoveParticipant(participant.id)
            },
        )
    }
}

@Composable
private fun RestrictionsRow(group: Group, onClick: () -> Unit) {
    val pairs = group.restrictionPairs()
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.group_restrictions)) },
            supportingContent = {
                Text(
                    if (pairs.isEmpty()) {
                        stringResource(R.string.group_restrictions_none)
                    } else {
                        pluralStringResource(
                            R.plurals.group_restrictions_count,
                            pairs.size,
                            pairs.size,
                        )
                    },
                )
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}

/**
 * Campo de adicionar. Aceita colar a lista inteira porque é assim que as pessoas
 * já têm os nomes: numa mensagem do grupo do WhatsApp.
 */
@Composable
private fun AddParticipantField(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.group_add_hint)) },
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = {
                onAdd(text)
                text = ""
            },
            enabled = text.isNotBlank(),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.group_add))
        }
    }
}

@Composable
private fun ParticipantRow(participant: Participant, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
    ) {
        ListItem(
            headlineContent = { Text(participant.name) },
            supportingContent = if (participant.wishes.isEmpty()) {
                null
            } else {
                {
                    Text(
                        pluralStringResource(
                            R.plurals.participant_wishes_count,
                            participant.wishes.size,
                            participant.wishes.size,
                        ),
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        )
    }
}
