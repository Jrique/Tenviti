package br.com.tenviti.app.telas.componentes.tela.telaConfiguracoes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CardAcoes(
    mostrarEquipe: Boolean,
    onAlterarSenhaClick: () -> Unit,
    onEquipeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ações", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            BotaoAcao(texto = "Alterar minha senha", icone = Icons.Default.LockReset, onClick = onAlterarSenhaClick)
            if (mostrarEquipe) {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                BotaoAcao(texto = "Equipe da loja", icone = Icons.Default.Groups, onClick = onEquipeClick)
            }
        }
    }
}

@Composable
private fun BotaoAcao(texto: String, icone: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icone, contentDescription = null, tint = Color(0xFF790001))
            Spacer(modifier = Modifier.width(16.dp))
            Text(texto, color = Color.Black, fontSize = 16.sp)
        }
    }
}
