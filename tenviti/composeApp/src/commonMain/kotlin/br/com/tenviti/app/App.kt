package br.com.tenviti.app

import androidx.compose.runtime.*
import br.com.tenviti.app.data.Session
import br.com.tenviti.app.telas.TelaLogin
import br.com.tenviti.app.telas.TelaLojas
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val usuario = Session.usuarioLogado

    if (usuario == null) {
        // Session é estado do Compose: ao logar, esta função recompõe sozinha.
        TelaLogin(onLoginSuccess = {})
        return
    }

    if (usuario.ehSuperAdmin) {
        TelaLojas(onLogout = Session::encerrar)
        return
    }

    var telaAtual by remember(usuario.id) { mutableStateOf(Tela.inicialPara(usuario)) }
    val onTelaSelecionada: (Tela) -> Unit = { novaTela -> telaAtual = novaTela }
    val onLogout: () -> Unit = { Session.encerrar() }

    if (rememberWindowSize() == WindowSize.COMPACT) {
        LayoutEstiloAndroid(telaAtual, onTelaSelecionada, onLogout)
    } else {
        LayoutEstiloDesktop(telaAtual, onTelaSelecionada, onLogout)
    }
}
