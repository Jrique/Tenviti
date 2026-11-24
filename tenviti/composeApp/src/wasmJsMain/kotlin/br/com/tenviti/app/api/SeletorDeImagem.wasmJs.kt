package br.com.tenviti.app.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import br.com.tenviti.app.data.ArquivoSelecionado
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

@Composable
actual fun rememberSeletorDeImagem(aoEscolher: (ArquivoSelecionado) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(aoEscolher)
    return remember {
        {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/png,image/jpeg,image/webp"
            input.onchange = {
                val arquivo = input.files?.item(0)
                if (arquivo != null) {
                    val leitor = FileReader()
                    leitor.onload = {
                        val dados = Int8Array(leitor.result as ArrayBuffer)
                        callback(ArquivoSelecionado(arquivo.name, ByteArray(dados.length) { dados[it] }))
                        null
                    }
                    leitor.readAsArrayBuffer(arquivo)
                }
                null
            }
            input.click()
        }
    }
}
