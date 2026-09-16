package com.secretsanta.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.secretsanta.app.R
import com.secretsanta.app.ui.components.SuspenseOverlay
import com.secretsanta.app.ui.group.GroupScreen
import com.secretsanta.app.ui.groups.GroupsScreen
import com.secretsanta.app.ui.restrictions.RestrictionsScreen
import com.secretsanta.app.ui.reveal.PassThePhoneScreen

private object Routes {
    const val GROUPS = "groups"
    const val GROUP = "group"
    const val RESTRICTIONS = "restrictions"
    const val REVEAL = "reveal"
}

@Composable
fun SecretSantaApp(viewModel: SecretSantaViewModel = viewModel()) {
    val navController = rememberNavController()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()

    // A navegação para a revelação fica aqui, com chave em [phase], e não dentro
    // da tela de grupo: disparada de dentro da composição, ela voltaria a
    // navegar assim que o usuário saísse da revelação, prendendo o botão voltar.
    LaunchedEffect(phase) {
        if (phase is DrawPhase.Ready) navController.navigate(Routes.REVEAL)
    }

    // O grupo aberto vive no ViewModel, não na rota: as telas de grupo,
    // restrições e revelação olham todas para o mesmo objeto, e passar o id por
    // argumento só criaria três lugares para ele ficar desatualizado.
    NavHost(navController = navController, startDestination = Routes.GROUPS) {
        composable(Routes.GROUPS) {
            GroupsScreen(
                groups = groups,
                onOpen = { id ->
                    viewModel.openGroup(id)
                    navController.navigate(Routes.GROUP)
                },
                onCreate = { name ->
                    viewModel.createGroup(name)
                    navController.navigate(Routes.GROUP)
                },
                onDelete = viewModel::deleteGroup,
            )
        }

        composable(Routes.GROUP) {
            val group = editing ?: return@composable
            GroupScreen(
                group = group,
                onBack = { navController.popBackStack() },
                onRename = viewModel::renameGroup,
                onBudgetChange = viewModel::updateBudget,
                onAddParticipants = viewModel::addParticipants,
                onEditParticipant = viewModel::updateParticipant,
                onRemoveParticipant = viewModel::removeParticipant,
                onOpenRestrictions = { navController.navigate(Routes.RESTRICTIONS) },
                onDraw = viewModel::draw,
            )

            when (val current = phase) {
                is DrawPhase.Suspense -> SuspenseOverlay(
                    samples = group.names,
                    label = stringResource(R.string.draw_suspense),
                    modifier = Modifier,
                )

                is DrawPhase.Failed -> AlertDialog(
                    onDismissRequest = viewModel::dismissDraw,
                    title = { Text(stringResource(R.string.draw_error_title)) },
                    text = { Text(drawErrorText(current.error, current.offendingName)) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissDraw) {
                            Text(stringResource(R.string.draw_dismiss))
                        }
                    },
                )

                is DrawPhase.Ready, DrawPhase.Idle -> Unit
            }
        }

        composable(Routes.RESTRICTIONS) {
            val group = editing ?: return@composable
            RestrictionsScreen(
                group = group,
                onBack = { navController.popBackStack() },
                onAdd = viewModel::addRestriction,
                onRemove = viewModel::removeRestriction,
            )
        }

        composable(Routes.REVEAL) {
            val group = editing ?: return@composable
            val ready = phase as? DrawPhase.Ready ?: return@composable
            PassThePhoneScreen(
                group = group,
                ready = ready,
                onRevealed = viewModel::markRevealed,
                onExit = {
                    viewModel.dismissDraw()
                    navController.popBackStack(Routes.GROUP, inclusive = false)
                },
            )
        }
    }
}
