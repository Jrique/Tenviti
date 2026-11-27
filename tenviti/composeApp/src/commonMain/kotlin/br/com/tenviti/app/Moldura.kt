package br.com.tenviti.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.data.Session
import br.com.tenviti.app.data.UsuarioSessao
import br.com.tenviti.app.data.horaLocal
import br.com.tenviti.app.telas.*
import br.com.tenviti.app.ui.*
import br.com.tenviti.shared.StatusPedido
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Nome, endereço e situação da loja do usuário (mostrados no menu e na barra do topo). */
class EstadoLojaViewModel : ViewModel() {
    private val api = ApiProvider.painelApi

    var nome by mutableStateOf("")
        private set
    var slug by mutableStateOf("")
        private set
    var aberta by mutableStateOf<Boolean?>(null)
        private set
    var pausada by mutableStateOf(false)
        private set
    var alterandoPausa by mutableStateOf(false)
        private set
    var erro by mutableStateOf<String?>(null)

    fun carregar() {
        viewModelScope.launch {
            try {
                val loja = api.minhaLoja()
                nome = loja.nome
                slug = loja.slug
                val vitrine = api.vitrine(loja.slug)
                aberta = vitrine.aberta
                pausada = vitrine.pausada
            } catch (e: Throwable) {
                erro = e.message
            }
        }
    }

    /** Liga/desliga o recebimento de pedidos (pausa da loja). */
    fun alternarPausa() {
        viewModelScope.launch {
            try {
                alterandoPausa = true
                val info = api.informacoesLoja()
                val salvo = api.salvarInformacoesLoja(info.copy(funcionamento = info.funcionamento.copy(pausada = !pausada)))
                pausada = salvo.funcionamento.pausada
                if (slug.isNotEmpty()) aberta = api.vitrine(slug).aberta
            } catch (e: Throwable) {
                erro = e.message
            } finally {
                alterandoPausa = false
            }
        }
    }

