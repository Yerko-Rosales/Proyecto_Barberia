package com.example

import com.example.ui.util.ValidationUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

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
