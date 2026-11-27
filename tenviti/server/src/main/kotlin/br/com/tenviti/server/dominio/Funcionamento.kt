package br.com.tenviti.server.dominio

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// ---------- Contato e endereço ----------

data class Endereco(
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String? = null,
    val bairro: String = "",
    val cidade: String = "",
    val uf: String = "",
    val cep: String = "",
) {
    init {
        exigir(logradouro.length <= 150 && numero.length <= 20 && (complemento?.length ?: 0) <= 100) { "Endereço muito longo." }
        exigir(bairro.length <= 80 && cidade.length <= 80) { "Bairro ou cidade muito longos." }
        exigir(uf.isEmpty() || Regex("^[A-Z]{2}$").matches(uf)) { "UF inválida: use 2 letras (ex.: SP)." }
        exigir(cep.isEmpty() || Regex("^[0-9]{8}$").matches(cep)) { "CEP inválido: use 8 números." }
    }

    /** Tem o mínimo para um entregador chegar. */
    val completoParaEntrega: Boolean get() = listOf(logradouro, numero, bairro, cidade).all { it.isNotBlank() }

    val vazio: Boolean get() = listOf(logradouro, numero, bairro, cidade, uf, cep).all { it.isBlank() } && complemento.isNullOrBlank()
}

data class ContatoLoja(
    val telefone: String? = null,
    val instagram: String? = null,
    val endereco: Endereco? = null,
) {
    init {
        exigir(telefone == null || Regex("^[0-9]{10,13}$").matches(telefone)) {
            "Telefone inválido: informe só números com DDD (ex.: 11987654321)."
        }
        exigir(instagram == null || Regex("^[A-Za-z0-9._]{1,30}$").matches(instagram)) {
            "Instagram inválido: informe só o usuário, sem o @."
        }
    }
}

// ---------- Horários ----------

/** Intervalo de funcionamento num dia da semana. Se [fecha] não é depois de [abre], o intervalo vira a noite. */
data class IntervaloHorario(val dia: DayOfWeek, val abre: LocalTime, val fecha: LocalTime) {
    val viraNoite: Boolean get() = !fecha.isAfter(abre)

    /** Minutos do dia em que abre e fecha (fechamento pode passar de 1440 quando vira a noite). */
    internal val inicioMin: Int get() = abre.toSecondOfDay() / 60
    internal val fimMin: Int get() = fecha.toSecondOfDay() / 60 + if (viraNoite) 24 * 60 else 0
}

// ---------- Encomendas ----------

data class ConfigEncomenda(
    val aceitaAgendamento: Boolean = false,
    val exigeAgendamento: Boolean = false,
    val antecedenciaMinimaHoras: Int = 0,
    val diasMaximos: Int = 14,
    val limitePedidosPorDia: Int? = null,
    val intervaloMinutos: Int = 30,
) {
    init {
        exigir(!exigeAgendamento || aceitaAgendamento) { "Para exigir agendamento, a loja precisa aceitar agendamento." }
        exigir(antecedenciaMinimaHoras in 0..(24 * 30)) { "Antecedência mínima deve ficar entre 0 e 720 horas." }
        exigir(diasMaximos in 1..90) { "O agendamento pode ser de 1 a 90 dias à frente." }
        exigir(limitePedidosPorDia == null || limitePedidosPorDia > 0) { "O limite de pedidos por dia deve ser maior que zero." }
        exigir(intervaloMinutos in 5..240) { "O intervalo entre horários deve ficar entre 5 e 240 minutos." }
    }
}

data class DiaAgenda(val data: LocalDate, val horarios: List<LocalTime>, val lotado: Boolean)

/** Resultado da validação de um pedido novo: quando será produzido. */
data class JanelaDoPedido(val agendadoPara: ZonedDateTime?, val diaProducao: LocalDate)

/**
 * Regras de funcionamento da loja: aberta/fechada, pausa manual, horários e agenda de encomendas.
 * Sem horários cadastrados a loja é considerada sempre aberta (comportamento anterior à Fase 1).
 */
