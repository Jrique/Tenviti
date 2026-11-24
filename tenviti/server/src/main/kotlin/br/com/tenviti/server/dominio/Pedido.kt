package br.com.tenviti.server.dominio

import br.com.tenviti.shared.StatusPedido
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate

/**
 * Pedido de uma loja.
 *
 * Fase 0: ainda guarda a descrição em texto e o total informado pelo cardápio.
 * Na Fase 2 o pedido passa a ter itens estruturados e o total é calculado aqui, no domínio.
 */
class Pedido(
    val id: Long,
    val lojaId: Long,
    val codigo: String,
    val descricao: String,
    val totalCentavos: Long,
    status: StatusPedido,
    val criadoEm: Instant,
    atualizadoEm: Instant,
    /** Encomenda: quando o cliente quer receber/retirar. */
    val agendadoPara: Instant? = null,
    /** Dia em que o pedido conta para o limite de produção (fuso da loja). */
    val diaProducao: LocalDate,
) {
    var status: StatusPedido = status; private set
    var atualizadoEm: Instant = atualizadoEm; private set

    /** Pedido finalizado ou cancelado não muda mais de status. */
    fun alterarStatus(novo: StatusPedido, agora: Instant = Instant.now()) {
        if (novo == status) return
        if (status.ehFinal) {
            throw Conflito(
                "Este pedido já está ${status.rotulo.lowercase()} e não pode mudar de status.",
                "pedido_encerrado",
            )
        }
        status = novo
        atualizadoEm = agora
    }

    companion object {
        fun novoDaVitrine(
            lojaId: Long,
            codigo: String,
            descricao: String,
            totalCentavos: Long,
            janela: JanelaDoPedido,
            agora: Instant = Instant.now(),
        ): Pedido {
            exigir(descricao.isNotBlank()) { "O pedido está vazio." }
            exigir(descricao.length <= 10_000) { "A descrição do pedido é muito longa." }
            exigir(totalCentavos > 0) { "O total do pedido deve ser maior que zero." }
            return Pedido(
                0, lojaId, codigo, descricao.trim(), totalCentavos, StatusPedido.NOVO, agora, agora,
                agendadoPara = janela.agendadoPara?.toInstant(),
                diaProducao = janela.diaProducao,
            )
        }
    }
}

/** Gera códigos curtos e difíceis de adivinhar (o cliente usa o código para acompanhar o pedido). */
object GeradorCodigoPedido {
    // Sem caracteres ambíguos (0/O, 1/I/L).
    private const val ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    private val aleatorio = SecureRandom()

    fun gerar(tamanho: Int = 8): String = buildString {
        repeat(tamanho) { append(ALFABETO[aleatorio.nextInt(ALFABETO.length)]) }
    }
}
