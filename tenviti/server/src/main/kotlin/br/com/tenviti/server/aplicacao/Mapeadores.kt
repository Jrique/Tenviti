package br.com.tenviti.server.aplicacao

import br.com.tenviti.server.dominio.Aparencia
import br.com.tenviti.server.dominio.Categoria
import br.com.tenviti.server.dominio.ConfigEncomenda
import br.com.tenviti.server.dominio.ConfigEntrega
import br.com.tenviti.server.dominio.ContatoLoja
import br.com.tenviti.server.dominio.DadoInvalido
import br.com.tenviti.server.dominio.Depoimento
import br.com.tenviti.server.dominio.DiaAgenda
import br.com.tenviti.server.dominio.Endereco
import br.com.tenviti.server.dominio.Funcionamento
import br.com.tenviti.server.dominio.GrupoOpcoes
import br.com.tenviti.server.dominio.IntervaloHorario
import br.com.tenviti.server.dominio.ItemPedido
import br.com.tenviti.server.dominio.Loja
import br.com.tenviti.server.dominio.Opcao
import br.com.tenviti.server.dominio.Pedido
import br.com.tenviti.server.dominio.Produto
import br.com.tenviti.server.dominio.Usuario
import br.com.tenviti.shared.AparenciaDto
import br.com.tenviti.shared.ClientePedidoDto
import br.com.tenviti.shared.CategoriaDto
import br.com.tenviti.shared.ContatoLojaDto
import br.com.tenviti.shared.DepoimentoDto
import br.com.tenviti.shared.DiaAgendaDto
import br.com.tenviti.shared.EncomendaConfigDto
import br.com.tenviti.shared.EnderecoDto
import br.com.tenviti.shared.EntregaConfigDto
import br.com.tenviti.shared.FuncionamentoDto
import br.com.tenviti.shared.GrupoOpcoesDto
import br.com.tenviti.shared.InformacoesLojaDto
import br.com.tenviti.shared.IntervaloHorarioDto
import br.com.tenviti.shared.ItemPedidoDto
import br.com.tenviti.shared.OpcaoDoItemDto
import br.com.tenviti.shared.LojaDto
import br.com.tenviti.shared.OpcaoDto
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.PedidoPublicoDto
import br.com.tenviti.shared.ProdutoDto
import br.com.tenviti.shared.UsuarioDto
import br.com.tenviti.shared.VitrineLojaDto
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Conversões entre o domínio e os DTOs de transporte (módulo shared).

private val HORA = DateTimeFormatter.ofPattern("HH:mm")

fun Aparencia.paraDto() = AparenciaDto(
    corPrimaria, corSecundaria, corFundo, logoUrl, bannerUrl, modeloLayout, mensagemBoasVindas, aviso, cssExtra,
)

fun AparenciaDto.paraDominio() = Aparencia(
    corPrimaria = corPrimaria.uppercase(),
    corSecundaria = corSecundaria.uppercase(),
    corFundo = corFundo.uppercase(),
    logoUrl = logoUrl?.trim()?.ifEmpty { null },
    bannerUrl = bannerUrl?.trim()?.ifEmpty { null },
    modeloLayout = modeloLayout,
    mensagemBoasVindas = mensagemBoasVindas?.trim()?.ifEmpty { null },
    aviso = aviso?.trim()?.ifEmpty { null },
    cssExtra = cssExtra?.ifBlank { null },
)

fun Loja.paraDto() = LojaDto(id, slug, nome, ativa, fusoHorario, aparencia.paraDto(), modulos, personalizacaoLiberada)

fun Loja.paraVitrine(agora: ZonedDateTime): VitrineLojaDto {
    val f = funcionamentoEfetivo
    return VitrineLojaDto(
        slug = slug,
        nome = nome,
        aparencia = aparencia.paraDto(),
        modulos = modulos,
        contato = contato.paraDto(),
        horarios = f.horarios.map { it.paraDto() },
        encomenda = f.encomenda.paraDto(),
        entrega = entrega.paraDto(),
        aberta = f.estaAberta(agora),
        pausada = f.pausada,
        mensagemPausa = f.mensagemPausa,
    )
}

// ---------- Informações da loja ----------

fun Loja.paraInformacoesDto() = InformacoesLojaDto(contato.paraDto(), funcionamento.paraDto(), entrega.paraDto())

fun ConfigEntrega.paraDto() = EntregaConfigDto(taxaCentavos, pedidoMinimoCentavos)

fun EntregaConfigDto.paraDominio() = ConfigEntrega(taxaCentavos, pedidoMinimoCentavos)

fun ContatoLoja.paraDto() = ContatoLojaDto(telefone, instagram, endereco?.paraDto())

fun Endereco.paraDto() = EnderecoDto(logradouro, numero, complemento, bairro, cidade, uf, cep)

/** Normaliza o que veio do formulário: espaços, UF em maiúsculas, CEP só com números. */
fun EnderecoDto.paraDominio() = Endereco(
    logradouro = logradouro.trim(),
    numero = numero.trim(),
    complemento = complemento?.trim()?.ifEmpty { null },
    bairro = bairro.trim(),
    cidade = cidade.trim(),
    uf = uf.trim().uppercase(),
    cep = cep.filter { c -> c.isDigit() },
)

fun Funcionamento.paraDto() = FuncionamentoDto(pausada, mensagemPausa, horarios.map { it.paraDto() }, encomenda.paraDto())

