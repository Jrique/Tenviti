package br.com.tenviti.app.telas.componentes.tela.telaVendas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.tenviti.app.data.Pedido
import br.com.tenviti.app.getPlatform
import androidx.compose.runtime.getValue

private data class ProximaAcaoStatus(
    val novoStatusLabel: String,   // "Visto", "Em preparo", etc (lado UI)
    val textoBotao: String         // texto completo do botão
)

private fun calcularProximaAcao(statusAtual: String): ProximaAcaoStatus? =
    when (statusAtual.lowercase()) {
        "novo" -> ProximaAcaoStatus("Visto", "Marcar como VISTO")
        "visto" -> ProximaAcaoStatus("Em preparo", "Marcar como EM PREPARO")
        "em preparo" -> ProximaAcaoStatus("Saiu para entrega", "Marcar como SAIU PARA ENTREGA")
        "saiu para entrega" -> ProximaAcaoStatus("Finalizado", "Marcar como FINALIZADO")
        "finalizado" -> null
        else -> null
    }

@Composable
fun ModalDetalhesPedido(
    pedido: Pedido,
    onDismissRequest: () -> Unit,
    onChangeStatus: (Pedido, String) -> Unit,
    onPrint: (Pedido) -> Unit,
) {
    val scrollState = rememberScrollState()
    val proximaAcao = calcularProximaAcao(pedido.status)

    var mostrarConfirmacaoStatus by remember { mutableStateOf(false) }
    var mostrarConfirmacaoImprimir by remember { mutableStateOf(false) }



    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabeçalho
                CabecalhoModal(pedido)

                // Corpo
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CorpoModal(pedido)
                }

                // Rodapé: botões
                RodapeModal(
                    proximaAcao = proximaAcao,
                    onDismissRequest = onDismissRequest,
                    onClickStatus = { mostrarConfirmacaoStatus = true },
                    onClickImprimir = { mostrarConfirmacaoImprimir = true },

                )
            }
        }
    }

    // --- Modal de confirmação de mudança de status ---
    if (mostrarConfirmacaoStatus && proximaAcao != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoStatus = false },
            title = { Text("Alterar status do pedido") },
            text = {
                Text(
                    "Tem certeza que deseja ${proximaAcao.textoBotao.lowercase()}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacaoStatus = false
                        onChangeStatus(pedido, proximaAcao.novoStatusLabel)
                    }
                ) {
                    Text("Confirmar", color = Color(0xFF790001))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoStatus = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- Modal de confirmação de impressão ---
    if (mostrarConfirmacaoImprimir) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoImprimir = false },
            title = { Text("Imprimir pedido") },
            text = { Text("Deseja realmente enviar este pedido para impressão?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacaoImprimir = false
                        onPrint(pedido)
                    }
                ) {
                    Text("Confirmar", color = Color(0xFF790001))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoImprimir = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}


@Composable
private fun CabecalhoModal(pedido: Pedido) {
    val isMobile = getPlatform().name.contains("Android")
    if (isMobile) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Detalhes do pedido ${pedido.codigo}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            StatusBadge(pedido)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Detalhes do pedido ${pedido.codigo}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            StatusBadge(pedido)
        }
    }
}

@Composable
private fun CorpoModal(pedido: Pedido) {
    // texto completo vindo do backend
    val detalhes = pedido.descricao.ifBlank { "Sem detalhes cadastrados." }

    InfoLinha(
        label = "Cliente:",
        value = extrairClienteDoTexto(detalhes).ifBlank { "Não informado" }
    )

    InfoLinha(
        label = "Horário:",
        value = pedido.horario.ifBlank { "Não informado" }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("Detalhes do pedido:", fontWeight = FontWeight.Bold)
    Text(
        text = detalhes,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Total: ${pedido.valor}",
            fontWeight = FontWeight.Bold
        )
    }
}

private fun extrairClienteDoTexto(descricao: String): String {
    val linhas = descricao
        .lineSequence()
        .map { it.trim() }
        .toList()

    // procura a linha que começa com "Cliente:"
    val idxCliente = linhas.indexOfFirst { it.startsWith("Cliente", ignoreCase = true) }
    if (idxCliente == -1) return ""

    // geralmente o nome vem na próxima linha começando com "-"
    val linhaNome = linhas
        .drop(idxCliente + 1)
        .firstOrNull { it.startsWith("-") }
        ?: return ""

    return linhaNome.removePrefix("-").trim()
}


@Composable
private fun RodapeModal(
    proximaAcao: ProximaAcaoStatus?,
    onDismissRequest: () -> Unit,
    onClickStatus: () -> Unit,
    onClickImprimir: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismissRequest) {
            Text("Fechar", color = Color(0xFF790001))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botão imprimir (sempre aparece)
        OutlinedButton(onClick = onClickImprimir) {
            Text("Imprimir")
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botão de status: só aparece se ainda tiver próxima etapa
        if (proximaAcao != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClickStatus,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001))
            ) {
                Text(proximaAcao.textoBotao)
            }
        }
    }
}



@Composable
private fun StatusBadge(pedido: Pedido) {
    val statusExibicao = if (pedido.status.equals("Saiu para entrega", ignoreCase = true)) {
        "Em Entrega"
    } else {
        pedido.status
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(pedido.corStatus.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(statusExibicao, color = pedido.corStatus, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun InfoLinha(label: String, value: String) {
    val isMobile = getPlatform().name.contains("Android")
    if (isMobile) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
            Text(value)
        }
    }
}