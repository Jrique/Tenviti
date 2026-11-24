package br.com.tenviti.server.aplicacao

import br.com.tenviti.server.dominio.AcessoNegado
import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.server.dominio.Conflito
import br.com.tenviti.server.dominio.DadoInvalido
import br.com.tenviti.server.dominio.NaoEncontrado
import br.com.tenviti.server.dominio.RepositorioUsuarios
import br.com.tenviti.server.dominio.Transacoes
import br.com.tenviti.server.dominio.Usuario
import br.com.tenviti.server.infra.seguranca.Senhas
import br.com.tenviti.shared.AtualizarUsuarioDto
import br.com.tenviti.shared.NovoUsuarioDto
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.UsuarioDto

/** Usuários da própria loja (equipe). Dono e gerente gerenciam; gerente não mexe em dono nem em outro gerente. */
class ServicoUsuarios(
    private val tx: Transacoes,
    private val usuarios: RepositorioUsuarios,
    private val senhas: Senhas,
) {
    suspend fun listar(ator: Ator): List<UsuarioDto> {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar { usuarios.listarDaLoja(lojaId).map { it.paraDto() } }
    }

    suspend fun criar(ator: Ator, req: NovoUsuarioDto): UsuarioDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        exigirPapelDeLoja(req.papel)
        if (!ator.podeGerenciarPapel(req.papel)) throw AcessoNegado("Você não pode cadastrar usuários com o papel ${req.papel.rotulo}.")
        Usuario.validarSenhaNova(req.senha)
        val novo = Usuario.novo(lojaId, req.nome, req.email, senhas.gerarHash(req.senha), req.papel)
        return tx.executar {
            if (usuarios.existeEmail(novo.email)) throw Conflito("Já existe um usuário com o e-mail ${novo.email}.")
            usuarios.salvar(novo).paraDto()
        }
    }

    suspend fun atualizar(ator: Ator, id: Long, req: AtualizarUsuarioDto): UsuarioDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        exigirPapelDeLoja(req.papel)
        val novaSenha = req.senha?.takeIf { it.isNotEmpty() }?.also { Usuario.validarSenhaNova(it) }
        val novoHash = novaSenha?.let { senhas.gerarHash(it) }

        return tx.executar {
            val usuario = usuarios.buscarDaLoja(lojaId, id) ?: throw NaoEncontrado("Usuário não encontrado.")
            if (!ator.podeGerenciarPapel(usuario.papel) || !ator.podeGerenciarPapel(req.papel)) throw AcessoNegado()
            if (usuario.id == ator.usuarioId && (!req.ativo || req.papel != usuario.papel)) {
                throw DadoInvalido("Você não pode desativar nem mudar o próprio papel.")
            }
            if (usuarios.existeEmail(req.email, exceto = id)) throw Conflito("Já existe um usuário com o e-mail ${req.email}.")
            usuario.atualizarDados(req.nome, req.email, req.papel, req.ativo)
            novoHash?.let(usuario::trocarSenha)
            usuarios.salvar(usuario).paraDto()
        }
    }

    suspend fun remover(ator: Ator, id: Long) {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        if (id == ator.usuarioId) throw DadoInvalido("Você não pode remover o próprio usuário.")
        tx.executar {
            val usuario = usuarios.buscarDaLoja(lojaId, id) ?: throw NaoEncontrado("Usuário não encontrado.")
            if (!ator.podeGerenciarPapel(usuario.papel)) throw AcessoNegado()
            usuarios.remover(lojaId, id)
        }
    }

    private fun exigirPapelDeLoja(papel: Papel) {
        if (!papel.ehDaLoja) throw AcessoNegado("Não é possível criar super admin pelo painel da loja.")
    }
}
