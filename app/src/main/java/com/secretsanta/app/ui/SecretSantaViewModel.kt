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
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Onde o sorteio está, do clique até a última pessoa ver o resultado. */
sealed interface DrawPhase {
    data object Idle : DrawPhase
    data object Suspense : DrawPhase
    data class Failed(val error: DrawError, val offendingName: String? = null) : DrawPhase

    /**
     * Sorteio pronto. Fica **só em memória**, de propósito: o resultado nunca é
     * gravado numa lista que o organizador possa abrir depois. Ele é a única
     * pessoa que não pode ver os pares, e a forma mais segura de garantir isso é
     * não ter onde consultá-los.
     *
     * O preço é que fechar o app antes de todo mundo ver obriga a sortear de
     * novo. A revelação por link (a caminho) resolve isso guardando só o payload
     * codificado, que nenhuma tela do app decodifica.
     */
    data class Ready(
        val assignments: List<Assignment>,
        val revealed: Set<String> = emptySet(),
    ) : DrawPhase {
        val pending: List<Assignment> get() = assignments.filterNot { it.giver in revealed }
        val isComplete: Boolean get() = pending.isEmpty()
    }
}

class SecretSantaViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = GroupRepository(app)

    val groups: StateFlow<List<Group>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editing = MutableStateFlow<Group?>(null)
    val editing: StateFlow<Group?> = _editing.asStateFlow()

    private val _phase = MutableStateFlow<DrawPhase>(DrawPhase.Idle)
    val phase: StateFlow<DrawPhase> = _phase.asStateFlow()

    // region grupo

    /** Cria um grupo vazio e o deixa aberto para edição. Devolve o id para navegar. */
    fun createGroup(name: String): String {
        val group = Group(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = System.currentTimeMillis(),
        )
        _editing.value = group
        _phase.value = DrawPhase.Idle
        persist(group)
        return group.id
    }

    fun openGroup(id: String) {
        // Abrir um grupo descarta qualquer sorteio em andamento: manter o
        // resultado de outro grupo em memória só daria chance de exibi-lo no
        // lugar errado.
        _phase.value = DrawPhase.Idle
        viewModelScope.launch { _editing.value = repository.group(id) }
    }

    fun deleteGroup(id: String) {
        viewModelScope.launch { repository.delete(id) }
        if (_editing.value?.id == id) _editing.value = null
    }

    fun renameGroup(name: String) = update { it.copy(name = name) }

    fun updateBudget(budget: String) = update { it.copy(budget = budget) }

    // endregion

    // region participantes

    /**
     * Aceita tanto um nome quanto uma lista colada do grupo do WhatsApp — o
     * mesmo separador da engine. Nomes já presentes são ignorados em vez de
     * entrarem repetidos, o que faria o sorteio ser recusado depois.
     */
    fun addParticipants(raw: String) = update { group ->
        val existing = group.participants.map { it.name.trim().lowercase() }.toMutableSet()
        val added = SecretSantaEngine.parse(raw).mapNotNull { name ->
            if (!existing.add(name.trim().lowercase())) return@mapNotNull null
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

    fun draw() {
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
                _phase.value = DrawPhase.Suspense
                update { it.copy(drawnAt = System.currentTimeMillis()) }
                viewModelScope.launch {
                    repository.incrementDrawCount()
                    delay(SUSPENSE_MILLIS)
                    _phase.value = DrawPhase.Ready(outcome.assignments)
                }
            }
        }
    }

    fun markRevealed(giver: String) {
        val ready = _phase.value as? DrawPhase.Ready ?: return
        _phase.value = ready.copy(revealed = ready.revealed + giver)
    }

    fun dismissDraw() {
        _phase.value = DrawPhase.Idle
    }

    // endregion

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
    }
}
