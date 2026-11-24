package br.com.cardapio.server.infra.banco

import br.com.cardapio.server.dominio.Aparencia
import br.com.cardapio.server.dominio.Categoria
import br.com.cardapio.server.dominio.ConfigEncomenda
import br.com.cardapio.server.dominio.ContatoLoja
import br.com.cardapio.server.dominio.Depoimento
import br.com.cardapio.server.dominio.Endereco
import br.com.cardapio.server.dominio.Funcionamento
import br.com.cardapio.server.dominio.GrupoOpcoes
import br.com.cardapio.server.dominio.IntervaloHorario
import br.com.cardapio.server.dominio.Loja
import br.com.cardapio.server.dominio.Opcao
import br.com.cardapio.server.dominio.Pedido
import br.com.cardapio.server.dominio.Produto
import br.com.cardapio.server.dominio.RepositorioCatalogo
import br.com.cardapio.server.dominio.RepositorioDepoimentos
import br.com.cardapio.server.dominio.RepositorioLojas
import br.com.cardapio.server.dominio.RepositorioPedidos
import br.com.cardapio.server.dominio.RepositorioUsuarios
import br.com.cardapio.server.dominio.Usuario
import br.com.cardapio.shared.ItemPersonalizavel
import br.com.cardapio.shared.ModeloLayout
import br.com.cardapio.shared.Modulo
import br.com.cardapio.shared.Papel
import br.com.cardapio.shared.StatusPedido
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Implementações com Exposed (DSL). Devem ser chamadas dentro de Transacoes.executar { }.

class RepositorioLojasExposed : RepositorioLojas {
    override fun buscar(id: Long) =
        LojaTabela.selectAll().where { LojaTabela.id eq id }.singleOrNull()?.paraLoja()

    override fun buscarPorSlug(slug: String) =
        LojaTabela.selectAll().where { LojaTabela.slug eq slug }.singleOrNull()?.paraLoja()

    override fun listar() = LojaTabela.selectAll().orderBy(LojaTabela.nome).map { it.paraLoja() }

    override fun existeSlug(slug: String, exceto: Long?): Boolean =
        LojaTabela.selectAll().where {
            val base = LojaTabela.slug eq slug
            if (exceto != null) base and (LojaTabela.id neq exceto) else base
        }.any()

    override fun salvar(loja: Loja): Loja {
        val id = if (loja.id == 0L) {
            LojaTabela.insertAndGetId { preencher(it, loja) }.value
        } else {
            LojaTabela.update({ LojaTabela.id eq loja.id }) { preencher(it, loja) }
            loja.id
        }
        // horários: substitui todos
        HorarioFuncionamentoTabela.deleteWhere { HorarioFuncionamentoTabela.lojaId eq id }
        loja.funcionamento.horarios.forEach { h ->
            HorarioFuncionamentoTabela.insert {
                it[lojaId] = id
                it[diaSemana] = h.dia.value.toShort()
                it[abre] = h.abre
                it[fecha] = h.fecha
            }
        }
        return buscar(id)!!
    }

    private fun preencher(it: UpdateBuilder<*>, loja: Loja) {
        val ap = loja.aparencia
        val c = loja.contato
        val e = c.endereco
        val f = loja.funcionamento
        it[LojaTabela.slug] = loja.slug
        it[LojaTabela.nome] = loja.nome
        it[LojaTabela.ativa] = loja.ativa
        it[LojaTabela.fusoHorario] = loja.fusoHorario
        it[LojaTabela.corPrimaria] = ap.corPrimaria
        it[LojaTabela.corSecundaria] = ap.corSecundaria
        it[LojaTabela.corFundo] = ap.corFundo
        it[LojaTabela.logoUrl] = ap.logoUrl
        it[LojaTabela.bannerUrl] = ap.bannerUrl
        it[LojaTabela.modeloLayout] = ap.modeloLayout.name
        it[LojaTabela.mensagemBoasVindas] = ap.mensagemBoasVindas
        it[LojaTabela.aviso] = ap.aviso
        it[LojaTabela.cssExtra] = ap.cssExtra
        it[LojaTabela.modulos] = loja.modulos.map(Modulo::name).sorted()
        it[LojaTabela.personalizacaoLiberada] = loja.personalizacaoLiberada.map(ItemPersonalizavel::name).sorted()
        it[LojaTabela.telefone] = c.telefone
        it[LojaTabela.instagram] = c.instagram
        it[LojaTabela.enderecoLogradouro] = e?.logradouro
        it[LojaTabela.enderecoNumero] = e?.numero
        it[LojaTabela.enderecoComplemento] = e?.complemento
        it[LojaTabela.enderecoBairro] = e?.bairro
        it[LojaTabela.enderecoCidade] = e?.cidade
        it[LojaTabela.enderecoUf] = e?.uf
        it[LojaTabela.enderecoCep] = e?.cep
        it[LojaTabela.pausada] = f.pausada
        it[LojaTabela.mensagemPausa] = f.mensagemPausa
        it[LojaTabela.aceitaAgendamento] = f.encomenda.aceitaAgendamento
        it[LojaTabela.exigeAgendamento] = f.encomenda.exigeAgendamento
        it[LojaTabela.antecedenciaMinimaHoras] = f.encomenda.antecedenciaMinimaHoras
        it[LojaTabela.diasMaximosAgendamento] = f.encomenda.diasMaximos
        it[LojaTabela.limitePedidosDia] = f.encomenda.limitePedidosPorDia
        it[LojaTabela.intervaloAgendamentoMinutos] = f.encomenda.intervaloMinutos
    }

