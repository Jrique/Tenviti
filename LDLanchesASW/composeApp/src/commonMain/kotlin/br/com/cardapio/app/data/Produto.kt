package br.com.cardapio.app.data

import br.com.cardapio.shared.GrupoOpcoesDto
import br.com.cardapio.shared.emReais

data class Produto(
    val id: Long? = null,
    val nome: String,
    val descricao: String = "",
    val preco: Double,
    /** Preço "por" quando em promoção (menor que [preco]). */
    val precoPromocional: Double? = null,
    val imagemUrl: String? = null,
    /** Nome da categoria (exibição). */
    val tipo: String = "",
    val categoriaId: Long? = null,
    val grupos: List<GrupoOpcoesDto> = emptyList(),
    val destacado: Boolean = false,
    /** false = pausado/esgotado (some do cardápio sem ser apagado). */
    val ativo: Boolean = true,
) {
    /** Texto curto com as opções, para listas e o "ver opções". */
    val resumoOpcoes: String
        get() = if (grupos.isEmpty()) {
            "Este produto não tem opções."
        } else {
            grupos.joinToString("\n\n") { g ->
                val regra = when {
                    g.minimo > 0 && g.minimo == g.maximo -> "escolha ${g.minimo}"
                    g.minimo > 0 -> "de ${g.minimo} a ${g.maximo}"
                    else -> "até ${g.maximo}"
                }
                val opcoes = g.opcoes.joinToString(", ") { o ->
                    o.nome + (if (o.precoCentavos > 0) " (+${o.precoCentavos.emReais()})" else "") + (if (!o.ativo) " [oculta]" else "")
                }
                "${g.nome} — $regra\n$opcoes"
            }
        }
}

/** Arquivo escolhido pelo usuário no seletor da plataforma (Android, desktop ou web). */
class ArquivoSelecionado(val nome: String, val bytes: ByteArray)
