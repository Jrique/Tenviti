package br.com.tenviti.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.SessaoDto

/**
 * Sessão do usuário logado. É estado do Compose: quando muda (login, logout ou token expirado),
 * a tela inicial se recompõe sozinha.
 */
object Session {
    var usuarioLogado by mutableStateOf<UsuarioSessao?>(null)
        private set

    fun iniciar(sessao: SessaoDto) {
        val u = sessao.usuario
        usuarioLogado = UsuarioSessao(u.id, u.nome, u.email, u.papel, u.lojaId, u.lojaSlug, sessao.token)
    }

    fun encerrar() {
        usuarioLogado = null
    }
}

data class UsuarioSessao(
    val id: Long,
    val nome: String,
    val email: String,
    val papel: Papel,
    val lojaId: Long?,
    val lojaSlug: String?,
    val token: String,
) {
    val ehSuperAdmin: Boolean get() = papel == Papel.SUPER_ADMIN
    val gerenciaEquipe: Boolean get() = papel == Papel.DONO || papel == Papel.GERENTE
    val editaCatalogo: Boolean get() = papel == Papel.DONO || papel == Papel.GERENTE
    val vePainelFinanceiro: Boolean get() = papel == Papel.DONO || papel == Papel.GERENTE
    val personalizaLoja: Boolean get() = papel == Papel.DONO
}