    private fun ResultRow.paraLoja(): Loja {
        val id = this[LojaTabela.id].value
        val horarios = HorarioFuncionamentoTabela.selectAll()
            .where { HorarioFuncionamentoTabela.lojaId eq id }
            .orderBy(HorarioFuncionamentoTabela.diaSemana to SortOrder.ASC, HorarioFuncionamentoTabela.abre to SortOrder.ASC)
            .map { IntervaloHorario(DayOfWeek.of(it[HorarioFuncionamentoTabela.diaSemana].toInt()), it[HorarioFuncionamentoTabela.abre], it[HorarioFuncionamentoTabela.fecha]) }
        val endereco = Endereco(
            logradouro = this[LojaTabela.enderecoLogradouro].orEmpty(),
            numero = this[LojaTabela.enderecoNumero].orEmpty(),
            complemento = this[LojaTabela.enderecoComplemento],
            bairro = this[LojaTabela.enderecoBairro].orEmpty(),
            cidade = this[LojaTabela.enderecoCidade].orEmpty(),
            uf = this[LojaTabela.enderecoUf].orEmpty(),
            cep = this[LojaTabela.enderecoCep].orEmpty(),
        )
        return Loja(
            id = id,
            slug = this[LojaTabela.slug],
            nome = this[LojaTabela.nome],
            ativa = this[LojaTabela.ativa],
            fusoHorario = this[LojaTabela.fusoHorario],
            aparencia = Aparencia(
                corPrimaria = this[LojaTabela.corPrimaria],
                corSecundaria = this[LojaTabela.corSecundaria],
                corFundo = this[LojaTabela.corFundo],
                logoUrl = this[LojaTabela.logoUrl],
                bannerUrl = this[LojaTabela.bannerUrl],
                modeloLayout = enumOuPadrao(this[LojaTabela.modeloLayout], ModeloLayout.LISTA),
                mensagemBoasVindas = this[LojaTabela.mensagemBoasVindas],
                aviso = this[LojaTabela.aviso],
                cssExtra = this[LojaTabela.cssExtra],
            ),
            modulos = this[LojaTabela.modulos].mapNotNull { nome -> Modulo.entries.find { it.name == nome } }.toSet(),
            personalizacaoLiberada = this[LojaTabela.personalizacaoLiberada]
                .mapNotNull { nome -> ItemPersonalizavel.entries.find { it.name == nome } }.toSet(),
            contato = ContatoLoja(
                telefone = this[LojaTabela.telefone],
                instagram = this[LojaTabela.instagram],
                endereco = endereco.takeUnless { it.vazio },
            ),
            funcionamento = Funcionamento(
                pausada = this[LojaTabela.pausada],
                mensagemPausa = this[LojaTabela.mensagemPausa],
                horarios = horarios,
                encomenda = ConfigEncomenda(
                    aceitaAgendamento = this[LojaTabela.aceitaAgendamento],
                    exigeAgendamento = this[LojaTabela.exigeAgendamento],
                    antecedenciaMinimaHoras = this[LojaTabela.antecedenciaMinimaHoras],
                    diasMaximos = this[LojaTabela.diasMaximosAgendamento],
                    limitePedidosPorDia = this[LojaTabela.limitePedidosDia],
                    intervaloMinutos = this[LojaTabela.intervaloAgendamentoMinutos],
                ),
            ),
        )
    }
}

