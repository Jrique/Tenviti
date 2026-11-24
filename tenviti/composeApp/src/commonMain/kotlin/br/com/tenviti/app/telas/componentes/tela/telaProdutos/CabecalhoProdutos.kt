package br.com.tenviti.app.telas.componentes.tela.telaProdutos

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
fun CabecalhoProdutos(
    totalProdutos: Int,
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
                Modifier.width(200.dp).height(150.dp)
            },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(if (isMobile) 16.dp else 24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isMobile) "Produtos Cadastrados" else "Produtos\nCadastrados",
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontSize = if (isMobile) 14.sp else 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(if (isMobile) 4.dp else 8.dp))
                    Text(
                        text = totalProdutos.toString(),
                        color = Color.Black,
                        fontSize = if (isMobile) 36.sp else 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isMobile) {
            FloatingActionButton(
                onClick = onAdicionarClick,
                containerColor = Color(0xFF790001),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        } else {
            Button(
                onClick = onAdicionarClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar", color = Color.White)
            }
        }
    }
}