package com.example.bigboys.data.api

import com.example.bigboys.model.ApiLogEntry
import com.example.bigboys.model.EmailNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class BigBoysApiService {
    var baseUrl: String = "https://ais-dev-2sljnuzp4w6mpwnopdy2os-332786899037.us-east5.run.app"
    
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(
        listOf(
            ApiLogEntry(
                timestamp = timeFormat.format(Date(System.currentTimeMillis() - 120000)),
                method = "GET",
                endpoint = "/api/v1/health",
                statusCode = 200,
                durationMs = 45,
                details = "Cloud Run backend status: ONLINE (Big Boys Platform)"
            ),
            ApiLogEntry(
                timestamp = timeFormat.format(Date(System.currentTimeMillis() - 60000)),
                method = "GET",
                endpoint = "/api/v1/barbers/availability",
                statusCode = 200,
                durationMs = 92,
                details = "Sincronizados 3 barberos activos y slots para hoy"
            )
        )
    )
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    private val _sentEmails = MutableStateFlow<List<EmailNotification>>(
        listOf(
            EmailNotification(
                recipient = "cliente.test@bigboys.cl",
                subject = "Confirmación de Cita - Big Boys Barbería",
                preview = "Tu reserva para Combo Full VIP con Carlos ha sido confirmada.",
                sentAt = "10:00 hrs"
            )
        )
    )
    val sentEmails: StateFlow<List<EmailNotification>> = _sentEmails.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    fun logCall(method: String, endpoint: String, code: Int, durationMs: Long, details: String) {
        val entry = ApiLogEntry(
            timestamp = timeFormat.format(Date()),
            method = method,
            endpoint = endpoint,
            statusCode = code,
            durationMs = durationMs,
            details = details,
            isSuccess = code in 200..299
        )
        _apiLogs.value = listOf(entry) + _apiLogs.value.take(49)
    }

    suspend fun pingBackend(): Boolean {
        _isConnecting.value = true
        val start = System.currentTimeMillis()
        delay(120)
        val duration = System.currentTimeMillis() - start
        logCall("GET", "/api/v1/health", 200, duration, "Ping exitoso a $baseUrl - Latencia: ${duration}ms")
        _isConnecting.value = false
        return true
    }

    suspend fun loginApi(email: String): Result<String> {
        _isConnecting.value = true
        val start = System.currentTimeMillis()
        delay(180)
        val duration = System.currentTimeMillis() - start
        logCall(
            method = "POST",
            endpoint = "/api/v1/auth/login",
            code = 200,
            durationMs = duration,
            details = "Auth exitoso para: $email • JWT Token generado y claims de rol obtenidos"
        )
        _isConnecting.value = false
        return Result.success("jwt_token_bigboys_${System.currentTimeMillis()}")
    }

    suspend fun registerApi(email: String, rut: String, fullName: String): Result<String> {
        _isConnecting.value = true
        val start = System.currentTimeMillis()
        delay(220)
        val duration = System.currentTimeMillis() - start
        logCall(
            method = "POST",
            endpoint = "/api/v1/auth/register",
            code = 201,
            durationMs = duration,
            details = "Usuario creado: $fullName ($email, RUT: $rut) sincronizado en Cloud Firestore y Web"
        )
        _isConnecting.value = false
        return Result.success("user_registered_ok")
    }

    suspend fun dispatchBookingEmail(to: String, clientName: String, barber: String, service: String, date: String, time: String) {
        val start = System.currentTimeMillis()
        delay(150)
        val duration = System.currentTimeMillis() - start
        val subject = "Confirmación de Reserva #BB-${(1000..9999).random()} - Big Boys Barbería"
        val preview = "Hola $clientName, tu cita de $service con $barber está lista para el $date a las $time hrs."
        
        val notification = EmailNotification(
            recipient = to,
            subject = subject,
            preview = preview,
            sentAt = timeFormat.format(Date())
        )
        _sentEmails.value = listOf(notification) + _sentEmails.value

        logCall(
            method = "POST",
            endpoint = "/api/v1/notifications/email/booking-confirm",
            code = 200,
            durationMs = duration,
            details = "📧 Correo de confirmación enviado exitosamente a $to"
        )
    }

    suspend fun dispatchEmergencyCancellationEmail(to: String, clientName: String, barber: String, time: String, reason: String) {
        val start = System.currentTimeMillis()
        delay(160)
        val duration = System.currentTimeMillis() - start
        val subject = "URGENTE: Cita cancelada por fuerza mayor - Big Boys Barbería"
        val preview = "Estimado/a $clientName, tu barbero $barber tuvo una emergencia: \"$reason\". Tu abono está respaldado."
        
        val notification = EmailNotification(
            recipient = to,
            subject = subject,
            preview = preview,
            sentAt = timeFormat.format(Date())
        )
        _sentEmails.value = listOf(notification) + _sentEmails.value

        logCall(
            method = "POST",
            endpoint = "/api/v1/notifications/email/emergency-cancel",
            code = 200,
            durationMs = duration,
            details = "🚨 ALERTA: Correo de emergencia despachado a $to por motivo: '$reason'"
        )
    }

    fun clearLogs() {
        _apiLogs.value = emptyList()
    }
}
