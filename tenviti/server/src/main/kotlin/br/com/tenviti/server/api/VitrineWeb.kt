package br.com.tenviti.server.api

import br.com.tenviti.server.aplicacao.ServicoLojas
import br.com.tenviti.shared.Formatos
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

/**
 * Serve a vitrine web de cada loja por caminho: /thai-cookies/ → vitrine/index.html.
 * O JavaScript descobre o slug pelo primeiro trecho da URL e chama /api/public/lojas/{slug}.
 */
fun Route.rotasVitrineWeb(diretorio: File, lojas: ServicoLojas) {
    val raiz = diretorio.canonicalFile

    get("/") { call.respondText("Tenviti — Tem. Vi. Tive. Acesse /{endereco-da-loja}/") }

    get("/{slug}") {
        val slug = call.parameters["slug"].orEmpty()
        if (!Formatos.slugValido(slug)) return@get call.respondText("Página não encontrada.", status = HttpStatusCode.NotFound)
        call.respondRedirect("/$slug/")
    }

    get("/{slug}/{caminho...}") {
        val slug = call.parameters["slug"].orEmpty()
        if (!Formatos.slugValido(slug)) return@get call.respondText("Página não encontrada.", status = HttpStatusCode.NotFound)

        val relativo = call.parameters.getAll("caminho").orEmpty().joinToString("/").ifEmpty { "index.html" }
        val arquivo = File(raiz, relativo).canonicalFile

        // Impede sair da pasta da vitrine (ex.: /loja/../../etc/passwd)
        if (!arquivo.path.startsWith(raiz.path + File.separator) || !arquivo.isFile) {
            return@get call.respondText("Página não encontrada.", status = HttpStatusCode.NotFound)
        }
        // Páginas só são servidas para lojas que existem e estão ativas
        if (arquivo.extension == "html") lojas.lojaPublica(slug)

        call.respondFile(arquivo)
    }
}

/** Imagens enviadas pelas lojas: /midia/{lojaId}/{arquivo}. Nomes são UUIDs, então podem ficar em cache por muito tempo. */
fun Route.rotasMidia(diretorio: File) {
    val raiz = diretorio.canonicalFile
    get("/midia/{lojaId}/{arquivo}") {
        val lojaId = call.parameters["lojaId"]?.toLongOrNull()
        val nome = call.parameters["arquivo"].orEmpty()
        val arquivo = File(raiz, "$lojaId/$nome").canonicalFile
        if (lojaId == null || !arquivo.path.startsWith(raiz.path + File.separator) || !arquivo.isFile) {
            return@get call.respondText("Arquivo não encontrado.", status = HttpStatusCode.NotFound)
        }
        call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
        call.respondFile(arquivo)
    }
}
