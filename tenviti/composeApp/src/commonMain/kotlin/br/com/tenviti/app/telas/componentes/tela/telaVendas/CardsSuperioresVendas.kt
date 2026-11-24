package br.com.tenviti.app.telas.componentes.tela.telaVendas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardsSuperioresVendas(onFiltrarClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card de Total de Vendas
        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total de Vendas", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1.580", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFiltrarClick, // <--- USO AQUI
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF790001)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(Color(0xFF790001))
                    )
                ) {
                    Text("Filtrar período")
                }
            }
        }

        // Card de Meta de Vendas
        Card(
            modifier = Modifier.weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Meta Venda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.63f).height(12.dp).clip(CircleShape)
                            .background(Color(0xFF790001))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("R$ 15.8k", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Meta: R$ 25k", color = Color.Gray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF790001)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(Color(0xFF790001))
                    )
                ) {
                    Text("Definir Meta")
                }
            }
        }
    }
}