data class Funcionamento(
    val pausada: Boolean = false,
    val mensagemPausa: String? = null,
    val horarios: List<IntervaloHorario> = emptyList(),
    val encomenda: ConfigEncomenda = ConfigEncomenda(),
) {
    init {
        exigir((mensagemPausa?.length ?: 0) <= 200) { "A mensagem de pausa pode ter no máximo 200 caracteres." }
        exigir(horarios.size <= 50) { "Cadastre no máximo 50 intervalos de horário." }
    }

    fun estaAberta(agora: ZonedDateTime): Boolean {
        if (pausada) return false
        if (horarios.isEmpty()) return true
        val minutoAgora = agora.hour * 60 + agora.minute
        val hoje = agora.dayOfWeek
        val ontem = hoje.minus(1)
        return horarios.any { h ->
            (h.dia == hoje && minutoAgora >= h.inicioMin && minutoAgora < h.fimMin) ||
                (h.dia == ontem && h.viraNoite && minutoAgora < h.fimMin - 24 * 60)
        }
    }

    /** Horários de encomenda de uma data, a cada [ConfigEncomenda.intervaloMinutos], dentro do funcionamento do dia. */
    fun horariosDoDia(data: LocalDate): List<LocalTime> =
        horarios.filter { it.dia == data.dayOfWeek }
            .flatMap { h ->
                val fim = minOf(h.fimMin, 24 * 60) // horário de encomenda fica no mesmo dia
                (h.inicioMin until fim step encomenda.intervaloMinutos).map { LocalTime.of(it / 60, it % 60) }
            }
            .distinct()
            .sorted()

    /** Dias e horários em que o cliente pode agendar, já marcando os dias lotados. */
    fun agenda(agora: ZonedDateTime, pedidosPorDia: Map<LocalDate, Int>): List<DiaAgenda> {
        if (pausada || !encomenda.aceitaAgendamento) return emptyList()
        val minimo = agora.plusHours(encomenda.antecedenciaMinimaHoras.toLong())
        val zona = agora.zone
        return (0..encomenda.diasMaximos).mapNotNull { i ->
            val data = agora.toLocalDate().plusDays(i.toLong())
            val livres = horariosDoDia(data).filter { !data.atTime(it).atZone(zona).isBefore(minimo) }
            if (livres.isEmpty()) {
                null
            } else {
                val limite = encomenda.limitePedidosPorDia
                DiaAgenda(data, livres, lotado = limite != null && (pedidosPorDia[data] ?: 0) >= limite)
            }
        }
    }

    /**
     * Confere se um pedido novo pode ser aceito agora (ou para [agendadoPara]) e devolve o dia de produção.
     * [pedidosNoDia] conta os pedidos já aceitos para um dia (para o limite diário).
     */
    fun validarNovoPedido(
        agora: ZonedDateTime,
        agendadoPara: LocalDateTime?,
        pedidosNoDia: (LocalDate) -> Int,
    ): JanelaDoPedido {
        if (pausada) {
            throw Conflito(
                listOfNotNull("A loja não está recebendo pedidos no momento.", mensagemPausa).joinToString(" "),
                "loja_pausada",
            )
        }
        val janela = if (agendadoPara == null) {
            if (encomenda.exigeAgendamento) throw DadoInvalido("Escolha a data e o horário da sua encomenda.", "agendamento_obrigatorio")
            if (!estaAberta(agora)) {
                val dica = if (encomenda.aceitaAgendamento) " Você pode agendar sua encomenda para outro horário." else ""
                throw Conflito("A loja está fechada agora.$dica", "loja_fechada")
            }
            JanelaDoPedido(null, agora.toLocalDate())
        } else {
            if (!encomenda.aceitaAgendamento) throw DadoInvalido("Esta loja não aceita pedidos agendados.", "agendamento_indisponivel")
            val dia = agenda(agora, emptyMap()).find { it.data == agendadoPara.toLocalDate() }
            if (dia == null || agendadoPara.toLocalTime() !in dia.horarios) {
                throw DadoInvalido("Esse horário não está disponível para encomenda. Escolha outro.", "horario_indisponivel")
            }
            JanelaDoPedido(agendadoPara.atZone(agora.zone), agendadoPara.toLocalDate())
        }
        val limite = encomenda.limitePedidosPorDia
        if (limite != null && pedidosNoDia(janela.diaProducao) >= limite) {
            throw Conflito("Não há mais vagas para esse dia. Escolha outra data.", "dia_lotado")
        }
        return janela
    }

    companion object {
        fun agoraEm(zona: ZoneId): ZonedDateTime = ZonedDateTime.now(zona)
    }
}
