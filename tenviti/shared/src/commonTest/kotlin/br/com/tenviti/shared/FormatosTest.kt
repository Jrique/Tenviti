package br.com.tenviti.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormatosTest {

    @Test
    fun formataCentavosEmReais() {
        assertEquals("R$ 0,00", 0L.emReais())
        assertEquals("R$ 25,90", 2590L.emReais())
        assertEquals("R$ 1.234,56", 123456L.emReais())
        assertEquals("R$ 1.000.000,00", 100000000L.emReais())
        assertEquals("-R$ 5,00", (-500L).emReais())
    }

    @Test
    fun converteTextoEmCentavos() {
        assertEquals(2590L, "25,90".reaisParaCentavos())
        assertEquals(2590L, "25.90".reaisParaCentavos())
        assertEquals(2500L, "25".reaisParaCentavos())
        assertEquals(2550L, "25,5".reaisParaCentavos())
        assertEquals(123450L, "R$ 1.234,50".reaisParaCentavos())
        assertNull("abc".reaisParaCentavos())
        assertNull("1,234".reaisParaCentavos())
        assertNull("".reaisParaCentavos())
    }

    @Test
    fun validaSlug() {
        assertTrue(Formatos.slugValido("doces-da-ana"))
        assertTrue(Formatos.slugValido("pizzaria10"))
        assertFalse(Formatos.slugValido("Doces da Ana"))
        assertFalse(Formatos.slugValido("-ana"))
        assertFalse(Formatos.slugValido("a"))
        assertFalse(Formatos.slugValido("api"))
    }

    @Test
    fun sugereSlugSemAcento() {
        assertEquals("doces-da-ana", Formatos.sugerirSlug("Doces da Ana"))
        assertEquals("acai-do-joao", Formatos.sugerirSlug("Açaí do João!"))
    }

    @Test
    fun validaCor() {
        assertTrue(Formatos.corValida("#790001"))
        assertFalse(Formatos.corValida("790001"))
        assertFalse(Formatos.corValida("#79000"))
    }
}
