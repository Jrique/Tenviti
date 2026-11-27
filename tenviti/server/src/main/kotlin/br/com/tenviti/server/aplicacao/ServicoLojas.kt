package br.com.tenviti.server.aplicacao

import br.com.tenviti.server.dominio.AcessoNegado
import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.server.dominio.Conflito
import br.com.tenviti.server.dominio.Funcionamento
import br.com.tenviti.server.dominio.Loja
import br.com.tenviti.server.dominio.NaoEncontrado
import br.com.tenviti.server.dominio.RepositorioLojas
import br.com.tenviti.server.dominio.RepositorioUsuarios
import br.com.tenviti.server.dominio.Transacoes
import br.com.tenviti.server.dominio.Usuario
import br.com.tenviti.server.infra.seguranca.Senhas
import br.com.tenviti.shared.AparenciaDto
import br.com.tenviti.shared.AtualizarLojaDto
import br.com.tenviti.shared.InformacoesLojaDto
import br.com.tenviti.shared.LojaDto
import br.com.tenviti.shared.NovaLojaDto
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.VitrineLojaDto

class ServicoLojas(
    private val tx: Transacoes,
    private val lojas: RepositorioLojas,
    private val usuarios: RepositorioUsuarios,
    private val senhas: Senhas,
) {
    // ---------- Super admin ----------

    suspend fun listar(ator: Ator): List<LojaDto> {
        ator.exigirSuperAdmin()
        return tx.executar { lojas.listar().map { it.paraDto() } }
    }

    suspend fun buscar(ator: Ator, id: Long): LojaDto {
        ator.exigirSuperAdmin()
        return tx.executar { lojas.buscar(id)?.paraDto() } ?: throw lojaNaoEncontrada()
    }

    /** Cria a loja já com o usuário dono. */
    suspend fun criar(ator: Ator, req: NovaLojaDto): LojaDto {
        ator.exigirSuperAdmin()
        // Trabalhar (validações de domínio acontecem na construção)
        val loja = Loja.nova(
            slug = req.slug.trim().lowercase(),
            nome = req.nome,
            fusoHorario = req.fusoHorario,
            aparencia = req.aparencia.paraDominio(),
            modulos = req.modulos,
            personalizacaoLiberada = req.personalizacaoLiberada,
        )
        Usuario.validarSenhaNova(req.donoSenha)
        val hash = senhas.gerarHash(req.donoSenha)

        // Buscar + Persistir na mesma transação para não deixar loja sem dono
        return tx.executar {
            if (lojas.existeSlug(loja.slug)) throw Conflito("Já existe uma loja com o endereço \"${loja.slug}\".")
            if (usuarios.existeEmail(req.donoEmail)) throw Conflito("Já existe um usuário com o e-mail ${req.donoEmail}.")
            val salva = lojas.salvar(loja)
            usuarios.salvar(Usuario.novo(salva.id, req.donoNome, req.donoEmail, hash, Papel.DONO))
            salva.paraDto()
        }
    }

    suspend fun atualizar(ator: Ator, id: Long, req: AtualizarLojaDto): LojaDto {
        ator.exigirSuperAdmin()
        val aparencia = req.aparencia.paraDominio()
        return tx.executar {
            val loja = lojas.buscar(id) ?: throw lojaNaoEncontrada()
            val slug = req.slug.trim().lowercase()
            if (lojas.existeSlug(slug, exceto = id)) throw Conflito("Já existe uma loja com o endereço \"$slug\".")
            loja.configurarPeloSuperAdmin(
                slug, req.nome, req.ativa, req.fusoHorario, aparencia, req.modulos, req.personalizacaoLiberada,
            )
            lojas.salvar(loja).paraDto()
        }
    }

    // ---------- Painel da loja ----------

    /**
     * Confere, a cada requisição do painel, que o usuário ainda está ativo e que a loja dele está ativa.
     * Assim, desativar um usuário ou uma loja corta o acesso na hora, sem esperar o token expirar.
     */
    suspend fun lojaDoPainel(ator: Ator): Loja {
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val usuario = usuarios.buscarDaLoja(lojaId, ator.usuarioId)
            if (usuario == null || !usuario.podeEntrar()) throw AcessoNegado("Seu usuário está desativado.", "usuario_inativo")
            val loja = lojas.buscar(lojaId) ?: throw lojaNaoEncontrada()
            if (!loja.ativa) throw AcessoNegado("Esta loja está desativada. Fale com o suporte.", "loja_inativa")
            loja
        }
    }

    suspend fun minhaLoja(ator: Ator): LojaDto {
        val lojaId = ator.lojaObrigatoria()
        return tx.executar { lojas.buscar(lojaId)?.paraDto() } ?: throw lojaNaoEncontrada()
    }

    /** O dono altera a aparência; o domínio bloqueia o que não foi liberado pelo super admin. */
    suspend fun personalizar(ator: Ator, req: AparenciaDto): LojaDto {
        ator.exigirPapel(Papel.DONO)
        val lojaId = ator.lojaObrigatoria()
        val nova = req.paraDominio()
        return tx.executar {
            val loja = lojas.buscar(lojaId) ?: throw lojaNaoEncontrada()
            loja.personalizarPeloDono(nova)
            lojas.salvar(loja).paraDto()
        }
    }

    /** Contato, endereço, horários e regras de encomenda (dono e gerente). */
    suspend fun informacoes(ator: Ator): InformacoesLojaDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar { lojas.buscar(lojaId)?.paraInformacoesDto() } ?: throw lojaNaoEncontrada()
    }

    suspend fun atualizarInformacoes(ator: Ator, req: InformacoesLojaDto): InformacoesLojaDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        // Trabalhar: o domínio valida telefone, endereço, horários e regras de encomenda
        val contato = req.contato.paraDominio()
        val funcionamento = req.funcionamento.paraDominio()
        val entrega = req.entrega.paraDominio()
        return tx.executar {
            val loja = lojas.buscar(lojaId) ?: throw lojaNaoEncontrada()
            loja.atualizarInformacoes(contato, funcionamento, entrega)
            lojas.salvar(loja).paraInformacoesDto()
        }
    }

    // ---------- Público ----------

    suspend fun vitrine(slug: String): VitrineLojaDto {
        val loja = lojaPublica(slug)
        return loja.paraVitrine(Funcionamento.agoraEm(loja.zona))
    }

    /** Resolve o slug público para o id da loja (somente lojas ativas). */
    suspend fun lojaPublica(slug: String): Loja =
        tx.executar { lojas.buscarPorSlug(slug)?.takeIf { it.visivelAoPublico() } } ?: throw lojaNaoEncontrada()

    private fun lojaNaoEncontrada() = NaoEncontrado("Loja não encontrada.", "loja_nao_encontrada")
}
