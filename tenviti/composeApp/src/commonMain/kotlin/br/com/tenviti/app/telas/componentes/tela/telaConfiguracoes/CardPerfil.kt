package br.com.tenviti.app.telas.componentes.tela.telaConfiguracoes

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.data.Session

@Composable
fun CardPerfil() {
    val usuario = Session.usuarioLogado
    val nomeExibicao = usuario?.nome ?: "Usuário"
    val emailExibicao = usuario?.email ?: "Não logado"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar com a inicial do nome (foto de perfil fica para depois)
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(TenvitiTema.cores.acentoTexto),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    nomeExibicao.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(nomeExibicao, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(emailExibicao, style = MaterialTheme.typography.titleMedium, color = TenvitiTema.cores.textoSuave)
                Text(
                    listOfNotNull(usuario?.papel?.rotulo, usuario?.lojaSlug?.let { "loja: $it" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TenvitiTema.cores.base
                )
            }
        }
    }
}