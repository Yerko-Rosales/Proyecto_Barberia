package com.example

import com.example.ui.util.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun testValidChileanRutsModulo11() {
        // Mathematically verified Chilean RUTs (Modulo 11)
        assertTrue(ValidationUtils.isValidChileanRut("19.342.110-5"))
        assertTrue(ValidationUtils.isValidChileanRut("19342110-5"))
        assertTrue(ValidationUtils.isValidChileanRut("193421105"))
        assertTrue(ValidationUtils.isValidChileanRut("11.111.111-1"))
        assertTrue(ValidationUtils.isValidChileanRut("12.345.678-5"))
        assertTrue(ValidationUtils.isValidChileanRut("22.222.222-2"))
        assertTrue(ValidationUtils.isValidChileanRut("15.228.344-K"))
        assertTrue(ValidationUtils.isValidChileanRut("15228344k"))
        assertTrue(ValidationUtils.isValidChileanRut("30.686.957-4"))
    }

    @Test
    fun testInvalidChileanRuts() {
        assertFalse(ValidationUtils.isValidChileanRut(""))
        assertFalse(ValidationUtils.isValidChileanRut("19.342.110-4")) // Wrong DV
        assertFalse(ValidationUtils.isValidChileanRut("12345678-9")) // Wrong DV
        assertFalse(ValidationUtils.isValidChileanRut("123")) // Too short
        assertFalse(ValidationUtils.isValidChileanRut("abcdefgh-k")) // Non-digits
        assertFalse(ValidationUtils.isValidChileanRut("0-0"))
    }

    @Test
    fun testRutFormatting() {
        assertEquals("19.342.110-5", ValidationUtils.formatChileanRut("193421105"))
        assertEquals("15.228.344-K", ValidationUtils.formatChileanRut("15228344K"))
    }

    @Test
    fun testValidEmailFormats() {
        assertTrue(ValidationUtils.isValidEmail("cliente@bigboys.cl"))
        assertTrue(ValidationUtils.isValidEmail("juan.perez@gmail.com"))
        assertTrue(ValidationUtils.isValidEmail("carlos_123@outlook.es"))
        assertTrue(ValidationUtils.isValidEmail("test.user+promo@dominio.cl"))
    }

    @Test
    fun testInvalidEmailFormats() {
        assertFalse(ValidationUtils.isValidEmail(""))
        assertFalse(ValidationUtils.isValidEmail("sinarroba.com"))
        assertFalse(ValidationUtils.isValidEmail("dos@@arrobas.com"))
        assertFalse(ValidationUtils.isValidEmail("usuario@"))
        assertFalse(ValidationUtils.isValidEmail("@dominio.cl"))
        assertFalse(ValidationUtils.isValidEmail("usuario@dominio"))
        assertFalse(ValidationUtils.isValidEmail("usuario space@dominio.cl"))
        assertFalse(ValidationUtils.isValidEmail(".usuario@dominio.cl"))
        assertFalse(ValidationUtils.isValidEmail("usuario.@dominio.cl"))
    }
}
