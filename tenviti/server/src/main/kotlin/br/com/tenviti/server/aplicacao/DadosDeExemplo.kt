package br.com.tenviti.server.aplicacao

import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.AparenciaDto
import br.com.tenviti.shared.ContatoLojaDto
import br.com.tenviti.shared.DepoimentoDto
import br.com.tenviti.shared.EncomendaConfigDto
import br.com.tenviti.shared.EnderecoDto
import br.com.tenviti.shared.EntregaConfigDto
import br.com.tenviti.shared.FuncionamentoDto
import br.com.tenviti.shared.GrupoOpcoesDto
import br.com.tenviti.shared.InformacoesLojaDto
import br.com.tenviti.shared.IntervaloHorarioDto
import br.com.tenviti.shared.ModeloLayout
import br.com.tenviti.shared.Modulo
import br.com.tenviti.shared.NovaLojaDto
import br.com.tenviti.shared.OpcaoDto
import br.com.tenviti.shared.SalvarCategoriaDto
import br.com.tenviti.shared.SalvarProdutoDto
import org.slf4j.LoggerFactory

/** Dono de uma loja de exemplo, lido do ambiente (THAI_DONO_*, LD_DONO_*). */
data class DonoDeExemplo(val email: String, val senha: String)

/**
 * Cria as lojas de exemplo (Thai Cookies e LD Lanches) com cardápio, horários e depoimentos fictícios,
 * para o ambiente local poder ser recriado do zero. Só cria o que ainda não existe: rodar de novo não
 * duplica nada e não mexe em lojas já cadastradas.
 *
 * Usa os mesmos serviços do painel, então todas as regras de domínio valem para os dados de exemplo.
 */
