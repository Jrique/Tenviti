package br.com.tenviti.app.ui.componentesUi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.tenviti.app.WindowSize
import br.com.tenviti.app.rememberWindowSize

/** Gráfico de faturamento: versão compacta no celular, completa no computador. */
@Composable
fun GraficoFaturamento(
    modifier: Modifier = Modifier,
    exibirEixoY: Boolean = true,
    dados: List<Double> = emptyList(),
    labels: List<String> = emptyList(),
) {
    if (rememberWindowSize() == WindowSize.COMPACT) {
        GraficoFaturamentoAndroidUI(modifier, exibirEixoY, dados, labels)
    } else {
        GraficoFaturamentoDesktopUI(modifier, exibirEixoY, dados, labels)
    }
}
