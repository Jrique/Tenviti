package br.com.tenviti.app.ui.componentesUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tenviti.app.data.Pedido

// UI Comum para UltimosPedidos
@Composable
fun UltimosPedidosDesktopUI(
    pedidos: List<Pedido>,
    modificador: Modifier,
    cabecalho: @Composable () -> Unit
) {
    Column(modifier = modificador) {
        cabecalho()
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabeçalho
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("Últimos Pedidos", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pedido", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center )
                        Text("Descrição", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                        Text("Valor", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                        Text("Horário", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                        Text("Status", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Lista de pedidos
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
                    pedidos.forEach { pedido -> LinhaItemPedidoDesktopUI(pedido) }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun LinhaItemPedidoDesktopUI(pedido: Pedido) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coluna Pedido
        Row(
            modifier = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
            Spacer(modifier = Modifier.width(16.dp))
            Text(pedido.codigo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        // Coluna Descrição
        Text(
            text = extrairResumoProduto(pedido.descricao),
            modifier = Modifier.weight(3f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Coluna Valor
        Text(
            pedido.valor,
            modifier = Modifier.weight(1.5f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        // Coluna Horário
        Text(
            pedido.horario,
            modifier = Modifier.weight(2f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        // Coluna Status
        Box(
            modifier = Modifier.weight(2f),
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(pedido.corStatus, MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
