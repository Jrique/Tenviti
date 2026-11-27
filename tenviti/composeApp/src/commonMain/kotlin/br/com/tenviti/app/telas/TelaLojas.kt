package br.com.tenviti.app.telas

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.api.apiBaseUrl
import br.com.tenviti.app.telas.componentes.EditorAparencia
import br.com.tenviti.app.telas.componentes.corDoHex
import br.com.tenviti.shared.AtualizarLojaDto
import br.com.tenviti.shared.Formatos
import br.com.tenviti.shared.ItemPersonalizavel
import br.com.tenviti.shared.LojaDto
import br.com.tenviti.shared.Modulo
import br.com.tenviti.shared.NovaLojaDto
import kotlinx.coroutines.launch

private val Vinho: Color @Composable get() = TenvitiTema.cores.base

/** Painel do super admin: cadastra lojas, liga módulos e define o que cada dono pode personalizar. */
@Composable
fun TelaLojas(onLogout: () -> Unit) {
    val api = ApiProvider.painelApi
    val escopo = rememberCoroutineScope()

    var lojas by remember { mutableStateOf(emptyList<LojaDto>()) }
    var carregando by remember { mutableStateOf(true) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var criando by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<LojaDto?>(null) }

    fun recarregar() {
        escopo.launch {
            try {
                carregando = true
                lojas = api.listarLojas()
            } catch (e: Exception) {
                mensagem = e.message
            } finally {
                carregando = false
            }
        }
    }
    LaunchedEffect(Unit) { recarregar() }

    Column(Modifier.fillMaxSize().background(TenvitiTema.cores.fundo).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Lojas", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { criando = true }, colors = ButtonDefaults.buttonColors(containerColor = Vinho)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Nova loja")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Sair") }
        }
        Spacer(Modifier.height(16.dp))
        mensagem?.let { Text(it, color = Vinho); Spacer(Modifier.height(8.dp)) }

        if (carregando) {
            CircularProgressIndicator(color = Vinho)
        } else if (lojas.isEmpty()) {
            Text("Nenhuma loja cadastrada ainda. Clique em \"Nova loja\".", color = TenvitiTema.cores.textoSuave)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(lojas, key = { it.id }) { loja -> CartaoLoja(loja) { editando = loja } }
            }
        }
    }

    if (criando) {
        DialogNovaLoja(
            onDismiss = { criando = false },
            onSalvar = { dto, aoFalhar ->
                escopo.launch {
                    try {
                        api.criarLoja(dto)
                        criando = false
                        recarregar()
                    } catch (e: Exception) {
                        aoFalhar(e.message ?: "Erro ao criar loja.")
                    }
                }
            },
        )
    }

    editando?.let { loja ->
        DialogEditarLoja(
            loja = loja,
            onDismiss = { editando = null },
            onSalvar = { dto, aoFalhar ->
                escopo.launch {
                    try {
                        api.atualizarLoja(loja.id, dto)
                        editando = null
                        recarregar()
                    } catch (e: Exception) {
                        aoFalhar(e.message ?: "Erro ao salvar loja.")
                    }
                }
            },
        )
    }
}

@Composable
private fun CartaoLoja(loja: LojaDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(corDoHex(loja.aparencia.corPrimaria) ?: Vinho, RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(loja.nome, fontWeight = FontWeight.Bold)
                Text("${apiBaseUrl()}/${loja.slug}/", style = MaterialTheme.typography.bodySmall, color = TenvitiTema.cores.textoSuave)
                Text(loja.modulos.joinToString { it.rotulo }, style = MaterialTheme.typography.bodySmall)
            }
            AssistChip(onClick = onClick, label = { Text(if (loja.ativa) "Ativa" else "Desativada") })
        }
    }
}

