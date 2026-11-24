package br.com.tenviti.app.ui.componentesUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tenviti.app.data.Pedido
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun UltimosPedidosAndroidUI(pedidos: List<Pedido>, modificador: Modifier, cabecalho: @Composable () -> Unit) {
    Column(modifier = modificador) {
        Text("Últimos Pedidos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Pedido", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text("Desc.", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text("Status", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                    pedidos.forEach { pedido -> LinhaItemPedidoAndroidUI(pedido) }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun LinhaItemPedidoAndroidUI(pedido: Pedido) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coluna Pedido
        Row(
            modifier = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center // Adicionado para centralizar
        ) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
            Spacer(modifier = Modifier.width(12.dp))
            Text(pedido.codigo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        // Coluna Descrição
        val descricaoResumida = remember(pedido.descricao) {
            extrairResumoProduto(pedido.descricao)
        }

        Text(
            text = descricaoResumida,
            modifier = Modifier.weight(2f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Coluna Status
        Box(
            modifier = Modifier.weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            val statusExibicao = remember(pedido.status) {
                if (pedido.status.equals("Saiu para entrega", ignoreCase = true)) {
                    "Em Entrega"
                } else {
                    pedido.status
                }
            }

            Text(
                text = statusExibicao,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .background(pedido.corStatus, MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
fun resumirDescricao(
    descricaoCompleta: String,
    maxLinhas: Int = 4,
    maxChars: Int = 160
): String {
    if (descricaoCompleta.isBlank()) return ""

    val linhas = descricaoCompleta
        .lineSequence()
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }
        .toList()

    val preview = linhas.take(maxLinhas).joinToString("\n")

    return if (preview.length > maxChars) {
        preview.take(maxChars) + "..."
    } else {
        preview
    }
}

fun extrairResumoProduto(descricaoCompleta: String): String {
    if (descricaoCompleta.isBlank()) return ""

    val linhas = descricaoCompleta
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()

    val idxProdutos = linhas.indexOfFirst { it.startsWith("Produtos", ignoreCase = true) }

    if (idxProdutos != -1) {
        val linhaProduto = linhas
            .drop(idxProdutos + 1)
            .firstOrNull { it.startsWith("-") && it.length > 1 }

        if (linhaProduto != null) {
            return linhaProduto.removePrefix("-").trim()
        }
    }

    // fallback: primeira linha não vazia
    return linhas.firstOrNull().orEmpty()
}