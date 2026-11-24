package br.com.tenviti.app.ui.componentesUi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.tenviti.app.data.Pedido
import br.com.tenviti.app.telas.componentes.tela.telaVendas.ModalDetalhesPedido
import kotlin.math.ceil
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
fun ListaPedidosVendasAndroidUI(
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
    var mostrarModalLista by remember { mutableStateOf(false) }

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

    Column(modifier = modifier) {
        Text(
            "Pedidos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            CabecalhoListaPedidos()
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.padding(vertical = 8.dp)) {

                // últimos 5 pedidos (ou menos)
                base.take(5).forEach { pedido ->
                    Box(
                        modifier = Modifier
                            .clickable { pedidoSelecionado = pedido }
                    ) {
                        LinhaItemPedidoAndroidUI(pedido = pedido)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.Gray.copy(alpha = 0.1f)
                    )
                }

                TextButton(
                    onClick = { mostrarModalLista = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        "Ver mais",
                        color = Color(0xFF790001),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (mostrarModalLista) {
        ModalPedidosPaginados(
            pedidos = base,
            onDismissRequest = { mostrarModalLista = false },
            onPedidoClick = { pedido ->
                pedidoSelecionado = pedido
            }
        )
    }

    pedidoSelecionado?.let { pedido ->
        ModalDetalhesPedido(
            pedido = pedido,
            onDismissRequest = { pedidoSelecionado = null },
            onChangeStatus = { p, novoStatus ->
                onChangeStatus(p, novoStatus)
                // depois de mudar status, fecha o modal
                pedidoSelecionado = null
            },
            onPrint = { p ->
                onPrint(p)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModalPedidosPaginados(
    pedidos: List<Pedido>,
    onDismissRequest: () -> Unit,
    onPedidoClick: (Pedido) -> Unit
) {
    var paginaAtual by remember { mutableIntStateOf(1) }
    var filtroStatus by remember { mutableStateOf(StatusPedido.TODOS) }
    val itensPorPagina = 20

    val base = pedidos

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

        else ->
            base
    }

    val totalPaginas = ceil(
        pedidosFiltradosPorStatus.size.toFloat() / itensPorPagina
    ).toInt().coerceAtLeast(1)

    if (paginaAtual > totalPaginas) paginaAtual = totalPaginas

    val pedidosDaPagina = pedidosFiltradosPorStatus
        .drop((paginaAtual - 1) * itensPorPagina)
        .take(itensPorPagina)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Todos os Pedidos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        BotaoFiltroStatus(
                            "Todos",
                            filtroStatus == StatusPedido.TODOS
                        ) {
                            filtroStatus = StatusPedido.TODOS
                            paginaAtual = 1
                        }

                        BotaoFiltroStatus(
                            "Novo",
                            filtroStatus == StatusPedido.NOVO
                        ) {
                            filtroStatus = StatusPedido.NOVO
                            paginaAtual = 1
                        }
                        BotaoFiltroStatus(
                            "Visto",
                            filtroStatus == StatusPedido.VISTO

                        ) {
                            filtroStatus = StatusPedido.VISTO
                            paginaAtual = 1
                        }
                        BotaoFiltroStatus(
                            "Em preparo",
                            filtroStatus == StatusPedido.PREPARO
                        ) {
                            filtroStatus = StatusPedido.PREPARO
                            paginaAtual = 1
                        }
                        BotaoFiltroStatus(
                            "Saiu para entrega",
                            filtroStatus == StatusPedido.SAIU_PARA_ENTREGA
                        ) {
                            filtroStatus = StatusPedido.SAIU_PARA_ENTREGA
                            paginaAtual = 1
                        }
                        BotaoFiltroStatus(
                            "Finalizado",
                            filtroStatus == StatusPedido.FINALIZADO
                        ) {
                            filtroStatus = StatusPedido.FINALIZADO
                            paginaAtual = 1
                        }

                    }

                    Button(
                        onClick = { /* futuramente: filtro por período */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF790001)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Filtrar por Período")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    stickyHeader {
                        CabecalhoListaPedidosModal()
                    }

                    if (pedidosDaPagina.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Nenhum pedido encontrado",
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(pedidosDaPagina) { pedido ->
                            Box(
                                modifier = Modifier
                                    .clickable { onPedidoClick(pedido) }
                            ) {
                                LinhaItemPedidoAndroidUI(pedido = pedido)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Fechar", color = Color(0xFF790001))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (paginaAtual > 1) paginaAtual-- },
                            enabled = paginaAtual > 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Página Anterior"
                            )
                        }
                        Text(
                            "$paginaAtual / $totalPaginas",
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                if (paginaAtual < totalPaginas) paginaAtual++
                            },
                            enabled = paginaAtual < totalPaginas
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Próxima Página"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabecalhoListaPedidos() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Pedido",
                    modifier = Modifier.weight(1.5f),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Desc.",
                    modifier = Modifier.weight(2f),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Status",
                    modifier = Modifier.weight(1.5f),
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CabecalhoListaPedidosModal() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9E6))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Pedido",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            "Descrição",
            modifier = Modifier.weight(2f),
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Text(
            "Status",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BotaoFiltroStatus(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    val corTexto = if (selecionado) Color(0xFF790001) else Color.Gray
    Text(
        text = texto,
        color = corTexto,
        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}
