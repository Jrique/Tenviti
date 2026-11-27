package br.com.tenviti.app.telas.componentes.tela.telaFaturamento

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.data.ResumoDashboard
import br.com.tenviti.app.ui.componentesUi.GraficoFaturamento

@Composable
fun CardGraficoFaturamento(
    dados: ResumoDashboard?,
    onFiltrarClick: () -> Unit
) {
    val listaFaturamento = dados?.faturamentoDiario ?: emptyList()
    val listaLabels = dados?.diasDaSemana ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 450.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Text(
                "Gráfico de Faturamento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (listaFaturamento.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Sem dados para o período", color = TenvitiTema.cores.textoSuave)
                }
            } else {
                GraficoFaturamento(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    exibirEixoY = true,
                    dados = listaFaturamento,
                    labels = listaLabels
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onFiltrarClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TenvitiTema.cores.base
                ),
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = SolidColor(TenvitiTema.cores.base)
                )
            ) {
                Text("Filtrar período")
            }
        }
    }
}