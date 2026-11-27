package br.com.tenviti.app.data

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Converte a data ISO (UTC) do servidor em "Hoje, 15:30", "Ontem, 09:10" ou "12/11/2026, 20:00". */
fun formatarHorarioPedido(iso: String, fuso: TimeZone = TimeZone.currentSystemDefault()): String {
    val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    val local = instante.toLocalDateTime(fuso)
    val hoje = Clock.System.now().toLocalDateTime(fuso).date
    val ontem = hoje.minus(1, DateTimeUnit.DAY)
    val hora = "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    val dia = when (local.date) {
        hoje -> "Hoje"
        ontem -> "Ontem"
        else -> "${local.dayOfMonth.toString().padStart(2, '0')}/${local.monthNumber.toString().padStart(2, '0')}/${local.year}"
    }
    return "$dia, $hora"
}

/** Data/hora da encomenda no fuso local: "20/11 10:00". */
fun formatarAgendamento(iso: String, fuso: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = runCatching { Instant.parse(iso) }.getOrNull()?.toLocalDateTime(fuso) ?: return iso
    fun dois(n: Int) = n.toString().padStart(2, '0')
    return "${dois(local.dayOfMonth)}/${dois(local.monthNumber)} ${dois(local.hour)}:${dois(local.minute)}"
}

private val DIAS_SEMANA = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")
private val MESES = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")

/** "Quinta, 27 de novembro". */
fun dataDeHojePorExtenso(fuso: TimeZone = TimeZone.currentSystemDefault()): String {
    val hoje = Clock.System.now().toLocalDateTime(fuso)
    val semana = DIAS_SEMANA[hoje.dayOfWeek.ordinal].replaceFirstChar { it.uppercase() }
    return "$semana, ${hoje.dayOfMonth} de ${MESES[hoje.monthNumber - 1]}"
}

/** "Bom dia", "Boa tarde" ou "Boa noite" conforme a hora local. */
fun saudacao(fuso: TimeZone = TimeZone.currentSystemDefault()): String = when (Clock.System.now().toLocalDateTime(fuso).hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/** O instante ISO cai hoje (fuso local)? */
fun ehHoje(iso: String, fuso: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return false
    return instante.toLocalDateTime(fuso).date == Clock.System.now().toLocalDateTime(fuso).date
}

/** Tempo desde o instante: "agora", "12 min", "2 h", "3 d". */
fun tempoDesde(iso: String): String {
    val instante = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val min = (Clock.System.now() - instante).inWholeMinutes
    return when {
        min < 1 -> "agora"
        min < 60 -> "$min min"
        min < 60 * 24 -> "${min / 60} h"
        else -> "${min / (60 * 24)} d"
    }
}

/** Só a hora local de um instante ISO: "14:30". */
fun horaLocal(iso: String, fuso: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = runCatching { Instant.parse(iso) }.getOrNull()?.toLocalDateTime(fuso) ?: return ""
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
