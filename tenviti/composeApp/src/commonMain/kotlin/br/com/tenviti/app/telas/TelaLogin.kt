package br.com.tenviti.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.ui.LogoTenviti

@Composable
fun TelaLogin(onLoginSuccess: () -> Unit) {
    val vm = viewModel { AuthViewModel() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.erro) {
        vm.erro?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F6)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(400.dp).padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9E6))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LogoTenviti(tamanho = 40.sp, comSlogan = true)

                Text("Entrar no painel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = vm.login,
                    onValueChange = { vm.login = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vm.senha,
                    onValueChange = { vm.senha = it },
                    label = { Text("Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (vm.carregando) CircularProgressIndicator(color = Color(0xFF790001))

                Button(
                    onClick = { vm.realizarLogin(onLoginSuccess) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001)),
                    enabled = !vm.carregando
                ) {
                    Text("Entrar")
                }

                Text(
                    "Não tem acesso? Peça ao responsável pela sua loja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}