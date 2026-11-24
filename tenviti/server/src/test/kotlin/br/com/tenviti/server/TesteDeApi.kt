package br.com.tenviti.server

import br.com.tenviti.server.api.JsonApi
import br.com.tenviti.server.infra.banco.ConfigBanco
import br.com.tenviti.server.infra.seguranca.ConfigJwt
import br.com.tenviti.shared.LoginRequisicao
import br.com.tenviti.shared.LojaDto
import br.com.tenviti.shared.NovaLojaDto
import br.com.tenviti.shared.SalvarProdutoDto
import br.com.tenviti.shared.SessaoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.sql.DriverManager
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

/**
 * Base dos testes de integração da API contra um PostgreSQL real.
 * Cada teste começa com o banco zerado (schema recriado + migrações).
 */
abstract class TesteDeApi {
    protected val banco = ConfigBanco(
        url = System.getenv("DB_URL_TESTE"),
        usuario = System.getenv("DB_USUARIO_TESTE"),
        senha = System.getenv("DB_SENHA_TESTE"),
        maxConexoes = 4,
    )

    protected val config = Configuracao(
        porta = 0,
        banco = banco,
        jwt = ConfigJwt(segredo = "segredo-de-teste-com-mais-de-32-caracteres!!"),
        origensCors = listOf("http://localhost:8080"),
        diretorioVitrineWeb = File("../../vitrine"),
        diretorioMidia = kotlin.io.path.createTempDirectory("midia-teste").toFile(),
        superAdminEmail = SUPER_EMAIL,
        superAdminSenha = SUPER_SENHA,
    )

    @BeforeTest
    fun zerarBanco() {
        DriverManager.getConnection(banco.url, banco.usuario, banco.senha).use { c ->
            c.createStatement().use { it.execute("drop schema public cascade; create schema public;") }
        }
    }

    protected fun cenario(
        configuracao: Configuracao = config,
        bloco: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
    ) = testApplication {
        application { modulo(configuracao) }
        val cliente = createClient { install(ContentNegotiation) { json(JsonApi) } }
        bloco(cliente)
    }

    protected suspend fun HttpClient.login(email: String, senha: String): HttpResponse =
        post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequisicao(email, senha))
        }

    protected suspend fun HttpClient.token(email: String, senha: String): String {
        val r = login(email, senha)
        assertEquals(HttpStatusCode.OK, r.status, r.bodyAsText())
        return r.body<SessaoDto>().token
    }

    protected suspend fun HttpClient.criarLoja(
        tokenSuper: String,
        slug: String,
        donoEmail: String,
        modulos: Set<br.com.tenviti.shared.Modulo> = br.com.tenviti.shared.Modulo.PADRAO,
    ): LojaDto {
        val r = post("/api/admin/lojas") {
            bearerAuth(tokenSuper)
            contentType(ContentType.Application.Json)
            setBody(
                NovaLojaDto(
                    slug = slug, nome = "Loja $slug", donoNome = "Dono $slug", donoEmail = donoEmail,
                    donoSenha = SENHA_DONO, modulos = modulos,
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, r.status, r.bodyAsText())
        return r.body()
    }

    protected suspend fun HttpClient.criarProduto(token: String, nome: String, precoCentavos: Long = 2590): HttpResponse =
        criarProduto(token, SalvarProdutoDto(nome = nome, precoCentavos = precoCentavos))

    protected suspend fun HttpClient.criarProduto(token: String, dto: SalvarProdutoDto): HttpResponse =
        post("/api/painel/produtos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

    companion object {
        const val SUPER_EMAIL = "super@tenviti.dev"
        const val SUPER_SENHA = "senha-super-forte"
        const val SENHA_DONO = "senha-do-dono"
    }
}
