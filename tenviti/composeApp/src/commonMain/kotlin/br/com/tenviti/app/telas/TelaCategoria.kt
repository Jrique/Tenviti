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
import br.com.tenviti.app.data.Categoria
import br.com.tenviti.app.getPlatform
import br.com.tenviti.app.telas.componentes.tela.telaCategorias.CabecalhoCategorias
import br.com.tenviti.app.telas.componentes.tela.telaCategorias.ModalGerenciarCategoria
import br.com.tenviti.app.telas.componentes.tela.telaCategorias.TabelaCategorias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCategorias() {
    // ViewModel e estado base
    val categoriasViewModel = remember { CategoriasViewModel() }
    val scope = rememberCoroutineScope()

    val categorias = categoriasViewModel.categorias

    var categoriaParaEditar by remember { mutableStateOf<Categoria?>(null) }
    var categoriaParaDeletar by remember { mutableStateOf<Categoria?>(null) }
    var modoAdicionar by remember { mutableStateOf(false) }
    var textoPesquisa by remember { mutableStateOf("") }

    val isMobile = getPlatform().name.contains("Android")
    val scrollState = rememberScrollState()

    // Carrega categorias do backend na primeira composição
    LaunchedEffect(Unit) {
        categoriasViewModel.carregar()
    }

    // Lógica da pesquisa
    val categoriasFiltradas by remember(textoPesquisa, categorias) {
        mutableStateOf(
            if (textoPesquisa.isBlank()) {
                categorias
            } else {
                categorias.filter { cat ->
                    cat.nome.contains(textoPesquisa, ignoreCase = true)
                }
            }
        )
    }

    // Layout principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F6))
            .verticalScroll(scrollState)
            .padding(if (isMobile) 16.dp else 18.dp)
    ) {
        // Título da Tela
        Text(
            text = "Categorias",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = if (isMobile) 32.sp else 50.sp
        )
        Spacer(modifier = Modifier.height(if (isMobile) 16.dp else 8.dp))

        // Cabeçalho (usa o total vindo do ViewModel)
        CabecalhoCategorias(
            totalCategorias = categorias.size,
            isMobile = isMobile,
            onAdicionarClick = { modoAdicionar = true }
        )
        Spacer(modifier = Modifier.height(if (isMobile) 16.dp else 32.dp))

        // Barra de Pesquisa
        TextField(
            value = textoPesquisa,
            onValueChange = { textoPesquisa = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Pesquisar categoria...") },
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
        Spacer(modifier = Modifier.height(if (isMobile) 16.dp else 32.dp))

        // Tabela de Categorias
        TabelaCategorias(
            categorias = categoriasFiltradas,
            onEditClick = { categoria -> categoriaParaEditar = categoria },
            onDeleteClick = { categoria -> categoriaParaDeletar = categoria }
        )
    }

    // -Modal para Adicionar/Editar
    if (modoAdicionar || categoriaParaEditar != null) {
        ModalGerenciarCategoria(
            categoria = categoriaParaEditar,
            onDismiss = {
                modoAdicionar = false
                categoriaParaEditar = null
            },
            onSave = { categoriaSalva ->
                scope.launch {
                    // Se tiver id -> editar, se não tiver -> criar
                    categoriasViewModel.salvar(
                        nome = categoriaSalva.nome,
                        idExistente = categoriaSalva.id
                    )
                    modoAdicionar = false
                    categoriaParaEditar = null
                }
            }
        )
    }

    // ---- Diálogo de Confirmação para Deletar ----
    categoriaParaDeletar?.let { categoria ->
        AlertDialog(
            onDismissRequest = { categoriaParaDeletar = null },
            title = { Text("Confirmar Exclusão") },
            text = {
                Text("Tem certeza que deseja excluir a categoria \"${categoria.nome}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            categoria.id?.let { categoriasViewModel.excluir(it) }
                            categoriaParaDeletar = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { categoriaParaDeletar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}