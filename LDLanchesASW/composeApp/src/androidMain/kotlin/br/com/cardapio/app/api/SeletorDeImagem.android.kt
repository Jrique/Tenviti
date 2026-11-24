package br.com.cardapio.app.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import br.com.cardapio.app.data.ArquivoSelecionado

@Composable
actual fun rememberSeletorDeImagem(aoEscolher: (ArquivoSelecionado) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val callback by rememberUpdatedState(aoEscolher)
    val lancador = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) callback(ArquivoSelecionado(uri.lastPathSegment ?: "imagem", bytes))
        }
    }
    return { lancador.launch("image/*") }
}