class RepositorioUsuariosExposed : RepositorioUsuarios {
    override fun buscar(id: Long) =
        UsuarioTabela.selectAll().where { UsuarioTabela.id eq id }.singleOrNull()?.paraUsuario()

    override fun buscarDaLoja(lojaId: Long, id: Long) =
        UsuarioTabela.selectAll().where { (UsuarioTabela.id eq id) and (UsuarioTabela.lojaId eq lojaId) }
            .singleOrNull()?.paraUsuario()

    override fun buscarPorEmail(email: String) =
        UsuarioTabela.selectAll().where { UsuarioTabela.email.lowerCase() eq Usuario.normalizarEmail(email) }
            .singleOrNull()?.paraUsuario()

    override fun listarDaLoja(lojaId: Long) =
        UsuarioTabela.selectAll().where { UsuarioTabela.lojaId eq lojaId }.orderBy(UsuarioTabela.nome).map { it.paraUsuario() }

    override fun existeEmail(email: String, exceto: Long?): Boolean =
        UsuarioTabela.selectAll().where {
            val base = UsuarioTabela.email.lowerCase() eq Usuario.normalizarEmail(email)
            if (exceto != null) base and (UsuarioTabela.id neq exceto) else base
        }.any()

    override fun existeSuperAdmin(): Boolean =
        UsuarioTabela.selectAll().where { UsuarioTabela.papel eq Papel.SUPER_ADMIN.name }.any()

    override fun salvar(usuario: Usuario): Usuario {
        return if (usuario.id == 0L) {
            val id = UsuarioTabela.insertAndGetId {
                it[lojaId] = usuario.lojaId
                it[nome] = usuario.nome
                it[email] = usuario.email
                it[senhaHash] = usuario.senhaHash
                it[papel] = usuario.papel.name
                it[ativo] = usuario.ativo
            }.value
            buscar(id)!!
        } else {
            UsuarioTabela.update({ UsuarioTabela.id eq usuario.id }) {
                it[nome] = usuario.nome
                it[email] = usuario.email
                it[senhaHash] = usuario.senhaHash
                it[papel] = usuario.papel.name
                it[ativo] = usuario.ativo
            }
            usuario
        }
    }

    override fun remover(lojaId: Long, id: Long) {
        UsuarioTabela.deleteWhere { (UsuarioTabela.id eq id) and (UsuarioTabela.lojaId eq lojaId) }
    }

    private fun ResultRow.paraUsuario() = Usuario(
        id = this[UsuarioTabela.id].value,
        lojaId = this[UsuarioTabela.lojaId],
        nome = this[UsuarioTabela.nome],
        email = this[UsuarioTabela.email],
        senhaHash = this[UsuarioTabela.senhaHash],
        papel = Papel.valueOf(this[UsuarioTabela.papel]),
        ativo = this[UsuarioTabela.ativo],
    )
}

class RepositorioCatalogoExposed : RepositorioCatalogo {
    override fun listarCategorias(lojaId: Long) =
        CategoriaTabela.selectAll().where { CategoriaTabela.lojaId eq lojaId }
            .orderBy(CategoriaTabela.ordem to SortOrder.ASC, CategoriaTabela.nome to SortOrder.ASC)
            .map { it.paraCategoria() }

    override fun buscarCategoria(lojaId: Long, id: Long) =
        CategoriaTabela.selectAll().where { (CategoriaTabela.id eq id) and (CategoriaTabela.lojaId eq lojaId) }
            .singleOrNull()?.paraCategoria()

    override fun existeCategoriaComNome(lojaId: Long, nome: String, exceto: Long?): Boolean =
        CategoriaTabela.selectAll().where {
            val base = (CategoriaTabela.lojaId eq lojaId) and (CategoriaTabela.nome.lowerCase() eq nome.trim().lowercase())
            if (exceto != null) base and (CategoriaTabela.id neq exceto) else base
        }.any()

    override fun salvarCategoria(categoria: Categoria): Categoria {
        return if (categoria.id == 0L) {
            val id = CategoriaTabela.insertAndGetId {
                it[lojaId] = categoria.lojaId
                it[nome] = categoria.nome
                it[ordem] = categoria.ordem
            }.value
            buscarCategoria(categoria.lojaId, id)!!
        } else {
            CategoriaTabela.update({ (CategoriaTabela.id eq categoria.id) and (CategoriaTabela.lojaId eq categoria.lojaId) }) {
                it[nome] = categoria.nome
                it[ordem] = categoria.ordem
            }
            categoria
        }
    }

