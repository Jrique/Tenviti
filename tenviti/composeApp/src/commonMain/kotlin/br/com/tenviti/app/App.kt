package br.com.tenviti.app

import androidx.compose.runtime.*
import br.com.tenviti.app.data.Session
import br.com.tenviti.app.telas.TelaLogin
import br.com.tenviti.app.telas.TelaLojas
import br.com.tenviti.app.ui.TemaTenviti
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    TemaTenviti {
        val usuario = Session.usuarioLogado
        when {
            // Session é estado do Compose: ao logar, esta função recompõe sozinha.
            usuario == null -> TelaLogin(onLoginSuccess = {})
            usuario.ehSuperAdmin -> TelaLojas(onLogout = Session::encerrar)
            else -> MolduraPainel(usuario, onLogout = Session::encerrar)
        }
    }
}
