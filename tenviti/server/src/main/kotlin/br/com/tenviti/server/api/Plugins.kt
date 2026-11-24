package br.com.tenviti.server.api

import br.com.tenviti.server.dominio.AcessoNegado
import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.server.dominio.Conflito
import br.com.tenviti.server.dominio.CredenciaisInvalidas
import br.com.tenviti.server.dominio.DadoInvalido
import br.com.tenviti.server.dominio.ErroDeNegocio
import br.com.tenviti.server.dominio.NaoEncontrado
import br.com.tenviti.server.infra.seguranca.Tokens
import br.com.tenviti.shared.ErroDto
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

const val AUTH_JWT = "jwt"

val JsonApi = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Principal do Ktor que carrega o ator autenticado. */
data class PrincipalAtor(val ator: Ator)

fun ApplicationCall.ator(): Ator =
    principal<PrincipalAtor>()?.ator ?: throw AcessoNegado("Faça login para continuar.", "nao_autenticado")

fun Application.configurarPlugins(tokens: Tokens, origensCors: List<String>) {
    install(ContentNegotiation) { json(JsonApi) }

    install(CallLogging) {
        level = Level.INFO
        filter { it.request.path().startsWith("/api") }
    }

    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        if ("*" in origensCors) {
            anyHost()
        } else {
            origensCors.forEach { origem ->
                val (esquema, host) = origem.split("://", limit = 2).let { if (it.size == 2) it[0] to it[1] else "https" to it[0] }
                allowHost(host, schemes = listOf(esquema))
            }
        }
    }

    install(Authentication) {
        jwt(AUTH_JWT) {
            verifier(tokens.verificador)
            validate { credencial ->
                val p = credencial.payload
                Tokens.atorDe(p.subject, p.getClaim(Tokens.CLAIM_PAPEL).asString(), p.getClaim(Tokens.CLAIM_LOJA).asLong())
                    ?.let { PrincipalAtor(it) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErroDto("Sua sessão expirou ou é inválida. Faça login novamente.", "nao_autenticado"))
            }
        }
    }

    val log = LoggerFactory.getLogger("Erros")
    install(StatusPages) {
        exception<ErroDeNegocio> { call, e ->
            val status = when (e) {
                is DadoInvalido -> HttpStatusCode.UnprocessableEntity
                is NaoEncontrado -> HttpStatusCode.NotFound
                is AcessoNegado -> if (e.codigo == "nao_autenticado") HttpStatusCode.Unauthorized else HttpStatusCode.Forbidden
                is Conflito -> HttpStatusCode.Conflict
                is CredenciaisInvalidas -> HttpStatusCode.Unauthorized
            }
            call.respond(status, ErroDto(e.message ?: "Erro.", e.codigo))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErroDto("Os dados enviados estão incompletos ou em formato inválido.", "requisicao_invalida"))
        }
        exception<Throwable> { call, e ->
            log.error("Erro inesperado em {}", call.request.path(), e)
            call.respond(HttpStatusCode.InternalServerError, ErroDto("Ocorreu um erro inesperado. Tente novamente.", "erro_interno"))
        }
    }
}
