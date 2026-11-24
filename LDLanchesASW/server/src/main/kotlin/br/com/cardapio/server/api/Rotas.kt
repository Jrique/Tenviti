package br.com.cardapio.server.api

import br.com.cardapio.server.aplicacao.ServicoArquivos
import br.com.cardapio.server.aplicacao.ServicoAutenticacao
import br.com.cardapio.server.aplicacao.ServicoDepoimentos
import br.com.cardapio.server.aplicacao.ServicoCatalogo
import br.com.cardapio.server.aplicacao.ServicoLojas
import br.com.cardapio.server.aplicacao.ServicoPedidos
import br.com.cardapio.server.aplicacao.ServicoUsuarios
import br.com.cardapio.server.dominio.DadoInvalido
import br.com.cardapio.shared.AlterarStatusDto
import br.com.cardapio.shared.AparenciaDto
import br.com.cardapio.shared.AtualizarLojaDto
import br.com.cardapio.shared.AtualizarUsuarioDto
import br.com.cardapio.shared.DepoimentoDto
import br.com.cardapio.shared.InformacoesLojaDto
import br.com.cardapio.shared.LoginRequisicao
import br.com.cardapio.shared.NovaLojaDto
import br.com.cardapio.shared.NovoPedidoPublicoDto
import br.com.cardapio.shared.NovoUsuarioDto
import br.com.cardapio.shared.SalvarCategoriaDto
import br.com.cardapio.shared.SalvarProdutoDto
import br.com.cardapio.shared.TrocarSenhaRequisicao
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.LocalDate
import java.time.format.DateTimeParseException

class Servicos(
    val autenticacao: ServicoAutenticacao,
    val lojas: ServicoLojas,
    val usuarios: ServicoUsuarios,
    val catalogo: ServicoCatalogo,
    val pedidos: ServicoPedidos,
    val depoimentos: ServicoDepoimentos,
    val arquivos: ServicoArquivos,
)

fun Route.rotasApi(s: Servicos) {
    get("/saude") { call.respondText("ok") }

    route("/api") {
        rotasAutenticacao(s)
        rotasPublicas(s)
        authenticate(AUTH_JWT) {
            rotasSuperAdmin(s)
            rotasPainel(s)
        }
    }
}

