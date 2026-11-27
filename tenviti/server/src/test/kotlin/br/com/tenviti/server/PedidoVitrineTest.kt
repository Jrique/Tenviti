package br.com.tenviti.server

import br.com.tenviti.shared.EntregaConfigDto
import br.com.tenviti.shared.ErroDto
import br.com.tenviti.shared.FormaPagamento
import br.com.tenviti.shared.GrupoOpcoesDto
import br.com.tenviti.shared.InformacoesLojaDto
import br.com.tenviti.shared.ItemNovoPedidoDto
import br.com.tenviti.shared.Modulo
import br.com.tenviti.shared.OpcaoDto
import br.com.tenviti.shared.OpcaoEscolhidaDto
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.PedidoPublicoDto
import br.com.tenviti.shared.ProdutoDto
import br.com.tenviti.shared.SalvarProdutoDto
import br.com.tenviti.shared.TipoEntrega
import br.com.tenviti.shared.VitrineLojaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Bloco 2 da Fase 2: pedido com itens estruturados e preço calculado no servidor. */
class PedidoVitrineTest : TesteDeApi() {

    private val sabores = listOf("Nutella", "Red Velvet", "Maracujá")

    private val box4 = SalvarProdutoDto(
        nome = "Box com 4 cookies",
        precoCentavos = 4400,
        grupos = listOf(
            GrupoOpcoesDto(nome = "Escolha 4 sabores", minimo = 4, maximo = 4, permiteRepetir = true, opcoes = sabores.map { OpcaoDto(nome = it) }),
            GrupoOpcoesDto(nome = "Presente", minimo = 0, maximo = 1, opcoes = listOf(OpcaoDto(nome = "Laço", precoCentavos = 300))),
        ),
    )

    private suspend fun HttpClient.lojaComDono(slug: String, modulos: Set<Modulo> = Modulo.PADRAO): String {
        val superToken = token(SUPER_EMAIL, SUPER_SENHA)
        criarLoja(superToken, slug, "dono@$slug.com", modulos)
        return token("dono@$slug.com", SENHA_DONO)
    }

    private suspend fun HttpClient.configurarEntrega(dono: String, taxa: Long, minimo: Long): HttpResponse =
        put("/api/painel/loja/informacoes") {
            bearerAuth(dono)
            contentType(ContentType.Application.Json)
            setBody(InformacoesLojaDto(entrega = EntregaConfigDto(taxaCentavos = taxa, pedidoMinimoCentavos = minimo)))
        }

    private fun ProdutoDto.opcao(nome: String) = grupos.flatMap { it.opcoes }.first { it.nome == nome }.id

    private suspend fun HttpResponse.codigoDeErro() = body<ErroDto>().codigo

    @Test
    fun totalECalculadoNoServidorComOpcoesEFicaRegistradoComoFoiComprado() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val box = cliente.criarProduto(dono, box4).body<ProdutoDto>()

        val r = cliente.pedirNaVitrine(
            "thai",
            listOf(
                ItemNovoPedidoDto(
                    box.id, quantidade = 2,
                    opcoes = listOf(
                        OpcaoEscolhidaDto(box.opcao("Nutella"), 2),
                        OpcaoEscolhidaDto(box.opcao("Red Velvet"), 2),
                        OpcaoEscolhidaDto(box.opcao("Laço")),
                    ),
                    observacao = "Sem cartão",
                ),
            ),
        )
        assertEquals(HttpStatusCode.Created, r.status, r.bodyAsText())
        val publico = r.body<PedidoPublicoDto>()
        assertEquals((4400 + 300) * 2L, publico.totalCentavos)
        assertEquals(1, publico.itens.size)
        // a consulta pública não expõe dados pessoais
        assertFalse(r.bodyAsText().contains("Maria"))