@Composable
private fun DialogNovaLoja(onDismiss: () -> Unit, onSalvar: (NovaLojaDto, (String) -> Unit) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var slugEditado by remember { mutableStateOf(false) }
    var donoNome by remember { mutableStateOf("") }
    var donoEmail by remember { mutableStateOf("") }
    var donoSenha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova loja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nome, onValueChange = {
                    nome = it
                    if (!slugEditado) slug = Formatos.sugerirSlug(it)
                }, label = { Text("Nome da loja") })
                OutlinedTextField(
                    value = slug, onValueChange = { slug = it.lowercase(); slugEditado = true },
                    label = { Text("Endereço do cardápio") },
                    supportingText = { Text("${apiBaseUrl()}/$slug/") },
                    isError = slug.isNotEmpty() && !Formatos.slugValido(slug),
                )
                HorizontalDivider()
                Text("Acesso do dono", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = donoNome, onValueChange = { donoNome = it }, label = { Text("Nome do dono") })
                OutlinedTextField(
                    value = donoEmail, onValueChange = { donoEmail = it }, label = { Text("E-mail do dono") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = donoSenha, onValueChange = { donoSenha = it },
                    label = { Text("Senha inicial (mín. ${Formatos.SENHA_TAMANHO_MINIMO})") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                mensagem?.let { Text(it, color = Vinho) }
            }
        },
        confirmButton = {
            Button(onClick = {
                mensagem = when {
                    nome.isBlank() -> "Informe o nome da loja."
                    !Formatos.slugValido(slug) -> "Endereço inválido: use letras minúsculas, números e hífen (mín. 3)."
                    donoNome.isBlank() -> "Informe o nome do dono."
                    !Formatos.emailValido(donoEmail) -> "Informe um e-mail válido para o dono."
                    !Formatos.senhaValida(donoSenha) -> "A senha precisa ter pelo menos ${Formatos.SENHA_TAMANHO_MINIMO} caracteres."
                    else -> null
                }
                if (mensagem == null) onSalvar(NovaLojaDto(slug, nome, donoNome, donoEmail, donoSenha)) { mensagem = it }
            }, colors = ButtonDefaults.buttonColors(containerColor = Vinho)) { Text("Criar loja") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Vinho) } },
    )
}

@Composable
private fun DialogEditarLoja(loja: LojaDto, onDismiss: () -> Unit, onSalvar: (AtualizarLojaDto, (String) -> Unit) -> Unit) {
    var nome by remember { mutableStateOf(loja.nome) }
    var slug by remember { mutableStateOf(loja.slug) }
    var ativa by remember { mutableStateOf(loja.ativa) }
    var fuso by remember { mutableStateOf(loja.fusoHorario) }
    var modulos by remember { mutableStateOf(loja.modulos) }
    var liberados by remember { mutableStateOf(loja.personalizacaoLiberada) }
    var aparencia by remember { mutableStateOf(loja.aparencia) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 900.dp).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = TenvitiTema.cores.fundo,
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Editar ${loja.nome}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = slug, onValueChange = { slug = it.lowercase() }, label = { Text("Endereço") },
                            isError = !Formatos.slugValido(slug), modifier = Modifier.weight(1f),
                        )
                        Text("Ativa")
                        Switch(checked = ativa, onCheckedChange = { ativa = it })
                    }
                    OutlinedTextField(value = fuso, onValueChange = { fuso = it }, label = { Text("Fuso horário (ex.: America/Sao_Paulo)") })

                    Text("Módulos ligados", fontWeight = FontWeight.Bold)
                    GradeDeCaixas(Modulo.entries, { it.rotulo }, modulos) { modulos = it }

                    Text("O que o dono pode personalizar sozinho", fontWeight = FontWeight.Bold)
                    GradeDeCaixas(ItemPersonalizavel.entries, { it.rotulo }, liberados) { liberados = it }

                    HorizontalDivider()
                    Text("Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    EditorAparencia(aparencia, liberado = { true }, onChange = { aparencia = it })
                }
                mensagem?.let { Text(it, color = Vinho) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = Vinho) }
                    Button(
                        onClick = { onSalvar(AtualizarLojaDto(slug, nome, ativa, fuso, aparencia, modulos, liberados)) { mensagem = it } },
                        colors = ButtonDefaults.buttonColors(containerColor = Vinho),
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> GradeDeCaixas(opcoes: List<T>, rotulo: (T) -> String, selecionados: Set<T>, onChange: (Set<T>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { opcao ->
            FilterChip(
                selected = opcao in selecionados,
                onClick = { onChange(if (opcao in selecionados) selecionados - opcao else selecionados + opcao) },
                label = { Text(rotulo(opcao)) },
            )
        }
    }
}
