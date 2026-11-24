package br.com.cardapio.app.telas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.cardapio.app.api.ApiProvider
import br.com.cardapio.app.api.PainelApi
import br.com.cardapio.app.data.Pedido
import br.com.cardapio.app.data.formatarAgendamento
import br.com.cardapio.app.data.formatarHorarioPedido
import br.com.cardapio.shared.PedidoDto
import br.com.cardapio.shared.StatusPedido
import br.com.cardapio.shared.emReais
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/** Formata um valor em reais (Double) como "R$ 1.234,56". */
fun Double.toReal(): String = (this * 100).roundToLong().emReais()

class PedidosViewModel : ViewModel() {

    private val api: PainelApi = ApiProvider.painelApi

    val carregando = mutableStateOf(false)
    val erro = mutableStateOf<String?>(null)
    val pedidos = mutableStateListOf<Pedido>()

    init {
        carregar()
    }

    /** [inicio] e [fim] no formato AAAA-MM-DD. */
    fun carregar(inicio: String? = null, fim: String? = null) {
        viewModelScope.launch {
            try {
                carregando.value = true
                erro.value = null
                val lista = api.listarPedidos(inicio, fim).sortedByDescending { it.id }
                pedidos.clear()
                pedidos.addAll(lista.map { it.toPedido() })
            } catch (e: Throwable) {
                erro.value = e.message
            } finally {
                carregando.value = false
            }
        }
    }

    fun alterarStatus(pedido: Pedido, novoStatusLabel: String) {
        val novoStatus = labelParaStatus(novoStatusLabel) ?: return
        viewModelScope.launch {
            try {
                carregando.value = true
                erro.value = null
                val atualizado = api.alterarStatusPedido(pedido.id, novoStatus).toPedido()
                val idx = pedidos.indexOfFirst { it.id == pedido.id }
                if (idx != -1) {
                    pedidos.removeAt(idx)
                    pedidos.add(0, atualizado)
                } else {
                    carregar()
                }
            } catch (e: Throwable) {
                erro.value = e.message
            } finally {
                carregando.value = false
            }
        }
    }

    private fun PedidoDto.toPedido() = Pedido(
        id = id,
        codigo = codigo,
        descricao = descricao,
        valor = totalCentavos.emReais(),
        horario = formatarHorarioPedido(criadoEm) + (agendadoPara?.let { " · encomenda p/ ${formatarAgendamento(it)}" } ?: ""),
        status = status.rotulo,
        corStatus = corDoStatus(status),
    )

    private fun labelParaStatus(label: String): StatusPedido? =
        StatusPedido.entries.find { it.rotulo.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }

    private fun corDoStatus(status: StatusPedido): Color = when (status) {
        StatusPedido.NOVO -> Color(0xFF2196F3)
        StatusPedido.VISTO -> Color(0xFF4CAF50)
        StatusPedido.EM_PREPARO -> Color(0xFFFFC107)
        StatusPedido.SAIU_PARA_ENTREGA -> Color(0xFF9C27B0)
        StatusPedido.FINALIZADO -> Color(0xFFF44336)
        StatusPedido.CANCELADO -> Color.Gray
    }
}
