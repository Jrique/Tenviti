package br.com.tenviti.app.ui

/** Pequenas preferências do painel guardadas no aparelho (ex.: modo claro/escuro). */
expect object PreferenciasLocais {
    fun ler(chave: String): String?
    fun gravar(chave: String, valor: String)
}
