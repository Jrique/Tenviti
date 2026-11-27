package br.com.tenviti.app.telas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.EstadoLojaViewModel
import br.com.tenviti.app.Tela
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.data.UsuarioSessao
import br.com.tenviti.app.data.dataDeHojePorExtenso
import br.com.tenviti.app.data.ehHoje
import br.com.tenviti.app.data.formatarAgendamento
import br.com.tenviti.app.data.saudacao
import br.com.tenviti.app.telas.componentes.tela.telaFaturamento.PeriodoFiltroGlobal
import br.com.tenviti.app.ui.*
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.StatusPedido
import br.com.tenviti.shared.TipoEntrega
import br.com.tenviti.shared.emReais
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Primeira tela do painel: o que está acontecendo hoje e o que pede ação agora. */
@Composable
fun TelaHoje(
    usuario: UsuarioSessao,
    pedidosVm: PedidosViewModel,
    loja: EstadoLojaViewModel,
    onAbrirPedido: (Long) -> Unit,
    onIrPara: (Tela) -> Unit,
) {
    val c = TenvitiTema.cores
    val financeiro = usuario.vePainelFinanceiro
    val resumoVm = viewModel { DashboardViewModel() }
    LaunchedEffect(Unit) { if (financeiro) resumoVm.carregar(PeriodoFiltroGlobal.HOJE) }

    val pedidos = pedidosVm.pedidos
    val deHoje = pedidos.filter { ehHoje(it.criadoEm) && it.status != StatusPedido.CANCELADO }
    val ativos = pedidos.filter { !it.status.ehFinal }
        .sortedWith(compareBy<PedidoDto> { ordemDaFila(it.status) }.thenByDescending { it.id })
    val agora = Clock.System.now()
    val encomendas = pedidos.filter { p ->
        !p.status.ehFinal && p.agendadoPara?.let { iso -> ehHoje(iso) || (runCatching { Instant.parse(iso) }.getOrNull()?.let { t -> t > agora } == true) } == true
    }.sortedBy { it.agendadoPara }
    val atrasadas = encomendas.count { p -> p.agendadoPara?.let { runCatching { Instant.parse(it) }.getOrNull()?.let { t -> t < agora } } == true }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largo = maxWidth >= 900.dp
        val larguraTela = maxWidth
        val margem = if (maxWidth >= 600.dp) 32.dp else 16.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = margem).padding(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CabecalhoTela(
                titulo = "${saudacao()}, ${usuario.nome.substringBefore(' ')}",
                sobretitulo = dataDeHojePorExtenso(),
            ) {
                if (financeiro) {
                    val opcoes = listOf(PeriodoFiltroGlobal.HOJE, PeriodoFiltroGlobal.SEMANAL, PeriodoFiltroGlobal.MENSAL)
                    SeletorSegmentado(
                        listOf("Hoje", "7 dias", "30 dias"),
                        opcoes.indexOf(resumoVm.filtroAtual).coerceAtLeast(0),
                        onSelecionar = { resumoVm.carregar(opcoes[it]) },
                    )
                }
            }

            // ---------- indicadores
            val largura = if (largo) 220.dp else (larguraTela - margem * 2 - 14.dp) / 2 - 1.dp
            val compacto = !largo
            FlowRowIndicadores(largura) {
                if (financeiro) {
                    val dados = resumoVm.dados
                    val periodo = when (resumoVm.filtroAtual) {
                        PeriodoFiltroGlobal.HOJE -> "hoje"; PeriodoFiltroGlobal.SEMANAL -> "em 7 dias"; else -> "em 30 dias"
                    }
                    CartaoIndicador(
                        rotulo = "Vendido $periodo",
                        valor = dados?.totalFaturamento?.toReal() ?: "…",
                        detalhe = dados?.let { "${it.totalVendas} ${if (it.totalVendas == 1) "pedido" else "pedidos"} no período" },
                        destaque = true,
                        modifier = Modifier.width(largura),
                        compacto = compacto,
                    ) {
                        val serie = dados?.faturamentoDiario.orEmpty()
                        if (serie.size >= 2) MiniGrafico(serie, c.menuVerde)
                    }
                }
                CartaoIndicador(
                    rotulo = "Pedidos hoje",
                    valor = deHoje.size.toString(),
                    detalhe = "${deHoje.count { it.status in setOf(StatusPedido.NOVO, StatusPedido.VISTO) }} novos · " +
                        "${deHoje.count { it.status == StatusPedido.EM_PREPARO }} em preparo · " +
                        "${deHoje.count { it.status == StatusPedido.FINALIZADO }} entregues",
                    modifier = Modifier.width(largura),
                        compacto = compacto,
                ) { BarraDeStatus(deHoje) }
                if (financeiro) {
                    val dados = resumoVm.dados
                    CartaoIndicador(
                        rotulo = "Ticket médio",
                        valor = if (dados != null && dados.totalVendas > 0) (dados.totalFaturamento / dados.totalVendas).toReal() else "—",
                        modifier = Modifier.width(largura),
                        compacto = compacto,
                    ) {
                        maisPedido(pedidos)?.let { Text("Mais pedido: $it", style = MaterialTheme.typography.labelSmall, color = c.sobreBaseSuave, modifier = Modifier.background(c.baseSuave, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) }
                    }
                }
                CartaoIndicador(
                    rotulo = "Encomendas marcadas",
                    valor = encomendas.size.toString(),
                    detalhe = encomendas.firstOrNull()?.agendadoPara?.let { "Próxima: ${formatarAgendamento(it)}" } ?: "Nenhuma encomenda à frente",
                    modifier = Modifier.width(largura),
                        compacto = compacto,
                ) {
                    if (atrasadas > 0) Etiqueta("$atrasadas com horário passado", c.aviso)
                }
            }

            // ---------- fila e coluna lateral
            if (largo) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                    FilaAgora(ativos, pedidosVm, onAbrirPedido, onIrPara, Modifier.weight(3f))
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ProximasEncomendas(encomendas, onAbrirPedido)
                        LinkDaVitrine(loja)
                    }
                }
            } else {
                FilaAgora(ativos, pedidosVm, onAbrirPedido, onIrPara, Modifier.fillMaxWidth())
                ProximasEncomendas(encomendas, onAbrirPedido)
                LinkDaVitrine(loja)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowIndicadores(largura: androidx.compose.ui.unit.Dp, conteudo: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { conteudo() }
}

private fun ordemDaFila(s: StatusPedido) = when (s) {
    StatusPedido.NOVO -> 0; StatusPedido.VISTO -> 1; StatusPedido.EM_PREPARO -> 2; StatusPedido.SAIU_PARA_ENTREGA -> 3; else -> 4
}

private fun maisPedido(pedidos: List<PedidoDto>): String? =
    pedidos.flatMap { p -> p.itens.map { it.nome to it.quantidade } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        .maxByOrNull { it.value }?.key

@Composable
private fun MiniGrafico(valores: List<Double>, cor: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val max = valores.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val passo = size.width / (valores.size - 1)
        val path = Path()
        valores.forEachIndexed { i, v ->
            val p = Offset(i * passo, size.height - (v / max * (size.height - 4)).toFloat() - 2)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, cor, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun BarraDeStatus(pedidos: List<PedidoDto>) {
    val c = TenvitiTema.cores
    val novos = pedidos.count { it.status in setOf(StatusPedido.NOVO, StatusPedido.VISTO) }
    val preparo = pedidos.count { it.status == StatusPedido.EM_PREPARO || it.status == StatusPedido.SAIU_PARA_ENTREGA }
    val prontos = pedidos.count { it.status == StatusPedido.FINALIZADO }
    Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (novos + preparo + prontos == 0) {
            Box(Modifier.fillMaxSize().background(c.bordaSuave, RoundedCornerShape(99.dp)))
        } else {
            if (novos > 0) Box(Modifier.weight(novos.toFloat()).fillMaxHeight().background(c.info.cheia, RoundedCornerShape(99.dp)))
            if (preparo > 0) Box(Modifier.weight(preparo.toFloat()).fillMaxHeight().background(c.aviso.cheia, RoundedCornerShape(99.dp)))
            if (prontos > 0) Box(Modifier.weight(prontos.toFloat()).fillMaxHeight().background(c.sucesso.cheia, RoundedCornerShape(99.dp)))
        }
    }
}

@Composable
private fun FilaAgora(
    ativos: List<PedidoDto>,
    pedidosVm: PedidosViewModel,
    onAbrirPedido: (Long) -> Unit,
    onIrPara: (Tela) -> Unit,
    modifier: Modifier,
) {
    val c = TenvitiTema.cores
    Cartao(modifier = modifier, espacamento = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Agora", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { onIrPara(Tela.PEDIDOS) }) { Text("Abrir quadro de pedidos") }
        }
        if (ativos.isEmpty()) {
            Text(
                if (pedidosVm.jaCarregou) "Nenhum pedido em andamento. Quando chegar um, ele aparece aqui." else "Carregando pedidos…",
                style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        ativos.take(6).forEach { p -> LinhaDePedido(p, pedidosVm, onAbrirPedido) }
        if (ativos.size > 6) Text("e mais ${ativos.size - 6} no quadro de pedidos", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinhaDePedido(p: PedidoDto, pedidosVm: PedidosViewModel, onAbrir: (Long) -> Unit) {
    val c = TenvitiTema.cores
    val cor = corDoStatus(p.status)
    FlowRow(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, c.bordaSuave, RoundedCornerShape(14.dp)).clickable { onAbrir(p.id) }.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(cor.fundo, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(siglaDeEntrega(p), color = cor.texto, fontFamily = FonteTitulo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f).widthIn(min = 160.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.codigo.take(4)}", fontFamily = FonteTitulo, style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                Spacer(Modifier.width(8.dp))
                Text(p.tituloCurto(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(detalheDoPedido(p), style = MaterialTheme.typography.bodySmall, color = c.textoSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(p.totalEmReais, fontFamily = FonteTitulo, style = MaterialTheme.typography.titleMedium)
        Etiqueta(p.status.rotulo, cor)
        p.status.proximo()?.let { proximo ->
            OutlinedButton(
                onClick = { pedidosVm.alterarStatus(p.id, proximo) },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, c.base),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.base),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp),
            ) { Text(p.status.acao().orEmpty(), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** "EN" entrega, "RE" retirada, "—" pedido antigo. */
fun siglaDeEntrega(p: PedidoDto) = when (p.tipoEntrega) { TipoEntrega.ENTREGA -> "EN"; TipoEntrega.RETIRADA -> "RE"; null -> "PD" }

/** "Encomenda 28/11 12:00 · Entrega · Pix". */
fun detalheDoPedido(p: PedidoDto): String = listOfNotNull(
    p.agendadoPara?.let { "Encomenda ${formatarAgendamento(it)}" },
    p.tipoEntrega?.rotulo,
    p.formaPagamento?.rotulo,
    p.cliente?.nome,
).joinToString(" · ").ifEmpty { "Pedido pelo cardápio" }

@Composable
private fun ProximasEncomendas(encomendas: List<PedidoDto>, onAbrir: (Long) -> Unit) {
    val c = TenvitiTema.cores
    Cartao(Modifier.fillMaxWidth()) {
        Text("Próximas encomendas", style = MaterialTheme.typography.titleMedium)
        if (encomendas.isEmpty()) {
            Text("Nenhuma encomenda marcada.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        }
        val agora = Clock.System.now()
        encomendas.take(5).forEach { p ->
            val passou = p.agendadoPara?.let { runCatching { Instant.parse(it) }.getOrNull()?.let { t -> t < agora } } == true
            Row(Modifier.fillMaxWidth().clickable { onAbrir(p.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatarAgendamento(p.agendadoPara.orEmpty()), fontFamily = FonteTitulo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp), color = if (passou) c.aviso.texto else c.texto)
                Box(Modifier.width(3.dp).height(34.dp).background(if (passou) c.aviso.cheia else c.base, RoundedCornerShape(99.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.tituloCurto(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(p.cliente?.nome, p.tipoEntrega?.rotulo).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun LinkDaVitrine(loja: EstadoLojaViewModel) {
    if (loja.slug.isEmpty()) return
    val c = TenvitiTema.cores
    val area = LocalClipboardManager.current
    val endereco = ApiProvider.painelApi.enderecoVitrine(loja.slug)
    var copiado by remember { mutableStateOf(false) }
    Cartao(Modifier.fillMaxWidth(), cor = c.acentoSuave, borda = null) {
        Text("Link da sua vitrine", style = MaterialTheme.typography.titleSmall, color = c.sobreAcentoSuave)
        Text(endereco, fontFamily = FonteTitulo, style = MaterialTheme.typography.bodySmall, color = c.sobreAcentoSuave, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BotaoDestaque(if (copiado) "Copiado" else "Copiar link", onClick = { area.setText(AnnotatedString(endereco)); copiado = true })
            Text("Cole na bio do Instagram e no WhatsApp.", style = MaterialTheme.typography.labelSmall, color = c.sobreAcentoSuave)
        }
    }
}
