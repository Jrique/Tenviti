package br.com.tenviti.app.ui

import android.content.Context
import android.content.SharedPreferences

actual object PreferenciasLocais {
    private var prefs: SharedPreferences? = null

    /** Chamado pela MainActivity antes de mostrar o painel. */
    fun iniciar(contexto: Context) {
        prefs = contexto.getSharedPreferences("tenviti", Context.MODE_PRIVATE)
    }

    actual fun ler(chave: String): String? = prefs?.getString(chave, null)

    actual fun gravar(chave: String, valor: String) {
        prefs?.edit()?.putString(chave, valor)?.apply()
    }
}
