package br.com.cardapio.app.api

import br.com.cardapio.app.data.Session
import br.com.cardapio.shared.ErroDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Erro devolvido pela API, já com a mensagem pronta para mostrar ao usuário. */
class ErroApi(val status: Int, mensagem: String, val codigo: String?) : Exception(mensagem)

private val jsonApi = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Valida respostas: erro vira [ErroApi] com a mensagem do servidor; 401 encerra a sessão. */
private fun io.ktor.client.HttpClientConfig<*>.validarRespostas() {
    HttpResponseValidator {
        validateResponse { resposta ->
            if (resposta.status.isSuccess()) return@validateResponse
            val erro = runCatching { jsonApi.decodeFromString<ErroDto>(resposta.bodyAsText()) }.getOrNull()
            // Token expirado/inválido: volta para a tela de login
            if (resposta.status == HttpStatusCode.Unauthorized && Session.usuarioLogado != null) {
                Session.encerrar()
            }
            throw ErroApi(
                status = resposta.status.value,
                mensagem = erro?.mensagem ?: "Não foi possível completar a operação (erro ${resposta.status.value}).",
                codigo = erro?.codigo,
            )
        }
    }
}

/** Cliente para envio de arquivos (multipart): não força Content-Type JSON. */
fun createUploadClient() = HttpClient {
    install(ContentNegotiation) { json(jsonApi) }
    validarRespostas()
    defaultRequest {
        Session.usuarioLogado?.token?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}

fun createHttpClient() = HttpClient {
    install(ContentNegotiation) { json(jsonApi) }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
        sanitizeHeader { it == HttpHeaders.Authorization } // nunca registrar o token
    }

    validarRespostas()

    defaultRequest {
        contentType(ContentType.Application.Json)
        Session.usuarioLogado?.token?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}