fun IntervaloHorario.paraDto() = IntervaloHorarioDto(dia.value, abre.format(HORA), fecha.format(HORA))

fun ConfigEncomenda.paraDto() =
    EncomendaConfigDto(aceitaAgendamento, exigeAgendamento, antecedenciaMinimaHoras, diasMaximos, limitePedidosPorDia, intervaloMinutos)

fun ContatoLojaDto.paraDominio() = ContatoLoja(
    telefone = telefone?.filter { it.isDigit() }?.ifEmpty { null },
    instagram = instagram?.trim()?.removePrefix("@")?.ifEmpty { null },
    endereco = endereco?.paraDominio()?.takeUnless { it.vazio },
)

fun FuncionamentoDto.paraDominio() = Funcionamento(
    pausada = pausada,
    mensagemPausa = mensagemPausa?.trim()?.ifEmpty { null },
    horarios = horarios.map { it.paraDominio() },
    encomenda = ConfigEncomenda(
        aceitaAgendamento = encomenda.aceitaAgendamento,
        exigeAgendamento = encomenda.exigeAgendamento,
        antecedenciaMinimaHoras = encomenda.antecedenciaMinimaHoras,
        diasMaximos = encomenda.diasMaximos,
        limitePedidosPorDia = encomenda.limitePedidosPorDia,
        intervaloMinutos = encomenda.intervaloMinutos,
    ),
)

fun IntervaloHorarioDto.paraDominio(): IntervaloHorario {
    if (diaSemana !in 1..7) throw DadoInvalido("Dia da semana inválido: use de 1 (segunda) a 7 (domingo).")
    return IntervaloHorario(DayOfWeek.of(diaSemana), hora(abre), hora(fecha))
}

private fun hora(texto: String): LocalTime = try {
    LocalTime.parse(texto.trim(), HORA)
} catch (_: DateTimeParseException) {
    throw DadoInvalido("Horário inválido: \"$texto\". Use HH:mm (ex.: 18:30).")
}

fun DiaAgenda.paraDto() = DiaAgendaDto(data.toString(), horarios.map { it.format(HORA) }, lotado)

// ---------- Usuários ----------

fun Usuario.paraDto(lojaSlug: String? = null) = UsuarioDto(id, nome, email, papel, lojaId, lojaSlug, ativo)

// ---------- Catálogo ----------

fun Categoria.paraDto() = CategoriaDto(id, nome, ordem)

fun Produto.paraDto(categoriaNome: String?, somenteOpcoesAtivas: Boolean = false) = ProdutoDto(
    id = id,
    categoriaId = categoriaId,
    categoriaNome = categoriaNome,
    nome = nome,
    descricao = descricao,
    precoCentavos = precoCentavos,
    precoPromocionalCentavos = precoPromocionalCentavos,
    imagemUrl = imagemUrl,
    grupos = grupos.map { g ->
        GrupoOpcoesDto(
            g.id, g.nome, g.minimo, g.maximo, g.permiteRepetir,
            g.opcoes.filter { !somenteOpcoesAtivas || it.ativo }.map { OpcaoDto(it.id, it.nome, it.precoCentavos, it.ativo) },
        )
    },
    destaque = destaque,
    ativo = ativo,
)

fun GrupoOpcoesDto.paraDominio() = GrupoOpcoes(
    nome = nome.trim(),
    minimo = minimo,
    maximo = maximo,
    permiteRepetir = permiteRepetir,
    opcoes = opcoes.map { Opcao(nome = it.nome.trim(), precoCentavos = it.precoCentavos, ativo = it.ativo) },
)

fun Depoimento.paraDto() = DepoimentoDto(id, autor, texto, nota, ativo, ordem)

// ---------- Pedidos ----------

fun ItemPedido.paraDto() = ItemPedidoDto(
    produtoId = produtoId,
    nome = nome,
    quantidade = quantidade,
    precoUnitarioCentavos = precoUnitarioCentavos,
    opcoes = opcoes.map { OpcaoDoItemDto(it.grupo, it.nome, it.precoCentavos, it.quantidade) },
    observacao = observacao,
    totalCentavos = totalCentavos,
)

fun Pedido.paraDto() = PedidoDto(
    id = id,
    codigo = codigo,
    descricao = descricao,
    totalCentavos = totalCentavos,
    status = status,
    criadoEm = criadoEm.toString(),
    agendadoPara = agendadoPara?.toString(),
    itens = itens.map { it.paraDto() },
    cliente = cliente?.let { ClientePedidoDto(it.nome, it.telefone) },
    tipoEntrega = recebimento?.tipo,
    endereco = recebimento?.endereco?.paraDto(),
    formaPagamento = pagamento?.forma,
    trocoParaCentavos = pagamento?.trocoParaCentavos,
    observacao = observacao,
    subtotalCentavos = subtotalCentavos,
    taxaEntregaCentavos = taxaEntregaCentavos,
)

/** O cliente vê itens e valores, mas não os dados pessoais (quem tem o código não vê nome/telefone/endereço). */
fun Pedido.paraPublico() = PedidoPublicoDto(
    codigo = codigo,
    status = status,
    totalCentavos = totalCentavos,
    criadoEm = criadoEm.toString(),
    agendadoPara = agendadoPara?.toString(),
    itens = itens.map { it.paraDto() },
    subtotalCentavos = subtotalCentavos,
    taxaEntregaCentavos = taxaEntregaCentavos,
    tipoEntrega = recebimento?.tipo,
    formaPagamento = pagamento?.forma,
)
