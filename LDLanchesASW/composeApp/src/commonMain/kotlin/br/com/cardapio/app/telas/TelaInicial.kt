package br.com.cardapio.app.telas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ldlanchesasw.composeapp.generated.resources.Res
import ldlanchesasw.composeapp.generated.resources.logo
import br.com.cardapio.app.Tela
import br.com.cardapio.app.WindowSize
import br.com.cardapio.app.data.Pedido
import br.com.cardapio.app.data.Session
import br.com.cardapio.app.getPlatform
import br.com.cardapio.app.rememberWindowSize
import br.com.cardapio.app.ui.componentesUi.GraficoFaturamentoAndroidUI
import br.com.cardapio.app.ui.componentesUi.GraficoFaturamentoDesktopUI
import br.com.cardapio.app.ui.componentesUi.GraficoVendasAndroidUI
import br.com.cardapio.app.ui.componentesUi.GraficoVendasDesktopUI
import br.com.cardapio.app.ui.componentesUi.UltimosPedidosAndroidUI
import br.com.cardapio.app.ui.componentesUi.UltimosPedidosDesktopUI
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun MenuLateral(
    modificador: Modifier = Modifier,
    telaAtual: Tela,
    onTelaSelecionada: (Tela) -> Unit,
    onLogout: () -> Unit
) {
    val isMobile = getPlatform().name.contains("Android")

    Column(
        modifier = modificador
            .fillMaxSize()
            .then(if (isMobile) Modifier.statusBarsPadding() else Modifier)
            .background(Color(0xFFFFB4A9))
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Cada papel vê só as telas que pode usar (ver Tela.visivelPara)
        val usuario = Session.usuarioLogado
        val itens = listOf(
            Tela.DASHBOARD to Icons.Default.Dashboard,
            Tela.PRODUTOS to Icons.Default.Inventory,
            Tela.CATEGORIAS to Icons.Default.Category,
            Tela.FATURAMENTO to Icons.Default.AttachMoney,
            Tela.VENDAS to Icons.Default.ShoppingCart,
            Tela.DADOS_LOJA to Icons.Default.Storefront,
            Tela.APARENCIA to Icons.Default.Palette,
        )
        itens.filter { (tela, _) -> usuario != null && tela.visivelPara(usuario) }.forEach { (tela, icone) ->
            ItemMenu(
                icone = icone,
                texto = tela.titulo,
                estaSelecionado = telaAtual == tela,
                onClick = { onTelaSelecionada(tela) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        ItemMenu(
            icone = Icons.Default.Settings,
            texto = Tela.CONFIGURACOES.titulo,
            estaSelecionado = telaAtual == Tela.CONFIGURACOES,
            onClick = { onTelaSelecionada(Tela.CONFIGURACOES) }
        )

        ItemMenu(
            icone = Icons.AutoMirrored.Filled.Logout,
            texto = "Logout",
            onClick = onLogout
        )


    }
}

@Composable
fun ItemMenu(icone: ImageVector, texto: String, estaSelecionado: Boolean = false, onClick: () -> Unit) {
    val corDeFundo = if (estaSelecionado) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color.Transparent
    val corIndicador = if (estaSelecionado) Color(0xFFD32F2F) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(corDeFundo)
            .padding(vertical = 4.dp)
            .drawBehind {
                if (estaSelecionado) {
                    val larguraIndicador = 4.dp.toPx()
                    drawRoundRect(
                        color = corIndicador,
                        size = Size(width = larguraIndicador, height = size.height),
                        cornerRadius = CornerRadius(x = 10.dp.toPx(), y = 10.dp.toPx())
                    )
                }
            }
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Icon(imageVector = icone, contentDescription = texto, tint = Color.Black)
        Spacer(modifier = Modifier.width(16.dp))
        Text(texto, color = Color.Black, fontWeight = if (estaSelecionado) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun TelaInicial() {
    val dashboardVm = remember { DashboardViewModel() }
    val dados = dashboardVm.dados

    val faturamento = dados?.faturamentoDiario ?: listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val vendas = dados?.vendasDiarias?.map { it.toDouble() } ?: listOf(0.0)
    val dias = dados?.diasDaSemana ?: listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

    val totalFat = dados?.totalFaturamento ?: 0.0
    val totalVendas = dados?.totalVendas ?: 0

    val pedidosVm = remember { PedidosViewModel() }

    LaunchedEffect(Unit) {
        pedidosVm.carregar()
    }

    val pedidos = pedidosVm.pedidos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F6))
            .padding(18.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 50.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CartaoInfo(
                titulo = "Faturamento (7 dias)",
                valor = totalFat.toReal(),
                modificador = Modifier.weight(1f),
                conteudoGrafico = {
                    GraficoFaturamento(
                        modifier = Modifier.fillMaxSize(),
                        exibirEixoY = false,
                        dados = faturamento,
                        labels = dias
                    )
                }
            )
            CartaoInfo(
                titulo = "Vendas (7 dias)",
                valor = "${totalVendas}",
                modificador = Modifier.weight(1f),
                conteudoGrafico = {
                    GraficoVendas(
                        modifier = Modifier.fillMaxSize(),
                        exibirEixoY = false,
                        dados = vendas,
                        labels = dias
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        UltimosPedidos(
            pedidos = pedidos,                // <<< AQUI agora vem do backend
            modificador = Modifier.weight(1f)
        )
    }
}


@Composable
fun CartaoInfo(
    titulo: String,
    valor: String,
    modificador: Modifier = Modifier,
    conteudoGrafico: @Composable () -> Unit = { GraficoPlaceholder() }
) {
    Card(
        modifier = modificador.height(250.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = titulo, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f))
            {
                conteudoGrafico()
            }
        }
    }
}

@Composable
fun GraficoFaturamento(
    modifier: Modifier = Modifier,
    exibirEixoY: Boolean = true,
    dados: List<Double> = emptyList(), // <--- Novo parâmetro
    labels: List<String> = emptyList() // <--- Novo parâmetro
) {
    val windowSize = rememberWindowSize()

    if (windowSize == WindowSize.COMPACT) {
        GraficoFaturamentoAndroidUI(modifier, exibirEixoY, dados, labels)
    } else {
        GraficoFaturamentoDesktopUI(modifier, exibirEixoY, dados, labels)
    }
}

@Composable
fun GraficoVendas(
    modifier: Modifier = Modifier,
    exibirEixoY: Boolean = true,
    dados: List<Double> = emptyList(), // <--- Novo parâmetro
    labels: List<String> = emptyList() // <--- Novo parâmetro
) {
    val windowSize = rememberWindowSize()

    if (windowSize == WindowSize.COMPACT) {
        GraficoVendasAndroidUI(modifier, exibirEixoY, dados, labels)
    } else {
        GraficoVendasDesktopUI(modifier, exibirEixoY, dados, labels)
    }
}

@Composable
fun GraficoPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text("Gráfico em breve", color = Color.Gray)
    }
}

@Composable
fun UltimosPedidos(pedidos: List<Pedido>, modificador: Modifier = Modifier, cabecalho: @Composable () -> Unit = {}) {
    val windowSize = rememberWindowSize()

    if (windowSize == WindowSize.COMPACT) {
        UltimosPedidosAndroidUI(pedidos = pedidos, modificador = modificador, cabecalho = cabecalho)
    } else {
        UltimosPedidosDesktopUI(pedidos = pedidos, modificador = modificador, cabecalho = cabecalho)
    }
}