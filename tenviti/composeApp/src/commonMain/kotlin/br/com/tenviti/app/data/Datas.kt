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
