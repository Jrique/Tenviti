package br.com.tenviti.app.telas

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tenviti.app.data.Session
import br.com.tenviti.app.getPlatform
import br.com.tenviti.app.telas.componentes.tela.telaConfiguracoes.*

@Composable
fun TelaConfiguracoes() {
    val scrollState = rememberScrollState()
    val isMobile = getPlatform().name.contains("Android")
    val usuario = Session.usuarioLogado

    var mostrarModalSenha by remember { mutableStateOf(false) }
    var mostrarModalEquipe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TenvitiTema.cores.fundo)
            .verticalScroll(scrollState)
            .padding(horizontal = if (isMobile) 16.dp else 32.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        CardPerfil()
        Spacer(modifier = Modifier.height(24.dp))
        CardAcoes(
            mostrarEquipe = usuario?.gerenciaEquipe == true,
            onAlterarSenhaClick = { mostrarModalSenha = true },
            onEquipeClick = { mostrarModalEquipe = true },
        )
    }

    if (mostrarModalSenha) ModalAlterarSenha(onDismiss = { mostrarModalSenha = false })
    if (mostrarModalEquipe) ModalGerenciarEquipe(onDismiss = { mostrarModalEquipe = false })
}
