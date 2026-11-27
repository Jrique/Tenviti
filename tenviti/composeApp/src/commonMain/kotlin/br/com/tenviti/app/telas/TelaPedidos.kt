package br.com.tenviti.app.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.data.ehHoje
import br.com.tenviti.app.data.formatarAgendamento
import br.com.tenviti.app.data.formatarHorarioPedido
import br.com.tenviti.app.data.tempoDesde
import br.com.tenviti.app.ui.*
import br.com.tenviti.shared.FormaPagamento
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.StatusPedido
import br.com.tenviti.shared.TipoEntrega
import br.com.tenviti.shared.emReais

private enum class Filtro(val rotulo: String) { TODOS("Todos"), ENTREGA("Entrega"), RETIRADA("Retirada"), ENCOMENDAS("Encomendas") }

private data class Coluna(val nome: String, val cor: @Composable () -> CorDeStatus, val filtro: (PedidoDto) -> Boolean)

private val COLUNAS = listOf(
    Coluna("Novos", { TenvitiTema.cores.info }) { it.status == StatusPedido.NOVO || it.status == StatusPedido.VISTO },
    Coluna("Em preparo", { TenvitiTema.cores.aviso }) { it.status == StatusPedido.EM_PREPARO },
    Coluna("A caminho", { corDoStatus(StatusPedido.SAIU_PARA_ENTREGA) }) { it.status == StatusPedido.SAIU_PARA_ENTREGA },
    Coluna("Concluídos hoje", { TenvitiTema.cores.sucesso }) { it.status == StatusPedido.FINALIZADO && ehHoje(it.criadoEm) },
)

