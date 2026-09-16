package com.secretsanta.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Único DataStore do app: grupos salvos e contadores dos gates de anúncio e review. */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "secretsanta")

/**
 * Grupos salvos, para que o mesmo pessoal possa ser reusado no ano seguinte.
 *
 * Persistência em DataStore Preferences com a lista serializada em JSON: o
 * volume é pequeno ([MAX_GROUPS] grupos) e não justifica um banco.
 *
 * Diferente do histórico do LuckyDraw, aqui o dado **não é descartável** — é uma
 * lista que a pessoa montou à mão. Por isso um JSON ilegível devolve lista vazia
 * para não crashar, mas nunca é sobrescrito automaticamente: ver [save].
 */
class GroupRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val groups: Flow<List<Group>> = context.appDataStore.data.map { prefs ->
        decode(prefs[GROUPS_KEY])
    }

    private fun decode(raw: String?): List<Group> {
        if (raw == null) return emptyList()
        return try {
            json.decodeFromString<List<Group>>(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Grupos ilegíveis, exibindo lista vazia", e)
            emptyList()
        }
    }

    suspend fun group(id: String): Group? =
        decode(context.appDataStore.data.first()[GROUPS_KEY]).firstOrNull { it.id == id }

    /**
     * Grava o grupo, substituindo a versão anterior se já existir.
     *
     * Se o conteúdo salvo estiver corrompido, aborta em vez de gravar por cima:
     * apagar em silêncio a lista de participantes que alguém digitou é pior do
     * que falhar de forma visível.
     */
    suspend fun save(group: Group) {
        context.appDataStore.edit { prefs ->
            val raw = prefs[GROUPS_KEY]
            val current = if (raw == null) {
                emptyList()
            } else {
                runCatching { json.decodeFromString<List<Group>>(raw) }.getOrElse {
                    Log.e(TAG, "Grupos corrompidos: não vou sobrescrever", it)
                    return@edit
                }
            }
            val updated = buildList {
                add(group)
                addAll(current.filterNot { it.id == group.id })
            }.take(MAX_GROUPS)
            prefs[GROUPS_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun delete(groupId: String) {
        context.appDataStore.edit { prefs ->
            val current = decode(prefs[GROUPS_KEY])
            prefs[GROUPS_KEY] = json.encodeToString(current.filterNot { it.id == groupId })
        }
    }

    /** Incrementa o total de sorteios já feitos e devolve o novo valor. */
    suspend fun incrementDrawCount(): Int {
        var updated = 0
        context.appDataStore.edit { prefs ->
            updated = (prefs[DRAW_COUNT_KEY] ?: 0) + 1
            prefs[DRAW_COUNT_KEY] = updated
        }
        return updated
    }

    suspend fun hasAskedForReview(): Boolean =
        context.appDataStore.data.first()[REVIEW_ASKED_KEY] ?: false

    suspend fun markReviewAsked() {
        context.appDataStore.edit { it[REVIEW_ASKED_KEY] = true }
    }

    companion object {
        private const val TAG = "GroupRepository"

        const val MAX_GROUPS = 50

        /**
         * Sorteios antes de convidar para avaliar. Cedo demais atrapalha quem só
         * queria sortear uma vez; tarde demais e a pessoa já fechou o app.
         */
        const val DRAWS_BEFORE_REVIEW = 2

        private val GROUPS_KEY = stringPreferencesKey("groups")
        private val DRAW_COUNT_KEY = intPreferencesKey("draw_count")
        private val REVIEW_ASKED_KEY = booleanPreferencesKey("review_asked")
    }
}
