package br.com.tenviti.server.dominio

import br.com.tenviti.shared.FormaPagamento
import br.com.tenviti.shared.StatusPedido
import br.com.tenviti.shared.TipoEntrega
import br.com.tenviti.shared.emReais
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Opção escolhida num item, registrada com nome e preço do momento da compra. */
data class OpcaoDoItem(val grupo: String, val nome: String, val precoCentavos: Long, val quantidade: Int)

/**
 * Item de um pedido: uma "foto" do produto na hora da compra. Se a loja mudar o preço ou o nome
 * depois, o pedido continua mostrando o que o cliente comprou.
 */
data class ItemPedido(
    val produtoId: Long?,
    val nome: String,
    val quantidade: Int,
    val precoUnitarioCentavos: Long,
    val opcoes: List<OpcaoDoItem> = emptyList(),
    val observacao: String? = null,
) {
    /** Preço de uma unidade já com as opções (ex.: lanche + 2x bacon). */
    val valorUnitarioCentavos: Long get() = precoUnitarioCentavos + opcoes.sumOf { it.precoCentavos * it.quantidade }
    val totalCentavos: Long get() = valorUnitarioCentavos * quantidade
}

/** Opção que o cliente escolheu na vitrine (por id) e quantas vezes, por unidade do produto. */
data class EscolhaDeOpcao(val opcaoId: Long, val quantidade: Int = 1)

data class ClientePedido(val nome: String, val telefone: String) {
    init {
        exigir(nome.isNotBlank() && nome.trim().length in 2..80) { "Informe seu nome (de 2 a 80 letras)." }
        exigir(Regex("^[0-9]{10,13}$").matches(telefone)) { "Telefone inválido: informe só números com DDD (ex.: 11987654321)." }
    }
}

/** Como o pedido chega ao cliente, já com a taxa definida pela loja. */
data class Recebimento(val tipo: TipoEntrega, val endereco: Endereco?, val taxaCentavos: Long) {
    init {
        exigir(tipo == TipoEntrega.RETIRADA || endereco != null) { "Informe o endereço de entrega." }
        exigir(taxaCentavos >= 0) { "Taxa de entrega inválida." }
    }
}

/** Dados de pagamento combinados com a loja. */
data class Pagamento(val forma: FormaPagamento, val trocoParaCentavos: Long? = null) {
    init {
        exigir(trocoParaCentavos == null || forma == FormaPagamento.DINHEIRO) { "Troco só vale para pagamento em dinheiro." }
    }
}

/**
 * Pedido de uma loja.
 *
 * Pedidos da vitrine (Fase 2 em diante) têm itens estruturados e o total é calculado aqui, a partir dos
 * preços do catálogo. Pedidos antigos (Fase 0/1) só têm a [descricao] em texto e o total informado na época.
 */