    override fun removerCategoria(lojaId: Long, id: Long) {
        CategoriaTabela.deleteWhere { (CategoriaTabela.id eq id) and (CategoriaTabela.lojaId eq lojaId) }
    }

    override fun listarProdutos(lojaId: Long, somenteAtivos: Boolean): List<Produto> {
        val grupos = gruposDaLoja(lojaId)
        return ProdutoTabela.selectAll().where {
            val base = ProdutoTabela.lojaId eq lojaId
            if (somenteAtivos) base and (ProdutoTabela.ativo eq true) else base
        }.orderBy(ProdutoTabela.nome).map { it.paraProduto(grupos[it[ProdutoTabela.id].value].orEmpty()) }
    }

    override fun buscarProduto(lojaId: Long, id: Long): Produto? {
        val linha = ProdutoTabela.selectAll().where { (ProdutoTabela.id eq id) and (ProdutoTabela.lojaId eq lojaId) }
            .singleOrNull() ?: return null
        return linha.paraProduto(gruposDaLoja(lojaId, produtoId = id)[id].orEmpty())
    }

    override fun salvarProduto(produto: Produto): Produto {
        val id = if (produto.id == 0L) {
            ProdutoTabela.insertAndGetId {
                it[lojaId] = produto.lojaId
                preencher(it, produto)
            }.value
        } else {
            ProdutoTabela.update({ (ProdutoTabela.id eq produto.id) and (ProdutoTabela.lojaId eq produto.lojaId) }) {
                preencher(it, produto)
            }
            produto.id
        }
        // grupos de opções: substitui todos (opções caem junto pelo ON DELETE CASCADE)
        GrupoOpcaoTabela.deleteWhere { (GrupoOpcaoTabela.produtoId eq id) and (GrupoOpcaoTabela.lojaId eq produto.lojaId) }
        produto.grupos.forEachIndexed { i, g ->
            val grupoId = GrupoOpcaoTabela.insertAndGetId {
                it[lojaId] = produto.lojaId
                it[produtoId] = id
                it[nome] = g.nome
                it[minimo] = g.minimo
                it[maximo] = g.maximo
                it[permiteRepetir] = g.permiteRepetir
                it[ordem] = i
            }.value
            g.opcoes.forEachIndexed { j, o ->
                OpcaoTabela.insert {
                    it[lojaId] = produto.lojaId
                    it[OpcaoTabela.grupoId] = grupoId
                    it[nome] = o.nome
                    it[precoCentavos] = o.precoCentavos
                    it[ativo] = o.ativo
                    it[ordem] = j
                }
            }
        }
        return buscarProduto(produto.lojaId, id)!!
    }

    override fun removerProduto(lojaId: Long, id: Long) {
        ProdutoTabela.deleteWhere { (ProdutoTabela.id eq id) and (ProdutoTabela.lojaId eq lojaId) }
    }

    private fun preencher(it: UpdateBuilder<*>, produto: Produto) {
        it[ProdutoTabela.categoriaId] = produto.categoriaId
        it[ProdutoTabela.nome] = produto.nome
        it[ProdutoTabela.descricao] = produto.descricao
        it[ProdutoTabela.precoCentavos] = produto.precoCentavos
        it[ProdutoTabela.precoPromocionalCentavos] = produto.precoPromocionalCentavos
        it[ProdutoTabela.imagemUrl] = produto.imagemUrl
        it[ProdutoTabela.destaque] = produto.destaque
        it[ProdutoTabela.ativo] = produto.ativo
    }

