package br.com.tenviti.app.ui

import java.util.prefs.Preferences

actual object PreferenciasLocais {
    private val prefs: Preferences? = runCatching { Preferences.userRoot().node("tenviti") }.getOrNull()

    actual fun ler(chave: String): String? = prefs?.get(chave, null)

    actual fun gravar(chave: String, valor: String) {
        prefs?.put(chave, valor)
    }
}
