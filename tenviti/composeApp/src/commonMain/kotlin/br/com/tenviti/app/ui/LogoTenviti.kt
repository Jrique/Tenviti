package br.com.tenviti.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Cores da marca Tenviti (a paleta completa do painel chega no Bloco 5 da Fase 2). */
object CoresTenviti {
    val coral = Color(0xFFE2725B)
    val petroleo = Color(0xFF1F5F5B)
    val creme = Color(0xFFFFF7EE)
}

/**
 * Logo em texto: "tenvi" + "TI" em destaque, que ensina a pronúncia (ten-vi-TI).
 * [comSlogan] mostra "Tem. Vi. Tive." logo abaixo.
 */
@Composable
fun LogoTenviti(
    modifier: Modifier = Modifier,
    tamanho: TextUnit = 32.sp,
    comSlogan: Boolean = false,
    corTexto: Color = CoresTenviti.petroleo,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = corTexto)) { append("tenvi") }
                withStyle(SpanStyle(color = CoresTenviti.coral)) { append("TI") }
            },
            style = MaterialTheme.typography.headlineLarge,
            fontSize = tamanho,
            fontWeight = FontWeight.Bold,
        )
        if (comSlogan) {
            Text(
                "Tem. Vi. Tive.",
                style = MaterialTheme.typography.labelSmall,
                fontSize = tamanho * 0.38f,
                color = corTexto.copy(alpha = 0.75f),
            )
        }
    }
}
