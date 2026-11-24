package br.com.tenviti.server.infra.arquivos

import br.com.tenviti.server.dominio.ArmazenamentoArquivos
import java.io.File
import java.util.UUID

/**
 * Guarda as imagens no disco do servidor, em <diretorio>/<lojaId>/<uuid>.<ext>, servidas em /midia/...
 * Para usar nuvem (S3/R2) no futuro, basta outra implementação de [ArmazenamentoArquivos].
 */
class ArmazenamentoEmDisco(private val diretorio: File) : ArmazenamentoArquivos {
    init {
        diretorio.mkdirs()
    }

    override fun salvar(lojaId: Long, extensao: String, conteudo: ByteArray): String {
        val pasta = File(diretorio, lojaId.toString()).apply { mkdirs() }
        val nome = "${UUID.randomUUID()}.$extensao"
        File(pasta, nome).writeBytes(conteudo)
        return "/midia/$lojaId/$nome"
    }
}