    /** Grupos (com opções) por produto, numa consulta por tabela. */
    private fun gruposDaLoja(lojaId: Long, produtoId: Long? = null): Map<Long, List<GrupoOpcoes>> {
        val linhasGrupo = GrupoOpcaoTabela.selectAll().where {
            val base = GrupoOpcaoTabela.lojaId eq lojaId
            if (produtoId != null) base and (GrupoOpcaoTabela.produtoId eq produtoId) else base
        }.orderBy(GrupoOpcaoTabela.ordem).toList()
        if (linhasGrupo.isEmpty()) return emptyMap()

        val idsGrupo = linhasGrupo.map { it[GrupoOpcaoTabela.id].value }
        val opcoesPorGrupo = OpcaoTabela.selectAll()
            .where { (OpcaoTabela.lojaId eq lojaId) and (OpcaoTabela.grupoId inList idsGrupo) }
            .orderBy(OpcaoTabela.ordem)
            .groupBy({ it[OpcaoTabela.grupoId] }) {
                Opcao(it[OpcaoTabela.id].value, it[OpcaoTabela.nome], it[OpcaoTabela.precoCentavos], it[OpcaoTabela.ativo])
            }

        return linhasGrupo.groupBy({ it[GrupoOpcaoTabela.produtoId] }) {
            GrupoOpcoes(
                id = it[GrupoOpcaoTabela.id].value,
                nome = it[GrupoOpcaoTabela.nome],
                minimo = it[GrupoOpcaoTabela.minimo],
                maximo = it[GrupoOpcaoTabela.maximo],
                permiteRepetir = it[GrupoOpcaoTabela.permiteRepetir],
                opcoes = opcoesPorGrupo[it[GrupoOpcaoTabela.id].value].orEmpty(),
            )
        }
    }

    private fun ResultRow.paraCategoria() = Categoria(
        id = this[CategoriaTabela.id].value,
        lojaId = this[CategoriaTabela.lojaId],
        nome = this[CategoriaTabela.nome],
        ordem = this[CategoriaTabela.ordem],
    )

    private fun ResultRow.paraProduto(grupos: List<GrupoOpcoes>) = Produto(
        id = this[ProdutoTabela.id].value,
        lojaId = this[ProdutoTabela.lojaId],
        categoriaId = this[ProdutoTabela.categoriaId],
        nome = this[ProdutoTabela.nome],
        descricao = this[ProdutoTabela.descricao],
        precoCentavos = this[ProdutoTabela.precoCentavos],
        precoPromocionalCentavos = this[ProdutoTabela.precoPromocionalCentavos],
        imagemUrl = this[ProdutoTabela.imagemUrl],
        grupos = grupos,
        destaque = this[ProdutoTabela.destaque],
        ativo = this[ProdutoTabela.ativo],
    )
}

class RepositorioDepoimentosExposed : RepositorioDepoimentos {
    override fun listar(lojaId: Long, somenteAtivos: Boolean): List<Depoimento> =
        DepoimentoTabela.selectAll().where {
            val base = DepoimentoTabela.lojaId eq lojaId
            if (somenteAtivos) base and (DepoimentoTabela.ativo eq true) else base
        }.orderBy(DepoimentoTabela.ordem to SortOrder.ASC, DepoimentoTabela.id to SortOrder.DESC).map { it.paraDepoimento() }

    override fun buscar(lojaId: Long, id: Long) =
        DepoimentoTabela.selectAll().where { (DepoimentoTabela.id eq id) and (DepoimentoTabela.lojaId eq lojaId) }
            .singleOrNull()?.paraDepoimento()

    override fun salvar(depoimento: Depoimento): Depoimento {
        val preencher: (UpdateBuilder<*>) -> Unit = {
            it[DepoimentoTabela.autor] = depoimento.autor
            it[DepoimentoTabela.texto] = depoimento.texto
            it[DepoimentoTabela.nota] = depoimento.nota?.toShort()
            it[DepoimentoTabela.ativo] = depoimento.ativo
            it[DepoimentoTabela.ordem] = depoimento.ordem
        }
        val id = if (depoimento.id == 0L) {
            DepoimentoTabela.insertAndGetId {
                it[lojaId] = depoimento.lojaId
                preencher(it)
            }.value
        } else {
            DepoimentoTabela.update({ (DepoimentoTabela.id eq depoimento.id) and (DepoimentoTabela.lojaId eq depoimento.lojaId) }) {
                preencher(it)
            }
            depoimento.id
        }
        return buscar(depoimento.lojaId, id)!!
    }

    override fun remover(lojaId: Long, id: Long) {
        DepoimentoTabela.deleteWhere { (DepoimentoTabela.id eq id) and (DepoimentoTabela.lojaId eq lojaId) }
    }

    private fun ResultRow.paraDepoimento() = Depoimento(
        id = this[DepoimentoTabela.id].value,
        lojaId = this[DepoimentoTabela.lojaId],
        autor = this[DepoimentoTabela.autor],
        texto = this[DepoimentoTabela.texto],
        nota = this[DepoimentoTabela.nota]?.toInt(),
        ativo = this[DepoimentoTabela.ativo],
        ordem = this[DepoimentoTabela.ordem],
    )
}

