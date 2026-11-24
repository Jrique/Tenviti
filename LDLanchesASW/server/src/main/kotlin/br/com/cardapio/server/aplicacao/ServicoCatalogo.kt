package br.com.cardapio.server.aplicacao

import br.com.cardapio.server.dominio.Ator
import br.com.cardapio.server.dominio.Categoria
import br.com.cardapio.server.dominio.Conflito
import br.com.cardapio.server.dominio.DadoInvalido
import br.com.cardapio.server.dominio.NaoEncontrado
import br.com.cardapio.server.dominio.Produto
import br.com.cardapio.server.dominio.RepositorioCatalogo
import br.com.cardapio.server.dominio.Transacoes
import br.com.cardapio.shared.CategoriaDto
import br.com.cardapio.shared.Papel
import br.com.cardapio.shared.ProdutoDto
import br.com.cardapio.shared.SalvarCategoriaDto
import br.com.cardapio.shared.SalvarProdutoDto

class ServicoCatalogo(
    private val tx: Transacoes,
    private val catalogo: RepositorioCatalogo,
) {
    private val quemEdita = arrayOf(Papel.DONO, Papel.GERENTE)

    // ---------- Categorias ----------

    suspend fun listarCategorias(lojaId: Long): List<CategoriaDto> =
        tx.executar { catalogo.listarCategorias(lojaId).map { it.paraDto() } }

    suspend fun criarCategoria(ator: Ator, req: SalvarCategoriaDto): CategoriaDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        val nova = Categoria.nova(lojaId, req.nome, req.ordem)
        return tx.executar {
            if (catalogo.existeCategoriaComNome(lojaId, nova.nome)) throw Conflito("Já existe a categoria \"${nova.nome}\".")
            catalogo.salvarCategoria(nova).paraDto()
        }
    }

    suspend fun atualizarCategoria(ator: Ator, id: Long, req: SalvarCategoriaDto): CategoriaDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val categoria = catalogo.buscarCategoria(lojaId, id) ?: throw categoriaNaoEncontrada()
            if (catalogo.existeCategoriaComNome(lojaId, req.nome, exceto = id)) throw Conflito("Já existe a categoria \"${req.nome.trim()}\".")
            categoria.renomear(req.nome, req.ordem)
            catalogo.salvarCategoria(categoria).paraDto()
        }
    }

    /** Produtos da categoria removida ficam sem categoria (não são apagados). */
    suspend fun removerCategoria(ator: Ator, id: Long) {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        tx.executar {
            catalogo.buscarCategoria(lojaId, id) ?: throw categoriaNaoEncontrada()
            catalogo.removerCategoria(lojaId, id)
        }
    }

    // ---------- Produtos ----------

    suspend fun listarProdutos(lojaId: Long, somenteAtivos: Boolean): List<ProdutoDto> = tx.executar {
        val nomes = catalogo.listarCategorias(lojaId).associate { it.id to it.nome }
        catalogo.listarProdutos(lojaId, somenteAtivos).map {
            it.paraDto(it.categoriaId?.let(nomes::get), somenteOpcoesAtivas = somenteAtivos)
        }
    }

    suspend fun criarProduto(ator: Ator, req: SalvarProdutoDto): ProdutoDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        val novo = Produto.novo(
            lojaId, req.categoriaId, req.nome, req.descricao, req.precoCentavos, req.precoPromocionalCentavos,
            req.imagemUrl, req.grupos.map { it.paraDominio() }, req.destaque, req.ativo,
        )
        return tx.executar {
            val categoria = categoriaDaLoja(lojaId, req.categoriaId)
            catalogo.salvarProduto(novo).paraDto(categoria?.nome)
        }
    }

    suspend fun atualizarProduto(ator: Ator, id: Long, req: SalvarProdutoDto): ProdutoDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        val grupos = req.grupos.map { it.paraDominio() }
        return tx.executar {
            val produto = catalogo.buscarProduto(lojaId, id) ?: throw produtoNaoEncontrado()
            val categoria = categoriaDaLoja(lojaId, req.categoriaId)
            produto.atualizar(
                req.categoriaId, req.nome, req.descricao, req.precoCentavos, req.precoPromocionalCentavos,
                req.imagemUrl, grupos, req.destaque, req.ativo,
            )
            catalogo.salvarProduto(produto).paraDto(categoria?.nome)
        }
    }

    suspend fun marcarDestaque(ator: Ator, id: Long, destaque: Boolean): ProdutoDto {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val produto = catalogo.buscarProduto(lojaId, id) ?: throw produtoNaoEncontrado()
            produto.marcarDestaque(destaque)
            val categoria = produto.categoriaId?.let { catalogo.buscarCategoria(lojaId, it) }
            catalogo.salvarProduto(produto).paraDto(categoria?.nome)
        }
    }

    /** Pausa (esgotado) ou volta a vender o produto. Atendente também pode, para avisar que acabou. */
    suspend fun definirDisponivel(ator: Ator, id: Long, disponivel: Boolean): ProdutoDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE, Papel.ATENDENTE, Papel.COZINHA)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val produto = catalogo.buscarProduto(lojaId, id) ?: throw produtoNaoEncontrado()
            produto.definirDisponivel(disponivel)
            val categoria = produto.categoriaId?.let { catalogo.buscarCategoria(lojaId, it) }
            catalogo.salvarProduto(produto).paraDto(categoria?.nome)
        }
    }

    suspend fun removerProduto(ator: Ator, id: Long) {
        ator.exigirPapel(*quemEdita)
        val lojaId = ator.lojaObrigatoria()
        tx.executar {
            catalogo.buscarProduto(lojaId, id) ?: throw produtoNaoEncontrado()
            catalogo.removerProduto(lojaId, id)
        }
    }

    /** Garante que a categoria escolhida é da mesma loja (evita vincular produto à categoria de outra loja). */
    private fun categoriaDaLoja(lojaId: Long, categoriaId: Long?): Categoria? =
        categoriaId?.let { catalogo.buscarCategoria(lojaId, it) ?: throw DadoInvalido("Categoria não encontrada.") }

    private fun categoriaNaoEncontrada() = NaoEncontrado("Categoria não encontrada.")
    private fun produtoNaoEncontrado() = NaoEncontrado("Produto não encontrado.")
}
