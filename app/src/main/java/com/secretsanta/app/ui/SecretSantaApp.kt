package com.secretsanta.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.secretsanta.app.R
import com.secretsanta.app.ads.ConsentManager
import com.secretsanta.app.ads.InterstitialManager
import com.secretsanta.app.share.revealLanguage
import com.secretsanta.app.ui.components.SuspenseOverlay
import com.secretsanta.app.ui.group.GroupScreen
import com.secretsanta.app.ui.groups.GroupsScreen
import com.secretsanta.app.ui.links.LinksScreen
import com.secretsanta.app.ui.restrictions.RestrictionsScreen
import com.secretsanta.app.ui.reveal.PassThePhoneScreen

private object Routes {
    const val GROUPS = "groups"
    const val GROUP = "group"
    const val RESTRICTIONS = "restrictions"
    const val REVEAL = "reveal"
    const val LINKS = "links"
}

private const val PRIVACY_URL = "https://secretsanta.borzanti.com/privacy/"

@Composable
fun SecretSantaApp(viewModel: SecretSantaViewModel = viewModel()) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val privacyOptionsRequired by ConsentManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    val language = revealLanguage(LocalConfiguration.current.locales[0].toLanguageTag())

    // Pedido vindo da tela de links ("Sortear de novo"): a tela de grupo abre a
    // escolha de modo ao voltar, em vez de o botão só navegar para trás.
    var promptDraw by rememberSaveable { mutableStateOf(false) }

    // A navegação disparada pelo sorteio fica aqui, com chave em [phase], e não
    // dentro da composição da tela de grupo: disparada de lá, voltaria a navegar
    // assim que o usuário saísse da tela seguinte, prendendo o botão voltar.
    LaunchedEffect(phase) {
        when (val current = phase) {
            is DrawPhase.AwaitingAd -> {
                if (current.showAd && activity != null) {
                    InterstitialManager.show(activity) { viewModel.onAdFinished() }
                } else {
                    viewModel.onAdFinished()
                }
            }

            is DrawPhase.Ready -> {
                if (navController.currentDestination?.route != Routes.REVEAL) {
                    navController.navigate(Routes.REVEAL)
                }
            }

            DrawPhase.LinksReady -> {
                navController.navigate(Routes.LINKS)
                viewModel.dismissDraw()
            }

            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reviewRequests.collect {
            activity?.let { ReviewPrompt.request(it) {} }
        }
    }

    // O grupo aberto vive no ViewModel, não na rota: as telas de grupo,
    // restrições, links e revelação olham todas para o mesmo objeto, e passar o
    // id por argumento só criaria vários lugares para ele ficar desatualizado.
    NavHost(navController = navController, startDestination = Routes.GROUPS) {
        composable(Routes.GROUPS) {
            GroupsScreen(
                groups = groups,
                showPrivacyOptions = privacyOptionsRequired,
                onOpen = { id ->
                    viewModel.openGroup(id)
                    navController.navigate(Routes.GROUP)
                },
                onCreate = { name ->
                    viewModel.createGroup(name)
                    navController.navigate(Routes.GROUP)
                },
                onDelete = viewModel::deleteGroup,
                onPrivacyOptions = { activity?.let(ConsentManager::showPrivacyOptions) },
                onPrivacyPolicy = {
                    activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$PRIVACY_URL#$language")))
                },
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
                onOpenLinks = { navController.navigate(Routes.LINKS) },
                onDraw = { mode -> viewModel.draw(mode, language) },
                promptDraw = promptDraw,
                onPromptDrawConsumed = { promptDraw = false },
            )

            when (val current = phase) {
                // O suspense cobre também a espera pelo anúncio, para a tela de
                // grupo não piscar entre a animação e o interstitial.
                DrawPhase.Suspense, is DrawPhase.AwaitingAd -> SuspenseOverlay(
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

                else -> Unit
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

        composable(Routes.LINKS) {
            val group = editing ?: return@composable
            val draw = group.linkDraw ?: return@composable
            LinksScreen(
                group = group,
                draw = draw,
                onBack = { navController.popBackStack(Routes.GROUP, inclusive = false) },
                onSetSent = viewModel::setLinkSent,
                onRedraw = {
                    // Volta ao grupo e abre a escolha de modo por lá: é onde a
                    // confirmação de re-sorteio e o suspense moram.
                    promptDraw = true
                    navController.popBackStack(Routes.GROUP, inclusive = false)
                },
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
