package com.secretsanta.app.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.ui.RevealMode

/**
 * Pergunta como o grupo vai revelar, antes de sortear.
 *
 * Links vêm primeiro e com destaque: é o modo que serve a quem não está junto —
 * família espalhada, equipe remota — e é o que leva o app para o celular de
 * todos os participantes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawModeSheet(
    onDismiss: () -> Unit,
    onChoose: (RevealMode) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.mode_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            ModeOption(
                icon = Icons.AutoMirrored.Outlined.Send,
                title = stringResource(R.string.mode_links),
                body = stringResource(R.string.mode_links_body),
                highlighted = true,
                onClick = { onChoose(RevealMode.LINKS) },
            )
            ModeOption(
                icon = Icons.Outlined.Groups,
                title = stringResource(R.string.mode_phone),
                body = stringResource(R.string.mode_phone_body),
                highlighted = false,
                onClick = { onChoose(RevealMode.PASS_THE_PHONE) },
            )
        }
    }
}

@Composable
private fun ModeOption(
    icon: ImageVector,
    title: String,
    body: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val container = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = MaterialTheme.shapes.medium,
    ) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null, tint = content) },
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium, color = content) },
            supportingContent = { Text(body, color = content.copy(alpha = 0.8f)) },
            colors = ListItemDefaults.colors(containerColor = container),
        )
    }
}
