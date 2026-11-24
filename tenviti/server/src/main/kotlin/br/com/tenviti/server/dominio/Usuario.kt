package br.com.tenviti.server.dominio

import br.com.tenviti.shared.Formatos
import br.com.tenviti.shared.Papel

/** Quem está fazendo a requisição (extraído do token). */
data class Ator(val usuarioId: Long, val papel: Papel, val lojaId: Long?) {

    /** Loja do ator; falha se for o super admin (que não pertence a nenhuma loja). */
    fun lojaObrigatoria(): Long = lojaId ?: throw AcessoNegado("Esta área é exclusiva dos usuários de uma loja.")

    fun exigirPapel(vararg permitidos: Papel) {
        if (papel !in permitidos) throw AcessoNegado()
    }

    fun exigirSuperAdmin() = exigirPapel(Papel.SUPER_ADMIN)

    /** Regra de hierarquia: quem pode criar/alterar/remover usuários com o papel [alvo]. */
    fun podeGerenciarPapel(alvo: Papel): Boolean = when (papel) {
        Papel.SUPER_ADMIN -> true
        Papel.DONO -> alvo != Papel.SUPER_ADMIN
        Papel.GERENTE -> alvo != Papel.SUPER_ADMIN && alvo != Papel.DONO && alvo != Papel.GERENTE
        else -> false
    }
}

/** Usuário do sistema. Nunca guarda a senha em texto: só o hash. */
class Usuario(
    val id: Long,
    val lojaId: Long?,
    nome: String,
    email: String,
    senhaHash: String,
    papel: Papel,
    ativo: Boolean,
) {
    var nome: String = nome; private set
    var email: String = email; private set
    var senhaHash: String = senhaHash; private set
    var papel: Papel = papel; private set
    var ativo: Boolean = ativo; private set

    init {
        exigir((papel == Papel.SUPER_ADMIN) == (lojaId == null)) {
            "Super admin não pertence a uma loja, e todo outro usuário precisa pertencer a uma."
        }
    }

    fun atualizarDados(nome: String, email: String, papel: Papel, ativo: Boolean) {
        validarNome(nome)
        validarEmail(email)
        exigir(papel.ehDaLoja == this.papel.ehDaLoja) { "Não é possível transformar um usuário de loja em super admin." }
        this.nome = nome.trim()
        this.email = normalizarEmail(email)
        this.papel = papel
        this.ativo = ativo
    }

    fun trocarSenha(novoHash: String) {
        senhaHash = novoHash
    }

    fun podeEntrar(): Boolean = ativo

    companion object {
        fun novo(lojaId: Long?, nome: String, email: String, senhaHash: String, papel: Papel): Usuario {
            validarNome(nome)
            validarEmail(email)
            return Usuario(0, lojaId, nome.trim(), normalizarEmail(email), senhaHash, papel, true)
        }

        fun validarSenhaNova(senha: String) = exigir(Formatos.senhaValida(senha)) {
            "A senha precisa ter pelo menos ${Formatos.SENHA_TAMANHO_MINIMO} caracteres."
        }

        fun normalizarEmail(email: String) = email.trim().lowercase()

        private fun validarNome(nome: String) = exigir(nome.isNotBlank() && nome.length <= 120) {
            "O nome é obrigatório e pode ter no máximo 120 caracteres."
        }

        private fun validarEmail(email: String) = exigir(Formatos.emailValido(email) && email.length <= 160) {
            "Informe um e-mail válido."
        }
    }
}
