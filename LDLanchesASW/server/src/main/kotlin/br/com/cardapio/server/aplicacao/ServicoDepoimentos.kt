package br.com.cardapio.server.aplicacao

import br.com.cardapio.server.dominio.Ator
import br.com.cardapio.server.dominio.Depoimento
import br.com.cardapio.server.dominio.NaoEncontrado
import br.com.cardapio.server.dominio.RepositorioDepoimentos
import br.com.cardapio.server.dominio.Transacoes
import br.com.cardapio.shared.DepoimentoDto
import br.com.cardapio.shared.Papel

/** Depoimentos de clientes cadastrados pelo dono/gerente e exibidos no cardápio. */
class ServicoDepoimentos(
    private val tx: Transacoes,
    private val depoimentos: RepositorioDepoimentos,
) {
    private val quemEdita = arrayOf(Papel.DONO, Papel.GERENTE)

    suspend fun listarPublicos(lojaId: Long): List<DepoimentoDto> =
        tx.executar { depoimentos.listar(lojaId, somenteAtivos = true).map { it.paraDto() } }

    suspend fun listar(ator: Ator): List<DepoimentoDto> {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar { depoimentos.listar(lojaId, somenteAtivos = false).map { it.paraDto() } }
    }

    suspend fun criar(ator: Ator, req: DepoimentoDto): DepoimentoDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        val novo = Depoimento.novo(lojaId, req.autor, req.texto, req.nota, req.ativo, req.ordem)
        return tx.executar { depoimentos.salvar(novo).paraDto() }
    }

    suspend fun atualizar(ator: Ator, id: Long, req: DepoimentoDto): DepoimentoDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val depoimento = depoimentos.buscar(lojaId, id) ?: throw naoEncontrado()
            depoimento.atualizar(req.autor, req.texto, req.nota, req.ativo, req.ordem)
            depoimentos.salvar(depoimento).paraDto()
        }
    }

    suspend fun remover(ator: Ator, id: Long) {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        tx.executar {
            depoimentos.buscar(lojaId, id) ?: throw naoEncontrado()
            depoimentos.remover(lojaId, id)
        }
    }

    private fun naoEncontrado() = NaoEncontrado("Depoimento não encontrado.")
}