/** Quadro de pedidos: colunas por status no computador, abas no celular, e o pedido aberto ao lado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPedidos(pedidosVm: PedidosViewModel, pedidoParaAbrir: Long?, onPedidoAberto: () -> Unit) {
    val c = TenvitiTema.cores
    var filtro by remember { mutableStateOf(Filtro.TODOS) }
    var selecionadoId by remember { mutableStateOf<Long?>(null) }
    var abaCelular by remember { mutableStateOf(0) }
    LaunchedEffect(pedidoParaAbrir) {
        if (pedidoParaAbrir != null) {
            selecionadoId = pedidoParaAbrir
            onPedidoAberto()
        }
    }

    val filtrados = pedidosVm.pedidos.filter {
        when (filtro) {
            Filtro.TODOS -> true
            Filtro.ENTREGA -> it.tipoEntrega == TipoEntrega.ENTREGA
            Filtro.RETIRADA -> it.tipoEntrega == TipoEntrega.RETIRADA
            Filtro.ENCOMENDAS -> it.agendadoPara != null
        }
    }
    val selecionado = pedidosVm.pedidos.firstOrNull { it.id == selecionadoId }
    val canceladosHoje = pedidosVm.pedidos.count { it.status == StatusPedido.CANCELADO && ehHoje(it.criadoEm) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val largo = maxWidth >= 1000.dp
        val margem = if (maxWidth >= 600.dp) 32.dp else 16.dp
        Column(Modifier.fillMaxSize().padding(horizontal = margem).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CabecalhoTela(
                titulo = "Pedidos",
                subtitulo = "Avance cada pedido pelo botão do cartão. O cliente acompanha o status pelo código." +
                    if (canceladosHoje > 0) " $canceladosHoje cancelado(s) hoje." else "",
            ) {
                SeletorSegmentado(Filtro.entries.map { it.rotulo }, filtro.ordinal, { filtro = Filtro.entries[it] })
                IconButton(onClick = { pedidosVm.carregar() }) { Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar pedidos", tint = c.texto) }
            }
            pedidosVm.erro?.let { Text(it, color = c.erro.texto, style = MaterialTheme.typography.bodySmall) }

            if (largo) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.weight(3f).fillMaxHeight().verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        COLUNAS.forEach { col ->
                            val lista = filtrados.filter(col.filtro)
                            ColunaDoQuadro(col.nome, col.cor(), lista, selecionadoId, Modifier.weight(1f)) { selecionadoId = it.id }
                        }
                    }
                    Box(Modifier.weight(1.35f).fillMaxHeight().padding(bottom = 24.dp)) {
                        if (selecionado != null) {
                            DetalheDoPedido(selecionado, pedidosVm, Modifier.fillMaxSize())
                        } else {
                            Cartao(Modifier.fillMaxWidth()) {
                                Text("Escolha um pedido", style = MaterialTheme.typography.titleMedium)
                                Text("Clique num cartão para ver itens, cliente, endereço e pagamento.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                        }
                    }
                }
            } else {
                ScrollableTabRow(selectedTabIndex = abaCelular, edgePadding = 0.dp, containerColor = c.fundo, divider = {}) {
                    COLUNAS.forEachIndexed { i, col ->
                        Tab(selected = i == abaCelular, onClick = { abaCelular = i }, text = {
                            Text("${col.nome} ${filtrados.count(col.filtro)}", style = MaterialTheme.typography.labelMedium)
                        })
                    }
                }
                val lista = filtrados.filter(COLUNAS[abaCelular].filtro)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (lista.isEmpty()) Text("Nada por aqui.", color = c.textoSuave, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    lista.forEach { p -> CartaoDoPedido(p, false) { selecionadoId = p.id } }
                }
                if (selecionado != null) {
                    ModalBottomSheet(onDismissRequest = { selecionadoId = null }, containerColor = c.fundo) {
                        DetalheDoPedido(selecionado, pedidosVm, Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColunaDoQuadro(nome: String, cor: CorDeStatus, pedidos: List<PedidoDto>, selecionadoId: Long?, modifier: Modifier, onClick: (PedidoDto) -> Unit) {
    val c = TenvitiTema.cores
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (c.escuro) c.superficieAlt else c.superficieAlt).padding(10.dp).heightIn(min = 520.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Ponto(cor.cheia)
            Spacer(Modifier.width(8.dp))
            Text(nome, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(pedidos.size.toString(), fontFamily = FonteTitulo, style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
        }
        pedidos.forEach { p -> CartaoDoPedido(p, p.id == selecionadoId) { onClick(p) } }
    }
}

@Composable
private fun CartaoDoPedido(p: PedidoDto, selecionado: Boolean, onClick: () -> Unit) {
    val c = TenvitiTema.cores
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = c.superficie,
        border = BorderStroke(if (selecionado) 2.dp else 1.dp, if (selecionado) c.base else c.borda),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.codigo.take(4)}", fontFamily = FonteTitulo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(tempoDesde(p.criadoEm), style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
            }
            Text(p.tituloCurto(), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            p.cliente?.let { Text(it.nome, style = MaterialTheme.typography.labelSmall, color = c.textoSuave) }
            Text(p.totalEmReais, fontFamily = FonteTitulo, style = MaterialTheme.typography.titleMedium, softWrap = false)
            Etiqueta(
                p.agendadoPara?.let { "Encomenda ${formatarAgendamento(it)}" } ?: (p.tipoEntrega?.rotulo ?: "Pedido"),
                if (p.agendadoPara != null) CorDeStatus(c.baseSuave, c.sobreBaseSuave, c.base) else c.neutro,
            )
        }
    }
}

@Composable
private fun DetalheDoPedido(p: PedidoDto, pedidosVm: PedidosViewModel, modifier: Modifier) {
    val c = TenvitiTema.cores
    val uri = LocalUriHandler.current
    var confirmarCancelamento by remember(p.id) { mutableStateOf(false) }

    Cartao(modifier.verticalScroll(rememberScrollState()), espacamento = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("PEDIDO #${p.codigo}", fontFamily = FonteTitulo, style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                Text(p.totalEmReais, style = MaterialTheme.typography.headlineMedium)
                Text("Feito ${formatarHorarioPedido(p.criadoEm).replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
            }
            Etiqueta(p.status.rotulo, corDoStatus(p.status))
        }
        EtapasDoPedido(p.status)

        HorizontalDivider(color = c.bordaSuave)
        if (p.itens.isEmpty()) {
            // pedido antigo, só com texto
            Text(p.descricao, style = MaterialTheme.typography.bodySmall)
        } else {
            p.itens.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row {
                        Text("${item.quantidade}x ${item.nome}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(item.totalCentavos.emReais(), fontFamily = FonteTitulo, style = MaterialTheme.typography.titleSmall)
                    }
                    item.opcoes.groupBy { it.grupo }.forEach { (grupo, opcoes) ->
                        Text("$grupo: " + opcoes.joinToString(", ") { "${it.quantidade}x ${it.nome}" }, style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    }
                    item.observacao?.let { Text("Obs.: $it", style = MaterialTheme.typography.bodySmall, color = c.aviso.texto) }
                }
            }
            if (p.taxaEntregaCentavos > 0) {
                Row {
                    Text("Taxa de entrega", style = MaterialTheme.typography.bodySmall, color = c.textoSuave, modifier = Modifier.weight(1f))
                    Text(p.taxaEntregaCentavos.emReais(), style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                }
            }

            Column(
                Modifier.fillMaxWidth().background(c.superficieAlt, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Info("CLIENTE", p.cliente?.nome ?: "—", p.cliente?.telefone?.let(::formatarTelefone), Modifier.weight(1f))
                    Info(
                        if (p.agendadoPara != null) "ENCOMENDA" else "QUANDO",
                        p.agendadoPara?.let(::formatarAgendamento) ?: "O quanto antes",
                        p.tipoEntrega?.rotulo, Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val e = p.endereco
                    Info(
                        if (p.tipoEntrega == TipoEntrega.ENTREGA) "ENDEREÇO" else "RETIRADA",
                        e?.let { "${it.logradouro}, ${it.numero}" } ?: "No balcão da loja",
                        e?.let { listOf(it.bairro, "${it.cidade}/${it.uf}").filter { s -> s.isNotBlank() && s != "/" }.joinToString(" · ") + (it.complemento?.let { cpl -> " · $cpl" } ?: "") },
                        Modifier.weight(1f),
                    )
                    Info(
                        "PAGAMENTO", p.formaPagamento?.rotulo ?: "—",
                        p.trocoParaCentavos?.let { "Troco para ${it.emReais()}" } ?: if (p.formaPagamento == FormaPagamento.DINHEIRO) "Sem troco" else null,
                        Modifier.weight(1f),
                    )
                }
            }
            p.observacao?.let {
                Text("Obs.: $it", style = MaterialTheme.typography.bodySmall, color = c.aviso.texto,
                    modifier = Modifier.fillMaxWidth().background(c.aviso.fundo, RoundedCornerShape(12.dp)).padding(10.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            p.status.proximo()?.let { proximo ->
                BotaoPrincipal(p.status.acao().orEmpty() + if (proximo == StatusPedido.EM_PREPARO) " pedido" else "", { pedidosVm.alterarStatus(p.id, proximo) }, Modifier.weight(1f))
            }
            p.cliente?.telefone?.let { tel ->
                OutlinedIconButton(
                    onClick = { uri.openUri("https://wa.me/55$tel") },
                    border = BorderStroke(1.dp, c.borda), modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp),
                ) { Icon(Icons.Outlined.Chat, contentDescription = "Falar com o cliente no WhatsApp", tint = c.texto) }
            }
        }
        if (!p.status.ehFinal) {
            TextButton(onClick = { confirmarCancelamento = true }, colors = ButtonDefaults.textButtonColors(contentColor = c.erro.texto)) { Text("Cancelar pedido") }
        }
    }

    if (confirmarCancelamento) {
        AlertDialog(
            onDismissRequest = { confirmarCancelamento = false },
            title = { Text("Cancelar o pedido #${p.codigo}?") },
            text = { Text("O cliente vê o pedido como cancelado. Isso não pode ser desfeito.") },
            confirmButton = {
                TextButton(onClick = { pedidosVm.alterarStatus(p.id, StatusPedido.CANCELADO); confirmarCancelamento = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.erro.texto)) { Text("Cancelar pedido") }
            },
            dismissButton = { TextButton(onClick = { confirmarCancelamento = false }) { Text("Voltar") } },
        )
    }
}

@Composable
private fun Info(rotulo: String, valor: String, detalhe: String?, modifier: Modifier) {
    val c = TenvitiTema.cores
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
        Text(valor, style = MaterialTheme.typography.labelLarge)
        detalhe?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = c.textoSuave) }
    }
}

@Composable
private fun EtapasDoPedido(status: StatusPedido) {
    val c = TenvitiTema.cores
    val etapas = listOf("Recebido", "Preparo", "A caminho", "Entregue")
    val atual = when (status) {
        StatusPedido.NOVO, StatusPedido.VISTO -> 0
        StatusPedido.EM_PREPARO -> 1
        StatusPedido.SAIU_PARA_ENTREGA -> 2
        StatusPedido.FINALIZADO -> 3
        StatusPedido.CANCELADO -> -1
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        etapas.forEachIndexed { i, nome ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(
                    when { atual < 0 -> c.erro.fundo; i <= atual -> c.base; else -> c.borda }, RoundedCornerShape(99.dp)))
                Text(nome, style = MaterialTheme.typography.labelSmall, color = if (i <= atual) c.texto else c.textoSuave)
            }
        }
    }
}

/** "11987654321" → "(11) 98765-4321". */
fun formatarTelefone(t: String): String {
    val d = t.filter { it.isDigit() }
    return when (d.length) {
        11 -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.substring(6)}"
        else -> t
    }
}
