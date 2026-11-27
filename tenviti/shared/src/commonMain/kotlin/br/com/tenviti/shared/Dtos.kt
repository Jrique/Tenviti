package br.com.tenviti.shared

import kotlinx.serialization.Serializable

// Contratos da API (JSON) compartilhados entre servidor, app e testes.
// Valores em dinheiro sempre em CENTAVOS (Long) para evitar erro de arredondamento.
// Datas/horas sempre em ISO-8601 (UTC) como String; quem exibe converte para o fuso local.

// ---------- Erros ----------

@Serializable
data class ErroDto(val mensagem: String, val codigo: String)

// ---------- Autenticação ----------

@Serializable
data class LoginRequisicao(val email: String, val senha: String)

@Serializable
data class SessaoDto(val token: String, val usuario: UsuarioDto)

@Serializable
data class TrocarSenhaRequisicao(val senhaAtual: String, val novaSenha: String)

// ---------- Usuários ----------

@Serializable
data class UsuarioDto(
    val id: Long,
    val nome: String,
    val email: String,
    val papel: Papel,
    val lojaId: Long? = null,
    val lojaSlug: String? = null,
    val ativo: Boolean = true,
)

@Serializable
data class NovoUsuarioDto(
    val nome: String,
    val email: String,
    val senha: String,
    val papel: Papel,
)

/** Atualização de usuário. [senha] nula ou vazia mantém a senha atual. */
@Serializable
data class AtualizarUsuarioDto(
    val nome: String,
    val email: String,
    val papel: Papel,
    val ativo: Boolean = true,
    val senha: String? = null,
)

// ---------- Lojas ----------

@Serializable
data class AparenciaDto(
    val corPrimaria: String = "#790001",
    val corSecundaria: String = "#FFC107",
    val corFundo: String = "#FFF8F6",
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val modeloLayout: ModeloLayout = ModeloLayout.LISTA,
    val mensagemBoasVindas: String? = null,
    val aviso: String? = null,
    val cssExtra: String? = null,
)

@Serializable
data class LojaDto(
    val id: Long,
    val slug: String,
    val nome: String,
    val ativa: Boolean,
    val fusoHorario: String,
    val aparencia: AparenciaDto,
    val modulos: Set<Modulo>,
    val personalizacaoLiberada: Set<ItemPersonalizavel>,
)

@Serializable
data class NovaLojaDto(
    val slug: String,
    val nome: String,
    val donoNome: String,
    val donoEmail: String,
    val donoSenha: String,
    val fusoHorario: String = "America/Sao_Paulo",
    val modulos: Set<Modulo> = Modulo.PADRAO,
    val personalizacaoLiberada: Set<ItemPersonalizavel> = ItemPersonalizavel.PADRAO,
    val aparencia: AparenciaDto = AparenciaDto(),
)

/** Atualização completa feita pelo super admin. */
@Serializable
data class AtualizarLojaDto(
    val slug: String,
    val nome: String,
    val ativa: Boolean,
    val fusoHorario: String,
    val aparencia: AparenciaDto,
    val modulos: Set<Modulo>,
    val personalizacaoLiberada: Set<ItemPersonalizavel>,
)

// ---------- Dados e funcionamento da loja ----------

@Serializable
data class EnderecoDto(
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String? = null,
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val cep: String = "",
)

@Serializable
data class ContatoLojaDto(
    /** Telefone/WhatsApp só com dígitos, com DDD (ex.: 11987654321). */
    val telefone: String? = null,
    /** Usuário do Instagram sem o @. */
    val instagram: String? = null,
    val endereco: EnderecoDto? = null,
)

/** Intervalo de funcionamento. [diaSemana]: 1 = segunda … 7 = domingo. Horas "HH:mm"; fechar antes de abrir = vira a noite. */
@Serializable
data class IntervaloHorarioDto(val diaSemana: Int, val abre: String, val fecha: String)

@Serializable
data class EncomendaConfigDto(
    /** Cliente pode escolher data/horário (exige o módulo AGENDAMENTO). */
    val aceitaAgendamento: Boolean = false,
    /** Todo pedido precisa ser agendado (loja só trabalha por encomenda). */
    val exigeAgendamento: Boolean = false,
    val antecedenciaMinimaHoras: Int = 0,
    val diasMaximos: Int = 14,
    /** Máximo de pedidos por dia de produção; null = sem limite. */
    val limitePedidosPorDia: Int? = null,
    val intervaloMinutos: Int = 30,
)

