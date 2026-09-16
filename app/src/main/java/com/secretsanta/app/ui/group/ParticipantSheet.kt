package com.secretsanta.app.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.data.Participant

/**
 * Edição de uma pessoa: nome e até [Participant.MAX_WISHES] sugestões de
 * presente.
 *
 * As sugestões são sempre [Participant.MAX_WISHES] campos visíveis, em vez de um
 * botão "adicionar sugestão": com no máximo três, mostrar os campos vazios custa
 * menos toques e deixa claro qual é o limite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantSheet(
    participant: Participant,
    onDismiss: () -> Unit,
    onSave: (Participant) -> Unit,
    onRemove: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(participant.id) { mutableStateOf(participant.name) }
    val wishes = remember(participant.id) {
        List(Participant.MAX_WISHES) { participant.wishes.getOrElse(it) { "" } }.toMutableStateList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.participant_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.participant_name)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(R.string.participant_wishes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.participant_wishes_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            wishes.forEachIndexed { index, wish ->
                OutlinedTextField(
                    value = wish,
                    onValueChange = { wishes[index] = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.participant_wish_hint, index + 1)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    onSave(
                        participant.copy(
                            name = name.trim(),
                            // Campos em branco no meio não viram sugestão vazia.
                            wishes = wishes.map { it.trim() }.filter { it.isNotEmpty() },
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.common_save))
            }

            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text(
                    stringResource(R.string.participant_remove),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
