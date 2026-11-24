package br.com.tenviti.app.telas.componentes.tela.telaVendas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.telas.GraficoVendas

@Composable
fun CardGraficoVendas(
    onFiltrarClick: () -> Unit) {

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 450.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Gráfico de Vendas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            GraficoVendas(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                exibirEixoY = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onFiltrarClick,
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
}