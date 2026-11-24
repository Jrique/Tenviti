package br.com.tenviti.server

import br.com.tenviti.server.dominio.ConfigEncomenda
import br.com.tenviti.server.dominio.Conflito
import br.com.tenviti.server.dominio.DadoInvalido
import br.com.tenviti.server.dominio.Funcionamento
import br.com.tenviti.server.dominio.GrupoOpcoes
import br.com.tenviti.server.dominio.IntervaloHorario
import br.com.tenviti.server.dominio.Opcao
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regras de domínio da Fase 1 com relógio fixo (sem banco). */
class FuncionamentoTest {
    private val sp = ZoneId.of("America/Sao_Paulo")

    // 2026-11-18 é uma quarta-feira
    private fun em(dia: Int, hora: Int, minuto: Int = 0) = ZonedDateTime.of(2026, 11, dia, hora, minuto, 0, 0, sp)

    private val horarioComercial = listOf(
        IntervaloHorario(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(14, 0)),
        IntervaloHorario(DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(23, 0)),
        // sexta vira a noite até 2h do sábado
        IntervaloHorario(DayOfWeek.FRIDAY, LocalTime.of(18, 0), LocalTime.of(2, 0)),
    )

    @Test
    fun abertaDentroDosIntervalosEFechadaFora() {
        val f = Funcionamento(horarios = horarioComercial)
        assertTrue(f.estaAberta(em(18, 10, 0)))
        assertTrue(f.estaAberta(em(18, 13, 59)))
        assertFalse(f.estaAberta(em(18, 14, 0)))
        assertFalse(f.estaAberta(em(18, 16, 0)))
        assertTrue(f.estaAberta(em(18, 22, 30)))
        assertFalse(f.estaAberta(em(19, 12, 0))) // quinta sem horário
    }

    @Test
    fun intervaloQueViraANoiteContinuaAbertoNaMadrugada() {
        val f = Funcionamento(horarios = horarioComercial)
        assertTrue(f.estaAberta(em(20, 23, 0)))  // sexta 23h
        assertTrue(f.estaAberta(em(21, 1, 30)))  // sábado 1h30
        assertFalse(f.estaAberta(em(21, 2, 0)))  // sábado 2h
    }

    @Test
    fun semHorariosEstaSempreAbertaMasPausaFecha() {
        assertTrue(Funcionamento().estaAberta(em(18, 3, 0)))
        assertFalse(Funcionamento(pausada = true).estaAberta(em(18, 12, 0)))
    }

    @Test
    fun agendaRespeitaAntecedenciaIntervaloELimite() {
        val f = Funcionamento(
            horarios = listOf(
                IntervaloHorario(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                IntervaloHorario(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)),
            ),
            encomenda = ConfigEncomenda(
                aceitaAgendamento = true, antecedenciaMinimaHoras = 24, diasMaximos = 3,
                limitePedidosPorDia = 2, intervaloMinutos = 60,
            ),
        )
        // quarta 09:30 + 24h = quinta 09:30 → quinta só tem 10:00
        val agenda = f.agenda(em(18, 9, 30), mapOf(LocalDate.of(2026, 11, 20) to 2))
        assertEquals(listOf(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 20)), agenda.map { it.data })
        assertEquals(listOf(LocalTime.of(10, 0)), agenda[0].horarios)
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(10, 0)), agenda[1].horarios)
        assertFalse(agenda[0].lotado)
        assertTrue(agenda[1].lotado)
    }

    @Test
    fun pedidoAgendadoForaDaAgendaOuEmDiaLotadoERecusado() {
        val f = Funcionamento(
            horarios = listOf(IntervaloHorario(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))),
            encomenda = ConfigEncomenda(aceitaAgendamento = true, exigeAgendamento = true, limitePedidosPorDia = 1, intervaloMinutos = 60),
        )
        val agora = em(18, 15, 0)
        val quinta10h = LocalDateTime.of(2026, 11, 19, 10, 0)

        val janela = f.validarNovoPedido(agora, quinta10h) { 0 }
        assertEquals(LocalDate.of(2026, 11, 19), janela.diaProducao)

        assertFailsWith<DadoInvalido> { f.validarNovoPedido(agora, null) { 0 } }                                   // exige agendamento
        assertFailsWith<DadoInvalido> { f.validarNovoPedido(agora, LocalDateTime.of(2026, 11, 19, 10, 30)) { 0 } } // fora do intervalo
        assertFailsWith<DadoInvalido> { f.validarNovoPedido(agora, LocalDateTime.of(2026, 11, 19, 14, 0)) { 0 } }  // fora do horário
        assertFailsWith<Conflito> { f.validarNovoPedido(agora, quinta10h) { 1 } }                                    // lotado
    }

    @Test
    fun pedidoImediatoComLojaFechadaOuPausadaERecusado() {
        val f = Funcionamento(horarios = horarioComercial)
        assertFailsWith<Conflito> { f.validarNovoPedido(em(18, 16, 0), null) { 0 } }
        f.validarNovoPedido(em(18, 12, 0), null) { 0 }
        assertFailsWith<Conflito> { f.copy(pausada = true).validarNovoPedido(em(18, 12, 0), null) { 0 } }
    }

    @Test
    fun grupoDeOpcoesValidaMinimoEMaximo() {
        val sabores = listOf(Opcao(nome = "Nutella"), Opcao(nome = "Tradicional"))
        GrupoOpcoes(nome = "Escolha 4 sabores", minimo = 4, maximo = 4, permiteRepetir = true, opcoes = sabores)
        assertFailsWith<DadoInvalido> { GrupoOpcoes(nome = "Escolha 4", minimo = 4, maximo = 4, permiteRepetir = false, opcoes = sabores) }
        assertFailsWith<DadoInvalido> { GrupoOpcoes(nome = "X", minimo = 3, maximo = 2, opcoes = sabores) }
        assertFailsWith<DadoInvalido> { GrupoOpcoes(nome = "Vazio", opcoes = emptyList()) }
    }
}
