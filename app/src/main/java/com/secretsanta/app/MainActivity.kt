package com.secretsanta.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.secretsanta.app.ui.SecretSantaApp
import com.secretsanta.app.ui.theme.SecretSantaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // O app é sempre escuro, independente do tema do sistema: as barras
        // precisam ser forçadas a dark, senão a de navegação fica branca.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // TODO(semana 3): consentimento UMP + MobileAds + revelação por link.
        setContent {
            SecretSantaTheme {
                SecretSantaApp()
            }
        }
    }
}