@Serializable
data class FuncionamentoDto(
    val pausada: Boolean = false,
    val mensagemPausa: String? = null,
    val horarios: List<IntervaloHorarioDto> = emptyList(),
    val encomenda: EncomendaConfigDto = EncomendaConfigDto(),
)

/** Taxa de entrega fixa e valor mínimo do pedido (em centavos; 0 = sem taxa / sem mínimo). */
@Serializable
data class EntregaConfigDto(
    val taxaCentavos: Long = 0,
    val pedidoMinimoCentavos: Long = 0,
)

/** Dados operacionais editados pelo dono/gerente (diferente da aparência, que tem permissões próprias). */
@Serializable
data class InformacoesLojaDto(
    val contato: ContatoLojaDto = ContatoLojaDto(),
    val funcionamento: FuncionamentoDto = FuncionamentoDto(),
    val entrega: EntregaConfigDto = EntregaConfigDto(),
)

/** O que o cliente final vê da loja no cardápio público. */
@Serializable
data class VitrineLojaDto(
    val slug: String,
    val nome: String,
    val aparencia: AparenciaDto,
    val modulos: Set<Modulo>,
    val contato: ContatoLojaDto = ContatoLojaDto(),
    val horarios: List<IntervaloHorarioDto> = emptyList(),
    val encomenda: EncomendaConfigDto = EncomendaConfigDto(),
    val entrega: EntregaConfigDto = EntregaConfigDto(),
    /** Aberta agora (considera horários e pausa manual). */
    val aberta: Boolean = true,
    val pausada: Boolean = false,
    val mensagemPausa: String? = null,
)

/** Dia disponível para encomenda e seus horários ("HH:mm"). */
@Serializable
data class DiaAgendaDto(val data: String, val horarios: List<String>, val lotado: Boolean = false)

@Serializable
data class DepoimentoDto(
    val id: Long = 0,
    val autor: String,
    val texto: String,
    /** Nota de 1 a 5 (opcional). */
    val nota: Int? = null,
    val ativo: Boolean = true,
    val ordem: Int = 0,
)

@Serializable
data class ArquivoDto(val url: String)

// ---------- Catálogo ----------

@Serializable
data class CategoriaDto(val id: Long, val nome: String, val ordem: Int = 0)

@Serializable
data class SalvarCategoriaDto(val nome: String, val ordem: Int = 0)

@Serializable
data class OpcaoDto(
    val id: Long = 0,
    val nome: String,
    val precoCentavos: Long = 0,
    val ativo: Boolean = true,
)

/**
 * Grupo de opções de um produto. Ex.: "Escolha 4 sabores" (mínimo 4, máximo 4, pode repetir) ou
 * "Adicionais" (mínimo 0, máximo 3). [minimo] > 0 torna o grupo obrigatório.
 */
@Serializable
data class GrupoOpcoesDto(
    val id: Long = 0,
    val nome: String,
    val minimo: Int = 0,
    val maximo: Int = 1,
    /** Permite escolher a mesma opção mais de uma vez (ex.: 2x Nutella num box). */
    val permiteRepetir: Boolean = false,
    val opcoes: List<OpcaoDto> = emptyList(),
)

@Serializable
data class ProdutoDto(
    val id: Long,
    val categoriaId: Long? = null,
    val categoriaNome: String? = null,
    val nome: String,
    val descricao: String = "",
    val precoCentavos: Long,
    /** Preço "por" quando o produto está em promoção (menor que [precoCentavos]). */
    val precoPromocionalCentavos: Long? = null,
    val imagemUrl: String? = null,
    val grupos: List<GrupoOpcoesDto> = emptyList(),
    val destaque: Boolean = false,
    /** false = pausado/esgotado: some do cardápio público sem ser apagado. */
    val ativo: Boolean = true,
)

@Serializable
data class SalvarProdutoDto(
    val categoriaId: Long? = null,
    val nome: String,
    val descricao: String = "",
    val precoCentavos: Long,
    val precoPromocionalCentavos: Long? = null,
    val imagemUrl: String? = null,
    val grupos: List<GrupoOpcoesDto> = emptyList(),
    val destaque: Boolean = false,
    val ativo: Boolean = true,
)

// ---------- Pedidos ----------

