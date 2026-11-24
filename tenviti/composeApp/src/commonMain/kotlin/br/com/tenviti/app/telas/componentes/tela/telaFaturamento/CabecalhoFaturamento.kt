package br.com.tenviti.app.telas.componentes.tela.telaFaturamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tenviti.app.getPlatform

enum class PeriodoFiltroGlobal {
    HOJE, SEMANAL, MENSAL
}

@Composable
fun CabecalhoFaturamento(
    periodoSelecionado: PeriodoFiltroGlobal,
    onPeriodoSelecionado: (PeriodoFiltroGlobal) -> Unit
) {
    val isMobile = getPlatform().name.contains("Android")

    Text(
        text = "Faturamento",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        fontSize = if (isMobile) 32.sp else 50.sp
    )
    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotaoFiltroGlobal("Hoje", periodoSelecionado == PeriodoFiltroGlobal.HOJE) { onPeriodoSelecionado(PeriodoFiltroGlobal.HOJE) }
        BotaoFiltroGlobal("Semanal", periodoSelecionado == PeriodoFiltroGlobal.SEMANAL) { onPeriodoSelecionado(PeriodoFiltroGlobal.SEMANAL) }
        BotaoFiltroGlobal("Mensal", periodoSelecionado == PeriodoFiltroGlobal.MENSAL) { onPeriodoSelecionado(PeriodoFiltroGlobal.MENSAL) }
    }
}

@Composable
private fun BotaoFiltroGlobal(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val cores = if (selecionado) ButtonDefaults.buttonColors(containerColor = Color(0xFF790001))
    else ButtonDefaults.outlinedButtonColors()

    val borda = if(selecionado) null else ButtonDefaults.outlinedButtonBorder().copy(
        brush = SolidColor(Color(0xFF790001))
    )
    val corTexto = if (selecionado) Color.White else Color(0xFF790001)

    Button(
        onClick = onClick,
        colors = cores,
        border = borda
    ) {
        Text(texto, color = corTexto)
    }
}