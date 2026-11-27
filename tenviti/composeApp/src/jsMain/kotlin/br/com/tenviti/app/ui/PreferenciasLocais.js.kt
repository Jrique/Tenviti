package br.com.tenviti.app.ui

import kotlinx.browser.localStorage

actual object PreferenciasLocais {
    actual fun ler(chave: String): String? = try { localStorage.getItem(chave) } catch (_: Throwable) { null }

    actual fun gravar(chave: String, valor: String) {
        try { localStorage.setItem(chave, valor) } catch (_: Throwable) { /* navegador sem armazenamento: só não lembra */ }
    }
}
