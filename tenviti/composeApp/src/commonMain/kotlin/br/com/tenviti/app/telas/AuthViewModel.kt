package br.com.tenviti.app.telas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.data.Session
import kotlinx.coroutines.launch

/** Login do painel. Contas são criadas pelo super admin (lojas) ou pelo dono/gerente (equipe), nunca por cadastro aberto. */
class AuthViewModel : ViewModel() {
    private val api = ApiProvider.painelApi

    var login by mutableStateOf("")
    var senha by mutableStateOf("")

    var carregando by mutableStateOf(false)
    var erro by mutableStateOf<String?>(null)

    fun realizarLogin(onSuccess: () -> Unit) {
        if (login.isBlank() || senha.isBlank()) {
            erro = "Informe e-mail e senha."
            return
        }
        viewModelScope.launch {
            try {
                carregando = true
                erro = null
                Session.iniciar(api.login(login, senha))
                senha = ""
                onSuccess()
            } catch (e: Exception) {
                erro = e.message ?: "Não foi possível entrar. Tente novamente."
            } finally {
                carregando = false
            }
        }
    }
}
