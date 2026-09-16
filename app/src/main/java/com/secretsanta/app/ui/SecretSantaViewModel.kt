package com.secretsanta.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secretsanta.app.data.Group
import com.secretsanta.app.data.GroupRepository
import com.secretsanta.app.data.Participant
import com.secretsanta.app.draw.Assignment
import com.secretsanta.app.draw.DrawError
import com.secretsanta.app.draw.DrawOutcome
import com.secretsanta.app.draw.SecretSantaEngine
import com.secretsanta.app.draw.drawAssignments
import com.secretsanta.app.share.buildLinkDraw
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Como o grupo vai descobrir quem tirou quem. */
enum class RevealMode {
    /** Cada pessoa recebe um link no próprio celular. */
    LINKS,

    /** Todos juntos, o aparelho passa de mão em mão. */
    PASS_THE_PHONE,
}

/** Onde o sorteio está, do clique até a última pessoa ver o resultado. */
sealed interface DrawPhase {
    data object Idle : DrawPhase
    data object Suspense : DrawPhase
    data class Failed(val error: DrawError, val offendingName: String? = null) : DrawPhase

    /**
     * Suspense terminado; a UI exibe o interstitial se [showAd] e então chama
     * [SecretSantaViewModel.onAdFinished], que avança para [next]. O resultado
     * já está decidido — o anúncio nunca altera nem bloqueia o que vem depois.
     */
    data class AwaitingAd(val showAd: Boolean, val next: DrawPhase) : DrawPhase

    /**
     * Revelação presencial pronta. Fica **só em memória**, de propósito: o
     * organizador é a única pessoa que não pode ver os pares, e a forma mais
     * segura de garantir isso é não ter onde consultá-los.
     */
    data class Ready(
        val assignments: List<Assignment>,
        val revealed: Set<String> = emptySet(),
    ) : DrawPhase {
        val pending: List<Assignment> get() = assignments.filterNot { it.giver in revealed }
        val isComplete: Boolean get() = pending.isEmpty()
    }

    /**
     * Links gerados e já salvos no grupo. É só um sinal para a UI navegar: os
     * links em si vivem em [Group.linkDraw], que sobrevive ao app ser fechado.
     */
    data object LinksReady : DrawPhase
}

class SecretSantaViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GroupRepository(app)

    val groups: StateFlow<List<Group>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editing = MutableStateFlow<Group?>(null)
    val editing: StateFlow<Group?> = _editing.asStateFlow()

    private val _phase = MutableStateFlow<DrawPhase>(DrawPhase.Idle)
    val phase: StateFlow<DrawPhase> = _phase.asStateFlow()

    private val _reviewRequests = Channel<Unit>(Channel.CONFLATED)

    /** Emite quando é hora de convidar para avaliar. A UI chama o In-App Review. */
    val reviewRequests: Flow<Unit> = _reviewRequests.receiveAsFlow()

    // region grupo

    /** Cria um grupo vazio e o deixa aberto para edição. */
    fun createGroup(name: String) {
        val group = Group(
            id = UUID.randomUUID().toString(),
            name = name.trim().take(Participant.MAX_NAME),
            createdAt = System.currentTimeMillis(),
        )
        _editing.value = group
        _phase.value = DrawPhase.Idle
        persist(group)
    }

    fun openGroup(id: String) {
        // Abrir um grupo descarta qualquer sorteio presencial em andamento: manter
        // o resultado de outro grupo em memória só daria chance de exibi-lo no
        // lugar errado.
        _phase.value = DrawPhase.Idle
        viewModelScope.launch { _editing.value = repository.group(id) }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch { repository.delete(id) }
        if (_editing.value?.id == id) _editing.value = null
    }

    fun renameGroup(name: String) = update { it.copy(name = name.take(Participant.MAX_NAME)) }

    fun updateBudget(budget: String) = update { it.copy(budget = budget.take(MAX_BUDGET)) }

    // endregion

    // region participantes

    /**
     * Aceita tanto um nome quanto uma lista colada do grupo do WhatsApp — o
     * mesmo separador da engine. Nomes já presentes são ignorados em vez de
     * entrarem repetidos, o que faria o sorteio ser recusado depois.
     */
    fun addParticipants(raw: String) = update { group ->
        val existing = group.participants.map { it.name.trim().lowercase() }.toMutableSet()
        val added = SecretSantaEngine.parse(raw).mapNotNull { parsed ->
            val name = parsed.take(Participant.MAX_NAME).trim()
            if (!existing.add(name.lowercase())) return@mapNotNull null
            Participant(id = UUID.randomUUID().toString(), name = name)
        }
        group.copy(participants = group.participants + added)
    }

    fun updateParticipant(participant: Participant) = update { it.withParticipant(participant) }

    fun removeParticipant(participantId: String) = update { it.withoutParticipant(participantId) }

    // endregion

    // region restrições

    fun addRestriction(firstId: String, secondId: String) =
        update { it.withMutualRestriction(firstId, secondId) }

    fun removeRestriction(firstId: String, secondId: String) =
        update { it.withoutRestrictionsBetween(firstId, secondId) }

    // endregion

    // region sorteio

    /**
     * @param language idioma da página de revelação, usado só no modo [RevealMode.LINKS].
     */
    fun draw(mode: RevealMode, language: String) {
        val group = _editing.value ?: return

        // O resultado é decidido agora, no clique; o suspense da tela é só
        // encenação. Decidir depois abriria espaço para a animação e o resultado
        // discordarem se algo falhasse no meio.
        when (val outcome = group.drawAssignments()) {
            // Erro não merece espera: quem esqueceu de tirar uma restrição
            // precisa saber disso agora, não depois de uma animação.
            is DrawOutcome.Failure ->
                _phase.value = DrawPhase.Failed(outcome.error, outcome.offendingName)

            is DrawOutcome.Success -> {
                val now = System.currentTimeMillis()
                val next = when (mode) {
                    RevealMode.PASS_THE_PHONE -> {
                        // Um sorteio presencial substitui links antigos: dois
                        // resultados valendo ao mesmo tempo só confundem o grupo.
                        update { it.copy(drawnAt = now, linkDraw = null) }
                        DrawPhase.Ready(outcome.assignments)
                    }

                    RevealMode.LINKS -> {
                        // Salvo já no clique: se o app morrer durante o suspense
                        // ou o anúncio, os links não se perdem.
                        update {
                            it.copy(
                                drawnAt = now,
                                linkDraw = it.buildLinkDraw(outcome.assignments, language, now),
                            )
                        }
                        DrawPhase.LinksReady
                    }
                }

                _phase.value = DrawPhase.Suspense
                viewModelScope.launch {
                    val total = repository.incrementDrawCount()
                    delay(SUSPENSE_MILLIS)
                    _phase.value = DrawPhase.AwaitingAd(showAd = shouldShowAd(total), next = next)
                }
            }
        }
    }

    /** Chamado pela UI quando o anúncio foi fechado, falhou ou não precisava aparecer. */
    fun onAdFinished() {
        val awaiting = _phase.value as? DrawPhase.AwaitingAd ?: return
        _phase.value = awaiting.next
    }

    fun markRevealed(giver: String) {
        val ready = _phase.value as? DrawPhase.Ready ?: return
        val updated = ready.copy(revealed = ready.revealed + giver)
        _phase.value = updated
        if (updated.isComplete) maybeAskForReview()
    }

    fun dismissDraw() {
        _phase.value = DrawPhase.Idle
    }

    // endregion

    // region links

    fun setLinkSent(giverId: String, sent: Boolean) {
        update { group ->
            val draw = group.linkDraw ?: return@update group
            group.copy(
                linkDraw = draw.copy(
                    links = draw.links.map { if (it.giverId == giverId) it.copy(sent = sent) else it },
                ),
            )
        }
        if (_editing.value?.linkDraw?.isComplete == true) maybeAskForReview()
    }

    // endregion

    /**
     * Nunca no primeiro sorteio do aparelho — é a primeira impressão, e é quando
     * a pessoa decide se o app presta. Depois, em sorteios alternados. Amigo
     * secreto se sorteia poucas vezes, então a frequência real já é baixa.
     */
    private fun shouldShowAd(totalDraws: Int): Boolean =
        totalDraws >= 2 && totalDraws % AD_EVERY_N_DRAWS == 0

    /**
     * O convite para avaliar sai no momento de maior satisfação: quando o grupo
     * inteiro terminou de ver quem tirou, ou quando o último link foi enviado.
     */
    private fun maybeAskForReview() {
        viewModelScope.launch {
            if (repository.hasAskedForReview()) return@launch
            repository.markReviewAsked()
            _reviewRequests.send(Unit)
        }
    }

    /** Aplica a mudança na cópia em memória e persiste. */
    private fun update(transform: (Group) -> Group) {
        val updated = transform(_editing.value ?: return)
        _editing.value = updated
        persist(updated)
    }

    private fun persist(group: Group) {
        viewModelScope.launch { repository.save(group) }
    }

    companion object {
        /** Duração da encenação de suspense. Curto o bastante para não irritar. */
        const val SUSPENSE_MILLIS = 1_500L

        const val MAX_BUDGET = 20

        private const val AD_EVERY_N_DRAWS = 2
    }
}