class Pedido(
    val id: Long,
    val lojaId: Long,
    val codigo: String,
    /** Resumo em texto (gerado aqui nos pedidos novos; nos antigos, o texto enviado pelo cardápio). */
    val descricao: String,
    val totalCentavos: Long,
    status: StatusPedido,
    val criadoEm: Instant,
    atualizadoEm: Instant,
    /** Encomenda: quando o cliente quer receber/retirar. */
    val agendadoPara: Instant? = null,
    /** Dia em que o pedido conta para o limite de produção (fuso da loja). */
    val diaProducao: LocalDate,
    val itens: List<ItemPedido> = emptyList(),
    val cliente: ClientePedido? = null,
    val recebimento: Recebimento? = null,
    val pagamento: Pagamento? = null,
    val observacao: String? = null,
) {
    var status: StatusPedido = status; private set
    var atualizadoEm: Instant = atualizadoEm; private set

    /** Soma dos itens (sem a taxa de entrega). Nulo nos pedidos antigos. */
    val subtotalCentavos: Long? get() = itens.takeIf { it.isNotEmpty() }?.sumOf { it.totalCentavos }
    val taxaEntregaCentavos: Long get() = recebimento?.taxaCentavos ?: 0

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

    /**
     * Garante que o cliente vai pagar o que viu na tela: se os preços mudaram enquanto ele montava a
     * sacola, o pedido não entra e ele confere o novo valor.
     */
    fun conferirTotalVisto(totalVistoCentavos: Long?) {
        if (totalVistoCentavos != null && totalVistoCentavos != totalCentavos) {
            throw Conflito(
                "O valor do pedido mudou para ${totalCentavos.emReais()}. Confira a sacola e confirme de novo.",
                "total_alterado",
            )
        }
    }

    companion object {
        const val MAXIMO_ITENS = 50

        fun novoDaVitrine(
            lojaId: Long,
            codigo: String,
            itens: List<ItemPedido>,
            cliente: ClientePedido,
            recebimento: Recebimento,
            pagamento: Pagamento,
            observacao: String?,
            pedidoMinimoCentavos: Long,
            janela: JanelaDoPedido,
            agora: Instant = Instant.now(),
        ): Pedido {
            exigir(itens.isNotEmpty()) { "Sua sacola está vazia." }
            exigir(itens.size <= MAXIMO_ITENS) { "O pedido pode ter no máximo $MAXIMO_ITENS itens diferentes." }
            val obs = observacao?.trim()?.ifEmpty { null }
            exigir((obs?.length ?: 0) <= 500) { "A observação pode ter no máximo 500 caracteres." }

            val subtotal = itens.sumOf { it.totalCentavos }
            if (subtotal < pedidoMinimoCentavos) {
                throw DadoInvalido(
                    "O pedido mínimo desta loja é ${pedidoMinimoCentavos.emReais()}. " +
                        "Faltam ${(pedidoMinimoCentavos - subtotal).emReais()}.",
                    "pedido_minimo",
                )
            }
            val total = subtotal + recebimento.taxaCentavos
            val troco = pagamento.trocoParaCentavos
            exigir(troco == null || troco >= total) { "O troco precisa ser para um valor igual ou maior que o total (${total.emReais()})." }

            val clienteLimpo = cliente.copy(nome = cliente.nome.trim())
            val resumo = ResumoDoPedido.texto(itens, clienteLimpo, recebimento, pagamento, obs, janela, subtotal, total)
            return Pedido(
                0, lojaId, codigo, resumo, total, StatusPedido.NOVO, agora, agora,
                agendadoPara = janela.agendadoPara?.toInstant(),
                diaProducao = janela.diaProducao,
                itens = itens,
                cliente = clienteLimpo,
                recebimento = recebimento,
                pagamento = pagamento,
                observacao = obs,
            )
        }
    }
}

/** Texto do pedido no formato que o painel já exibe (seções "Produtos:", "Cliente:", "Total:"...). */
internal object ResumoDoPedido {
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")

    fun texto(
        itens: List<ItemPedido>,
        cliente: ClientePedido,
        recebimento: Recebimento,
        pagamento: Pagamento,
        observacao: String?,
        janela: JanelaDoPedido,
        subtotal: Long,
        total: Long,
    ): String = buildList {
        add("Produtos:")
        itens.forEach { item ->
            val opcoes = item.opcoes.groupBy { it.grupo }.entries.joinToString("") { (grupo, lista) ->
                " | $grupo: " + lista.joinToString(", ") { "${it.quantidade}x ${it.nome}" }
            }
            val obs = item.observacao?.let { " | Obs: $it" }.orEmpty()
            add("- ${item.quantidade}x ${item.nome} (${item.totalCentavos.emReais()})$opcoes$obs")
        }
        janela.agendadoPara?.let { add(""); add("Encomenda para: ${it.format(formatoData)}") }

        add("")
        val endereco = recebimento.endereco
        if (recebimento.tipo == TipoEntrega.RETIRADA || endereco == null) {
            add("Retirada no balcão")
        } else {
            add("Entrega:")
            add("- ${endereco.logradouro}, ${endereco.numero}")
            add("- " + listOf(endereco.bairro, listOf(endereco.cidade, endereco.uf).filter { it.isNotBlank() }.joinToString("/"))
                .filter { it.isNotBlank() }.joinToString(" - "))
            endereco.complemento?.takeIf { it.isNotBlank() }?.let { add("- $it") }
        }

        add(""); add("Cliente:"); add("- ${cliente.nome}")
        add(""); add("Contato:"); add("- ${cliente.telefone}")
        add(""); add("Pagamento:")
        add("- ${pagamento.forma.rotulo}" + (pagamento.trocoParaCentavos?.let { " (troco para ${it.emReais()})" } ?: ""))
        observacao?.let { add(""); add("Obs:"); add("- $it") }

        add("")
        if (recebimento.taxaCentavos > 0) {
            add("Subtotal: ${subtotal.emReais()}")
            add("Taxa de entrega: ${recebimento.taxaCentavos.emReais()}")
        }
        add("Total:"); add("- ${total.emReais()}")
    }.joinToString("\n")
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
