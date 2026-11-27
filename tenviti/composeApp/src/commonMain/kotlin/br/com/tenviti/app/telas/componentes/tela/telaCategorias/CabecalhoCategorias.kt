package br.com.tenviti.app.telas.componentes.tela.telaCategorias

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CabecalhoCategorias(
    totalCategorias: Int,
    isMobile: Boolean,
    onAdicionarClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (isMobile) Alignment.CenterVertically else Alignment.Bottom
    ) {
        // Card contador
        Card(
            modifier = if (isMobile) {
                Modifier.weight(1f).height(100.dp)
            } else {
                Modifier.width(250.dp).height(150.dp) // Um pouco mais largo para o texto
            },
            colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(if (isMobile) 16.dp else 24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isMobile) "Categorias Cadastradas" else "Categorias\nCadastradas",
                        textAlign = TextAlign.Center,
                        color = TenvitiTema.cores.texto,
                        fontSize = if (isMobile) 14.sp else 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(if (isMobile) 4.dp else 8.dp))
                    Text(
                        text = totalCategorias.toString(),
                        color = TenvitiTema.cores.texto,
                        fontSize = if (isMobile) 36.sp else 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Botão adaptativo
        if (isMobile) {
            FloatingActionButton(
                onClick = onAdicionarClick,
                containerColor = TenvitiTema.cores.base,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        } else {
            Button(
                onClick = onAdicionarClick,
                colors = ButtonDefaults.buttonColors(containerColor = TenvitiTema.cores.base)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar", color = Color.White)
            }
        }
    }
}