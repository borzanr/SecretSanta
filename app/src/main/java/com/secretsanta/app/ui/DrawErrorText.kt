package com.secretsanta.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.secretsanta.app.R
import com.secretsanta.app.draw.DrawError
import com.secretsanta.app.draw.SecretSantaEngine

/**
 * Traduz o motivo da recusa para uma frase que diz **o que fazer**, não só o que
 * deu errado — o erro de restrições impossíveis, em particular, é inútil sem a
 * instrução de remover uma regra.
 */
@Composable
fun drawErrorText(error: DrawError, offendingName: String?): String = when (error) {
    DrawError.TOO_FEW_PARTICIPANTS ->
        stringResource(R.string.draw_error_too_few, SecretSantaEngine.MIN_PARTICIPANTS)

    DrawError.TOO_MANY_PARTICIPANTS ->
        stringResource(R.string.draw_error_too_many, SecretSantaEngine.MAX_PARTICIPANTS)

    DrawError.DUPLICATE_NAME ->
        stringResource(R.string.draw_error_duplicate, offendingName?.trim().orEmpty())

    DrawError.IMPOSSIBLE_RESTRICTIONS ->
        stringResource(R.string.draw_error_impossible)
}
