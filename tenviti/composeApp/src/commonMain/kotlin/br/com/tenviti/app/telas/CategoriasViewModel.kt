package br.com.tenviti.app.telas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.api.PainelApi
import br.com.tenviti.app.data.Categoria
import br.com.tenviti.shared.CategoriaDto

class CategoriasViewModel(
    private val api: PainelApi = ApiProvider.painelApi
) {
    var categorias by mutableStateOf<List<Categoria>>(emptyList())
        private set

    var carregando by mutableStateOf(false)
        private set

    var erro by mutableStateOf<String?>(null)
        private set

    suspend fun carregar() {
        try {
            carregando = true
            erro = null
            categorias = api.listarCategorias().map { it.toDomain() }
        } catch (e: Exception) {
            erro = e.message
        } finally {
            carregando = false
        }
    }

    suspend fun salvar(nome: String, idExistente: Long? = null) {
        if (nome.isBlank()) return
        val idValido = idExistente?.takeIf { it > 0 }
        try {
            erro = null
            if (idValido == null) {
                categorias = categorias + api.criarCategoria(nome).toDomain()
            } else {
                val dto = api.atualizarCategoria(idValido, nome)
                categorias = categorias.map { if (it.id == idValido) dto.toDomain() else it }
            }
        } catch (e: Exception) {
            erro = e.message
        }
    }

    suspend fun excluir(id: Long) {
        try {
            erro = null
            api.removerCategoria(id)
            categorias = categorias.filterNot { it.id == id }
        } catch (e: Exception) {
            erro = e.message
        }
    }
}

fun CategoriaDto.toDomain() = Categoria(id = id, nome = nome)
