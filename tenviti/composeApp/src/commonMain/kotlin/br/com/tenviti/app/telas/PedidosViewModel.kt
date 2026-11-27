package br.com.tenviti.app.telas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.api.PainelApi
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.StatusPedido
import br.com.tenviti.shared.emReais
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** Formata um valor em reais (Double) como "R$ 1.234,56". */
fun Double.toReal(): String = (this * 100).roundToLong().emReais()

/**
 * Pedidos da loja (últimos 30 dias). Usado pela moldura (avisos de pedido novo),
 * pela tela Hoje e pelo quadro de pedidos.
 */
class PedidosViewModel : ViewModel() {

    private val api: PainelApi = ApiProvider.painelApi

    var pedidos by mutableStateOf<List<PedidoDto>>(emptyList())
        private set
    var carregando by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)
    var jaCarregou by mutableStateOf(false)
        private set

    /** [silencioso]: atualização automática, sem mostrar "carregando". */
    fun carregar(silencioso: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!silencioso) carregando = true
                erro = null
                pedidos = api.listarPedidos().sortedByDescending { it.id }
                jaCarregou = true
            } catch (e: Throwable) {
                if (!silencioso) erro = e.message
            } finally {
                carregando = false
            }
        }
    }

    fun alterarStatus(id: Long, novo: StatusPedido) {
        viewModelScope.launch {
            try {
                erro = null
                val atualizado = api.alterarStatusPedido(id, novo)
                pedidos = pedidos.map { if (it.id == id) atualizado else it }
            } catch (e: Throwable) {
                erro = e.message
            }
        }
    }
}

/** Próximo passo natural de um pedido (o botão principal do cartão). */
fun StatusPedido.proximo(): StatusPedido? = when (this) {
    StatusPedido.NOVO, StatusPedido.VISTO -> StatusPedido.EM_PREPARO
    StatusPedido.EM_PREPARO -> StatusPedido.SAIU_PARA_ENTREGA
    StatusPedido.SAIU_PARA_ENTREGA -> StatusPedido.FINALIZADO
    StatusPedido.FINALIZADO, StatusPedido.CANCELADO -> null
}

/** Texto do botão que leva ao próximo status. */
fun StatusPedido.acao(): String? = when (this) {
    StatusPedido.NOVO, StatusPedido.VISTO -> "Aceitar"
    StatusPedido.EM_PREPARO -> "Pronto"
    StatusPedido.SAIU_PARA_ENTREGA -> "Entregue"
    else -> null
}

/** Título curto do pedido: "Box com 4 cookies" ou "Box com 4 cookies + 2 itens". */
fun PedidoDto.tituloCurto(): String {
    if (itens.isEmpty()) {
        return descricao.lineSequence().map { it.trim().removePrefix("-").trim() }
            .firstOrNull { it.isNotBlank() && !it.endsWith(":") } ?: "Pedido"
    }
    val primeiro = itens.first()
    val nome = if (primeiro.quantidade > 1) "${primeiro.quantidade}x ${primeiro.nome}" else primeiro.nome
    return if (itens.size > 1) "$nome + ${itens.size - 1} ${if (itens.size == 2) "item" else "itens"}" else nome
}

val PedidoDto.totalEmReais: String get() = totalCentavos.emReais()
