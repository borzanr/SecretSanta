package com.secretsanta.app.share

import android.content.Context
import android.content.Intent
import com.secretsanta.app.R

/**
 * Abre o seletor do sistema com a mensagem pronta para uma pessoa.
 *
 * O nome dela vai no começo da mensagem e no título do seletor: quem envia 15
 * links seguidos precisa ver, na hora de escolher o contato, para quem é este.
 */
fun Context.shareRevealLink(giverName: String, eventName: String, url: String) {
    val message = if (eventName.isBlank()) {
        getString(R.string.share_message_no_event, giverName, url)
    } else {
        getString(R.string.share_message, giverName, eventName.trim(), url)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    val chooser = Intent.createChooser(send, getString(R.string.share_chooser, giverName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)
}
