package br.com.cardapio.app.telas.componentes.tela.telaProdutos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cardapio.app.data.Produto
import androidx.compose.material.icons.filled.Star

@Composable
fun ListaProdutosCards(
    produtos: List<Produto>,
    onDeleteRequest: (Produto) -> Unit,
    onEditRequest: (Produto) -> Unit,
    onPausarRequest: (Produto) -> Unit = {},
) {
    produtos.forEach { produto ->
        CardProduto(
            produto = produto,
            onDeleteClick = { onDeleteRequest(produto) },
            onEditClick = { onEditRequest(produto) },
            onPausarClick = { onPausarRequest(produto) },
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun CardProduto(
    produto: Produto,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onPausarClick: () -> Unit = {},
)  {
    var mostrarDescricao by remember { mutableStateOf(false) }
    var mostrarAdicionais by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (produto.destacado) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Produto em destaque",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(produto.nome, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (!produto.ativo) Text("Pausado — não aparece no cardápio", fontSize = 12.sp, color = Color(0xFF790001))
                        TextButton(onClick = onPausarClick, contentPadding = PaddingValues(0.dp)) {
                            Text(if (produto.ativo) "Pausar (esgotado)" else "Voltar a vender", fontSize = 13.sp, color = Color(0xFF790001))
                        }
                    }
                    Text(produto.tipo, fontSize = 14.sp, color = Color.Gray)
                }

                Text(
                    text = formatCurrency(produto.preco),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF790001)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { mostrarDescricao = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Info, "Descrição", tint = Color(0xFF790001))
                    }
                    OutlinedButton(
                        onClick = { mostrarAdicionais = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF790001),
                            contentColor = Color.White)) {
                        Text("Opções", fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, "Editar", tint = Color(0xFF790001))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, "Apagar", tint = Color(0xFF790001))
                    }
                }
            }
        }
    }

    if (mostrarDescricao) {
        ModalInfo(
            titulo = produto.nome,
            conteudo = produto.descricao,
            onDismiss = { mostrarDescricao = false }
        )
    }

    if (mostrarAdicionais) {
        ModalInfo(
            titulo = "Opções do produto",
            conteudo = produto.resumoOpcoes,
            onDismiss = { mostrarAdicionais = false }
        )
    }
}