package br.com.tenviti.app.telas.componentes.tela.telaCategorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.tenviti.app.data.Categoria

@Composable
fun ModalGerenciarCategoria(
    categoria: Categoria?,
    onDismiss: () -> Unit,
    onSave: (Categoria) -> Unit
) {
    var nome by remember { mutableStateOf(categoria?.nome ?: "") }
    val isEditing = categoria != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (isEditing) "Editar Categoria" else "Nova Categoria", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF790001)) }
                    Button(
                        onClick = {
                            val novaCategoria = Categoria(id = categoria?.id ?: 0, nome = nome)
                            onSave(novaCategoria)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001))
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}