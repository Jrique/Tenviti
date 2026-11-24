package br.com.tenviti.server

import br.com.tenviti.server.aplicacao.DonoDeExemplo
import br.com.tenviti.shared.DepoimentoDto
import br.com.tenviti.shared.ProdutoDto
import br.com.tenviti.shared.VitrineLojaDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DadosDeExemploTest : TesteDeApi() {

    private val comExemplos = config.copy(
        exemploThai = DonoDeExemplo("thai@exemplo.dev", "senha-da-thai"),
        exemploLd = DonoDeExemplo("ld@exemplo.dev", "senha-da-ld"),
    )

    @Test
    fun criaAsLojasDeExemploComCardapioHorariosEDepoimentos() = cenario(comExemplos) { cliente ->
        val thai = cliente.get("/api/public/lojas/thai-cookies").body<VitrineLojaDto>()
        assertEquals("Thai Cookies", thai.nome)
        assertTrue(thai.encomenda.exigeAgendamento)
        assertEquals(5, thai.horarios.size)
        val produtosThai = cliente.get("/api/public/lojas/thai-cookies/produtos").body<List<ProdutoDto>>()
        assertEquals(8, produtosThai.size)
        val box = produtosThai.single { it.nome == "Box com 4 cookies" }
        assertEquals(4, box.grupos.first().minimo)
        assertEquals(3, cliente.get("/api/public/lojas/thai-cookies/depoimentos").body<List<DepoimentoDto>>().size)

        assertEquals(3, cliente.get("/api/public/lojas/ld-lanches/produtos").body<List<ProdutoDto>>().size)
        // o dono de exemplo consegue entrar no painel
        cliente.token("thai@exemplo.dev", "senha-da-thai")
    }

    @Test
    fun subirDeNovoNaoDuplicaNada() {
        cenario(comExemplos) { }
        // segunda subida do servidor sobre o mesmo banco
        cenario(comExemplos) { cliente ->
            assertEquals(8, cliente.get("/api/public/lojas/thai-cookies/produtos").body<List<ProdutoDto>>().size)
            assertEquals(3, cliente.get("/api/public/lojas/thai-cookies/depoimentos").body<List<DepoimentoDto>>().size)
        }
    }

    @Test
    fun semDadosDeExemploNaoCriaLoja() = cenario { cliente ->
        assertEquals(HttpStatusCode.NotFound, cliente.get("/api/public/lojas/thai-cookies").status)
    }
}
