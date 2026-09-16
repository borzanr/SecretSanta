package com.secretsanta.app.share

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * O que uma pessoa precisa ver ao abrir o próprio link, e nada além disso.
 *
 * Os nomes no JSON são de uma letra porque tudo isto vai dentro de uma URL
 * colada no WhatsApp. **Mudar um [SerialName] quebra todos os links já enviados**
 * — e a página em `site/r/index.html` lê exatamente estas chaves.
 */
@Serializable
data class RevealPayload(
    /** Sem valor padrão de propósito: campo com padrão não é serializado. */
    @SerialName("v") val version: Int,
    @SerialName("e") val event: String,
    @SerialName("g") val giver: String,
    @SerialName("r") val receiver: String,
    @SerialName("b") val budget: String = "",
    @SerialName("w") val wishes: List<String> = emptyList(),
    /** Idioma da página: "en", "pt" ou "es". */
    @SerialName("l") val language: String = "en",
)

/**
 * Codifica e decodifica o link de revelação.
 *
 * O resultado vai no **fragmento** da URL (depois do `#`). Navegadores nunca
 * enviam o fragmento ao servidor: a página estática recebe só o HTML, e a
 * decodificação acontece inteira no aparelho de quem abriu. Nada trafega, nada
 * fica registrado em log de servidor.
 *
 * ### Isto é ofuscação, não criptografia
 * O XOR com chave fixa serve para uma única coisa: quem olhar a URL de relance
 * — o organizador colando no WhatsApp, a prévia do link — não lê o nome.
 * Qualquer pessoa determinada decodifica em um minuto, e a chave está no JS
 * público da página. Isso é aceitável porque o link carrega **só o par daquela
 * pessoa**: decodificar o próprio link não revela nada que ela não fosse ver.
 */
@OptIn(ExperimentalEncodingApi::class)
object RevealLink {

    const val VERSION = 1

    const val BASE_URL = "https://secretsanta.borzanti.com/r/"

    /** Mesma chave de `site/r/index.html`. Trocar invalida os links já enviados. */
    private val KEY = "borzanti-secret-santa".encodeToByteArray()

    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun url(payload: RevealPayload): String = BASE_URL + "#" + encode(payload)

    fun encode(payload: RevealPayload): String {
        val bytes = json.encodeToString(payload).encodeToByteArray()
        return base64.encode(xor(bytes))
    }

    /** `null` para qualquer entrada que não seja um link válido. */
    fun decode(token: String): RevealPayload? = runCatching {
        val bytes = xor(base64.decode(token.trim()))
        json.decodeFromString<RevealPayload>(bytes.decodeToString())
    }.getOrNull()

    private fun xor(bytes: ByteArray): ByteArray =
        ByteArray(bytes.size) { i -> (bytes[i].toInt() xor KEY[i % KEY.size].toInt()).toByte() }
}
