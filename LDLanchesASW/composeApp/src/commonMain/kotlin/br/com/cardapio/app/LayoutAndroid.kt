package br.com.cardapio.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import kotlinx.coroutines.launch
import br.com.cardapio.app.data.Session
import br.com.cardapio.app.telas.*
import br.com.cardapio.app.telas.PedidosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEstiloAndroid(
    telaAtual: Tela,
    onTelaSelecionada: (Tela) -> Unit,
    onLogout: () -> Unit
) {
    val estadoMenuGaveta = rememberDrawerState(initialValue = DrawerValue.Closed)
    val escopo = rememberCoroutineScope()

    // VIEW MODELS
    val pedidosVmNotif = remember { PedidosViewModel() }

    // ESTADOS
    var notificacoes by remember { mutableStateOf(listOf<NotificacaoPedido>()) }
    var painelAberto by remember { mutableStateOf(false) }

    // Toast
    var mostrarToast by remember { mutableStateOf(false) }
    var mensagemToast by remember { mutableStateOf("") }
    var corToast by remember { mutableStateOf(Color(0xFF790001)) }

    var pedidoFromNotificationId by remember { mutableStateOf<Long?>(null) }
    var knownIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val pedidos = pedidosVmNotif.pedidos

    // POLLING PEDIDOS
    val recebeNotificacoes = Session.usuarioLogado?.papel in Tela.PAPEIS_QUE_ATENDEM
    LaunchedEffect(recebeNotificacoes) {
        while (recebeNotificacoes) {
            pedidosVmNotif.carregar()
            delay(7000)
        }
    }

    // DETECÇÃO PEDIDOS
    LaunchedEffect(pedidos.size) {
        if (pedidos.isEmpty()) return@LaunchedEffect
        val atuaisIds = pedidos.map { it.id }.toSet()
        val novosIds = atuaisIds - knownIds

        if (knownIds.isNotEmpty() && novosIds.isNotEmpty()) {
            val novosPedidos = pedidos.filter { it.id in novosIds }.sortedByDescending { it.id }

            notificacoes = (novosPedidos.map { p ->
                NotificacaoPedido(id = p.id, codigo = "Novo Pedido #${p.codigo}", horario = p.horario)
            } + notificacoes).distinctBy { it.id }

            mensagemToast = "Você tem um novo pedido"
            corToast = Color(0xFF790001)
            mostrarToast = true
        }
        knownIds = atuaisIds
    }


    // Timer Toast
    LaunchedEffect(mostrarToast) {
        if (mostrarToast) {
            delay(3000)
            mostrarToast = false
        }
    }

    ModalNavigationDrawer(
        drawerState = estadoMenuGaveta,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                MenuLateral(
                    telaAtual = telaAtual,
                    onTelaSelecionada = { novaTela ->
                        onTelaSelecionada(novaTela)
                        escopo.launch { estadoMenuGaveta.close() }
                    },
                    onLogout = onLogout
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(telaAtual.titulo) },
                    navigationIcon = {
                        IconButton(onClick = { escopo.launch { estadoMenuGaveta.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (notificacoes.isNotEmpty()) painelAberto = !painelAberto
                        }) {
                            BadgedBox(
                                badge = {
                                    if (notificacoes.isNotEmpty()) {
                                        Badge(containerColor = Color(0xFF790001), contentColor = Color.White) {
                                            Text(notificacoes.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notificações",
                                    tint = if (notificacoes.isNotEmpty()) Color(0xFF790001) else Color.Gray
                                )
                            }
                        }
                    }
                )
            }
        ) { valoresDePreenchimento ->
            Box(modifier = Modifier.padding(valoresDePreenchimento)) {
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

                if (mostrarToast) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
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
                                if (corToast != Color(0xFF790001)) {
                                    Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
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

                if (painelAberto && notificacoes.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .width(300.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF8F6),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFF790001).copy(alpha = 0.1f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFE9E6))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notificações",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4E342E)
                                )
                                TextButton(
                                    onClick = {
                                        notificacoes = emptyList()
                                        painelAberto = false
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Limpar", fontSize = 12.sp, color = Color(0xFF790001))
                                }
                            }

                            HorizontalDivider(color = Color(0xFF790001).copy(alpha = 0.1f))

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                notificacoes.forEach { notif ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pedidoFromNotificationId = notif.id
                                                onTelaSelecionada(Tela.VENDAS)
                                                notificacoes = notificacoes.filterNot { it.id == notif.id }
                                                painelAberto = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                                text = notif.horario,
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
                        }
                    }
                }
            }
        }
    }
}