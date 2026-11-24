package br.com.tenviti.app.telas.componentes.tela.telaConfiguracoes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.data.Session
import br.com.tenviti.shared.Formatos
import br.com.tenviti.shared.NovoUsuarioDto
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.UsuarioDto
import kotlinx.coroutines.launch

private val Vinho = Color(0xFF790001)

/** Troca da própria senha (qualquer papel). */
@Composable
fun ModalAlterarSenha(onDismiss: () -> Unit) {
    val api = ApiProvider.painelApi
    val scope = rememberCoroutineScope()

    var senhaAtual by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 520.dp).wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Alterar minha senha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = senhaAtual, onValueChange = { senhaAtual = it }, label = { Text("Senha atual") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = novaSenha, onValueChange = { novaSenha = it },
                    label = { Text("Nova senha (mínimo ${Formatos.SENHA_TAMANHO_MINIMO} caracteres)") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmacao, onValueChange = { confirmacao = it }, label = { Text("Confirme a nova senha") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                )
                mensagem?.let { Text(it, color = Vinho) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Vinho) }
                    Button(
                        onClick = {
                            mensagem = when {
                                !Formatos.senhaValida(novaSenha) -> "A nova senha precisa ter pelo menos ${Formatos.SENHA_TAMANHO_MINIMO} caracteres."
                                novaSenha != confirmacao -> "A confirmação não confere com a nova senha."
                                else -> null
                            }
                            if (mensagem != null) return@Button
                            scope.launch {
                                try {
                                    carregando = true
                                    api.trocarSenha(senhaAtual, novaSenha)
                                    onDismiss()
                                } catch (e: Exception) {
                                    mensagem = e.message
                                } finally {
                                    carregando = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Vinho),
                        enabled = !carregando,
                    ) { Text(if (carregando) "Salvando..." else "Salvar") }
                }
            }
        }
    }
}

/** Equipe da loja. Dono e gerente cadastram; o servidor confere quem pode criar cada papel. */
@Composable
fun ModalGerenciarEquipe(onDismiss: () -> Unit) {
    val api = ApiProvider.painelApi
    val scope = rememberCoroutineScope()

    var usuarios by remember { mutableStateOf(emptyList<UsuarioDto>()) }
    var carregando by remember { mutableStateOf(true) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var mostrarFormularioNovo by remember { mutableStateOf(false) }

    fun carregarLista() {
        scope.launch {
            try {
                carregando = true
                usuarios = api.listarUsuarios()
            } catch (e: Exception) {
                mensagem = e.message
            } finally {
                carregando = false
            }
        }
    }

    LaunchedEffect(Unit) { carregarLista() }

    if (mostrarFormularioNovo) {
        NovoUsuarioModal(
            onDismiss = { mostrarFormularioNovo = false },
            onSave = { dto, aoFalhar ->
                scope.launch {
                    try {
                        api.criarUsuario(dto)
                        mostrarFormularioNovo = false
                        carregarLista()
                    } catch (e: Exception) {
                        aoFalhar(e.message ?: "Erro ao criar usuário.")
                    }
                }
            },
        )
        return
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 700.dp).fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Equipe da loja", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = { mostrarFormularioNovo = true }, colors = ButtonDefaults.buttonColors(containerColor = Vinho)) {
                        Text("Novo usuário")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                mensagem?.let { Text(it, color = Vinho); Spacer(Modifier.height(8.dp)) }

                if (carregando) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Vinho) }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(usuarios) { usuario ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(usuario.nome, fontWeight = FontWeight.Bold)
                                    Text("${usuario.email} · ${usuario.papel.rotulo}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (usuario.id != Session.usuarioLogado?.id) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                api.removerUsuario(usuario.id)
                                                carregarLista()
                                            } catch (e: Exception) {
                                                mensagem = e.message
                                            }
                                        }
                                    }) { Icon(Icons.Default.Delete, "Excluir", tint = Vinho) }
                                } else {
                                    Text("(Você)", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(0.3f))
                        }
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fechar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovoUsuarioModal(onDismiss: () -> Unit, onSave: (NovoUsuarioDto, (String) -> Unit) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var papel by remember { mutableStateOf(Papel.ATENDENTE) }
    var menuAberto by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    // Lista só os papéis que o usuário logado pode criar (o servidor confere de novo)
    val papeisPermitidos = when (Session.usuarioLogado?.papel) {
        Papel.DONO -> Papel.DA_LOJA
        Papel.GERENTE -> listOf(Papel.ATENDENTE, Papel.COZINHA, Papel.ENTREGADOR)
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo usuário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") })
                OutlinedTextField(
                    value = senha, onValueChange = { senha = it },
                    label = { Text("Senha (mín. ${Formatos.SENHA_TAMANHO_MINIMO} caracteres)") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                ExposedDropdownMenuBox(expanded = menuAberto, onExpandedChange = { menuAberto = it }) {
                    OutlinedTextField(
                        value = papel.rotulo, onValueChange = {}, readOnly = true, label = { Text("Papel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAberto) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        papeisPermitidos.forEach { opcao ->
                            DropdownMenuItem(text = { Text(opcao.rotulo) }, onClick = { papel = opcao; menuAberto = false })
                        }
                    }
                }
                mensagem?.let { Text(it, color = Vinho) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    mensagem = when {
                        nome.isBlank() -> "Informe o nome."
                        !Formatos.emailValido(email) -> "Informe um e-mail válido."
                        !Formatos.senhaValida(senha) -> "A senha precisa ter pelo menos ${Formatos.SENHA_TAMANHO_MINIMO} caracteres."
                        else -> null
                    }
                    if (mensagem == null) onSave(NovoUsuarioDto(nome, email, senha, papel)) { mensagem = it }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Vinho),
            ) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Vinho) } },
    )
}
