package br.com.tenviti.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Símbolo da marca: três pontos crescentes ("Tem. Vi. Tive."), o último em coral,
 * num quadrado petróleo arredondado. [sobreEscuro] usa a versão para fundos escuros.
 */
@Composable
fun SimboloTenviti(tamanho: Dp = 32.dp, sobreEscuro: Boolean = false, modifier: Modifier = Modifier) {
    val fundo = if (sobreEscuro) Color(0xFF1C3533) else Color(0xFF1F5F5B)
    val p1 = if (sobreEscuro) Color(0xFF3FC6A8) else Color(0xFF9FD3CC)
    val p2 = if (sobreEscuro) Color(0xFFCFE3E0) else Color(0xFFE1F2EF)
    val p3 = if (sobreEscuro) Color(0xFFF08C74) else Color(0xFFE2725B)
    val u = tamanho / 32f
    Box(
        modifier = modifier
            .size(tamanho)
            .background(fundo, RoundedCornerShape(tamanho * 0.3f))
            .semantics { contentDescription = "Tenviti" },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(u * 2.5f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(u * 5f).background(p1, CircleShape))
            Box(Modifier.size(u * 6.5f).background(p2, CircleShape))
            Box(Modifier.size(u * 8f).background(p3, CircleShape))
        }
    }
}

/**
 * Logo completo: símbolo + "tenvi" + "TI" em destaque (ensina a pronúncia ten-vi-TI).
 * [comSlogan] mostra "Tem. Vi. Tive." embaixo; [sobreEscuro] para o menu e fundos escuros.
 */
@Composable
fun LogoTenviti(
    modifier: Modifier = Modifier,
    tamanho: TextUnit = 24.sp,
    comSlogan: Boolean = false,
    comSimbolo: Boolean = true,
    sobreEscuro: Boolean = false,
) {
    val c = TenvitiTema.cores
    val corNome = if (sobreEscuro) Color.White else c.base
    val corTI = if (sobreEscuro) c.menuAcento else c.acento
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (comSimbolo) {
                SimboloTenviti(tamanho = (tamanho.value * 1.4f).dp, sobreEscuro = sobreEscuro)
                Spacer(Modifier.width((tamanho.value * 0.45f).dp))
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = corNome)) { append("tenvi") }
                    withStyle(SpanStyle(color = corTI)) { append("TI") }
                },
                fontFamily = FonteTitulo,
                fontSize = tamanho,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }
        if (comSlogan) {
            Text(
                "Tem. Vi. Tive.",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FonteTitulo,
                color = if (sobreEscuro) c.menuTextoSuave else c.textoSuave,
            )
        }
    }
}
