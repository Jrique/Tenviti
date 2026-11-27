package br.com.tenviti.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.ui.FonteTitulo
import br.com.tenviti.app.ui.LogoTenviti
import br.com.tenviti.app.ui.Ponto
import br.com.tenviti.app.ui.TenvitiTema

@Composable
fun TelaLogin(onLoginSuccess: () -> Unit) {
    val vm = viewModel { AuthViewModel() }
    val c = TenvitiTema.cores
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.erro) { vm.erro?.let { snackbarHostState.showSnackbar(it) } }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.fundo)) {
        val largo = maxWidth >= 900.dp
        Row(Modifier.fillMaxSize()) {
            if (largo) {
                // Lado da marca
                Column(
                    Modifier.weight(1f).fillMaxHeight().background(c.menuFundo).padding(56.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LogoTenviti(tamanho = 28.sp, sobreEscuro = true)
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            "Tem. Vi. Tive.",
                            fontFamily = FonteTitulo, fontSize = 52.sp, color = c.menuTextoAtivo,
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Text(
                            "A loja tem, o cliente vê, o pedido acontece. Vitrine, pedidos e encomendas num lugar só.",
                            style = MaterialTheme.typography.bodyLarge, color = c.menuTexto, modifier = Modifier.widthIn(max = 460.dp),
                        )
                        listOf(
                            "Vitrine com a cara da sua loja",
                            "Pedidos e encomendas organizados por status",
                            "Preço sempre calculado certo, sem erro de conta",
                        ).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Ponto(c.menuVerde, 8.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(item, style = MaterialTheme.typography.bodyMedium, color = c.menuTexto)
                            }
                        }
                    }
                    Text("tenviti.com.br", fontFamily = FonteTitulo, style = MaterialTheme.typography.labelMedium, color = c.menuTextoSuave)
                }
            }

            // Formulário
            Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!largo) LogoTenviti(tamanho = 30.sp, comSlogan = true)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Entrar no painel", style = MaterialTheme.typography.headlineMedium, color = c.texto)
                        Text("Use o e-mail e a senha da sua loja.", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    }
                    OutlinedTextField(
                        value = vm.login, onValueChange = { vm.login = it },
                        label = { Text("E-mail") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = vm.senha, onValueChange = { vm.senha = it },
                        label = { Text("Senha") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!vm.carregando) vm.realizarLogin(onLoginSuccess) }),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { vm.realizarLogin(onLoginSuccess) },
                        enabled = !vm.carregando,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        if (vm.carregando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.sobreBase)
                        else Text("Entrar", style = MaterialTheme.typography.labelLarge)
                    }
                    Text("Não tem acesso? Peça ao responsável pela sua loja.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
