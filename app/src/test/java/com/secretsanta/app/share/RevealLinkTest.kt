package com.secretsanta.app.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RevealLinkTest {

    private val sample = RevealPayload(
        version = RevealLink.VERSION,
        event = "Natal da Família",
        giver = "Ana",
        receiver = "Bruno",
        budget = "R$ 50",
        wishes = listOf("livro de receitas", "caneca"),
        language = "pt",
    )

    @Test
    fun `ida e volta preserva tudo`() {
        assertEquals(sample, RevealLink.decode(RevealLink.encode(sample)))
    }

    @Test
    fun `acentos e emoji sobrevivem`() {
        val payload = sample.copy(
            giver = "João 🎅",
            receiver = "Zoë Ñúñez",
            wishes = listOf("café ☕", "日本のお茶"),
        )
        assertEquals(payload, RevealLink.decode(RevealLink.encode(payload)))
    }

    @Test
    fun `campos opcionais vazios nao entram no link e voltam como padrao`() {
        val minimal = sample.copy(budget = "", wishes = emptyList())
        val decoded = RevealLink.decode(RevealLink.encode(minimal))
        assertEquals(minimal, decoded)
        assertTrue(RevealLink.encode(minimal).length < RevealLink.encode(sample).length)
    }

    @Test
    fun `token so usa caracteres seguros para url`() {
        val token = RevealLink.encode(sample.copy(wishes = listOf("?&=#+/ %")))
        assertTrue("token com caractere inseguro: $token", token.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun `nome nao aparece em claro no link`() {
        val url = RevealLink.url(sample)
        assertFalse(url.contains("Bruno"))
        assertFalse(url.contains("QnJ1bm8")) // "Bruno" em base64 puro, sem o XOR
    }

    @Test
    fun `link de pior caso continua curto o bastante para mensagem`() {
        val worst = sample.copy(
            event = "E".repeat(40),
            giver = "G".repeat(40),
            receiver = "R".repeat(40),
            budget = "B".repeat(20),
            wishes = List(3) { "W".repeat(60) },
        )
        val url = RevealLink.url(worst)
        assertTrue("URL com ${url.length} caracteres", url.length < 600)
    }

    @Test
    fun `entrada invalida devolve null em vez de estourar`() {
        assertNull(RevealLink.decode(""))
        assertNull(RevealLink.decode("não é base64!"))
        assertNull(RevealLink.decode("AAAA"))
    }

    @Test
    fun `campo desconhecido de versao futura e ignorado`() {
        // Simula um link gerado por uma versão nova com um campo extra.
        val withExtra = """{"v":2,"e":"X","g":"A","r":"B","novo":true}"""
        val token = encodeRaw(withExtra)
        assertEquals("B", RevealLink.decode(token)?.receiver)
    }

    /**
     * Vetor fixo. A página `site/r/index.html` é testada contra este mesmo token;
     * se este teste quebrar, os links já enviados deixaram de abrir.
     */
    @Test
    fun `vetor fixo compartilhado com a pagina web`() {
        assertEquals(FIXED_TOKEN, RevealLink.encode(sample))
    }

    private fun encodeRaw(json: String): String {
        // Monta o token de um JSON arbitrário (que RevealPayload não conseguiria
        // serializar), repetindo à mão o XOR + base64 da implementação.
        val key = "borzanti-secret-santa".encodeToByteArray()
        val bytes = json.encodeToByteArray()
        val xored = ByteArray(bytes.size) { (bytes[it].toInt() xor key[it % key.size].toInt()).toByte() }
        @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
        return kotlin.io.encoding.Base64.UrlSafe
            .withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT)
            .encode(xored)
    }

    companion object {
        const val FIXED_TOKEN =
            "GU0EWFtfWEtIUV9BPAQATB9BChVBJA4fucwCHQgPX0cEUF9WbB0ATFhDEE1IWCMcAQdCUUlBEEdODyFFTkFRQENQDUNUL0tBGhMRHUUQSFMTCxcECxsTCUNCVgpMHQAAE0cpAVENTE5DEhtQBw"
    }
}
