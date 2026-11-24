package br.com.cardapio.server.aplicacao

import br.com.cardapio.server.dominio.ArmazenamentoArquivos
import br.com.cardapio.server.dominio.Ator
import br.com.cardapio.server.dominio.DadoInvalido
import br.com.cardapio.shared.ArquivoDto
import br.com.cardapio.shared.Papel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Envio de imagens (fotos de produto, logo, banner) pelo painel. */
class ServicoArquivos(private val armazenamento: ArmazenamentoArquivos) {

    suspend fun enviarImagem(ator: Ator, conteudo: ByteArray): ArquivoDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val lojaId = ator.lojaObrigatoria()
        if (conteudo.isEmpty()) throw DadoInvalido("O arquivo está vazio.")
        if (conteudo.size > TAMANHO_MAXIMO) throw DadoInvalido("A imagem pode ter no máximo 5 MB.", "arquivo_grande")
        val extensao = TipoImagem.detectar(conteudo)
            ?: throw DadoInvalido("Formato não aceito. Envie uma imagem PNG, JPG ou WEBP.", "formato_invalido")
        val url = withContext(Dispatchers.IO) { armazenamento.salvar(lojaId, extensao, conteudo) }
        return ArquivoDto(url)
    }

    companion object {
        const val TAMANHO_MAXIMO = 5 * 1024 * 1024
    }
}

/**
 * Identifica o tipo real da imagem pelos primeiros bytes (não confia no nome nem no Content-Type enviados).
 * SVG não é aceito em upload porque pode conter scripts.
 */
object TipoImagem {
    fun detectar(b: ByteArray): String? = when {
        b.size >= 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte() -> "png"
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "jpg"
        b.size >= 12 && String(b, 0, 4, Charsets.US_ASCII) == "RIFF" && String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
        else -> null
    }
}
