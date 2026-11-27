package br.com.tenviti.app.telas.componentes.tela.telaProdutos

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt


@Composable
fun ModalInfo(titulo: String, conteudo: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TenvitiTema.cores.base, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                titulo,
                fontWeight = FontWeight.Bold,
                color = TenvitiTema.cores.base
            )
        },
        text = {
            Text(
                text = conteudo,
                color = TenvitiTema.cores.texto,
                fontSize = 16.sp
            )
        },
        containerColor = TenvitiTema.cores.superficie,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(16.dp)
    )
}

fun formatCurrency(value: Double): String {
    val cents = (value * 100.0).roundToInt()
    val reais = cents / 100
    val centavos = (cents % 100).let { if (it < 0) -it else it }
    return "R$ ${reais},${centavos.toString().padStart(2, '0')}"
}