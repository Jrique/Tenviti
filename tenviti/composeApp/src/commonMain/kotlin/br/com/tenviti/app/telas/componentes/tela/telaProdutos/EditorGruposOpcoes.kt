package br.com.tenviti.app.telas.componentes.tela.telaProdutos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tenviti.shared.GrupoOpcoesDto
import br.com.tenviti.shared.OpcaoDto
import br.com.tenviti.shared.emReais
import br.com.tenviti.shared.reaisParaCentavos

private val Vinho = Color(0xFF790001)

/**
 * Editor dos grupos de opções de um produto.
 * Ex.: "Escolha 4 sabores" (mínimo 4, máximo 4, pode repetir) ou "Adicionais" (mínimo 0, máximo 3).
 */
@Composable
fun EditorGruposOpcoes(grupos: List<GrupoOpcoesDto>, onChange: (List<GrupoOpcoesDto>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Opções do produto", fontWeight = FontWeight.Bold)
        Text(
            "Use para sabores, adicionais, ponto da carne… Mínimo maior que zero torna a escolha obrigatória.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        grupos.forEachIndexed { gi, grupo ->
            CartaoGrupo(
                grupo = grupo,
                onChange = { novo -> onChange(grupos.toMutableList().also { it[gi] = novo }) },
                onRemover = { onChange(grupos.filterIndexed { i, _ -> i != gi }) },
            )
        }

        OutlinedButton(onClick = {
            onChange(grupos + GrupoOpcoesDto(nome = "", minimo = 0, maximo = 1, opcoes = listOf(OpcaoDto(nome = ""))))
        }) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(4.dp))
            Text("Adicionar grupo de opções")
        }
    }
}

@Composable
private fun CartaoGrupo(grupo: GrupoOpcoesDto, onChange: (GrupoOpcoesDto) -> Unit, onRemover: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = grupo.nome, onValueChange = { onChange(grupo.copy(nome = it)) },
                    label = { Text("Nome do grupo (ex.: Escolha 4 sabores)") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemover) { Icon(Icons.Default.Delete, "Remover grupo", tint = Vinho) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CampoInteiro("Mínimo", grupo.minimo, Modifier.width(110.dp)) { onChange(grupo.copy(minimo = it)) }
                CampoInteiro("Máximo", grupo.maximo, Modifier.width(110.dp)) { onChange(grupo.copy(maximo = it)) }
                Switch(checked = grupo.permiteRepetir, onCheckedChange = { onChange(grupo.copy(permiteRepetir = it)) })
                Text("Pode repetir", style = MaterialTheme.typography.bodySmall)
            }

            grupo.opcoes.forEachIndexed { oi, opcao ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = opcao.nome,
                        onValueChange = { novo -> onChange(grupo.copy(opcoes = grupo.opcoes.toMutableList().also { it[oi] = opcao.copy(nome = novo) })) },
                        label = { Text("Opção") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    CampoDinheiro("+ R$", opcao.precoCentavos, Modifier.width(110.dp)) { centavos ->
                        onChange(grupo.copy(opcoes = grupo.opcoes.toMutableList().also { it[oi] = opcao.copy(precoCentavos = centavos ?: 0L) }))
                    }
                    Checkbox(
                        checked = opcao.ativo,
                        onCheckedChange = { ativo -> onChange(grupo.copy(opcoes = grupo.opcoes.toMutableList().also { it[oi] = opcao.copy(ativo = ativo) })) },
                    )
                    IconButton(onClick = { onChange(grupo.copy(opcoes = grupo.opcoes.filterIndexed { i, _ -> i != oi })) }) {
                        Icon(Icons.Default.Close, "Remover opção", tint = Color.Gray)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onChange(grupo.copy(opcoes = grupo.opcoes + OpcaoDto(nome = ""))) }) {
                    Text("+ opção", color = Vinho)
                }
                Spacer(Modifier.weight(1f))
                Text("Caixinha marcada = disponível", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun CampoInteiro(rotulo: String, valor: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var texto by remember(valor) { mutableStateOf(valor.toString()) }
    OutlinedTextField(
        value = texto,
        onValueChange = { novo ->
            texto = novo.filter { it.isDigit() }.take(3)
            texto.toIntOrNull()?.let(onChange)
        },
        label = { Text(rotulo) }, singleLine = true, modifier = modifier,
    )
}

/** Campo de dinheiro que guarda o texto digitado e só repassa valores válidos (em centavos). */
@Composable
fun CampoDinheiro(rotulo: String, centavos: Long?, modifier: Modifier = Modifier, permiteVazio: Boolean = false, onChange: (Long?) -> Unit) {
    var texto by remember { mutableStateOf(centavos?.takeIf { it > 0 }?.emReais()?.removePrefix("R$ ") ?: "") }
    OutlinedTextField(
        value = texto,
        onValueChange = { novo ->
            texto = novo
            when {
                novo.isBlank() -> onChange(if (permiteVazio) null else 0L)
                else -> novo.reaisParaCentavos()?.let(onChange)
            }
        },
        label = { Text(rotulo) }, singleLine = true, modifier = modifier,
        isError = texto.isNotBlank() && texto.reaisParaCentavos() == null,
    )
}
