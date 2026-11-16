package com.example

import com.example.data.supabase.model.UserDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEF-021: the customer login must reject staff accounts returned by api_login. */
class StaffLoginRoleTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loginResponse(role: String?): UserDto {
        val roleField = if (role == null) "" else ""","role":"$role""""
        return json.decodeFromString(
            UserDto.serializer(),
            """{"id":8,"full_name":"Prueba","email":"prueba@ejemplo.cl","phone":"","client_id":null,"points":0$roleField}"""
        )
    }

    @Test
    fun staffRolesAreRejectedByTheCustomerLogin() {
        listOf("ADMIN_SUCURSAL", "BARBERO", "DUENO").forEach { role ->
            assertTrue("$role debe quedar bloqueado", loginResponse(role).isStaff)
        }
    }

    @Test
    fun customersAndResponsesWithoutRoleCanSignIn() {
        assertFalse(loginResponse("CLIENTE").isStaff)
        assertFalse(loginResponse(null).isStaff)
    }
}