    val iniciais: String
        get() = nome.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

/** Pedido novo avisado na barra do topo. */
data class AvisoPedido(val id: Long, val codigo: String, val titulo: String, val hora: String)

@Composable
fun MolduraPainel(usuario: UsuarioSessao, onLogout: () -> Unit) {
    var telaAtual by remember(usuario.id) { mutableStateOf(Tela.inicialPara(usuario)) }
    var pedidoParaAbrir by remember { mutableStateOf<Long?>(null) }

    val pedidosVm = viewModel { PedidosViewModel() }
    val lojaVm = viewModel { EstadoLojaViewModel() }
    LaunchedEffect(Unit) { lojaVm.carregar() }

    // Pedidos atualizados a cada 10 s para avisar pedido novo
    val atende = usuario.papel in Tela.PAPEIS_QUE_ATENDEM
    var avisos by remember { mutableStateOf(listOf<AvisoPedido>()) }
    var idsConhecidos by remember { mutableStateOf<Set<Long>?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(atende) {
        while (atende) {
            pedidosVm.carregar(silencioso = pedidosVm.jaCarregou)
            delay(10_000)
        }
    }
    LaunchedEffect(pedidosVm.pedidos) {
        if (!pedidosVm.jaCarregou) return@LaunchedEffect
        val atuais = pedidosVm.pedidos.map { it.id }.toSet()
        val conhecidos = idsConhecidos
        if (conhecidos != null) {
            val novos = pedidosVm.pedidos.filter { it.id !in conhecidos && it.status == StatusPedido.NOVO }
            if (novos.isNotEmpty()) {
                avisos = (novos.map { AvisoPedido(it.id, it.codigo, it.tituloCurto(), horaLocal(it.criadoEm)) } + avisos).distinctBy { it.id }
                toast = if (novos.size == 1) "Pedido novo: ${novos.first().tituloCurto()}" else "${novos.size} pedidos novos"
            }
        }
        idsConhecidos = atuais
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(4000)
            toast = null
        }
    }

    val novosCount = pedidosVm.pedidos.count { it.status == StatusPedido.NOVO }
    val irPara: (Tela) -> Unit = { telaAtual = it }
    val abrirPedido: (Long) -> Unit = { id ->
        pedidoParaAbrir = id
        avisos = avisos.filterNot { it.id == id }
        telaAtual = Tela.PEDIDOS
    }

    val conteudo: @Composable () -> Unit = {
        when (telaAtual) {
            Tela.HOJE -> TelaHoje(usuario, pedidosVm, lojaVm, onAbrirPedido = abrirPedido, onIrPara = irPara)
            Tela.PEDIDOS -> TelaPedidos(pedidosVm, pedidoParaAbrir, onPedidoAberto = { pedidoParaAbrir = null })
            Tela.CATALOGO -> TelaCatalogo()
            Tela.FINANCEIRO -> TelaFaturamento()
            Tela.LOJA -> TelaDadosLoja()
            Tela.APARENCIA -> TelaAparencia()
            Tela.CONFIGURACOES -> TelaConfiguracoes()
        }
    }

    val c = TenvitiTema.cores
    Box(Modifier.fillMaxSize().background(c.fundo)) {
        if (rememberWindowSize() == WindowSize.COMPACT) {
            MolduraCelular(usuario, telaAtual, irPara, novosCount, avisos, abrirPedido, { avisos = emptyList() }, lojaVm, onLogout, conteudo)
        } else {
            Row(Modifier.fillMaxSize()) {
                MenuLateral(usuario, telaAtual, irPara, novosCount, lojaVm, onLogout)
                Column(Modifier.fillMaxSize()) {
                    BarraTopo(atende, avisos, abrirPedido, { avisos = emptyList() }, lojaVm)
                    Box(Modifier.fillMaxSize()) { conteudo() }
                }
            }
        }

        toast?.let { mensagem ->
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                shape = RoundedCornerShape(99.dp),
                color = c.info.cheia,
                shadowElevation = 6.dp,
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(mensagem, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- computador

@Composable
private fun MenuLateral(
    usuario: UsuarioSessao,
    telaAtual: Tela,
    onTela: (Tela) -> Unit,
    novos: Int,
    loja: EstadoLojaViewModel,
    onLogout: () -> Unit,
) {
    val c = TenvitiTema.cores
    Column(
        Modifier.width(248.dp).fillMaxHeight().background(c.menuFundo).padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LogoTenviti(tamanho = 22.sp(), sobreEscuro = true, modifier = Modifier.padding(start = 6.dp))
        CartaoDaLoja(loja)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Tela.entries.filter { it.visivelPara(usuario) }.forEach { tela ->
                ItemDoMenu(tela, ativo = tela == telaAtual, contador = if (tela == Tela.PEDIDOS) novos else 0) { onTela(tela) }
            }
        }
        BotaoModoEscuro()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            Box(Modifier.size(32.dp).background(c.menuVerde, CircleShape), contentAlignment = Alignment.Center) {
                Text(usuario.nome.take(1).uppercase(), color = Color(0xFF062420), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(usuario.nome, color = c.menuTextoAtivo, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(usuario.papel.rotulo, color = c.menuTextoSuave, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sair", tint = c.menuTexto)
            }
        }
    }
}

@Composable
private fun CartaoDaLoja(loja: EstadoLojaViewModel) {
    val c = TenvitiTema.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.menuCartao)
            .border(1.dp, c.menuBorda, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).background(c.menuItemAtivo, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(loja.iniciais, color = c.menuTextoAtivo, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(loja.nome.ifEmpty { "Sua loja" }, color = c.menuTextoAtivo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val (texto, cor) = when {
                loja.aberta == null -> "Carregando…" to c.menuTextoSuave
                loja.pausada -> "Pausada" to Color(0xFFF2CF7E)
                loja.aberta == true -> "Aberta agora" to Color(0xFF6FD3BF)
                else -> "Fechada agora" to c.menuTextoSuave
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ponto(cor, 7.dp)
                Spacer(Modifier.width(6.dp))
                Text(texto, color = cor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ItemDoMenu(tela: Tela, ativo: Boolean, contador: Int, onClick: () -> Unit) {
    val c = TenvitiTema.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (ativo) c.menuItemAtivo else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tela.icone, contentDescription = null, tint = if (ativo) c.menuTextoAtivo else c.menuTexto, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            tela.titulo, modifier = Modifier.weight(1f),
            color = if (ativo) c.menuTextoAtivo else c.menuTexto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (contador > 0) {
            Text(
                contador.toString(),
                modifier = Modifier.background(c.menuAcento, RoundedCornerShape(99.dp)).padding(horizontal = 7.dp, vertical = 1.dp),
                color = Color(0xFF2A0F08), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BotaoModoEscuro() {
    val c = TenvitiTema.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Switch) { PreferenciaTema.alternar(c.escuro) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (c.escuro) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, contentDescription = null, tint = c.menuTexto, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Modo escuro", color = c.menuTexto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = c.escuro, onCheckedChange = { PreferenciaTema.alternar(c.escuro) },
            colors = SwitchDefaults.colors(checkedTrackColor = c.menuVerde, checkedThumbColor = Color(0xFF062420), uncheckedTrackColor = c.menuCartao, uncheckedThumbColor = c.menuTexto, uncheckedBorderColor = c.menuBorda),
        )
    }
}

@Composable
private fun BarraTopo(
    atende: Boolean,
    avisos: List<AvisoPedido>,
    onAbrirPedido: (Long) -> Unit,
    onLimparAvisos: () -> Unit,
    loja: EstadoLojaViewModel,
) {
    val c = TenvitiTema.cores
    val uri = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (atende) {
            Surface(shape = RoundedCornerShape(12.dp), color = c.superficie, border = androidx.compose.foundation.BorderStroke(1.dp, c.borda)) {
                Row(Modifier.padding(start = 14.dp, end = 8.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (loja.pausada) "Pedidos pausados" else "Recebendo pedidos", style = MaterialTheme.typography.labelMedium, color = c.texto)
                    Spacer(Modifier.width(10.dp))
                    Switch(checked = !loja.pausada, enabled = !loja.alterandoPausa, onCheckedChange = { loja.alternarPausa() })
                }
            }
            SinoDeAvisos(avisos, onAbrirPedido, onLimparAvisos)
        }
        if (loja.slug.isNotEmpty()) {
            Button(
                onClick = { uri.openUri(ApiProvider.painelApi.enderecoVitrine(loja.slug)) },
                shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver minha vitrine", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SinoDeAvisos(avisos: List<AvisoPedido>, onAbrirPedido: (Long) -> Unit, onLimpar: () -> Unit, fundo: Boolean = true) {
    val c = TenvitiTema.cores
    var aberto by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { aberto = true },
            shape = RoundedCornerShape(12.dp),
            color = if (fundo) c.superficie else Color.Transparent,
            border = if (fundo) androidx.compose.foundation.BorderStroke(1.dp, c.borda) else null,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                BadgedBox(badge = { if (avisos.isNotEmpty()) Badge(containerColor = c.acento, contentColor = Color.White) { Text(avisos.size.toString()) } }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Avisos de pedidos novos", tint = c.texto)
                }
            }
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }, modifier = Modifier.width(320.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Pedidos novos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (avisos.isNotEmpty()) TextButton(onClick = { onLimpar(); aberto = false }) { Text("Limpar") }
            }
            if (avisos.isEmpty()) {
                Text("Nenhum pedido novo desde que você abriu o painel.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave, modifier = Modifier.padding(16.dp))
            }
            avisos.forEach { a ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("#${a.codigo} · ${a.titulo}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Chegou às ${a.hora}", style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
                        }
                    },
                    leadingIcon = { Ponto(c.info.cheia, 8.dp) },
                    onClick = { aberto = false; onAbrirPedido(a.id) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- celular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MolduraCelular(
    usuario: UsuarioSessao,
    telaAtual: Tela,
    onTela: (Tela) -> Unit,
    novos: Int,
    avisos: List<AvisoPedido>,
    onAbrirPedido: (Long) -> Unit,
    onLimparAvisos: () -> Unit,
    loja: EstadoLojaViewModel,
    onLogout: () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    val c = TenvitiTema.cores
    val visiveis = Tela.entries.filter { it.visivelPara(usuario) }
    val principais = Tela.PRINCIPAIS_NO_CELULAR.filter { it in visiveis }
    val extras = visiveis - principais.toSet()
    var maisAberto by remember { mutableStateOf(false) }
    val atende = usuario.papel in Tela.PAPEIS_QUE_ATENDEM

    Scaffold(
        containerColor = c.fundo,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(c.fundo).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimboloTenviti(30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(loja.nome.ifEmpty { "tenviTI" }, style = MaterialTheme.typography.titleMedium, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (loja.aberta != null) {
                        Text(
                            when { loja.pausada -> "Pausada"; loja.aberta == true -> "Aberta agora"; else -> "Fechada agora" },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (loja.aberta == true && !loja.pausada) c.sucesso.texto else c.textoSuave,
                        )
                    }
                }
                if (atende) SinoDeAvisos(avisos, onAbrirPedido, onLimparAvisos, fundo = false)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = c.superficie, tonalElevation = 0.dp) {
                principais.forEach { tela ->
                    NavigationBarItem(
                        selected = tela == telaAtual,
                        onClick = { onTela(tela) },
                        icon = {
                            BadgedBox(badge = { if (tela == Tela.PEDIDOS && novos > 0) Badge(containerColor = c.acento, contentColor = Color.White) { Text(novos.toString()) } }) {
                                Icon(tela.icone, contentDescription = null)
                            }
                        },
                        label = { Text(tela.titulo, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = c.baseSuave, selectedIconColor = c.sobreBaseSuave, selectedTextColor = c.texto, unselectedIconColor = c.textoSuave, unselectedTextColor = c.textoSuave),
                    )
                }
                NavigationBarItem(
                    selected = telaAtual in extras,
                    onClick = { maisAberto = true },
                    icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = null) },
                    label = { Text("Mais", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = c.baseSuave, selectedIconColor = c.sobreBaseSuave, selectedTextColor = c.texto, unselectedIconColor = c.textoSuave, unselectedTextColor = c.textoSuave),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { conteudo() }
    }

    if (maisAberto) {
        ModalBottomSheet(onDismissRequest = { maisAberto = false }, containerColor = c.superficie) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                extras.forEach { tela ->
                    ListItem(
                        headlineContent = { Text(tela.titulo) },
                        leadingContent = { Icon(tela.icone, contentDescription = null) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { maisAberto = false; onTela(tela) },
                        colors = ListItemDefaults.colors(containerColor = if (tela == telaAtual) c.baseSuave else c.superficie),
                    )
                }
                if (atende) {
                    ListItem(
                        headlineContent = { Text(if (loja.pausada) "Pedidos pausados" else "Recebendo pedidos") },
                        trailingContent = { Switch(checked = !loja.pausada, enabled = !loja.alterandoPausa, onCheckedChange = { loja.alternarPausa() }) },
                        colors = ListItemDefaults.colors(containerColor = c.superficie),
                    )
                }
                ListItem(
                    headlineContent = { Text("Modo escuro") },
                    leadingContent = { Icon(if (c.escuro) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, contentDescription = null) },
                    trailingContent = { Switch(checked = c.escuro, onCheckedChange = { PreferenciaTema.alternar(c.escuro) }) },
                    colors = ListItemDefaults.colors(containerColor = c.superficie),
                )
                ListItem(
                    headlineContent = { Text("Sair") },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { maisAberto = false; onLogout() },
                    colors = ListItemDefaults.colors(containerColor = c.superficie),
                )
            }
        }
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
