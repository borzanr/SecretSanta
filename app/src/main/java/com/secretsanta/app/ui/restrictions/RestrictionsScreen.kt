package com.secretsanta.app.ui.restrictions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.data.Group
import com.secretsanta.app.data.Participant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    group: Group,
    onBack: () -> Unit,
    onAdd: (String, String) -> Unit,
    onRemove: (String, String) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    val pairs = group.restrictionPairs()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.restrictions_title)) },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.restrictions_add)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.restrictions_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (pairs.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.restrictions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(pairs, key = { it.first.id + it.second.id }) { (first, second) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.small,
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.restrictions_pair, first.name, second.name))
                        },
                        trailingContent = {
                            IconButton(onClick = { onRemove(first.id, second.id) }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.restrictions_remove),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        }
    }

    if (adding) {
        PickPairDialog(
            participants = group.participants,
            onDismiss = { adding = false },
            onConfirm = { first, second ->
                adding = false
                onAdd(first, second)
            },
        )
    }
}

/**
 * Escolha dos dois nomes por chips. Selecionar um terceiro substitui o mais
 * antigo, em vez de travar a seleção — é o que a pessoa quer dizer quando toca
 * num nome novo com dois já marcados.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickPairDialog(
    participants: List<Participant>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var selected by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restrictions_pick)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    participants.forEach { participant ->
                        FilterChip(
                            selected = participant.id in selected,
                            onClick = {
                                selected = when {
                                    participant.id in selected -> selected - participant.id
                                    selected.size < 2 -> selected + participant.id
                                    else -> listOf(selected.last(), participant.id)
                                }
                            },
                            label = { Text(participant.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected[0], selected[1]) },
                enabled = selected.size == 2,
            ) { Text(stringResource(R.string.restrictions_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