/** Opção escolhida num item do pedido, como ficou registrada (nome e preço da hora da compra). */
@Serializable
data class OpcaoDoItemDto(
    val grupo: String,
    val nome: String,
    val precoCentavos: Long,
    val quantidade: Int,
)

/** Item de um pedido já feito. Nome e preços são uma "foto" do momento da compra. */
@Serializable
data class ItemPedidoDto(
    val produtoId: Long?,
    val nome: String,
    val quantidade: Int,
    val precoUnitarioCentavos: Long,
    val opcoes: List<OpcaoDoItemDto> = emptyList(),
    val observacao: String? = null,
    val totalCentavos: Long,
)

@Serializable
data class ClientePedidoDto(
    val nome: String,
    /** Só números, com DDD. */
    val telefone: String,
)

@Serializable
data class PedidoDto(
    val id: Long,
    val codigo: String,
    /** Resumo em texto (gerado pelo servidor; nos pedidos antigos é o texto enviado pelo cardápio). */
    val descricao: String,
    val totalCentavos: Long,
    val status: StatusPedido,
    val criadoEm: String,
    /** Data/hora da encomenda (ISO-8601 UTC), quando agendado. */
    val agendadoPara: String? = null,
    // Pedidos feitos a partir da Fase 2 (vazios/nulos nos pedidos antigos)
    val itens: List<ItemPedidoDto> = emptyList(),
    val cliente: ClientePedidoDto? = null,
    val tipoEntrega: TipoEntrega? = null,
    val endereco: EnderecoDto? = null,
    val formaPagamento: FormaPagamento? = null,
    val trocoParaCentavos: Long? = null,
    val observacao: String? = null,
    val subtotalCentavos: Long? = null,
    val taxaEntregaCentavos: Long = 0,
)

/** Opção escolhida pelo cliente: o id vem de ProdutoDto.grupos[].opcoes[].id. */
@Serializable
data class OpcaoEscolhidaDto(val opcaoId: Long, val quantidade: Int = 1)

/** Item da sacola: só ids e quantidades; nomes e preços quem define é o servidor. */
@Serializable
data class ItemNovoPedidoDto(
    val produtoId: Long,
    val quantidade: Int = 1,
    /** Opções por unidade do produto (ex.: 2x Nutella + 2x Red Velvet num box de 4). */
    val opcoes: List<OpcaoEscolhidaDto> = emptyList(),
    val observacao: String? = null,
)

/** Pedido enviado pela vitrine pública. O total é calculado no servidor. */
@Serializable
data class NovoPedidoPublicoDto(
    val itens: List<ItemNovoPedidoDto>,
    val cliente: ClientePedidoDto,
    val tipoEntrega: TipoEntrega,
    /** Obrigatório na entrega. */
    val endereco: EnderecoDto? = null,
    val formaPagamento: FormaPagamento,
    /** Pagamento em dinheiro: troco para quanto (opcional). */
    val trocoParaCentavos: Long? = null,
    val observacao: String? = null,
    /** Encomenda: data e hora locais da loja, "AAAA-MM-DDTHH:mm" (um dos horários de /agenda). */
    val agendadoPara: String? = null,
    /**
     * Total que o cliente viu na tela. Se os preços mudaram desde então, o pedido é recusado
     * (código "total_alterado") para o cliente conferir antes de confirmar.
     */
    val totalEsperadoCentavos: Long? = null,
)

/** O que o cliente consegue consultar do próprio pedido (sem dados pessoais). */
@Serializable
data class PedidoPublicoDto(
    val codigo: String,
    val status: StatusPedido,
    val totalCentavos: Long,
    val criadoEm: String,
    val agendadoPara: String? = null,
    val itens: List<ItemPedidoDto> = emptyList(),
    val subtotalCentavos: Long? = null,
    val taxaEntregaCentavos: Long = 0,
    val tipoEntrega: TipoEntrega? = null,
    val formaPagamento: FormaPagamento? = null,
)

@Serializable
data class AlterarStatusDto(val status: StatusPedido)

// ---------- Dashboard ----------

@Serializable
data class ResumoDashboardDto(
    val faturamentoDiarioCentavos: List<Long>,
    val vendasDiarias: List<Int>,
    val rotulosDias: List<String>,
    val totalFaturamentoCentavos: Long,
    val totalVendas: Int,
)
