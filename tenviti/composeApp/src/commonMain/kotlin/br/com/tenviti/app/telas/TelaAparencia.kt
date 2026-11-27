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
import br.com.tenviti.app.api.ApiProvider
import androidx.compose.ui.Alignment
import br.com.tenviti.app.api.apiBaseUrl
import br.com.tenviti.app.api.rememberSeletorDeImagem
import br.com.tenviti.app.data.ArquivoSelecionado
import br.com.tenviti.shared.ItemPersonalizavel
import br.com.tenviti.app.telas.componentes.EditorAparencia
import br.com.tenviti.shared.AparenciaDto
import br.com.tenviti.shared.LojaDto
import kotlinx.coroutines.launch

/** O dono personaliza o cardápio da loja dentro do que o super admin liberou. */
@Composable
fun TelaAparencia() {
    val api = ApiProvider.painelApi
    val escopo = rememberCoroutineScope()

    var loja by remember { mutableStateOf<LojaDto?>(null) }
    var aparencia by remember { mutableStateOf<AparenciaDto?>(null) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            loja = api.minhaLoja().also { aparencia = it.aparencia }
        } catch (e: Exception) {
            mensagem = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(TenvitiTema.cores.fundo).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Aparência da loja", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        val atual = loja
        val ap = aparencia
        if (atual == null || ap == null) {
            if (mensagem == null) CircularProgressIndicator(color = TenvitiTema.cores.base) else Text(mensagem!!, color = TenvitiTema.cores.base)
            return@Column
        }

        Text("Seu cardápio online: ${apiBaseUrl()}/${atual.slug}/", color = TenvitiTema.cores.textoSuave)

        // Envio de logo e banner (o endereço entra no campo correspondente)
        var enviando by remember { mutableStateOf(false) }
        fun enviar(arquivo: ArquivoSelecionado, aplicar: (AparenciaDto, String) -> AparenciaDto) {
            escopo.launch {
                try {
                    enviando = true
                    val url = api.enviarImagem(arquivo)
                    aparencia = aparencia?.let { aplicar(it, url) }
                    mensagem = "Imagem enviada. Clique em \"Salvar aparência\" para publicar."
                } catch (e: Exception) {
                    mensagem = e.message
                } finally {
                    enviando = false
                }
            }
        }
        val escolherLogo = rememberSeletorDeImagem { enviar(it) { ap, url -> ap.copy(logoUrl = url) } }
        val escolherBanner = rememberSeletorDeImagem { enviar(it) { ap, url -> ap.copy(bannerUrl = url) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = escolherLogo, enabled = !enviando && ItemPersonalizavel.LOGO in atual.personalizacaoLiberada) { Text("Enviar logo") }
            OutlinedButton(onClick = escolherBanner, enabled = !enviando && ItemPersonalizavel.BANNERS_E_TEXTOS in atual.personalizacaoLiberada) { Text("Enviar banner") }
            if (enviando) CircularProgressIndicator(color = TenvitiTema.cores.base, modifier = Modifier.size(24.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = TenvitiTema.cores.superficie)) {
            Column(Modifier.padding(16.dp)) {
                EditorAparencia(ap, liberado = { it in atual.personalizacaoLiberada }, onChange = { aparencia = it })
            }
        }

        mensagem?.let { Text(it, color = TenvitiTema.cores.base) }

        Button(
            onClick = {
                escopo.launch {
                    try {
                        salvando = true
                        mensagem = null
                        loja = api.personalizarLoja(ap).also { aparencia = it.aparencia }
                        mensagem = "Aparência salva. Recarregue o cardápio para ver as mudanças."
                    } catch (e: Exception) {
                        mensagem = e.message
                    } finally {
                        salvando = false
                    }
                }
            },
            enabled = !salvando,
            colors = ButtonDefaults.buttonColors(containerColor = TenvitiTema.cores.base),
        ) { Text(if (salvando) "Salvando..." else "Salvar aparência") }
    }
}
