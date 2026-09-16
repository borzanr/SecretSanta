package com.secretsanta.app.ui.links

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secretsanta.app.R
import com.secretsanta.app.data.Group
import com.secretsanta.app.data.LinkDraw
import com.secretsanta.app.data.SealedLink
import com.secretsanta.app.share.shareRevealLink

/**
 * Checklist de envio dos links.
 *
 * O organizador vê só nomes e o que já enviou — nunca quem tirou quem. Tocar em
 * "Enviar" abre o seletor do sistema e já marca como enviado; a caixa de seleção
 * existe para corrigir quando a pessoa cancelou o envio ou mandou por outro meio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    group: Group,
    draw: LinkDraw,
    onBack: () -> Unit,
    onSetSent: (giverId: String, sent: Boolean) -> Unit,
    onRedraw: () -> Unit,
) {
    val context = LocalContext.current
    val stale = group.isLinkDrawStale

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.links_title)) },
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
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(stringResource(R.string.links_done)) }
            }
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
                    stringResource(R.string.links_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (stale) {
                item { StaleWarning(onRedraw = onRedraw) }
            }

            item {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.links_progress, draw.sentCount, draw.links.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { draw.sentCount.toFloat() / draw.links.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            items(draw.links, key = { it.giverId }) { link ->
                LinkRow(
                    link = link,
                    // Link desatualizado não é enviado: mandaria o resultado velho.
                    enabled = !stale,
                    onShare = {
                        context.shareRevealLink(link.giverName, group.name, link.url)
                        onSetSent(link.giverId, true)
                    },
                    onToggleSent = { onSetSent(link.giverId, it) },
                )
            }

            if (draw.isComplete && !stale) {
                item {
                    Text(
                        stringResource(R.string.links_all_sent),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    link: SealedLink,
    enabled: Boolean,
    onShare: () -> Unit,
    onToggleSent: (Boolean) -> Unit,
) {
    val toggleLabel = stringResource(R.string.links_sent_toggle)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = link.sent,
                onCheckedChange = onToggleSent,
                modifier = Modifier.semantics { contentDescription = "$toggleLabel: ${link.giverName}" },
            )
            Text(
                link.giverName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                color = if (link.sent) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            val label = stringResource(if (link.sent) R.string.links_resend else R.string.links_send)
            if (link.sent) {
                OutlinedButton(onClick = onShare, enabled = enabled) { Text(label) }
            } else {
                FilledTonalButton(onClick = onShare, enabled = enabled) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun StaleWarning(onRedraw: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                Text(
                    stringResource(R.string.links_stale_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(stringResource(R.string.links_stale_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRedraw) { Text(stringResource(R.string.links_redraw)) }
        }
    }
}
