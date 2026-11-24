package br.com.tenviti.shared

import kotlinx.serialization.Serializable

/** Papel (perfil de acesso) de um usuário no sistema. */
@Serializable
enum class Papel(val rotulo: String) {
    /** Dono da plataforma: cadastra e configura as lojas. Não pertence a nenhuma loja. */
    SUPER_ADMIN("Super admin"),
    DONO("Dono"),
    GERENTE("Gerente"),
    ATENDENTE("Atendente"),
    COZINHA("Cozinha"),
    ENTREGADOR("Entregador");

    val ehDaLoja: Boolean get() = this != SUPER_ADMIN

    companion object {
        val DA_LOJA: List<Papel> = entries.filter { it.ehDaLoja }
    }
}

/** Status de um pedido. FINALIZADO e CANCELADO são estados finais. */
@Serializable
enum class StatusPedido(val rotulo: String) {
    NOVO("Novo"),
    VISTO("Visto"),
    EM_PREPARO("Em preparo"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    val ehFinal: Boolean get() = this == FINALIZADO || this == CANCELADO
}

/** Recursos que podem ser ligados ou desligados por loja (definido pelo super admin). */
@Serializable
enum class Modulo(val rotulo: String) {
    ENTREGA("Entrega"),
    RETIRADA("Retirada no balcão"),
    MESA("Pedido na mesa (QR code)"),
    CUPONS("Cupons de desconto"),
    ESTOQUE("Controle de estoque"),
    FIDELIDADE("Fidelidade / cashback"),
    AGENDAMENTO("Pedido agendado"),
    RELATORIOS("Relatórios avançados");

    companion object {
        /** Módulos ligados por padrão em uma loja nova. */
        val PADRAO: Set<Modulo> = setOf(ENTREGA, RETIRADA)
    }
}

/** Partes da aparência que o super admin pode liberar para o dono da loja editar sozinho. */
@Serializable
enum class ItemPersonalizavel(val rotulo: String) {
    CORES("Cores"),
    LOGO("Logo"),
    BANNERS_E_TEXTOS("Banners e textos"),
    LAYOUT("Modelo de layout"),
    CSS_EXTRA("CSS extra");

    companion object {
        /** O que o dono pode editar por padrão. CSS extra fica só com o super admin. */
        val PADRAO: Set<ItemPersonalizavel> = setOf(CORES, LOGO, BANNERS_E_TEXTOS, LAYOUT)
    }
}

/** Modelos de layout disponíveis para o cardápio web. */
@Serializable
enum class ModeloLayout(val rotulo: String) {
    LISTA("Lista"),
    GRADE("Grade"),
    FOTOS("Destaque de fotos")
}
