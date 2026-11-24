package br.com.cardapio.server.infra.banco

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

// Mapeamento das tabelas criadas pelas migrações em resources/db/migracao.
// O schema é responsabilidade do Flyway; o Exposed só lê e grava.

object LojaTabela : LongIdTable("loja") {
    val slug = varchar("slug", 50)
    val nome = varchar("nome", 120)
    val ativa = bool("ativa")
    val fusoHorario = varchar("fuso_horario", 50)

    // aparência
    val corPrimaria = varchar("cor_primaria", 7)
    val corSecundaria = varchar("cor_secundaria", 7)
    val corFundo = varchar("cor_fundo", 7)
    val logoUrl = varchar("logo_url", 500).nullable()
    val bannerUrl = varchar("banner_url", 500).nullable()
    val modeloLayout = varchar("modelo_layout", 20)
    val mensagemBoasVindas = varchar("mensagem_boas_vindas", 300).nullable()
    val aviso = varchar("aviso", 300).nullable()
    val cssExtra = text("css_extra").nullable()
    val modulos = array<String>("modulos")
    val personalizacaoLiberada = array<String>("personalizacao_liberada")

    // contato e endereço
    val telefone = varchar("telefone", 20).nullable()
    val instagram = varchar("instagram", 60).nullable()
    val enderecoLogradouro = varchar("endereco_logradouro", 150).nullable()
    val enderecoNumero = varchar("endereco_numero", 20).nullable()
    val enderecoComplemento = varchar("endereco_complemento", 100).nullable()
    val enderecoBairro = varchar("endereco_bairro", 80).nullable()
    val enderecoCidade = varchar("endereco_cidade", 80).nullable()
    val enderecoUf = varchar("endereco_uf", 2).nullable()
    val enderecoCep = varchar("endereco_cep", 9).nullable()

    // funcionamento e encomendas
    val pausada = bool("pausada")
    val mensagemPausa = varchar("mensagem_pausa", 200).nullable()
    val aceitaAgendamento = bool("aceita_agendamento")
    val exigeAgendamento = bool("exige_agendamento")
    val antecedenciaMinimaHoras = integer("antecedencia_minima_horas")
    val diasMaximosAgendamento = integer("dias_maximos_agendamento")
    val limitePedidosDia = integer("limite_pedidos_dia").nullable()
    val intervaloAgendamentoMinutos = integer("intervalo_agendamento_minutos")
}

object HorarioFuncionamentoTabela : LongIdTable("horario_funcionamento") {
    val lojaId = long("loja_id")
    val diaSemana = short("dia_semana")
    val abre = time("abre")
    val fecha = time("fecha")
}

object UsuarioTabela : LongIdTable("usuario") {
    val lojaId = long("loja_id").nullable()
    val nome = varchar("nome", 120)
    val email = varchar("email", 160)
    val senhaHash = varchar("senha_hash", 100)
    val papel = varchar("papel", 20)
    val ativo = bool("ativo")
}

object CategoriaTabela : LongIdTable("categoria") {
    val lojaId = long("loja_id")
    val nome = varchar("nome", 80)
    val ordem = integer("ordem")
}

object ProdutoTabela : LongIdTable("produto") {
    val lojaId = long("loja_id")
    val categoriaId = long("categoria_id").nullable()
    val nome = varchar("nome", 120)
    val descricao = varchar("descricao", 1000)
    val precoCentavos = long("preco_centavos")
    val precoPromocionalCentavos = long("preco_promocional_centavos").nullable()
    val imagemUrl = varchar("imagem_url", 500).nullable()
    val destaque = bool("destaque")
    val ativo = bool("ativo")
}

object GrupoOpcaoTabela : LongIdTable("grupo_opcao") {
    val lojaId = long("loja_id")
    val produtoId = long("produto_id")
    val nome = varchar("nome", 80)
    val minimo = integer("minimo")
    val maximo = integer("maximo")
    val permiteRepetir = bool("permite_repetir")
    val ordem = integer("ordem")
}

object OpcaoTabela : LongIdTable("opcao") {
    val lojaId = long("loja_id")
    val grupoId = long("grupo_id")
    val nome = varchar("nome", 80)
    val precoCentavos = long("preco_centavos")
    val ativo = bool("ativo")
    val ordem = integer("ordem")
}

object PedidoTabela : LongIdTable("pedido") {
    val lojaId = long("loja_id")
    val codigo = varchar("codigo", 20)
    val descricao = text("descricao")
    val totalCentavos = long("total_centavos")
    val status = varchar("status", 30)
    val criadoEm = timestampWithTimeZone("criado_em")
    val atualizadoEm = timestampWithTimeZone("atualizado_em")
    val agendadoPara = timestampWithTimeZone("agendado_para").nullable()
    val diaProducao = date("dia_producao")
}

object DepoimentoTabela : LongIdTable("depoimento") {
    val lojaId = long("loja_id")
    val autor = varchar("autor", 80)
    val texto = varchar("texto", 500)
    val nota = short("nota").nullable()
    val ativo = bool("ativo")
    val ordem = integer("ordem")
}
