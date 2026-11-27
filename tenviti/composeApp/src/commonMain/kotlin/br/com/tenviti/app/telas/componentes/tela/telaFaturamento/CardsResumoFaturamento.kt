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
import br.com.tenviti.app.telas.toReal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardsResumoFaturamento(
    dados: ResumoDashboard?,
    onFiltrarClick: () -> Unit // Recebe a ação de abrir o modal
) {

    val faturamentoTotal = dados?.totalFaturamento ?: 0.0
    val totalVendas = dados?.totalVendas ?: 0

    val ticketMedio = if (totalVendas > 0) faturamentoTotal / totalVendas else 0.0

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CartaoResumoComFiltro(
            titulo = "Faturamento Total",
            valor = faturamentoTotal.toReal(),
            onClickFiltro = onFiltrarClick,
            modifier = Modifier.weight(1f).widthIn(min = 250.dp)
        )
        CartaoResumoComFiltro(
            titulo = "Total de Vendas",
            valor = totalVendas.toString(),
            onClickFiltro = onFiltrarClick,
            modifier = Modifier.weight(1f).widthIn(min = 250.dp)
        )
        CartaoResumoComFiltro(
            titulo = "Ticket Médio",
            valor = ticketMedio.toReal(),
            onClickFiltro = onFiltrarClick,
            modifier = Modifier.weight(1f).widthIn(min = 250.dp)
        )
    }
}

@Composable
private fun CartaoResumoComFiltro(
    titulo: String,
    valor: String,
    onClickFiltro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = TenvitiTema.cores.textoSuave)
            Spacer(modifier = Modifier.height(8.dp))
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onClickFiltro,
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