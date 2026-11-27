package br.com.tenviti.app.telas.componentes.tela.telaCategorias

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.tenviti.app.data.Categoria

@Composable
fun TabelaCategorias(
    categorias: List<Categoria>,
    onEditClick: (Categoria) -> Unit,
    onDeleteClick: (Categoria) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho da Tabela
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("ID", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TenvitiTema.cores.textoSuave, textAlign = TextAlign.Center)
                Text("Nome da Categoria", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, color = TenvitiTema.cores.textoSuave, textAlign = TextAlign.Center)
                Text("Ações", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = TenvitiTema.cores.textoSuave, textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = TenvitiTema.cores.textoSuave.copy(alpha = 0.3f))

            // Linhas da Tabela
            if (categorias.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                    Text("Nenhuma categoria encontrada.", color = TenvitiTema.cores.textoSuave)
                }
            } else {
                categorias.forEach { categoria ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(categoria.id.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(categoria.nome, modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.Center) {
                            IconButton(onClick = { onEditClick(categoria) }) {
                                Icon(Icons.Default.Edit, "Editar", tint = TenvitiTema.cores.base)
                            }
                            IconButton(onClick = { onDeleteClick(categoria) }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = TenvitiTema.cores.base)
                            }
                        }
                    }
                    HorizontalDivider(color = TenvitiTema.cores.textoSuave.copy(alpha = 0.1f))
                }
            }
        }
    }
}