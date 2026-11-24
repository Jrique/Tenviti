package br.com.tenviti.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import br.com.tenviti.app.WindowSize
import br.com.tenviti.app.rememberWindowSize
import br.com.tenviti.app.telas.componentes.tela.telaProdutos.CabecalhoProdutos
import br.com.tenviti.app.telas.componentes.tela.telaProdutos.ListaProdutosCards
import br.com.tenviti.app.telas.componentes.tela.telaProdutos.ModalAdicionarProduto
import br.com.tenviti.app.telas.componentes.tela.telaProdutos.TabelaProdutos
import br.com.tenviti.app.data.Produto
import br.com.tenviti.app.data.Categoria

@Composable
fun TelaProdutos() {
    val windowSize = rememberWindowSize()
    val isMobile = windowSize == WindowSize.COMPACT

    // ViewModel de produtos
    val vm = remember { ProdutosViewModel() }
    val scope = rememberCoroutineScope()

    // ViewModel de categorias
    val categoriasViewModel = remember { CategoriasViewModel() }
    val categorias = categoriasViewModel.categorias

    var textoPesquisa by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var mostrarModalAdicionar by remember { mutableStateOf(false) }
    var produtoParaDeletar by remember { mutableStateOf<Produto?>(null) }
    var produtoParaEditar by remember { mutableStateOf<Produto?>(null) }

    // Carrega produtos E categorias na primeira composição
    LaunchedEffect(Unit) {
        vm.load()
        categoriasViewModel.carregar()
    }

    val produtosFiltrados by remember(textoPesquisa, vm.produtos) {
        mutableStateOf(
            run {
                val base = if (textoPesquisa.isBlank()) {
                    vm.produtos
                } else {
                    vm.produtos.filter {
                        it.nome.contains(textoPesquisa, ignoreCase = true) ||
                                it.descricao.contains(textoPesquisa, ignoreCase = true)
                    }
                }
                base.sortedWith(
                    compareByDescending<Produto> { it.destacado }
                        .thenByDescending { it.id ?: 0L }
                )
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.erro) {
        vm.erro?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF8F6))
                .padding(padding)
                .padding(if (isMobile) 16.dp else 18.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Produtos",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = if (isMobile) 32.sp else 50.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(if (isMobile) 16.dp else 8.dp))

            CabecalhoProdutos(
                totalProdutos = vm.produtos.size,
                isMobile = isMobile,
                onAdicionarClick = { mostrarModalAdicionar = true }
            )

            Spacer(Modifier.height(if (isMobile) 16.dp else 32.dp))

            TextField(
                value = textoPesquisa,
                onValueChange = { textoPesquisa = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar por nome ou descrição...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                shape = RoundedCornerShape(if (isMobile) 12.dp else 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(if (isMobile) 16.dp else 32.dp))

            if (vm.carregando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }

            if (isMobile) {
                ListaProdutosCards(
                    produtos = produtosFiltrados,
                    onDeleteRequest = { produtoParaDeletar = it },
                    onEditRequest   = { produtoParaEditar = it },
                    onPausarRequest = { p -> scope.launch { vm.alternarDisponivel(p) } },
                )
            } else {
                TabelaProdutos(
                    produtos = produtosFiltrados,
                    onDeleteRequest = { produtoParaDeletar = it },
                    onEditRequest   = { produtoParaEditar = it },
                    onPausarRequest = { p -> scope.launch { vm.alternarDisponivel(p) } },
                )
            }
        }
    }

    // Modal para ADICIONAR produto
    if (mostrarModalAdicionar) {
        ModalAdicionarProduto(
            produto = null,
            categorias = categorias,   // passa lista de categorias
            onDismiss = { mostrarModalAdicionar = false },
            onSave = { novo ->
                scope.launch {
                    vm.criar(novo) { mostrarModalAdicionar = false }
                }
            },
            onEnviarFoto = { arquivo -> vm.enviarFoto(arquivo) },
        )
    }

    // Modal para EDITAR produto (com produto selecionado)
    produtoParaEditar?.let { produto ->
        ModalAdicionarProduto(
            produto = produto,
            categorias = categorias,   //  passa lista de categorias
            onDismiss = { produtoParaEditar = null },
            onSave = { atualizado ->
                scope.launch {
                    vm.atualizar(atualizado) { produtoParaEditar = null }
                }
            },
            onToggleDestaque = { p ->
                scope.launch {
                    vm.toggleDestaque(p)
                    // opcional: sincronizar produto em edição com lista
                    produtoParaEditar = vm.produtos.firstOrNull { it.id == p.id }
                }
            },
            onEnviarFoto = { arquivo -> vm.enviarFoto(arquivo) },
        )
    }

    // Diálogo de confirmação de exclusão
    produtoParaDeletar?.let { produto ->
        AlertDialog(
            onDismissRequest = { produtoParaDeletar = null },
            title = { Text("Confirmar Exclusão") },
            text  = { Text("Deseja deletar \"${produto.nome}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            vm.deletar(produto.id ?: return@launch)
                            produtoParaDeletar = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { produtoParaDeletar = null }) {
                    Text("Cancelar", color = Color(0xFF790001))
                }
            }
        )
    }
}