class RepositorioPedidosExposed : RepositorioPedidos {
    override fun buscar(lojaId: Long, id: Long) =
        PedidoTabela.selectAll().where { (PedidoTabela.id eq id) and (PedidoTabela.lojaId eq lojaId) }
            .singleOrNull()?.paraPedido()

    override fun buscarPorCodigo(lojaId: Long, codigo: String) =
        PedidoTabela.selectAll().where { (PedidoTabela.codigo eq codigo) and (PedidoTabela.lojaId eq lojaId) }
            .singleOrNull()?.paraPedido()

    override fun existeCodigo(lojaId: Long, codigo: String) =
        PedidoTabela.selectAll().where { (PedidoTabela.codigo eq codigo) and (PedidoTabela.lojaId eq lojaId) }.any()

    override fun listar(lojaId: Long, de: Instant, ate: Instant, limite: Int): List<Pedido> =
        PedidoTabela.selectAll().where {
            (PedidoTabela.lojaId eq lojaId) and
                (PedidoTabela.criadoEm greaterEq de.atOffset(ZoneOffset.UTC)) and
                (PedidoTabela.criadoEm less ate.atOffset(ZoneOffset.UTC))
        }.orderBy(PedidoTabela.criadoEm, SortOrder.DESC).limit(limite).map { it.paraPedido() }

    override fun contarPorDia(lojaId: Long, de: LocalDate, ate: LocalDate): Map<LocalDate, Int> {
        val total = PedidoTabela.id.count()
        return PedidoTabela.select(PedidoTabela.diaProducao, total).where {
            (PedidoTabela.lojaId eq lojaId) and
                (PedidoTabela.diaProducao greaterEq de) and
                (PedidoTabela.diaProducao lessEq ate) and
                (PedidoTabela.status neq StatusPedido.CANCELADO.name)
        }.groupBy(PedidoTabela.diaProducao).associate { it[PedidoTabela.diaProducao] to it[total].toInt() }
    }

    override fun travarDia(lojaId: Long, dia: LocalDate) {
        // Lock consultivo do PostgreSQL liberado no fim da transação. Valores são números gerados aqui (sem entrada do usuário).
        TransactionManager.current().exec("SELECT pg_advisory_xact_lock(${lojaId.toInt()}, ${dia.toEpochDay().toInt()})")
    }

    override fun salvar(pedido: Pedido): Pedido {
        return if (pedido.id == 0L) {
            val id = PedidoTabela.insertAndGetId {
                it[lojaId] = pedido.lojaId
                it[codigo] = pedido.codigo
                it[descricao] = pedido.descricao
                it[totalCentavos] = pedido.totalCentavos
                it[status] = pedido.status.name
                it[criadoEm] = pedido.criadoEm.atOffset(ZoneOffset.UTC)
                it[atualizadoEm] = pedido.atualizadoEm.atOffset(ZoneOffset.UTC)
                it[agendadoPara] = pedido.agendadoPara?.atOffset(ZoneOffset.UTC)
                it[diaProducao] = pedido.diaProducao
            }.value
            buscar(pedido.lojaId, id)!!
        } else {
            PedidoTabela.update({ (PedidoTabela.id eq pedido.id) and (PedidoTabela.lojaId eq pedido.lojaId) }) {
                it[status] = pedido.status.name
                it[atualizadoEm] = pedido.atualizadoEm.atOffset(ZoneOffset.UTC)
            }
            pedido
        }
    }

    private fun ResultRow.paraPedido() = Pedido(
        id = this[PedidoTabela.id].value,
        lojaId = this[PedidoTabela.lojaId],
        codigo = this[PedidoTabela.codigo],
        descricao = this[PedidoTabela.descricao],
        totalCentavos = this[PedidoTabela.totalCentavos],
        status = StatusPedido.valueOf(this[PedidoTabela.status]),
        criadoEm = this[PedidoTabela.criadoEm].toInstant(),
        atualizadoEm = this[PedidoTabela.atualizadoEm].toInstant(),
        agendadoPara = this[PedidoTabela.agendadoPara]?.toInstant(),
        diaProducao = this[PedidoTabela.diaProducao],
    )
}

private inline fun <reified E : Enum<E>> enumOuPadrao(valor: String, padrao: E): E =
    enumValues<E>().find { it.name == valor } ?: padrao
