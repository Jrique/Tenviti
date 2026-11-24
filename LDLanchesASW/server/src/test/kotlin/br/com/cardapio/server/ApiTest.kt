package br.com.cardapio.server

import br.com.cardapio.server.api.JsonApi
import br.com.cardapio.server.infra.banco.ConfigBanco
import br.com.cardapio.server.infra.seguranca.ConfigJwt
import br.com.cardapio.shared.AlterarStatusDto
import br.com.cardapio.shared.AparenciaDto
import br.com.cardapio.shared.AtualizarLojaDto
import br.com.cardapio.shared.CategoriaDto
import br.com.cardapio.shared.ErroDto
import br.com.cardapio.shared.ItemPersonalizavel
import br.com.cardapio.shared.LoginRequisicao
import br.com.cardapio.shared.LojaDto
import br.com.cardapio.shared.NovaLojaDto
import br.com.cardapio.shared.NovoPedidoPublicoDto
import br.com.cardapio.shared.NovoUsuarioDto
import br.com.cardapio.shared.Papel
import br.com.cardapio.shared.PedidoDto
import br.com.cardapio.shared.PedidoPublicoDto
import br.com.cardapio.shared.ProdutoDto
import br.com.cardapio.shared.ResumoDashboardDto
import br.com.cardapio.shared.SalvarCategoriaDto
import br.com.cardapio.shared.SalvarProdutoDto
import br.com.cardapio.shared.SessaoDto
import br.com.cardapio.shared.StatusPedido
import br.com.cardapio.shared.VitrineLojaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.sql.DriverManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testes de integração da API contra um PostgreSQL real.
 * Cada teste começa com o banco zerado (schema recriado + migrações).
 */
class ApiTest : TesteDeApi() {
    // ---------- autenticação ----------

    @Test
    fun loginExigeSenhaCorreta() = cenario { cliente ->
        assertEquals(HttpStatusCode.OK, cliente.login(SUPER_EMAIL, SUPER_SENHA).status)

        val errada = cliente.login(SUPER_EMAIL, "senha-errada")
        assertEquals(HttpStatusCode.Unauthorized, errada.status)
        assertEquals("credenciais_invalidas", errada.body<ErroDto>().codigo)

        assertEquals(HttpStatusCode.Unauthorized, cliente.login("ninguem@exemplo.com", SUPER_SENHA).status)
    }

