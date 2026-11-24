package br.com.tenviti.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import br.com.tenviti.app.data.Session
import br.com.tenviti.app.telas.*
import br.com.tenviti.app.telas.PedidosViewModel

@Composable
fun LayoutEstiloDesktop(
    telaAtual: Tela,
    onTelaSelecionada: (Tela) -> Unit,
    onLogout: () -> Unit
) {
    //  VIEW MODELS
    val pedidosVmNotif = remember { PedidosViewModel() }

    // -ESTADOS
    var notificacoes by remember { mutableStateOf(listOf<NotificacaoPedido>()) }
    var painelAberto by remember { mutableStateOf(false) }

    var mostrarToast by remember { mutableStateOf(false) }
    var mensagemToast by remember { mutableStateOf("") }
    var corToast by remember { mutableStateOf(Color(0xFF790001)) }

    var pedidoFromNotificationId by remember { mutableStateOf<Long?>(null) }
    var knownIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val pedidos = pedidosVmNotif.pedidos

    // POLLING DE PEDIDOS (A cada 7s)
    val recebeNotificacoes = Session.usuarioLogado?.papel in Tela.PAPEIS_QUE_ATENDEM
    LaunchedEffect(recebeNotificacoes) {
        while (recebeNotificacoes) {
            pedidosVmNotif.carregar()
            delay(10000)
        }
    }

    // DETECÇÃO DE NOVOS PEDIDOS
    LaunchedEffect(pedidos.size) {
        if (pedidos.isEmpty()) return@LaunchedEffect

        val atuaisIds = pedidos.map { it.id }.toSet()
        val novosIds = atuaisIds - knownIds

        if (knownIds.isNotEmpty() && novosIds.isNotEmpty()) {
            val novosPedidos = pedidos
                .filter { it.id in novosIds }
                .sortedByDescending { it.id }

            notificacoes = (novosPedidos.map { p ->
                NotificacaoPedido(
                    id = p.id,
                    codigo = "Novo Pedido #${p.codigo}",
                    horario = p.horario
                )
            } + notificacoes).distinctBy { it.id }

            mensagemToast = "Você tem um novo pedido!"
            corToast = Color(0xFF790001)
            mostrarToast = true
        }
        knownIds = atuaisIds
    }


    // Timer para fechar o Toast
    LaunchedEffect(mostrarToast) {
        if (mostrarToast) {
            delay(4000)
            mostrarToast = false
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        MenuLateral(
            modificador = Modifier.width(260.dp),
            telaAtual = telaAtual,
            onTelaSelecionada = onTelaSelecionada,
            onLogout = onLogout
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (telaAtual) {
                Tela.DASHBOARD -> TelaInicial()
                Tela.PRODUTOS -> TelaProdutos()
                Tela.FATURAMENTO -> TelaFaturamento()
                Tela.VENDAS -> TelaVendas(
                    pedidoIdParaAbrir = pedidoFromNotificationId,
                    onPedidoIdConsumido = { pedidoFromNotificationId = null }
                )
                Tela.CATEGORIAS -> TelaCategorias()
                Tela.CONFIGURACOES -> TelaConfiguracoes()
                Tela.DADOS_LOJA -> TelaDadosLoja()
                Tela.APARENCIA -> TelaAparencia()
            }

            // COMPONENTES

            // Painel de Notificações
            NotificationBellAndPanel(
                notificacoes = notificacoes,
                painelAberto = painelAberto,
                onTogglePainel = {
                    if (notificacoes.isNotEmpty()) painelAberto = !painelAberto
                },
                onClickNotification = { notif ->
                    pedidoFromNotificationId = notif.id
                    onTelaSelecionada(Tela.VENDAS)
                    // Remove da lista ao clicar
                    notificacoes = notificacoes.filterNot { it.id == notif.id }
                    if(notificacoes.isEmpty()) painelAberto = false
                },
                onClearAll = {
                    notificacoes = emptyList()
                    painelAberto = false
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 24.dp)
            )

            // Toast Central (Aviso Flutuante)
            if (mostrarToast) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = corToast,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (corToast != Color(0xFF790001)) { // Se não for vinho, é alerta
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = mensagemToast,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationBellAndPanel(
    notificacoes: List<NotificacaoPedido>,
    painelAberto: Boolean,
    onTogglePainel: () -> Unit,
    onClickNotification: (NotificacaoPedido) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(
            onClick = onTogglePainel,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            BadgedBox(
                badge = {
                    if (notificacoes.isNotEmpty()) {
                        Badge(
                            containerColor = Color(0xFF790001),
                            contentColor = Color.White
                        ) {
                            Text(notificacoes.size.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notificações de novos pedidos",
                    tint = if (notificacoes.isNotEmpty()) Color(0xFF790001) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Painel suspenso (Dropdown)
        if (painelAberto && notificacoes.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 50.dp)
                    .width(320.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF8F6),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF790001).copy(alpha = 0.1f)) // Borda sutil
            ) {
                Column {
                    // Cabeçalho do Painel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFE9E6)) // Fundo do cabeçalho
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Novos Pedidos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E342E)
                        )
                        TextButton(
                            onClick = onClearAll,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Limpar", fontSize = 12.sp, color = Color(0xFF790001))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF790001).copy(alpha = 0.1f))

                    // Lista de Notificações
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        notificacoes.forEach { notif ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClickNotification(notif) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bolinha indicadora
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF790001), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = notif.codigo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Horário: ${notif.horario}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = Color.Gray.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Rodapézinho (Opcional)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Toque para ver detalhes",
                            fontSize = 10.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}