        // a loja muda o preço depois: o pedido continua com o preço da compra
        cliente.put("/api/painel/produtos/${box.id}") {
            bearerAuth(dono); contentType(ContentType.Application.Json); setBody(box4.copy(precoCentavos = 5000))
        }
        val pedido = cliente.get("/api/painel/pedidos") { bearerAuth(dono) }.body<List<PedidoDto>>().single()
        val item = pedido.itens.single()
        assertEquals(4400, item.precoUnitarioCentavos)
        assertEquals(9400, item.totalCentavos)
        assertEquals("Maria Cliente", pedido.cliente?.nome)
        assertEquals(TipoEntrega.RETIRADA, pedido.tipoEntrega)
        assertEquals(FormaPagamento.PIX, pedido.formaPagamento)
        assertTrue(pedido.descricao.contains("2x Box com 4 cookies (R$ 94,00) | Escolha 4 sabores: 2x Nutella, 2x Red Velvet | Presente: 1x Laço"), pedido.descricao)
        assertTrue(pedido.descricao.contains("Cliente:\n- Maria Cliente"), pedido.descricao)
    }

    @Test
    fun precoPromocionalVale() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val cookie = cliente.criarProduto(dono, SalvarProdutoDto(nome = "Tradicional", precoCentavos = 900, precoPromocionalCentavos = 790)).body<ProdutoDto>()
        val r = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(cookie.id, quantidade = 3)))
        assertEquals(3 * 790L, r.body<PedidoPublicoDto>().totalCentavos)
    }

    @Test
    fun regrasDasOpcoesSaoConferidasNoServidor() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val box = cliente.criarProduto(dono, box4).body<ProdutoDto>()
        val outro = cliente.criarProduto(dono, box4.copy(nome = "Outro box")).body<ProdutoDto>()

        // faltou sabor (mínimo 4)
        val poucos = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(box.id, opcoes = listOf(OpcaoEscolhidaDto(box.opcao("Nutella"), 3)))))
        assertEquals(HttpStatusCode.UnprocessableEntity, poucos.status)
        assertTrue(poucos.body<ErroDto>().mensagem.contains("escolha 4 em \"Escolha 4 sabores\""))

        // passou do máximo
        val muitos = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(box.id, opcoes = listOf(OpcaoEscolhidaDto(box.opcao("Nutella"), 5)))))
        assertEquals(HttpStatusCode.UnprocessableEntity, muitos.status)

        // grupo sem repetição: a mesma opção duas vezes
        val repetida = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(box.id, opcoes = listOf(
            OpcaoEscolhidaDto(box.opcao("Nutella"), 4), OpcaoEscolhidaDto(box.opcao("Laço"), 2),
        ))))
        assertEquals(HttpStatusCode.UnprocessableEntity, repetida.status)

        // opção de outro produto
        val deOutro = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(box.id, opcoes = listOf(OpcaoEscolhidaDto(outro.opcao("Nutella"), 4)))))
        assertEquals("opcao_invalida", deOutro.codigoDeErro())
    }

    @Test
    fun produtoPausadoOuDeOutraLojaNaoEntra() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val cookie = cliente.criarProduto(dono, "Cookie", 1200).body<ProdutoDto>()
        val donoOutra = cliente.lojaComDono("outra")
        val daOutra = cliente.criarProduto(donoOutra, "Lanche", 2000).body<ProdutoDto>()

        assertEquals("produto_indisponivel", cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(daOutra.id))).codigoDeErro())

        cliente.patch("/api/painel/produtos/${cookie.id}/disponivel?valor=false") { bearerAuth(dono) }
        val pausado = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(cookie.id)))
        assertEquals(HttpStatusCode.Conflict, pausado.status)
        assertEquals("produto_indisponivel", pausado.codigoDeErro())
    }

    @Test
    fun entregaCobraTaxaDaLojaEPedidoMinimoVale() = cenario { cliente ->
        val dono = cliente.lojaComDono("ld-lanches")
        val lanche = cliente.criarProduto(dono, "X-Salada", 1200).body<ProdutoDto>()
        assertEquals(HttpStatusCode.OK, cliente.configurarEntrega(dono, taxa = 500, minimo = 2000).status)
        assertEquals(EntregaConfigDto(500, 2000), cliente.get("/api/public/lojas/ld-lanches").body<VitrineLojaDto>().entrega)

        val abaixo = cliente.pedirNaVitrine("ld-lanches", listOf(ItemNovoPedidoDto(lanche.id)), TipoEntrega.ENTREGA, ENDERECO_CLIENTE)
        assertEquals("pedido_minimo", abaixo.codigoDeErro())

        val semEndereco = cliente.pedirNaVitrine("ld-lanches", listOf(ItemNovoPedidoDto(lanche.id, quantidade = 2)), TipoEntrega.ENTREGA)
        assertEquals(HttpStatusCode.UnprocessableEntity, semEndereco.status)

        val entrega = cliente.pedirNaVitrine("ld-lanches", listOf(ItemNovoPedidoDto(lanche.id, quantidade = 2)), TipoEntrega.ENTREGA, ENDERECO_CLIENTE)
        assertEquals(HttpStatusCode.Created, entrega.status, entrega.bodyAsText())
        val p = entrega.body<PedidoPublicoDto>()
        assertEquals(2400, p.subtotalCentavos)
        assertEquals(500, p.taxaEntregaCentavos)
        assertEquals(2900, p.totalCentavos)

        val retirada = cliente.pedirNaVitrine("ld-lanches", listOf(ItemNovoPedidoDto(lanche.id, quantidade = 2)), TipoEntrega.RETIRADA)
        assertEquals(2400, retirada.body<PedidoPublicoDto>().totalCentavos)

        val noPainel = cliente.get("/api/painel/pedidos") { bearerAuth(dono) }.body<List<PedidoDto>>()
            .first { it.tipoEntrega == TipoEntrega.ENTREGA }
        assertEquals("SP", noPainel.endereco?.uf)
        assertEquals("01001000", noPainel.endereco?.cep)
        assertTrue(noPainel.descricao.contains("Taxa de entrega: R$ 5,00"), noPainel.descricao)
    }

    @Test
    fun formaDeEntregaDesligadaNaoEAceita() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai", setOf(Modulo.RETIRADA))
        val cookie = cliente.criarProduto(dono, "Cookie", 1200).body<ProdutoDto>()
        val r = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(cookie.id)), TipoEntrega.ENTREGA, ENDERECO_CLIENTE)
        assertEquals("forma_de_entrega_indisponivel", r.codigoDeErro())
    }

    @Test
    fun totalDiferenteDoQueOClienteViuERecusado() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val cookie = cliente.criarProduto(dono, "Cookie", 1200).body<ProdutoDto>()

        val mudou = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(cookie.id)), totalEsperadoCentavos = 1000)
        assertEquals(HttpStatusCode.Conflict, mudou.status)
        val erro = mudou.body<ErroDto>()
        assertEquals("total_alterado", erro.codigo)
        assertTrue(erro.mensagem.contains("R$ 12,00"))
        assertTrue(cliente.get("/api/painel/pedidos") { bearerAuth(dono) }.body<List<PedidoDto>>().isEmpty())

        val igual = cliente.pedirNaVitrine("thai", listOf(ItemNovoPedidoDto(cookie.id)), totalEsperadoCentavos = 1200)
        assertEquals(HttpStatusCode.Created, igual.status)
    }

    @Test
    fun trocoSoEmDinheiroEMaiorQueOTotal() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        val cookie = cliente.criarProduto(dono, "Cookie", 1200).body<ProdutoDto>()
        val itens = listOf(ItemNovoPedidoDto(cookie.id))

        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.pedirNaVitrine("thai", itens, formaPagamento = FormaPagamento.PIX, trocoParaCentavos = 5000).status)
        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.pedirNaVitrine("thai", itens, formaPagamento = FormaPagamento.DINHEIRO, trocoParaCentavos = 1000).status)
        val ok = cliente.pedirNaVitrine("thai", itens, formaPagamento = FormaPagamento.DINHEIRO, trocoParaCentavos = 5000)
        assertEquals(HttpStatusCode.Created, ok.status, ok.bodyAsText())
        val pedido = cliente.get("/api/painel/pedidos") { bearerAuth(dono) }.body<List<PedidoDto>>().single()
        assertEquals(5000, pedido.trocoParaCentavos)
        assertTrue(pedido.descricao.contains("Dinheiro (troco para R$ 50,00)"), pedido.descricao)
    }

    @Test
    fun sacolaVaziaEClienteSemTelefoneSaoRecusados() = cenario { cliente ->
        cliente.lojaComDono("thai")
        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.pedirNaVitrine("thai", emptyList()).status)
        val dono = cliente.token("dono@thai.com", SENHA_DONO)
        val cookie = cliente.criarProduto(dono, "Cookie", 1200).body<ProdutoDto>()
        val semTelefone = cliente.pedirNaVitrine(
            "thai", listOf(ItemNovoPedidoDto(cookie.id)),
            cliente = br.com.tenviti.shared.ClientePedidoDto("Maria", "123"),
        )
        assertEquals(HttpStatusCode.UnprocessableEntity, semTelefone.status)
    }

    @Test
    fun pedidoAntigoSoComTextoContinuaAparecendo() = cenario { cliente ->
        val dono = cliente.lojaComDono("thai")
        DriverManager.getConnection(banco.url, banco.usuario, banco.senha).use { c ->
            c.createStatement().use {
                it.execute(
                    "insert into pedido (loja_id, codigo, descricao, total_centavos, status, dia_producao) " +
                        "select id, 'ANTIGO12', 'Produtos:\n- 1x Cookie', 1200, 'NOVO', current_date from loja where slug = 'thai'",
                )
            }
        }
        val pedido = cliente.get("/api/painel/pedidos") { bearerAuth(dono) }.body<List<PedidoDto>>().single()
        assertEquals("ANTIGO12", pedido.codigo)
        assertTrue(pedido.itens.isEmpty())
        assertNull(pedido.cliente)
        assertEquals(1200, pedido.totalCentavos)
    }
}
