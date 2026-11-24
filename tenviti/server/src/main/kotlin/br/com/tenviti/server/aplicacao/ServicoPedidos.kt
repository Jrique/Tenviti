package br.com.tenviti.server.aplicacao

import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.server.dominio.DadoInvalido
import br.com.tenviti.server.dominio.GeradorCodigoPedido
import br.com.tenviti.server.dominio.Loja
import br.com.tenviti.server.dominio.NaoEncontrado
import br.com.tenviti.server.dominio.Pedido
import br.com.tenviti.server.dominio.RepositorioPedidos
import br.com.tenviti.server.dominio.Transacoes
import br.com.tenviti.shared.AlterarStatusDto
import br.com.tenviti.shared.NovoPedidoPublicoDto
import br.com.tenviti.shared.Papel
import br.com.tenviti.shared.PedidoDto
import br.com.tenviti.shared.PedidoPublicoDto
import br.com.tenviti.shared.ResumoDashboardDto
import br.com.tenviti.shared.StatusPedido
import br.com.tenviti.shared.DiaAgendaDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

class ServicoPedidos(
    private val tx: Transacoes,
    private val pedidos: RepositorioPedidos,
    private val relogio: () -> Instant = Instant::now,
) {
    private val quemAtende = arrayOf(Papel.DONO, Papel.GERENTE, Papel.ATENDENTE, Papel.COZINHA)

    // ---------- Público (cardápio) ----------

    suspend fun criarDaVitrine(loja: Loja, req: NovoPedidoPublicoDto): PedidoPublicoDto {
        val agendadoPara = req.agendadoPara?.let { texto ->
            try {
                LocalDateTime.parse(texto)
            } catch (_: DateTimeParseException) {
                throw DadoInvalido("Data da encomenda inválida.")
            }
        }
        return tx.executar {
            val agora = relogio().atZone(loja.zona)
            // Trabalhar: a loja decide se aceita o pedido agora / no horário escolhido e em qual dia ele conta
            val diaPrevisto = agendadoPara?.toLocalDate() ?: agora.toLocalDate()
            pedidos.travarDia(loja.id, diaPrevisto) // evita estourar o limite com pedidos simultâneos
            val janela = loja.funcionamentoEfetivo.validarNovoPedido(agora, agendadoPara) { dia ->
                pedidos.contarPorDia(loja.id, dia, dia)[dia] ?: 0
            }
            val pedido = Pedido.novoDaVitrine(loja.id, gerarCodigoUnico(loja.id), req.descricao, req.totalCentavos, janela, agora.toInstant())
            pedidos.salvar(pedido).paraPublico()
        }
    }

    /** Dias e horários disponíveis para encomenda (com os dias lotados marcados). */
    suspend fun agenda(loja: Loja): List<DiaAgendaDto> {
        val agora = relogio().atZone(loja.zona)
        val f = loja.funcionamentoEfetivo
        if (!f.encomenda.aceitaAgendamento) return emptyList()
        val porDia = tx.executar {
            pedidos.contarPorDia(loja.id, agora.toLocalDate(), agora.toLocalDate().plusDays(f.encomenda.diasMaximos.toLong()))
        }
        return f.agenda(agora, porDia).map { it.paraDto() }
    }

    suspend fun consultarPublico(loja: Loja, codigo: String): PedidoPublicoDto =
        tx.executar { pedidos.buscarPorCodigo(loja.id, codigo.uppercase())?.paraPublico() }
            ?: throw NaoEncontrado("Pedido não encontrado.")

    // ---------- Painel ----------

    /** Lista pedidos por período (datas no fuso da loja). Sem período: últimos 30 dias. */
    suspend fun listar(ator: Ator, loja: Loja, inicio: LocalDate?, fim: LocalDate?): List<PedidoDto> {
        ator.exigirPapel(*quemAtende)
        val ate = (fim ?: LocalDate.now(loja.zona)).plusDays(1)
        val de = inicio ?: ate.minusDays(30)
        if (de.isAfter(ate)) throw DadoInvalido("A data inicial não pode ser depois da final.")
        return tx.executar {
            pedidos.listar(loja.id, inicioDoDia(de, loja), inicioDoDia(ate, loja), LIMITE_LISTAGEM).map { it.paraDto() }
        }
    }

    suspend fun alterarStatus(ator: Ator, id: Long, req: AlterarStatusDto): PedidoDto {
        ator.exigirPapel(*quemAtende)
        val lojaId = ator.lojaObrigatoria()
        return tx.executar {
            val pedido = pedidos.buscar(lojaId, id) ?: throw NaoEncontrado("Pedido não encontrado.")
            pedido.alterarStatus(req.status, relogio())
            pedidos.salvar(pedido).paraDto()
        }
    }

    /** Resumo diário de vendas para o dashboard. Pedidos cancelados não entram no faturamento. */
    suspend fun resumo(ator: Ator, loja: Loja, dias: Int?, inicio: LocalDate?, fim: LocalDate?): ResumoDashboardDto {
        ator.exigirPapel(Papel.DONO, Papel.GERENTE)
        val hoje = LocalDate.now(loja.zona)
        val (de, ate) = if (inicio != null && fim != null) {
            inicio to fim
        } else {
            val qtd = (dias ?: 7).coerceIn(1, 366)
            hoje.minusDays(qtd - 1L) to hoje
        }
        if (de.isAfter(ate)) throw DadoInvalido("A data inicial não pode ser depois da final.")
        if (de.plusDays(366).isBefore(ate)) throw DadoInvalido("O período máximo é de 1 ano.")

        val lista = tx.executar {
            pedidos.listar(loja.id, inicioDoDia(de, loja), inicioDoDia(ate.plusDays(1), loja), Int.MAX_VALUE)
        }.filter { it.status != StatusPedido.CANCELADO }

        val porDia = lista.groupBy { it.criadoEm.atZone(loja.zona).toLocalDate() }
        val dias = generateSequence(de) { it.plusDays(1) }.takeWhile { !it.isAfter(ate) }.toList()
        val pt = Locale.forLanguageTag("pt-BR")

        return ResumoDashboardDto(
            faturamentoDiarioCentavos = dias.map { d -> porDia[d].orEmpty().sumOf { it.totalCentavos } },
            vendasDiarias = dias.map { d -> porDia[d].orEmpty().size },
            rotulosDias = dias.map { d ->
                val semana = d.dayOfWeek.getDisplayName(TextStyle.SHORT, pt).replace(".", "").replaceFirstChar { it.uppercase() }
                "$semana ${d.dayOfMonth}/${d.monthValue}"
            },
            totalFaturamentoCentavos = lista.sumOf { it.totalCentavos },
            totalVendas = lista.size,
        )
    }

    private fun gerarCodigoUnico(lojaId: Long): String {
        repeat(5) {
            val codigo = GeradorCodigoPedido.gerar()
            if (!pedidos.existeCodigo(lojaId, codigo)) return codigo
        }
        error("Não foi possível gerar um código de pedido único.")
    }

    private fun inicioDoDia(data: LocalDate, loja: Loja): Instant = data.atStartOfDay(loja.zona).toInstant()

    companion object {
        const val LIMITE_LISTAGEM = 1000
    }
}
