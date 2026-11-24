package br.com.tenviti.app.data

import br.com.tenviti.shared.ResumoDashboardDto

/** Resumo do dashboard já em reais (Double) para os gráficos. */
data class ResumoDashboard(
    val faturamentoDiario: List<Double>,
    val vendasDiarias: List<Int>,
    val diasDaSemana: List<String>,
    val totalFaturamento: Double,
    val totalVendas: Int,
)

fun ResumoDashboardDto.paraUi() = ResumoDashboard(
    faturamentoDiario = faturamentoDiarioCentavos.map { it / 100.0 },
    vendasDiarias = vendasDiarias,
    diasDaSemana = rotulosDias,
    totalFaturamento = totalFaturamentoCentavos / 100.0,
    totalVendas = totalVendas,
)
