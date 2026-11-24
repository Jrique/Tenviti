package br.com.tenviti.app.telas.componentes.tela.telaProdutos

import br.com.tenviti.shared.emReais
import br.com.tenviti.shared.reaisParaCentavos
import kotlin.math.roundToLong

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import br.com.tenviti.app.api.rememberSeletorDeImagem
import br.com.tenviti.app.data.ArquivoSelecionado
import br.com.tenviti.app.data.Categoria
import br.com.tenviti.app.data.Produto
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalAdicionarProduto(
    produto: Produto? = null,
    onDismiss: () -> Unit,
    categorias: List<Categoria> = emptyList(),
    onSave: (Produto) -> Unit,
    onToggleDestaque: (Produto) -> Unit = {},
    /** Envia a foto escolhida e devolve o endereço dela (null se falhar). */
    onEnviarFoto: suspend (ArquivoSelecionado) -> String? = { null },
) {

    var nome by remember { mutableStateOf(produto?.nome ?: "") }
    var descricao by remember { mutableStateOf(produto?.descricao ?: "") }
    var preco by remember { mutableStateOf(produto?.preco?.let { (it * 100).roundToLong().emReais().removePrefix("R$ ") } ?: "") }
    var tipo by remember { mutableStateOf(produto?.tipo ?: "") }
    var precoPromocional by remember { mutableStateOf(produto?.precoPromocional?.let { (it * 100).roundToLong() }) }
    var imagemUrl by remember { mutableStateOf(produto?.imagemUrl) }
    var grupos by remember { mutableStateOf(produto?.grupos ?: emptyList()) }
    var disponivel by remember { mutableStateOf(produto?.ativo ?: true) }
    var enviandoFoto by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val escolherFoto = rememberSeletorDeImagem { arquivo ->
        escopo.launch {
            enviandoFoto = true
            onEnviarFoto(arquivo)?.let { imagemUrl = it }
            enviandoFoto = false
        }
    }
    var mostrarConfirmacaoDestaque by remember { mutableStateOf(false) }
    val isEditing = produto != null
    var categoriaSelecionada by remember {
        mutableStateOf<Categoria?>(categorias.find { it.nome == tipo })
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Editar Produto" else "Adicionar Novo Produto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(110.dp).clickable(enabled = !enviandoFoto) { escolherFoto() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (enviandoFoto) {
                                CircularProgressIndicator(color = Color(0xFF790001))
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, "Foto do produto", tint = Color.Gray)
                                Text(
                                    if (imagemUrl == null) "Toque para enviar a foto (PNG, JPG ou WEBP, até 5 MB)"
                                    else "Foto enviada ✓ — toque para trocar",
                                    color = Color.Gray,
                                )
                                imagemUrl?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    TextButton(onClick = { imagemUrl = null }) { Text("Remover foto", color = Color(0xFF790001)) }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Produto") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = preco, onValueChange = { preco = it }, label = { Text("Preço (ex.: 25,50)") },
                        isError = preco.isNotBlank() && preco.reaisParaCentavos() == null,
                        modifier = Modifier.weight(1f),
                    )
                    CampoDinheiro(
                        "Preço promocional (opcional)", precoPromocional, Modifier.weight(1f), permiteVazio = true,
                    ) { precoPromocional = it }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = disponivel, onCheckedChange = { disponivel = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (disponivel) "Disponível no cardápio" else "Pausado (esgotado) — não aparece no cardápio")
                }
                Spacer(modifier = Modifier.height(16.dp))


                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = categoriaSelecionada?.nome ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = "Categoria",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF790001)
                            )
                        },
                        placeholder = {
                            Text("Selecione a categoria")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = dropdownExpanded
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF790001),
                            unfocusedBorderColor = Color(0xFFB0B0B0),
                            cursorColor = Color(0xFF790001)
                        ),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        categorias.forEach { categoria: Categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria.nome) },
                                onClick = {
                                    categoriaSelecionada = categoria
                                    tipo = categoria.nome
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))
                EditorGruposOpcoes(grupos) { grupos = it }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFF790001))
                    }

                    if (produto != null && (produto.id ?: 0L) != 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { mostrarConfirmacaoDestaque = true }
                        ) {
                            Text(if (produto.destacado) "Não destacar" else "Destacar")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val produtoSalvo = Produto(
                                id = produto?.id ?: 0,
                                nome = nome,
                                descricao = descricao,
                                preco = (preco.reaisParaCentavos() ?: 0L) / 100.0,
                                precoPromocional = precoPromocional?.let { it / 100.0 },
                                imagemUrl = imagemUrl,
                                tipo = categoriaSelecionada?.nome ?: tipo,
                                categoriaId = categoriaSelecionada?.id ?: produto?.categoriaId,
                                grupos = grupos.map { g -> g.copy(nome = g.nome.trim(), opcoes = g.opcoes.filter { it.nome.isNotBlank() }) },
                                destacado = produto?.destacado ?: false,
                                ativo = disponivel
                            )
                            onSave(produtoSalvo)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001))
                    ) {
                        Text("Salvar Produto")
                    }
                }
                if (mostrarConfirmacaoDestaque && produto != null && (produto.id ?: 0L) != 0L) {
                    AlertDialog(
                        onDismissRequest = { mostrarConfirmacaoDestaque = false },
                        title = {
                            Text(
                                if (produto.destacado) "Remover destaque"
                                else "Destacar produto"
                            )
                        },
                        text = {
                            Text(
                                if (produto.destacado)
                                    "Deseja remover este produto da área de destaques do cardápio?"
                                else
                                    "Deseja destacar este produto no banner do cardápio-web?"
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    mostrarConfirmacaoDestaque = false
                                    onToggleDestaque(produto)
                                }
                            ) {
                                Text("Confirmar", color = Color(0xFF790001))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarConfirmacaoDestaque = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}