private fun Route.rotasAutenticacao(s: Servicos) = route("/auth") {
    post("/login") { call.respond(s.autenticacao.entrar(call.receive<LoginRequisicao>())) }

    authenticate(AUTH_JWT) {
        get("/eu") { call.respond(s.autenticacao.eu(call.ator())) }
        put("/eu/senha") {
            s.autenticacao.trocarSenha(call.ator(), call.receive<TrocarSenhaRequisicao>())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Cardápio público: só lojas ativas; nunca expõe dados de clientes. */
private fun Route.rotasPublicas(s: Servicos) = route("/public/lojas/{slug}") {
    get { call.respond(s.lojas.vitrine(call.slug())) }
    get("/categorias") {
        val loja = s.lojas.lojaPublica(call.slug())
        call.respond(s.catalogo.listarCategorias(loja.id))
    }
    get("/produtos") {
        val loja = s.lojas.lojaPublica(call.slug())
        call.respond(s.catalogo.listarProdutos(loja.id, somenteAtivos = true))
    }
    post("/pedidos") {
        val loja = s.lojas.lojaPublica(call.slug())
        call.respond(HttpStatusCode.Created, s.pedidos.criarDoCardapio(loja, call.receive<NovoPedidoPublicoDto>()))
    }
    get("/pedidos/{codigo}") {
        val loja = s.lojas.lojaPublica(call.slug())
        call.respond(s.pedidos.consultarPublico(loja, call.parameters["codigo"].orEmpty()))
    }
    get("/agenda") { call.respond(s.pedidos.agenda(s.lojas.lojaPublica(call.slug()))) }
    get("/depoimentos") { call.respond(s.depoimentos.listarPublicos(s.lojas.lojaPublica(call.slug()).id)) }
}

private fun Route.rotasSuperAdmin(s: Servicos) = route("/admin/lojas") {
    get { call.respond(s.lojas.listar(call.ator())) }
    post { call.respond(HttpStatusCode.Created, s.lojas.criar(call.ator(), call.receive<NovaLojaDto>())) }
    get("/{id}") { call.respond(s.lojas.buscar(call.ator(), call.id())) }
    put("/{id}") { call.respond(s.lojas.atualizar(call.ator(), call.id(), call.receive<AtualizarLojaDto>())) }
}

/** Painel da loja: toda rota passa por lojaDoPainel, que confere usuário e loja ativos. */
private fun Route.rotasPainel(s: Servicos) = route("/painel") {
    get("/loja") {
        s.lojas.lojaDoPainel(call.ator())
        call.respond(s.lojas.minhaLoja(call.ator()))
    }
    put("/loja/aparencia") {
        s.lojas.lojaDoPainel(call.ator())
        call.respond(s.lojas.personalizar(call.ator(), call.receive<AparenciaDto>()))
    }
    get("/loja/informacoes") {
        s.lojas.lojaDoPainel(call.ator())
        call.respond(s.lojas.informacoes(call.ator()))
    }
    put("/loja/informacoes") {
        s.lojas.lojaDoPainel(call.ator())
        call.respond(s.lojas.atualizarInformacoes(call.ator(), call.receive<InformacoesLojaDto>()))
    }

    post("/arquivos") {
        s.lojas.lojaDoPainel(call.ator())
        call.respond(HttpStatusCode.Created, s.arquivos.enviarImagem(call.ator(), call.lerArquivoEnviado()))
    }

    route("/depoimentos") {
        get {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.depoimentos.listar(call.ator()))
        }
        post {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(HttpStatusCode.Created, s.depoimentos.criar(call.ator(), call.receive<DepoimentoDto>()))
        }
        put("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.depoimentos.atualizar(call.ator(), call.id(), call.receive<DepoimentoDto>()))
        }
        delete("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            s.depoimentos.remover(call.ator(), call.id())
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/usuarios") {
        get {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.usuarios.listar(call.ator()))
        }
        post {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(HttpStatusCode.Created, s.usuarios.criar(call.ator(), call.receive<NovoUsuarioDto>()))
        }
        put("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.usuarios.atualizar(call.ator(), call.id(), call.receive<AtualizarUsuarioDto>()))
        }
        delete("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            s.usuarios.remover(call.ator(), call.id())
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/categorias") {
        get { call.respond(s.catalogo.listarCategorias(s.lojas.lojaDoPainel(call.ator()).id)) }
        post {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(HttpStatusCode.Created, s.catalogo.criarCategoria(call.ator(), call.receive<SalvarCategoriaDto>()))
        }
        put("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.catalogo.atualizarCategoria(call.ator(), call.id(), call.receive<SalvarCategoriaDto>()))
        }
        delete("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            s.catalogo.removerCategoria(call.ator(), call.id())
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/produtos") {
        get { call.respond(s.catalogo.listarProdutos(s.lojas.lojaDoPainel(call.ator()).id, somenteAtivos = false)) }
        post {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(HttpStatusCode.Created, s.catalogo.criarProduto(call.ator(), call.receive<SalvarProdutoDto>()))
        }
        put("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.catalogo.atualizarProduto(call.ator(), call.id(), call.receive<SalvarProdutoDto>()))
        }
        patch("/{id}/destaque") {
            s.lojas.lojaDoPainel(call.ator())
            val destaque = call.request.queryParameters["valor"]?.toBooleanStrictOrNull()
                ?: throw DadoInvalido("Informe ?valor=true ou ?valor=false.")
            call.respond(s.catalogo.marcarDestaque(call.ator(), call.id(), destaque))
        }
        patch("/{id}/disponivel") {
            s.lojas.lojaDoPainel(call.ator())
            val disponivel = call.request.queryParameters["valor"]?.toBooleanStrictOrNull()
                ?: throw DadoInvalido("Informe ?valor=true ou ?valor=false.")
            call.respond(s.catalogo.definirDisponivel(call.ator(), call.id(), disponivel))
        }
        delete("/{id}") {
            s.lojas.lojaDoPainel(call.ator())
            s.catalogo.removerProduto(call.ator(), call.id())
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/pedidos") {
        get {
            val loja = s.lojas.lojaDoPainel(call.ator())
            call.respond(s.pedidos.listar(call.ator(), loja, call.data("inicio"), call.data("fim")))
        }
        patch("/{id}/status") {
            s.lojas.lojaDoPainel(call.ator())
            call.respond(s.pedidos.alterarStatus(call.ator(), call.id(), call.receive<AlterarStatusDto>()))
        }
    }

    get("/dashboard/resumo") {
        val loja = s.lojas.lojaDoPainel(call.ator())
        val dias = call.request.queryParameters["dias"]?.toIntOrNull()
        call.respond(s.pedidos.resumo(call.ator(), loja, dias, call.data("inicio"), call.data("fim")))
    }
}

/** Lê o primeiro arquivo de um envio multipart (campo "arquivo"), com limite de tamanho. */
private suspend fun ApplicationCall.lerArquivoEnviado(): ByteArray {
    var conteudo: ByteArray? = null
    receiveMultipart(formFieldLimit = ServicoArquivos.TAMANHO_MAXIMO + 1024L).forEachPart { parte ->
        if (parte is PartData.FileItem && conteudo == null) {
            conteudo = parte.provider().readRemaining().readByteArray()
        }
        parte.dispose()
    }
    return conteudo ?: throw DadoInvalido("Nenhum arquivo enviado (campo \"arquivo\").")
}

private fun ApplicationCall.slug(): String = parameters["slug"].orEmpty().lowercase()

private fun ApplicationCall.id(): Long =
    parameters["id"]?.toLongOrNull() ?: throw DadoInvalido("Identificador inválido.")

/** Lê uma data no formato AAAA-MM-DD da query string. */
private fun ApplicationCall.data(nome: String): LocalDate? {
    val valor = request.queryParameters[nome]?.takeIf { it.isNotBlank() } ?: return null
    return try {
        LocalDate.parse(valor)
    } catch (_: DateTimeParseException) {
        throw DadoInvalido("Data inválida em \"$nome\": use o formato AAAA-MM-DD.")
    }
}
