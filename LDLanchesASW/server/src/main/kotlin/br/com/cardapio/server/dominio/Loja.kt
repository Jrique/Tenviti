package br.com.cardapio.server.dominio

import br.com.cardapio.shared.Formatos
import br.com.cardapio.shared.ItemPersonalizavel
import br.com.cardapio.shared.ModeloLayout
import br.com.cardapio.shared.Modulo
import java.time.ZoneId

/** Identidade visual da loja. Valida os próprios campos na criação. */
data class Aparencia(
    val corPrimaria: String = "#790001",
    val corSecundaria: String = "#FFC107",
    val corFundo: String = "#FFF8F6",
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val modeloLayout: ModeloLayout = ModeloLayout.LISTA,
    val mensagemBoasVindas: String? = null,
    val aviso: String? = null,
    val cssExtra: String? = null,
) {
    init {
        listOf(corPrimaria, corSecundaria, corFundo).forEach { cor ->
            exigir(Formatos.corValida(cor)) { "Cor inválida: \"$cor\". Use o formato #RRGGBB." }
        }
        listOfNotNull(logoUrl, bannerUrl).forEach { url ->
            exigir(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/")) {
                "Endereço de imagem inválido: \"$url\"."
            }
            exigir(url.length <= 500) { "Endereço de imagem muito longo (máximo 500 caracteres)." }
        }
        exigir((mensagemBoasVindas?.length ?: 0) <= 300) { "A mensagem de boas-vindas pode ter no máximo 300 caracteres." }
        exigir((aviso?.length ?: 0) <= 300) { "O aviso pode ter no máximo 300 caracteres." }
        exigir((cssExtra?.length ?: 0) <= 20_000) { "O CSS extra pode ter no máximo 20.000 caracteres." }
        exigir(cssExtra?.contains("</style", ignoreCase = true) != true) { "O CSS extra não pode conter a tag </style>." }
    }

    /** Quais itens personalizáveis mudaram entre esta aparência e [nova]. */
    fun itensAlteradosPara(nova: Aparencia): Set<ItemPersonalizavel> = buildSet {
        if (corPrimaria != nova.corPrimaria || corSecundaria != nova.corSecundaria || corFundo != nova.corFundo) {
            add(ItemPersonalizavel.CORES)
        }
        if (logoUrl != nova.logoUrl) add(ItemPersonalizavel.LOGO)
        if (bannerUrl != nova.bannerUrl || mensagemBoasVindas != nova.mensagemBoasVindas || aviso != nova.aviso) {
            add(ItemPersonalizavel.BANNERS_E_TEXTOS)
        }
        if (modeloLayout != nova.modeloLayout) add(ItemPersonalizavel.LAYOUT)
        if (cssExtra != nova.cssExtra) add(ItemPersonalizavel.CSS_EXTRA)
    }
}

/**
 * Loja (tenant). Cada loja tem seu cardápio, usuários, pedidos e personalização.
 * O super admin configura tudo; o dono só altera os itens de aparência que foram liberados para ele.
 */
class Loja(
    val id: Long,
    slug: String,
    nome: String,
    ativa: Boolean,
    fusoHorario: String,
    aparencia: Aparencia,
    modulos: Set<Modulo>,
    personalizacaoLiberada: Set<ItemPersonalizavel>,
    contato: ContatoLoja = ContatoLoja(),
    funcionamento: Funcionamento = Funcionamento(),
) {
    var slug: String = slug; private set
    var nome: String = nome; private set
    var ativa: Boolean = ativa; private set
    var fusoHorario: String = fusoHorario; private set
    var aparencia: Aparencia = aparencia; private set
    var modulos: Set<Modulo> = modulos; private set
    var personalizacaoLiberada: Set<ItemPersonalizavel> = personalizacaoLiberada; private set
    var contato: ContatoLoja = contato; private set
    var funcionamento: Funcionamento = funcionamento; private set

    init {
        validarSlug(slug)
        validarNome(nome)
        validarFuso(fusoHorario)
    }

    val zona: ZoneId get() = ZoneId.of(fusoHorario)

    /** Alteração feita pelo super admin: pode mudar qualquer coisa. */
    fun configurarPeloSuperAdmin(
        slug: String,
        nome: String,
        ativa: Boolean,
        fusoHorario: String,
        aparencia: Aparencia,
        modulos: Set<Modulo>,
        personalizacaoLiberada: Set<ItemPersonalizavel>,
    ) {
        validarSlug(slug)
        validarNome(nome)
        validarFuso(fusoHorario)
        this.slug = slug
        this.nome = nome.trim()
        this.ativa = ativa
        this.fusoHorario = fusoHorario
        this.aparencia = aparencia
        this.modulos = modulos
        this.personalizacaoLiberada = personalizacaoLiberada
    }

    /** Alteração feita pelo dono: só os itens que o super admin liberou. */
    fun personalizarPeloDono(nova: Aparencia) {
        val bloqueados = aparencia.itensAlteradosPara(nova) - personalizacaoLiberada
        if (bloqueados.isNotEmpty()) {
            throw AcessoNegado(
                "Sua loja não tem permissão para alterar: ${bloqueados.joinToString { it.rotulo.lowercase() }}. " +
                    "Fale com o suporte para liberar.",
                "personalizacao_bloqueada",
            )
        }
        aparencia = nova
    }

    fun visivelAoPublico(): Boolean = ativa

    /** Dono/gerente atualizam contato, endereço, horários e regras de encomenda. */
    fun atualizarInformacoes(contato: ContatoLoja, funcionamento: Funcionamento) {
        this.contato = contato
        this.funcionamento = funcionamento
    }

    /**
     * Funcionamento que vale na prática: sem o módulo AGENDAMENTO (ligado pelo super admin),
     * a loja não aceita encomendas agendadas, mesmo que a configuração esteja salva.
     */
    val funcionamentoEfetivo: Funcionamento
        get() = if (Modulo.AGENDAMENTO in modulos) {
            funcionamento
        } else {
            funcionamento.copy(encomenda = funcionamento.encomenda.copy(aceitaAgendamento = false, exigeAgendamento = false))
        }

    companion object {
        fun nova(
            slug: String,
            nome: String,
            fusoHorario: String,
            aparencia: Aparencia,
            modulos: Set<Modulo>,
            personalizacaoLiberada: Set<ItemPersonalizavel>,
        ) = Loja(0, slug, nome.trim(), true, fusoHorario, aparencia, modulos, personalizacaoLiberada)

        private fun validarSlug(slug: String) = exigir(Formatos.slugValido(slug)) {
            "Endereço da loja inválido: use de 3 a 50 letras minúsculas, números ou hífen (ex.: ld-lanches)."
        }

        private fun validarNome(nome: String) = exigir(nome.isNotBlank() && nome.length <= 120) {
            "O nome da loja é obrigatório e pode ter no máximo 120 caracteres."
        }

        private fun validarFuso(fuso: String) = exigir(runCatching { ZoneId.of(fuso) }.isSuccess) {
            "Fuso horário inválido: \"$fuso\"."
        }
    }
}
