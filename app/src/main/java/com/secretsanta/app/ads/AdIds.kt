package com.secretsanta.app.ads

import com.google.android.gms.ads.MobileAds
import com.secretsanta.app.BuildConfig

/**
 * IDs de anúncio, centralizados para a troca de teste → produção ser de uma linha.
 *
 * Builds debug SEMPRE usam os IDs de teste oficiais do Google. Isso é proposital:
 * clicar nos próprios anúncios reais durante o desenvolvimento é a forma mais
 * comum de ter a conta AdMob banida.
 *
 * O APPLICATION_ID (com "~") fica no AndroidManifest.xml e é o mesmo em todos os
 * builds — o que separa teste de produção são os unit IDs (com "/") daqui.
 */
object AdIds {

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    // TODO(publicação): criar as unidades deste app no AdMob (mesmo publisher,
    // ca-app-pub-5043299631567488) e preencher. Enquanto estiverem vazias, o
    // release simplesmente não carrega anúncio — ver [enabled].
    private const val PROD_BANNER = ""
    private const val PROD_INTERSTITIAL = ""

    /**
     * Falha fechada: um release sem IDs de produção não pede anúncio nenhum, em
     * vez de pedir com ID de teste (impressões que não pagam) ou com ID errado
     * (tráfego inválido na conta).
     */
    val enabled: Boolean
        get() = BuildConfig.DEBUG || (PROD_BANNER.isNotBlank() && PROD_INTERSTITIAL.isNotBlank())

    val banner: String get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER
    val interstitial: String get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL

    /**
     * Aparelhos que sempre recebem anúncios de teste, mesmo num build de release
     * com os IDs reais. O hash deriva do identificador de publicidade do aparelho,
     * não do app — por isso os mesmos valores do LuckyDraw valem aqui.
     *
     * ⚠️ O hash muda quando o identificador de publicidade é redefinido. A fonte
     * de verdade é o painel: **AdMob → Configurações → Dispositivos de teste**.
     * Esta lista é só uma segunda camada.
     */
    val TEST_DEVICE_IDS: List<String> = listOf(
        "8B3FEDD52B52AD340B8E016780B25AD0", // Redmi Note 9S — após reset do AAID
        "E3153D8858021F4D58DC5E9F4F11BDD9", // Redmi Note 9S — hash anterior
    )

    /** Aplica os aparelhos de teste ao SDK. Chamar antes de carregar anúncios. */
    fun applyTestDevices() {
        if (TEST_DEVICE_IDS.isEmpty()) return
        MobileAds.setRequestConfiguration(
            MobileAds.getRequestConfiguration().toBuilder()
                .setTestDeviceIds(TEST_DEVICE_IDS)
                .build()
        )
    }
}
