package br.com.cardapio.server.dominio

import java.time.Instant
import java.time.LocalDate

// Contratos de persistência. O domínio define o que precisa; a infra implementa.
// Todo método de dados de loja recebe o lojaId — é isso que garante o isolamento entre lojas.

interface RepositorioLojas {
    fun buscar(id: Long): Loja?
    fun buscarPorSlug(slug: String): Loja?
    fun listar(): List<Loja>
    fun existeSlug(slug: String, exceto: Long? = null): Boolean
    fun salvar(loja: Loja): Loja
}

interface RepositorioUsuarios {
    fun buscar(id: Long): Usuario?
    fun buscarDaLoja(lojaId: Long, id: Long): Usuario?
    fun buscarPorEmail(email: String): Usuario?
    fun listarDaLoja(lojaId: Long): List<Usuario>
    fun existeEmail(email: String, exceto: Long? = null): Boolean
    fun existeSuperAdmin(): Boolean
    fun salvar(usuario: Usuario): Usuario
    fun remover(lojaId: Long, id: Long)
}

interface RepositorioCatalogo {
    fun listarCategorias(lojaId: Long): List<Categoria>
    fun buscarCategoria(lojaId: Long, id: Long): Categoria?
    fun existeCategoriaComNome(lojaId: Long, nome: String, exceto: Long? = null): Boolean
    fun salvarCategoria(categoria: Categoria): Categoria
    fun removerCategoria(lojaId: Long, id: Long)

    fun listarProdutos(lojaId: Long, somenteAtivos: Boolean = false): List<Produto>
    fun buscarProduto(lojaId: Long, id: Long): Produto?
    fun salvarProduto(produto: Produto): Produto
    fun removerProduto(lojaId: Long, id: Long)
}

interface RepositorioDepoimentos {
    fun listar(lojaId: Long, somenteAtivos: Boolean): List<Depoimento>
    fun buscar(lojaId: Long, id: Long): Depoimento?
    fun salvar(depoimento: Depoimento): Depoimento
    fun remover(lojaId: Long, id: Long)
}

/** Onde ficam as imagens enviadas pelas lojas (disco hoje; nuvem no futuro, sem mudar quem usa). */
interface ArmazenamentoArquivos {
    /** Grava o arquivo e devolve o endereço público dele (ex.: /midia/3/abc.png). */
    fun salvar(lojaId: Long, extensao: String, conteudo: ByteArray): String
}

interface RepositorioPedidos {
    fun buscar(lojaId: Long, id: Long): Pedido?
    fun buscarPorCodigo(lojaId: Long, codigo: String): Pedido?
    fun existeCodigo(lojaId: Long, codigo: String): Boolean
    fun listar(lojaId: Long, de: Instant, ate: Instant, limite: Int): List<Pedido>
    /** Pedidos não cancelados por dia de produção, no intervalo [de, ate]. */
    fun contarPorDia(lojaId: Long, de: LocalDate, ate: LocalDate): Map<LocalDate, Int>
    /** Trava (até o fim da transação) a contagem de pedidos de um dia da loja, para o limite diário. */
    fun travarDia(lojaId: Long, dia: LocalDate)
    fun salvar(pedido: Pedido): Pedido
}

/** Executa um bloco dentro de uma transação de banco. */
interface Transacoes {
    suspend fun <T> executar(bloco: () -> T): T
}
