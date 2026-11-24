package br.com.cardapio.server.aplicacao

import br.com.cardapio.server.dominio.Aparencia
import br.com.cardapio.server.dominio.Categoria
import br.com.cardapio.server.dominio.ConfigEncomenda
import br.com.cardapio.server.dominio.ContatoLoja
import br.com.cardapio.server.dominio.DadoInvalido
import br.com.cardapio.server.dominio.Depoimento
import br.com.cardapio.server.dominio.DiaAgenda
import br.com.cardapio.server.dominio.Endereco
import br.com.cardapio.server.dominio.Funcionamento
import br.com.cardapio.server.dominio.GrupoOpcoes
import br.com.cardapio.server.dominio.IntervaloHorario
import br.com.cardapio.server.dominio.Loja
import br.com.cardapio.server.dominio.Opcao
import br.com.cardapio.server.dominio.Pedido
import br.com.cardapio.server.dominio.Produto
import br.com.cardapio.server.dominio.Usuario
import br.com.cardapio.shared.AparenciaDto
import br.com.cardapio.shared.CategoriaDto
import br.com.cardapio.shared.ContatoLojaDto
import br.com.cardapio.shared.DepoimentoDto
import br.com.cardapio.shared.DiaAgendaDto
import br.com.cardapio.shared.EncomendaConfigDto
import br.com.cardapio.shared.EnderecoDto
import br.com.cardapio.shared.FuncionamentoDto
import br.com.cardapio.shared.GrupoOpcoesDto
import br.com.cardapio.shared.InformacoesLojaDto
import br.com.cardapio.shared.IntervaloHorarioDto
import br.com.cardapio.shared.LojaDto
import br.com.cardapio.shared.OpcaoDto
import br.com.cardapio.shared.PedidoDto
import br.com.cardapio.shared.PedidoPublicoDto
import br.com.cardapio.shared.ProdutoDto
import br.com.cardapio.shared.UsuarioDto
import br.com.cardapio.shared.VitrineLojaDto
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
        aberta = f.estaAberta(agora),
        pausada = f.pausada,
        mensagemPausa = f.mensagemPausa,
    )
}

// ---------- Informações da loja ----------

fun Loja.paraInformacoesDto() = InformacoesLojaDto(contato.paraDto(), funcionamento.paraDto())

fun ContatoLoja.paraDto() = ContatoLojaDto(telefone, instagram, endereco?.let {
    EnderecoDto(it.logradouro, it.numero, it.complemento, it.bairro, it.cidade, it.uf, it.cep)
})

fun Funcionamento.paraDto() = FuncionamentoDto(pausada, mensagemPausa, horarios.map { it.paraDto() }, encomenda.paraDto())

fun IntervaloHorario.paraDto() = IntervaloHorarioDto(dia.value, abre.format(HORA), fecha.format(HORA))

fun ConfigEncomenda.paraDto() =
    EncomendaConfigDto(aceitaAgendamento, exigeAgendamento, antecedenciaMinimaHoras, diasMaximos, limitePedidosPorDia, intervaloMinutos)

fun ContatoLojaDto.paraDominio() = ContatoLoja(
    telefone = telefone?.filter { it.isDigit() }?.ifEmpty { null },
    instagram = instagram?.trim()?.removePrefix("@")?.ifEmpty { null },
    endereco = endereco?.let {
        Endereco(
            logradouro = it.logradouro.trim(),
            numero = it.numero.trim(),
            complemento = it.complemento?.trim()?.ifEmpty { null },
            bairro = it.bairro.trim(),
            cidade = it.cidade.trim(),
            uf = it.uf.trim().uppercase(),
            cep = it.cep.filter { c -> c.isDigit() },
        )
    }?.takeUnless { it.vazio },
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

fun Pedido.paraDto() = PedidoDto(id, codigo, descricao, totalCentavos, status, criadoEm.toString(), agendadoPara?.toString())

fun Pedido.paraPublico() = PedidoPublicoDto(codigo, status, totalCentavos, criadoEm.toString(), agendadoPara?.toString())
