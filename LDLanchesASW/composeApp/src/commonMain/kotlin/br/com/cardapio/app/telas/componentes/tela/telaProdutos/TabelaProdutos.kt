package br.com.cardapio.app.telas.componentes.tela.telaProdutos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.cardapio.app.data.Produto
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star

/** Largura fixa da coluna de ações: cabe pausar, editar e apagar em qualquer tamanho de tela. */
private val LARGURA_ACOES = 128.dp

@Composable
fun TabelaProdutos(
    produtos: List<Produto>,
    onDeleteRequest: (Produto) -> Unit,
    onEditRequest: (Produto) -> Unit,
    onPausarRequest: (Produto) -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Cabeçalho
            CabecalhoTabela()
            HorizontalDivider(color = Color.Black.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 6.dp))

            // Linhas
            produtos.forEach { produto ->
                LinhaTabelaProduto(
                    produto = produto,
                    onDeleteClick = { onDeleteRequest(produto) },
                    onEditClick = { onEditRequest(produto) },
                    onPausarClick = { onPausarRequest(produto) },
                )
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun CabecalhoTabela() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
        }

        Text(
            text = "Preço",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = "Nome",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(2.5f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = "Descrição",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(3f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = "Tipo",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = "Opções",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = "Ações",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(LARGURA_ACOES)
        )
    }
}

@Composable
fun LinhaTabelaProduto(
    produto: Produto,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleDestaque: (Produto) -> Unit = {},
    onPausarClick: () -> Unit = {},
) {
    var mostrarAdicionais by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.7f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (produto.destacado) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Produto em destaque",
                    tint = Color(0xFFFFB300) // amarelinho
                )
            }
        }

        Text(
            text = formatCurrency(produto.preco),
            color = Color(0xFF790001),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = if (produto.ativo) produto.nome else "${produto.nome} (pausado)",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(2.5f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = produto.descricao,
            color = Color.DarkGray,
            fontSize = 14.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(3f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = produto.tipo,
            color = Color.Black,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 6.dp)
        )
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = { mostrarAdicionais = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Ver mais",
                    color = Color(0xFF790001),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.width(LARGURA_ACOES),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPausarClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (produto.ativo) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (produto.ativo) "Pausar (esgotado)" else "Voltar a vender",
                    tint = Color(0xFF790001),
                )
            }
            IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Edit, "Editar", tint = Color(0xFF790001))
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, "Apagar", tint = Color(0xFF790001))
            }
        }
    }

    if (mostrarAdicionais) {
        ModalInfo(
            titulo = "Opções do produto",
            conteudo = produto.resumoOpcoes,
            onDismiss = { mostrarAdicionais = false }
        )
    }
}