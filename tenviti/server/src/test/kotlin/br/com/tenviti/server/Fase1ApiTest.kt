package br.com.tenviti.server

import br.com.tenviti.shared.ArquivoDto
import br.com.tenviti.shared.ContatoLojaDto
import br.com.tenviti.shared.DepoimentoDto
import br.com.tenviti.shared.DiaAgendaDto
import br.com.tenviti.shared.EncomendaConfigDto
import br.com.tenviti.shared.EnderecoDto
import br.com.tenviti.shared.ErroDto
import br.com.tenviti.shared.FuncionamentoDto
import br.com.tenviti.shared.GrupoOpcoesDto
import br.com.tenviti.shared.InformacoesLojaDto
import br.com.tenviti.shared.IntervaloHorarioDto
import br.com.tenviti.shared.Modulo
import br.com.tenviti.shared.ItemNovoPedidoDto
import br.com.tenviti.shared.OpcaoDto
import br.com.tenviti.shared.PedidoPublicoDto
import br.com.tenviti.shared.ProdutoDto
import br.com.tenviti.shared.SalvarProdutoDto
import br.com.tenviti.shared.VitrineLojaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Fase1ApiTest : TesteDeApi() {

    private suspend fun HttpClient.salvarInformacoes(token: String, info: InformacoesLojaDto): HttpResponse =
        put("/api/painel/loja/informacoes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(info)
        }

    /** Pede 1 unidade do primeiro produto da vitrine. */
    private suspend fun HttpClient.pedir(slug: String, agendadoPara: String? = null): HttpResponse {
        val produto = get("/api/public/lojas/$slug/produtos").body<List<ProdutoDto>>().first()
        return pedirNaVitrine(slug, listOf(ItemNovoPedidoDto(produto.id)), agendadoPara = agendadoPara)
    }

    private val todosOsDias = (1..7).map { IntervaloHorarioDto(it, "00:00", "23:59") }

    // ---------- opções e promoção ----------

    @Test
    fun produtoComGruposDeOpcoesEPromocao() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)

        val box = SalvarProdutoDto(
            nome = "Box 4 cookies", precoCentavos = 4400, precoPromocionalCentavos = 3990,
            imagemUrl = "/midia/1/box.png",
            grupos = listOf(
                GrupoOpcoesDto(
                    nome = "Escolha 4 sabores", minimo = 4, maximo = 4, permiteRepetir = true,
                    opcoes = listOf(OpcaoDto(nome = "Nutella"), OpcaoDto(nome = "Maracujá", ativo = false), OpcaoDto(nome = "Red Velvet", precoCentavos = 100)),
                ),
                GrupoOpcoesDto(nome = "Presente", minimo = 0, maximo = 1, opcoes = listOf(OpcaoDto(nome = "Laço", precoCentavos = 300))),
            ),
        )
        val r = cliente.criarProduto(dono, box)
        assertEquals(HttpStatusCode.Created, r.status, r.bodyAsText())
        val salvo = r.body<ProdutoDto>()
        assertEquals(3990, salvo.precoPromocionalCentavos)
        assertEquals(listOf("Escolha 4 sabores", "Presente"), salvo.grupos.map { it.nome })
        assertEquals(3, salvo.grupos[0].opcoes.size) // painel vê inclusive a opção desativada

        // cardápio público esconde a opção desativada
        val publico = cliente.get("/api/public/lojas/thai/produtos").body<List<ProdutoDto>>().single()
        assertEquals(listOf("Nutella", "Red Velvet"), publico.grupos[0].opcoes.map { it.nome })

        // promoção maior ou igual ao preço é recusada
        val promoInvalida = cliente.criarProduto(dono, box.copy(precoPromocionalCentavos = 4400))
        assertEquals(HttpStatusCode.UnprocessableEntity, promoInvalida.status)

        // mínimo impossível sem permitir repetir
        val grupoImpossivel = cliente.criarProduto(dono, box.copy(grupos = listOf(box.grupos[0].copy(permiteRepetir = false))))
        assertEquals(HttpStatusCode.UnprocessableEntity, grupoImpossivel.status)
    }

    @Test
    fun produtoPausadoSomeDaVitrinePublica() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)
        val p = cliente.criarProduto(dono, "Nutella").body<ProdutoDto>()

        val pausado = cliente.patch("/api/painel/produtos/${p.id}/disponivel?valor=false") { bearerAuth(dono) }
        assertEquals(HttpStatusCode.OK, pausado.status)
        assertTrue(cliente.get("/api/public/lojas/thai/produtos").body<List<ProdutoDto>>().isEmpty())

        cliente.patch("/api/painel/produtos/${p.id}/disponivel?valor=true") { bearerAuth(dono) }
        assertEquals(1, cliente.get("/api/public/lojas/thai/produtos").body<List<ProdutoDto>>().size)
    }

    // ---------- dados da loja e funcionamento ----------

    @Test
    fun informacoesDaLojaAparecemNaVitrine() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)

        val info = InformacoesLojaDto(
            contato = ContatoLojaDto(
                telefone = "(11) 98765-4321", instagram = "@thaicookies_",
                endereco = EnderecoDto("Rua das Flores", "100", null, "Centro", "São Paulo", "sp", "01000-000"),
            ),
            funcionamento = FuncionamentoDto(horarios = todosOsDias),
        )
        val r = cliente.salvarInformacoes(dono, info)
        assertEquals(HttpStatusCode.OK, r.status, r.bodyAsText())
        val salvo = r.body<InformacoesLojaDto>()
        assertEquals("11987654321", salvo.contato.telefone)
        assertEquals("thaicookies_", salvo.contato.instagram)
        assertEquals("SP", salvo.contato.endereco?.uf)
        assertEquals("01000000", salvo.contato.endereco?.cep)

        val vitrine = cliente.get("/api/public/lojas/thai").body<VitrineLojaDto>()
        assertEquals("thaicookies_", vitrine.contato.instagram)
        assertEquals(7, vitrine.horarios.size)
        assertTrue(vitrine.aberta)

        val telefoneRuim = cliente.salvarInformacoes(dono, info.copy(contato = info.contato.copy(telefone = "123")))
        assertEquals(HttpStatusCode.UnprocessableEntity, telefoneRuim.status)
        val horaRuim = cliente.salvarInformacoes(dono, info.copy(funcionamento = FuncionamentoDto(horarios = listOf(IntervaloHorarioDto(1, "25:00", "26:00")))))
        assertEquals(HttpStatusCode.UnprocessableEntity, horaRuim.status)
    }

    @Test
    fun lojaPausadaNaoRecebePedidos() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)
        cliente.criarProduto(dono, "Cookie", 1200)
        assertEquals(HttpStatusCode.Created, cliente.pedir("thai").status)

        cliente.salvarInformacoes(dono, InformacoesLojaDto(funcionamento = FuncionamentoDto(pausada = true, mensagemPausa = "Voltamos segunda!")))
        val r = cliente.pedir("thai")
        assertEquals(HttpStatusCode.Conflict, r.status)
        val erro = r.body<ErroDto>()
        assertEquals("loja_pausada", erro.codigo)
        assertTrue(erro.mensagem.contains("Voltamos segunda!"))
        assertEquals(false, cliente.get("/api/public/lojas/thai").body<VitrineLojaDto>().aberta)
    }

    // ---------- encomendas ----------

    @Test
    fun agendamentoExigeModuloLigadoPeloSuperAdmin() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "ld-lanches", "dono@ld.com") // sem módulo AGENDAMENTO
        val dono = cliente.token("dono@ld.com", SENHA_DONO)
        cliente.criarProduto(dono, "X-Tudo", 3290)
        cliente.salvarInformacoes(
            dono,
            InformacoesLojaDto(funcionamento = FuncionamentoDto(horarios = todosOsDias, encomenda = EncomendaConfigDto(aceitaAgendamento = true))),
        )
        assertTrue(cliente.get("/api/public/lojas/ld-lanches/agenda").body<List<DiaAgendaDto>>().isEmpty())
        val r = cliente.pedir("ld-lanches", agendadoPara = "2099-01-01T10:00")
        assertEquals(HttpStatusCode.UnprocessableEntity, r.status)
    }

    @Test
    fun encomendaAgendadaRespeitaAgendaELimiteDiario() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com", modulos = setOf(Modulo.RETIRADA, Modulo.AGENDAMENTO))
        val dono = cliente.token("dono@thai.com", SENHA_DONO)
        cliente.criarProduto(dono, "Cookie", 1200)
        val r = cliente.salvarInformacoes(
            dono,
            InformacoesLojaDto(
                funcionamento = FuncionamentoDto(
                    horarios = (1..7).map { IntervaloHorarioDto(it, "10:00", "18:00") },
                    encomenda = EncomendaConfigDto(
                        aceitaAgendamento = true, exigeAgendamento = true, antecedenciaMinimaHoras = 24,
                        diasMaximos = 7, limitePedidosPorDia = 1, intervaloMinutos = 60,
                    ),
                ),
            ),
        )
        assertEquals(HttpStatusCode.OK, r.status, r.bodyAsText())

        val agenda = cliente.get("/api/public/lojas/thai/agenda").body<List<DiaAgendaDto>>()
        assertTrue(agenda.isNotEmpty())
        val dia = agenda.first()
        val horario = "${dia.data}T${dia.horarios.first()}"

        // sem data: recusado (loja só trabalha por encomenda)
        assertEquals("agendamento_obrigatorio", cliente.pedir("thai").body<ErroDto>().codigo)

        // horário válido: aceito
        val ok = cliente.pedir("thai", horario)
        assertEquals(HttpStatusCode.Created, ok.status, ok.bodyAsText())
        assertTrue(ok.body<PedidoPublicoDto>().agendadoPara != null)

        // mesmo dia de novo: lotado (limite 1)
        val lotado = cliente.pedir("thai", horario)
        assertEquals(HttpStatusCode.Conflict, lotado.status)
        assertEquals("dia_lotado", lotado.body<ErroDto>().codigo)
        assertTrue(cliente.get("/api/public/lojas/thai/agenda").body<List<DiaAgendaDto>>().first { it.data == dia.data }.lotado)

        // horário fora da agenda: recusado
        val fora = cliente.pedir("thai", "${dia.data}T03:00")
        assertEquals("horario_indisponivel", fora.body<ErroDto>().codigo)
    }

    // ---------- depoimentos ----------

    @Test
    fun depoimentosAtivosAparecemNaVitrineEOutraLojaNaoMexe() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        cliente.criarLoja(superToken, "outra", "dono@outra.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)
        val outro = cliente.token("dono@outra.com", SENHA_DONO)

        suspend fun criar(d: DepoimentoDto) = cliente.post("/api/painel/depoimentos") {
            bearerAuth(dono)
            contentType(ContentType.Application.Json)
            setBody(d)
        }.body<DepoimentoDto>()

        val visivel = criar(DepoimentoDto(autor = "Ana", texto = "Melhor cookie de SP!", nota = 5))
        criar(DepoimentoDto(autor = "Bia", texto = "Oculto", ativo = false))

        val publicos = cliente.get("/api/public/lojas/thai/depoimentos").body<List<DepoimentoDto>>()
        assertEquals(listOf("Ana"), publicos.map { it.autor })

        val invasao = cliente.put("/api/painel/depoimentos/${visivel.id}") {
            bearerAuth(outro)
            contentType(ContentType.Application.Json)
            setBody(visivel.copy(texto = "Hackeado"))
        }
        assertEquals(HttpStatusCode.NotFound, invasao.status)

        val notaRuim = cliente.post("/api/painel/depoimentos") {
            bearerAuth(dono)
            contentType(ContentType.Application.Json)
            setBody(DepoimentoDto(autor = "X", texto = "Y", nota = 9))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, notaRuim.status)
    }

    // ---------- upload ----------

    @Test
    fun envioDeImagemAceitaPngERecusaOutrosFormatos() = cenario { cliente ->
        val superToken = cliente.token(SUPER_EMAIL, SUPER_SENHA)
        cliente.criarLoja(superToken, "thai", "dono@thai.com")
        val dono = cliente.token("dono@thai.com", SENHA_DONO)

        suspend fun enviar(bytes: ByteArray, nome: String) = cliente.submitFormWithBinaryData(
            url = "/api/painel/arquivos",
            formData = formData {
                append("arquivo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"$nome\"")
                })
            },
        ) { bearerAuth(dono) }

        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0)
        val ok = enviar(png, "foto.png")
        assertEquals(HttpStatusCode.Created, ok.status, ok.bodyAsText())
        val url = ok.body<ArquivoDto>().url
        assertTrue(url.startsWith("/midia/") && url.endsWith(".png"))
        assertEquals(HttpStatusCode.OK, cliente.get(url).status)

        // "imagem" que na verdade é um script: recusada pelo conteúdo, não pelo nome
        val falso = enviar("<svg onload=alert(1)>".encodeToByteArray(), "foto.png")
        assertEquals(HttpStatusCode.UnprocessableEntity, falso.status)
        assertEquals("formato_invalido", falso.body<ErroDto>().codigo)

        // sem login: bloqueado
        val anonimo = cliente.submitFormWithBinaryData(url = "/api/painel/arquivos", formData = formData { append("arquivo", png) })
        assertEquals(HttpStatusCode.Unauthorized, anonimo.status)
    }
}