class DadosDeExemplo(
    private val lojas: ServicoLojas,
    private val catalogo: ServicoCatalogo,
    private val depoimentos: ServicoDepoimentos,
) {
    private val log = LoggerFactory.getLogger(DadosDeExemplo::class.java)

    /** Processo interno do servidor agindo como super admin (não é um usuário que faz login). */
    private val sistema = Ator(usuarioId = 0, papel = Papel.SUPER_ADMIN, lojaId = null)

    suspend fun criar(thai: DonoDeExemplo?, ld: DonoDeExemplo?) {
        val existentes = lojas.listar(sistema).map { it.slug }.toSet()
        if (thai != null && "thai-cookies" !in existentes) criarThaiCookies(thai)
        if (ld != null && "ld-lanches" !in existentes) criarLdLanches(ld)
    }

    private suspend fun criarThaiCookies(dono: DonoDeExemplo) {
        val loja = lojas.criar(sistema, NovaLojaDto(
            slug = "thai-cookies",
            nome = "Thai Cookies",
            donoNome = "Thai",
            donoEmail = dono.email,
            donoSenha = dono.senha,
            modulos = setOf(Modulo.ENTREGA, Modulo.RETIRADA, Modulo.AGENDAMENTO),
            aparencia = AparenciaDto(
                corPrimaria = "#72C2F6",
                corSecundaria = "#F27D96",
                corFundo = "#EAF5FA",
                logoUrl = "/thai-cookies/lojas/thai-cookies/logo.svg",
                bannerUrl = "/thai-cookies/lojas/thai-cookies/banner.svg",
                modeloLayout = ModeloLayout.GRADE,
                mensagemBoasVindas = "Cookies feitos com carinho 🍪\nProdução artesanal em São Paulo. Escolha seus sabores e encomende!",
                aviso = "Cookies por encomenda • faça seu pedido com 1 dia de antecedência",
                cssExtra = "body{background-image:linear-gradient(rgba(114,194,246,.16) 1px,transparent 1px)," +
                    "linear-gradient(90deg,rgba(114,194,246,.16) 1px,transparent 1px);background-size:28px 28px;}",
            ),
        ))
        val donoDaLoja = Ator(usuarioId = 0, papel = Papel.DONO, lojaId = loja.id)

        lojas.atualizarInformacoes(donoDaLoja, InformacoesLojaDto(
            contato = ContatoLojaDto(
                telefone = "11999990000",
                instagram = "thaicookies_",
                endereco = EnderecoDto("Rua das Confeitarias", "123", "Retirada na portaria", "Vila Mariana", "São Paulo", "SP", "04101000"),
            ),
            funcionamento = FuncionamentoDto(
                // terça a sexta 10h–18h, sábado 10h–14h
                horarios = (2..5).map { IntervaloHorarioDto(it, "10:00", "18:00") } + IntervaloHorarioDto(6, "10:00", "14:00"),
                encomenda = EncomendaConfigDto(
                    aceitaAgendamento = true,
                    exigeAgendamento = true,
                    antecedenciaMinimaHoras = 24,
                    diasMaximos = 14,
                    limitePedidosPorDia = 8,
                    intervaloMinutos = 60,
                ),
            ),
            entrega = EntregaConfigDto(taxaCentavos = 1000, pedidoMinimoCentavos = 3000),
        ))

        val classicos = catalogo.criarCategoria(donoDaLoja, SalvarCategoriaDto("Cookies clássicos", 0)).id
        val recheados = catalogo.criarCategoria(donoDaLoja, SalvarCategoriaDto("Cookies recheados", 1)).id
        val boxes = catalogo.criarCategoria(donoDaLoja, SalvarCategoriaDto("Boxes", 2)).id

        val extras = GrupoOpcoesDto(nome = "Extras", minimo = 0, maximo = 2, opcoes = listOf(
            OpcaoDto(nome = "Calda extra de Nutella", precoCentavos = 300),
            OpcaoDto(nome = "Embalagem para presente", precoCentavos = 400),
        ))
        val sabores = listOf("Tradicional", "Nutella", "Dark Nutella", "Maracujá", "Red Velvet", "Black Cookie")
        fun escolhaDeSabores(quantidade: Int) = GrupoOpcoesDto(
            nome = "Escolha $quantidade sabores", minimo = quantidade, maximo = quantidade, permiteRepetir = true,
            opcoes = sabores.map { OpcaoDto(nome = it) },
        )
        val presente = GrupoOpcoesDto(nome = "Presente", minimo = 0, maximo = 1,
            opcoes = listOf(OpcaoDto(nome = "Laço de presente", precoCentavos = 300)))

        listOf(
            SalvarProdutoDto(classicos, "Tradicional", "Massa amanteigada com gotas de chocolate ao leite. Crocante por fora, macio por dentro.",
                900, precoPromocionalCentavos = 790, grupos = listOf(extras)),
            SalvarProdutoDto(classicos, "Nutella", "Cookie tradicional recheado e finalizado com fios de Nutella.",
                1200, grupos = listOf(extras), destaque = true),
            SalvarProdutoDto(classicos, "Maracujá", "Massa clara com recheio cremoso de maracujá e gotas de chocolate branco.",
                1200, grupos = listOf(extras)),
            SalvarProdutoDto(recheados, "Dark Nutella", "Massa de cacau com gotas de chocolate branco e coração de Nutella.",
                1300, grupos = listOf(extras), destaque = true),
            SalvarProdutoDto(recheados, "Red Velvet", "Massa red velvet com recheio cremoso de cream cheese.",
                1300, grupos = listOf(extras)),
            SalvarProdutoDto(recheados, "Black Cookie", "Massa de cacau black com recheio de creme de baunilha.",
                1300, grupos = listOf(extras)),
            SalvarProdutoDto(boxes, "Box com 4 cookies", "Escolha 4 sabores. Ótimo para presentear.",
                4400, grupos = listOf(escolhaDeSabores(4), presente), destaque = true),
            SalvarProdutoDto(boxes, "Box com 6 cookies", "Escolha 6 sabores.",
                6200, grupos = listOf(escolhaDeSabores(6), presente)),
        ).forEach { catalogo.criarProduto(donoDaLoja, it) }

        listOf(
            DepoimentoDto(autor = "Mariana S.", texto = "O Dark Nutella é simplesmente o melhor cookie que já comi. Chegou quentinho!", nota = 5, ordem = 0),
            DepoimentoDto(autor = "Rafael T.", texto = "Encomendei o box de 6 para o aniversário da minha mãe e todo mundo amou.", nota = 5, ordem = 1),
            DepoimentoDto(autor = "Juliana P.", texto = "Recheio cremoso de verdade, dá pra ver o carinho em cada cookie.", nota = 5, ordem = 2),
        ).forEach { depoimentos.criar(donoDaLoja, it) }

        log.info("Loja de exemplo criada: thai-cookies")
    }

    private suspend fun criarLdLanches(dono: DonoDeExemplo) {
        val loja = lojas.criar(sistema, NovaLojaDto(
            slug = "ld-lanches",
            nome = "LD Lanches",
            donoNome = "LD",
            donoEmail = dono.email,
            donoSenha = dono.senha,
            modulos = setOf(Modulo.ENTREGA, Modulo.RETIRADA),
            aparencia = AparenciaDto(
                corPrimaria = "#C62828",
                corSecundaria = "#FFB703",
                corFundo = "#FFF8F6",
                logoUrl = "/ld-lanches/lojas/ld-lanches/logo.png",
                bannerUrl = "/ld-lanches/lojas/ld-lanches/banner.png",
                modeloLayout = ModeloLayout.LISTA,
                aviso = "Entregamos em todo o Itaim Paulista!",
            ),
        ))
        val donoDaLoja = Ator(usuarioId = 0, papel = Papel.DONO, lojaId = loja.id)
        lojas.atualizarInformacoes(donoDaLoja, InformacoesLojaDto(entrega = EntregaConfigDto(taxaCentavos = 500)))

        val lanches = catalogo.criarCategoria(donoDaLoja, SalvarCategoriaDto("Lanches", 0)).id
        val bebidas = catalogo.criarCategoria(donoDaLoja, SalvarCategoriaDto("Bebidas", 1)).id

        val adicionais = GrupoOpcoesDto(nome = "Adicionais", minimo = 0, maximo = 10, permiteRepetir = true, opcoes = listOf(
            OpcaoDto(nome = "Bacon", precoCentavos = 400),
            OpcaoDto(nome = "Ovo", precoCentavos = 200),
        ))
        listOf(
            SalvarProdutoDto(lanches, "X-Tudo", "Hambúrguer, ovo, bacon, presunto, queijo", 3290, grupos = listOf(adicionais), destaque = true),
            SalvarProdutoDto(lanches, "X-Salada", "Hambúrguer, alface, tomate, queijo", 2490, grupos = listOf(adicionais)),
            SalvarProdutoDto(bebidas, "Refrigerante lata", "", 600),
        ).forEach { catalogo.criarProduto(donoDaLoja, it) }

        log.info("Loja de exemplo criada: ld-lanches")
    }
}
