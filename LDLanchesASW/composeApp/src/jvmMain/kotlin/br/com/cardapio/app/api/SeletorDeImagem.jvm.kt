package br.com.cardapio.app.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import br.com.cardapio.app.data.ArquivoSelecionado
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberSeletorDeImagem(aoEscolher: (ArquivoSelecionado) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(aoEscolher)
    return remember {
        {
            val dialogo = FileDialog(null as Frame?, "Escolha uma imagem", FileDialog.LOAD).apply {
                setFilenameFilter { _, nome -> nome.lowercase().let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".webp") } }
                isVisible = true
            }
            val nome = dialogo.file
            val pasta = dialogo.directory
            if (nome != null && pasta != null) {
                val arquivo = File(pasta, nome)
                callback(ArquivoSelecionado(arquivo.name, arquivo.readBytes()))
            }
        }
    }
}