    @Test
    fun painelExigeToken() = cenario { cliente ->
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/api/painel/produtos").status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/api/painel/produtos") { bearerAuth("token-falso") }.status)
    }

    // ---------- lojas / multi-tenant ----------

    @Test
    fun superAdminCriaLojaEDonoConsegueEntrar() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        val loja = cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")

        val r = cliente.login("dono@ld.com", SENHA_DONO)
        assertEquals(HttpStatusCode.OK, r.status)
        val sessao = r.body<SessaoDto>()
        assertEquals(Papel.DONO, sessao.usuario.papel)
        assertEquals(loja.id, sessao.usuario.lojaId)
        assertEquals("ld-lanches", sessao.usuario.lojaSlug)
    }

    @Test
    fun somenteSuperAdminGerenciaLojas() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")
        val donoToken = cliente.token("dono@ld.com", SENHA_DONO)

        assertEquals(HttpStatusCode.Forbidden, cliente.get("/api/admin/lojas") { bearerAuth(donoToken) }.status)
    }

    @Test
    fun slugEEmailDuplicadosSaoRecusados() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")

        val mesmoSlug = cliente.post("/api/admin/lojas") {
            bearerAuth(superToken)
            contentType(ContentType.Application.Json)
            setBody(NovaLojaDto("ld-lanches", "Outra", "X", "outro@x.com", SENHA_DONO))
        }
        assertEquals(HttpStatusCode.Conflict, mesmoSlug.status)

        val mesmoEmail = cliente.post("/api/admin/lojas") {
            bearerAuth(superToken)
            contentType(ContentType.Application.Json)
            setBody(NovaLojaDto("outra-loja", "Outra", "X", "DONO@ld.com", SENHA_DONO))
        }
        assertEquals(HttpStatusCode.Conflict, mesmoEmail.status)
    }

    @Test
    fun umaLojaNaoEnxergaDadosDaOutra() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "loja-a", "a@a.com")
        cliente.criarLoja(superToken, "loja-b", "b@b.com")
        val tokenA = cliente.token("a@a.com", SENHA_DONO)
        val tokenB = cliente.token("b@b.com", SENHA_DONO)

        val produtoA = cliente.criarProduto(tokenA, "X-Tudo da A").body<ProdutoDto>()

        // B não vê o produto de A na listagem…
        val listaB = cliente.get("/api/painel/produtos") { bearerAuth(tokenB) }.body<List<ProdutoDto>>()
        assertTrue(listaB.isEmpty())
        // …nem consegue alterar pelo id
        val alterar = cliente.put("/api/painel/produtos/${produtoA.id}") {
            bearerAuth(tokenB)
            contentType(ContentType.Application.Json)
            setBody(SalvarProdutoDto(nome = "Hackeado", precoCentavos = 1))
        }
        assertEquals(HttpStatusCode.NotFound, alterar.status)

        // O cardápio público de cada loja só mostra o que é dela
        val publicoA = cliente.get("/api/public/lojas/loja-a/produtos").body<List<ProdutoDto>>()
        val publicoB = cliente.get("/api/public/lojas/loja-b/produtos").body<List<ProdutoDto>>()
        assertEquals(listOf("X-Tudo da A"), publicoA.map { it.nome })
        assertTrue(publicoB.isEmpty())
    }

    @Test
    fun produtoNaoPodeUsarCategoriaDeOutraLoja() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "loja-a", "a@a.com")
        cliente.criarLoja(superToken, "loja-b", "b@b.com")
        val tokenA = cliente.token("a@a.com", SENHA_DONO)
        val tokenB = cliente.token("b@b.com", SENHA_DONO)

        val categoriaB = cliente.post("/api/painel/categorias") {
            bearerAuth(tokenB)
            contentType(ContentType.Application.Json)
            setBody(SalvarCategoriaDto("Lanches"))
        }.body<CategoriaDto>()

        val r = cliente.post("/api/painel/produtos") {
            bearerAuth(tokenA)
            contentType(ContentType.Application.Json)
            setBody(SalvarProdutoDto(categoriaId = categoriaB.id, nome = "X", precoCentavos = 100))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, r.status)
    }

    // ---------- papéis ----------

    @Test
    fun atendenteNaoEditaCardapioEGerenteNaoCriaDono() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")
        val donoToken = cliente.token("dono@ld.com", SENHA_DONO)

        suspend fun criarUsuario(token: String, email: String, papel: Papel) = cliente.post("/api/painel/usuarios") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(NovoUsuarioDto("Fulano", email, SENHA_DONO, papel))
        }

        assertEquals(HttpStatusCode.Created, criarUsuario(donoToken, "atendente@ld.com", Papel.ATENDENTE).status)
        assertEquals(HttpStatusCode.Created, criarUsuario(donoToken, "gerente@ld.com", Papel.GERENTE).status)
        assertEquals(HttpStatusCode.Forbidden, criarUsuario(donoToken, "super2@ld.com", Papel.SUPER_ADMIN).status)

        val atendenteToken = cliente.token("atendente@ld.com", SENHA_DONO)
        assertEquals(HttpStatusCode.Forbidden, cliente.criarProduto(atendenteToken, "X").status)
        assertEquals(HttpStatusCode.OK, cliente.get("/api/painel/pedidos") { bearerAuth(atendenteToken) }.status)

        val gerenteToken = cliente.token("gerente@ld.com", SENHA_DONO)
        assertEquals(HttpStatusCode.Forbidden, criarUsuario(gerenteToken, "dono2@ld.com", Papel.DONO).status)
        assertEquals(HttpStatusCode.Created, cliente.criarProduto(gerenteToken, "X-Salada").status)
    }

    // ---------- personalização ----------

    @Test
    fun donoSoAlteraOQueFoiLiberado() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        val loja = cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")
        val donoToken = cliente.token("dono@ld.com", SENHA_DONO)

        // Cores estão liberadas por padrão
        val cores = cliente.put("/api/painel/loja/aparencia") {
            bearerAuth(donoToken)
            contentType(ContentType.Application.Json)
            setBody(loja.aparencia.copy(corPrimaria = "#123456"))
        }
        assertEquals(HttpStatusCode.OK, cores.status, cores.bodyAsText())
        assertEquals("#123456", cores.body<LojaDto>().aparencia.corPrimaria)

        // CSS extra não está liberado por padrão
        val css = cliente.put("/api/painel/loja/aparencia") {
            bearerAuth(donoToken)
            contentType(ContentType.Application.Json)
            setBody(cores.body<LojaDto>().aparencia.copy(cssExtra = "body { color: red; }"))
        }
        assertEquals(HttpStatusCode.Forbidden, css.status)
        assertEquals("personalizacao_bloqueada", css.body<ErroDto>().codigo)

        // Super admin tira a permissão de cores; o dono passa a ser bloqueado
        val atual = cliente.get("/api/admin/lojas/${loja.id}") { bearerAuth(superToken) }.body<LojaDto>()
        cliente.put("/api/admin/lojas/${loja.id}") {
            bearerAuth(superToken)
            contentType(ContentType.Application.Json)
            setBody(
                AtualizarLojaDto(
                    atual.slug, atual.nome, atual.ativa, atual.fusoHorario, atual.aparencia, atual.modulos,
                    personalizacaoLiberada = setOf(ItemPersonalizavel.LOGO),
                ),
            )
        }
        val bloqueado = cliente.put("/api/painel/loja/aparencia") {
            bearerAuth(donoToken)
            contentType(ContentType.Application.Json)
            setBody(atual.aparencia.copy(corPrimaria = "#654321"))
        }
        assertEquals(HttpStatusCode.Forbidden, bloqueado.status)
    }

    @Test
    fun corInvalidaERecusada() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        val r = cliente.post("/api/admin/lojas") {
            bearerAuth(superToken)
            contentType(ContentType.Application.Json)
            setBody(NovaLojaDto("ld-lanches", "LD", "Dono", "d@ld.com", SENHA_DONO, aparencia = AparenciaDto(corPrimaria = "vermelho")))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, r.status)
    }

    @Test
    fun lojaDesativadaSomeDoPublicoEBloqueiaPainel() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        val loja = cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")
        val donoToken = cliente.token("dono@ld.com", SENHA_DONO)
        assertEquals(HttpStatusCode.OK, cliente.get("/api/public/lojas/ld-lanches").status)

        cliente.put("/api/admin/lojas/${loja.id}") {
            bearerAuth(superToken)
            contentType(ContentType.Application.Json)
            setBody(AtualizarLojaDto(loja.slug, loja.nome, false, loja.fusoHorario, loja.aparencia, loja.modulos, loja.personalizacaoLiberada))
        }

        assertEquals(HttpStatusCode.NotFound, cliente.get("/api/public/lojas/ld-lanches").status)
        assertEquals(HttpStatusCode.Forbidden, cliente.get("/api/painel/produtos") { bearerAuth(donoToken) }.status)
        assertEquals(HttpStatusCode.Unauthorized, cliente.login("dono@ld.com", SENHA_DONO).status)
    }

    // ---------- pedidos ----------

    @Test
    fun fluxoDePedidoDoCardapioAteFinalizar() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")
        val donoToken = cliente.token("dono@ld.com", SENHA_DONO)

        val vitrine = cliente.get("/api/public/lojas/ld-lanches").body<VitrineLojaDto>()
        assertEquals("Loja ld-lanches", vitrine.nome)

        val criado = cliente.post("/api/public/lojas/ld-lanches/pedidos") {
            contentType(ContentType.Application.Json)
            setBody(NovoPedidoPublicoDto("1x X-Tudo", 3500))
        }
        assertEquals(HttpStatusCode.Created, criado.status, criado.bodyAsText())
        val publico = criado.body<PedidoPublicoDto>()
        assertEquals(StatusPedido.NOVO, publico.status)
        assertEquals(8, publico.codigo.length)

        // A listagem completa de pedidos NÃO é pública
        assertEquals(HttpStatusCode.Unauthorized, cliente.get("/api/painel/pedidos").status)

        val lista = cliente.get("/api/painel/pedidos") { bearerAuth(donoToken) }.body<List<PedidoDto>>()
        assertEquals(1, lista.size)
        val pedido = lista.single()

        suspend fun mudar(status: StatusPedido) = cliente.patch("/api/painel/pedidos/${pedido.id}/status") {
            bearerAuth(donoToken)
            contentType(ContentType.Application.Json)
            setBody(AlterarStatusDto(status))
        }
        assertEquals(HttpStatusCode.OK, mudar(StatusPedido.EM_PREPARO).status)
        assertEquals(HttpStatusCode.OK, mudar(StatusPedido.FINALIZADO).status)
        assertEquals(HttpStatusCode.Conflict, mudar(StatusPedido.NOVO).status)

        val consulta = cliente.get("/api/public/lojas/ld-lanches/pedidos/${publico.codigo}").body<PedidoPublicoDto>()
        assertEquals(StatusPedido.FINALIZADO, consulta.status)

        val resumo = cliente.get("/api/painel/dashboard/resumo?dias=7") { bearerAuth(donoToken) }.body<ResumoDashboardDto>()
        assertEquals(1, resumo.totalVendas)
        assertEquals(3500, resumo.totalFaturamentoCentavos)
        assertEquals(7, resumo.rotulosDias.size)
    }

    @Test
    fun cardapioWebEServidoPorCaminhoDaLoja() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com")

        val pagina = cliente.get("/ld-lanches/")
        assertEquals(HttpStatusCode.OK, pagina.status)
        assertTrue(pagina.bodyAsText().contains("<html", ignoreCase = true))

        assertEquals(HttpStatusCode.NotFound, cliente.get("/loja-que-nao-existe/").status)
        assertEquals(HttpStatusCode.NotFound, cliente.get("/ld-lanches/..%2F..%2Fsettings.gradle.kts").status)
        assertFalse(cliente.get("/api/public/lojas/loja-que-nao-existe").status.isSuccess())
    }
}

