package br.com.tenviti.app.ui.componentesUi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.data.Pedido
import br.com.tenviti.app.telas.componentes.tela.telaVendas.ModalDetalhesPedido
import kotlin.math.ceil
import androidx.compose.runtime.LaunchedEffect

enum class StatusPedido { TODOS, NOVO, VISTO, PREPARO, SAIU_PARA_ENTREGA, FINALIZADO }
// UI Comum para a lista de pedidos da tela de vendas
@Composable
fun ListaPedidosVendasDesktopUI(
    pedidos: List<Pedido>,
    carregando: Boolean,
    erro: String?,
    onChangeStatus: (Pedido, String) -> Unit,
    onPrint: (Pedido) -> Unit,
    pedidoInicialSelecionadoId: Long? = null,
    onPedidoIdConsumido: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pedidoSelecionado by remember { mutableStateOf<Pedido?>(null) }
    var filtroStatus by remember { mutableStateOf(StatusPedido.TODOS) }
    var paginaAtual by remember { mutableStateOf(1) }

    val itensPorPagina = 10
    val base = pedidos

    LaunchedEffect(pedidoInicialSelecionadoId, pedidos.size) {
        if (pedidoInicialSelecionadoId != null) {
            val encontrado = pedidos.firstOrNull { it.id == pedidoInicialSelecionadoId }
            if (encontrado != null) {
                pedidoSelecionado = encontrado
                onPedidoIdConsumido()
            }
        }
    }

    val pedidosFiltradosPorStatus = when (filtroStatus) {
        StatusPedido.PREPARO ->
            base.filter { it.status.equals("Em preparo", ignoreCase = true) }

        StatusPedido.SAIU_PARA_ENTREGA ->
            base.filter { it.status.equals("Saiu para entrega", ignoreCase = true) }

        StatusPedido.FINALIZADO ->
            base.filter { it.status.equals("Finalizado", ignoreCase = true) }

        StatusPedido.VISTO ->
            base.filter { it.status.equals("Visto", ignoreCase = true) }

        StatusPedido.NOVO ->
            base.filter { it.status.equals("Novo", ignoreCase = true) }

        StatusPedido.TODOS ->
            base
    }

    val totalPaginas = ceil(pedidosFiltradosPorStatus.size.toFloat() / itensPorPagina).toInt().coerceAtLeast(1)
    if (paginaAtual > totalPaginas) paginaAtual = totalPaginas

    val pedidosDaPagina = pedidosFiltradosPorStatus.drop((paginaAtual - 1) * itensPorPagina).take(itensPorPagina)

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pedidos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BotaoFiltroStatusDesktopUI("Todos", filtroStatus == StatusPedido.TODOS) { filtroStatus = StatusPedido.TODOS; paginaAtual = 1 }
                        BotaoFiltroStatusDesktopUI("Novo", filtroStatus == StatusPedido.NOVO) { filtroStatus = StatusPedido.NOVO; paginaAtual = 1 }
                        BotaoFiltroStatusDesktopUI("Visto", filtroStatus == StatusPedido.VISTO) { filtroStatus = StatusPedido.VISTO; paginaAtual = 1 }
                        BotaoFiltroStatusDesktopUI("Em preparo", filtroStatus == StatusPedido.PREPARO) { filtroStatus = StatusPedido.PREPARO; paginaAtual = 1 }
                        BotaoFiltroStatusDesktopUI("Saiu para entrega", filtroStatus == StatusPedido.SAIU_PARA_ENTREGA) { filtroStatus = StatusPedido.SAIU_PARA_ENTREGA; paginaAtual = 1 }
                        BotaoFiltroStatusDesktopUI("Finalizado", filtroStatus == StatusPedido.FINALIZADO) { filtroStatus = StatusPedido.FINALIZADO; paginaAtual = 1 }

                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* Visual */ }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF790001)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(Color(0xFF790001))
                    ))
                { Text("Filtrar período") }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                CabecalhoListaPedidosDesktoUI()
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 8.dp))

                Column(modifier = Modifier.padding(vertical = 8.dp).defaultMinSize(minHeight = 400.dp)) {
                    if (pedidosDaPagina.isEmpty()){
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center){
                            Text("Nenhum pedido encontrado", color = Color.Gray)
                        }
                    } else {
                        pedidosDaPagina.forEach { pedido ->
                            Box(modifier = Modifier.clickable { pedidoSelecionado = pedido }) {
                                LinhaItemPedidoDesktopUI(pedido = pedido) // Supondo que esta função exista
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { if (paginaAtual > 1) paginaAtual-- }, enabled = paginaAtual > 1) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Página Anterior") }
                    Text("$paginaAtual / $totalPaginas", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (paginaAtual < totalPaginas) paginaAtual++ }, enabled = paginaAtual < totalPaginas) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Próxima Página") }
                }
            }
        }
    }

    pedidoSelecionado?.let { pedido ->
        ModalDetalhesPedido(
            pedido = pedido,
            onDismissRequest = { pedidoSelecionado = null },
            onChangeStatus = { p, novoStatus ->
                onChangeStatus(p, novoStatus)
                pedidoSelecionado = null
            },
            onPrint = { p ->
                onPrint(p)
            }
        )
    }
}

@Composable
fun CabecalhoListaPedidosDesktoUI() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Pedido", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center )
        Text("Descrição", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
        Text("Valor", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
        Text("Horário", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
        Text("Status", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun BotaoFiltroStatusDesktopUI(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val corTexto = if (selecionado) Color(0xFF790001) else Color.Gray
    Text(
        text = texto,
        color = corTexto,
        fontWeight = if(selecionado) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp)
    )
}