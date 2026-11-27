package br.com.tenviti.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tenviti.app.data.Produto
import br.com.tenviti.app.telas.componentes.tela.telaProdutos.ModalAdicionarProduto
import br.com.tenviti.app.ui.*
import kotlinx.coroutines.launch

/** Catálogo: produtos em cartões e, na outra aba, as categorias. */
@Composable
fun TelaCatalogo() {
    var aba by remember { mutableStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val margem = if (maxWidth >= 600.dp) 32.dp else 16.dp
        Column(Modifier.fillMaxSize().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CabecalhoTela("Catálogo", "O que aparece na sua vitrine.", modifier = Modifier.padding(horizontal = margem)) {
                SeletorSegmentado(listOf("Produtos", "Categorias"), aba, { aba = it })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (aba) {
                    0 -> ConteudoProdutos(margem)
                    else -> TelaCategorias(embutida = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConteudoProdutos(margem: androidx.compose.ui.unit.Dp) {
    val c = TenvitiTema.cores
    val vm = remember { ProdutosViewModel() }
    val categoriasVm = remember { CategoriasViewModel() }
    val scope = rememberCoroutineScope()

    var busca by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<String?>(null) }
    var adicionando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Produto?>(null) }
    var apagando by remember { mutableStateOf<Produto?>(null) }

    LaunchedEffect(Unit) {
        vm.load()
        categoriasVm.carregar()
    }

    val filtrados = vm.produtos
        .filter { categoria == null || it.tipo == categoria }
        .filter { busca.isBlank() || it.nome.contains(busca, true) || it.descricao.contains(busca, true) }
        .sortedWith(compareByDescending<Produto> { it.ativo }.thenByDescending { it.destacado }.thenBy { it.nome })

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val colunas = ((maxWidth - margem * 2) / 230.dp).toInt().coerceIn(1, 6)
        val largura = (maxWidth - margem * 2 - 14.dp * (colunas - 1)) / colunas - 1.dp // folga para arredondamento
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = margem).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it }, singleLine = true,
                    placeholder = { Text("Buscar produto") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.superficie, focusedContainerColor = c.superficie, unfocusedBorderColor = c.borda),
                    modifier = Modifier.widthIn(min = 220.dp, max = 360.dp),
                )
                FiltroCategoria("Tudo", categoria == null) { categoria = null }
                categoriasVm.categorias.forEach { cat -> FiltroCategoria(cat.nome, categoria == cat.nome) { categoria = cat.nome } }
                Button(onClick = { adicionando = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Novo produto")
                }
            }
            Text(
                "${vm.produtos.size} produtos · ${vm.produtos.count { !it.ativo }} pausados · ${vm.produtos.count { it.precoPromocional != null }} em promoção",
                style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
            )
            if (vm.carregando) LinearProgressIndicator(Modifier.fillMaxWidth())
            vm.erro?.let { Text(it, color = c.erro.texto, style = MaterialTheme.typography.bodySmall) }
            if (!vm.carregando && filtrados.isEmpty()) {
                Cartao(Modifier.fillMaxWidth()) {
                    Text(if (vm.produtos.isEmpty()) "Cadastre seu primeiro produto" else "Nenhum produto encontrado", style = MaterialTheme.typography.titleMedium)
                    Text(if (vm.produtos.isEmpty()) "Ele aparece na vitrine assim que você salvar." else "Mude a busca ou a categoria.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                filtrados.forEach { p ->
                    CartaoProduto(
                        p, Modifier.width(largura),
                        onEditar = { editando = p },
                        onApagar = { apagando = p },
                        onDisponivel = { scope.launch { vm.alternarDisponivel(p) } },
                    )
                }
            }
        }
    }

    if (adicionando) {
        ModalAdicionarProduto(
            produto = null, categorias = categoriasVm.categorias,
            onDismiss = { adicionando = false },
            onSave = { novo -> scope.launch { vm.criar(novo) { adicionando = false } } },
            onEnviarFoto = { arquivo -> vm.enviarFoto(arquivo) },
        )
    }
    editando?.let { produto ->
        ModalAdicionarProduto(
            produto = produto, categorias = categoriasVm.categorias,
            onDismiss = { editando = null },
            onSave = { atualizado -> scope.launch { vm.atualizar(atualizado) { editando = null } } },
            onToggleDestaque = { p -> scope.launch { vm.toggleDestaque(p); editando = vm.produtos.firstOrNull { it.id == p.id } } },
            onEnviarFoto = { arquivo -> vm.enviarFoto(arquivo) },
        )
    }
    apagando?.let { produto ->
        AlertDialog(
            onDismissRequest = { apagando = null },
            title = { Text("Apagar \"${produto.nome}\"?") },
            text = { Text("Ele some da vitrine e do catálogo. Se só acabou o estoque, prefira pausar.") },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch { vm.deletar(produto.id ?: return@launch); apagando = null } },
                    colors = ButtonDefaults.textButtonColors(contentColor = c.erro.texto),
                ) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { apagando = null }) { Text("Voltar") } },
        )
    }
}

@Composable
private fun FiltroCategoria(nome: String, ativo: Boolean, onClick: () -> Unit) {
    val c = TenvitiTema.cores
    FilterChip(
        selected = ativo, onClick = onClick, label = { Text(nome) },
        shape = RoundedCornerShape(99.dp),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.texto, selectedLabelColor = c.fundo, containerColor = c.superficie),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = ativo, borderColor = c.borda),
    )
}

@Composable
private fun CartaoProduto(p: Produto, modifier: Modifier, onEditar: () -> Unit, onApagar: () -> Unit, onDisponivel: () -> Unit) {
    val c = TenvitiTema.cores
    Cartao(modifier, preenchimento = PaddingValues(0.dp), espacamento = 0.dp) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).background(c.superficieAlt, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).alpha(if (p.ativo) 1f else 0.55f),
            contentAlignment = Alignment.Center,
        ) {
            Text(p.nome.split(' ').take(2).joinToString("") { it.take(1).uppercase() }, fontFamily = FonteTitulo, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = c.textoSuave)
            Row(Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.destacado) Etiqueta("Destaque", CorDeStatus(c.acentoSuave, c.sobreAcentoSuave, c.acento))
                if (p.precoPromocional != null) Etiqueta("Promoção", c.aviso)
                if (p.imagemUrl != null) Etiqueta("Com foto", c.neutro)
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (p.tipo.isNotBlank()) Text(p.tipo, style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
            Text(p.nome, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (p.grupos.isNotEmpty()) Text("${p.grupos.size} grupo(s) de opções", style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text((p.precoPromocional ?: p.preco).toReal(), fontFamily = FonteTitulo, style = MaterialTheme.typography.titleMedium)
                    p.precoPromocional?.let { Text(p.preco.toReal(), style = MaterialTheme.typography.labelSmall, color = c.textoSuave, textDecoration = TextDecoration.LineThrough) }
                }
                Etiqueta(if (p.ativo) "Disponível" else "Pausado", if (p.ativo) c.sucesso else c.neutro)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = p.ativo, onCheckedChange = { onDisponivel() }, modifier = Modifier.padding(end = 4.dp))
                Text(if (p.ativo) "Vendendo" else "Pausado", style = MaterialTheme.typography.labelSmall, color = c.textoSuave, modifier = Modifier.weight(1f))
                IconButton(onClick = onEditar) { Icon(Icons.Outlined.Edit, contentDescription = "Editar ${p.nome}", tint = c.texto) }
                IconButton(onClick = onApagar) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Apagar ${p.nome}", tint = c.erro.texto) }
            }
        }
    }
}
