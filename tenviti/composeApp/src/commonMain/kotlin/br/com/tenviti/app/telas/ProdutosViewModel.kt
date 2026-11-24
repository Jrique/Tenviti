package br.com.tenviti.app.telas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.api.PainelApi
import br.com.tenviti.app.data.Produto
import br.com.tenviti.shared.ProdutoDto
import br.com.tenviti.shared.SalvarProdutoDto
import kotlin.math.roundToLong

class ProdutosViewModel(
    private val api: PainelApi = ApiProvider.painelApi
) {
    var carregando by mutableStateOf(false)
        private set

    /** Mensagem de erro para o Snackbar. */
    var erro by mutableStateOf<String?>(null)
        private set

    var produtos by mutableStateOf<List<Produto>>(emptyList())
        private set

    suspend fun load() = executar("Erro ao carregar produtos") {
        produtos = api.listarProdutos().map { it.toProduto() }
    }

    suspend fun criar(produto: Produto, onSuccess: () -> Unit) = executar("Erro ao criar produto") {
        api.criarProduto(produto.toDto())
        produtos = api.listarProdutos().map { it.toProduto() }
        onSuccess()
    }

    suspend fun atualizar(produto: Produto, onSuccess: () -> Unit) {
        val id = produto.id ?: return
        executar("Erro ao atualizar produto") {
            api.atualizarProduto(id, produto.toDto())
            produtos = api.listarProdutos().map { it.toProduto() }
            onSuccess()
        }
    }

    suspend fun deletar(id: Long) = executar("Erro ao excluir produto") {
        api.removerProduto(id)
        produtos = produtos.filterNot { it.id == id }
    }

    suspend fun toggleDestaque(produto: Produto) {
        val id = produto.id ?: return
        executar("Erro ao alterar destaque") {
            val atualizado = api.marcarDestaque(id, !produto.destacado).toProduto()
            produtos = produtos.map { if (it.id == id) atualizado else it }
        }
    }

    /** Pausa (esgotado) ou volta a vender, direto da lista. */
    suspend fun alternarDisponivel(produto: Produto) {
        val id = produto.id ?: return
        executar("Erro ao pausar produto") {
            val atualizado = api.definirDisponivel(id, !produto.ativo).toProduto()
            produtos = produtos.map { if (it.id == id) atualizado else it }
        }
    }

    /** Envia a foto e devolve o endereço (ou null se falhar; o erro vai para [erro]). */
    suspend fun enviarFoto(arquivo: br.com.tenviti.app.data.ArquivoSelecionado): String? {
        var url: String? = null
        executar("Erro ao enviar a foto") { url = api.enviarImagem(arquivo) }
        return url
    }

    private suspend fun executar(mensagemPadrao: String, bloco: suspend () -> Unit) {
        try {
            carregando = true
            erro = null
            bloco()
        } catch (e: Throwable) {
            erro = e.message ?: mensagemPadrao
        } finally {
            carregando = false
        }
    }
}

// ================= MAPEAMENTO DTO <-> UI =================

private fun ProdutoDto.toProduto() = Produto(
    id = id,
    nome = nome,
    descricao = descricao,
    preco = precoCentavos / 100.0,
    precoPromocional = precoPromocionalCentavos?.let { it / 100.0 },
    imagemUrl = imagemUrl,
    tipo = categoriaNome.orEmpty(),
    categoriaId = categoriaId,
    grupos = grupos,
    destacado = destaque,
    ativo = ativo,
)

fun Produto.toDto() = SalvarProdutoDto(
    categoriaId = categoriaId,
    nome = nome,
    descricao = descricao,
    precoCentavos = (preco * 100).roundToLong(),
    precoPromocionalCentavos = precoPromocional?.let { (it * 100).roundToLong() },
    imagemUrl = imagemUrl,
    grupos = grupos,
    destaque = destacado,
    ativo = ativo,
)
