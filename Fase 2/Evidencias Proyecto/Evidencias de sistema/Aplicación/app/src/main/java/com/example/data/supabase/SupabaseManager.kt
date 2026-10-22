package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.Appointment
import com.example.data.supabase.model.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase Manager for Big Boys Barbershop Chile.
 * The app never reads the tables directly: the 27 relational tables keep RLS closed and
 * every operation goes through the api_* functions (same layer used by the web).
 */
object SupabaseManager {

    private const val TAG = "SupabaseManager"
    private const val FORMAT = "APP"
    private const val CHANNEL = "APP"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client by lazy { SupabaseClientProvider.client }
    private val _manualRefreshSignal = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Session credentials used by api_listar_reservas to show the owner's contact data
    @Volatile private var sessionEmail: String? = null
    @Volatile private var sessionPasswordHash: String? = null

    fun init(context: Context) {
        Log.d(TAG, "Supabase Client initialized with Project URL: ${SupabaseClientProvider.SUPABASE_URL}")
    }

    fun setSession(email: String?, passwordHash: String?) {
        sessionEmail = email?.trim()?.lowercase()
        sessionPasswordHash = passwordHash
        triggerCloudRefresh()
    }

    /**
     * Triggers an immediate refresh of appointments from Supabase
     */
    fun triggerCloudRefresh() {
        _manualRefreshSignal.tryEmit(Unit)
    }

    private suspend fun callRpc(function: String, params: JsonObject = buildJsonObject { }) =
        client.postgrest.rpc(function, params)

    /** "dd-MM-yyyy HH:mm hrs" or "yyyy-MM-dd HH:mm" -> "yyyy-MM-dd HH:mm" */
    private fun toIsoDateTime(dateTime: String): String {
        val datePart = dateTime.substringBefore(" ").trim()
        val timePart = dateTime.substringAfter(" ").trim()
        val isoDate = com.example.ui.util.AppFormatters.toIsoDate(datePart)
        val isoHour = com.example.ui.util.AppFormatters.extractHourSlot(timePart)
        return "$isoDate $isoHour"
    }

    /**
     * Appointments from Supabase, refreshed by polling (every 8 seconds) and on demand.
     * The tables are closed (RLS), so Realtime on the old prototype table is no longer used.
     */
    fun observeAppointmentsRealtime(): Flow<List<Appointment>> = callbackFlow {
        try {
            trySend(fetchAppointmentsFromCloud())
        } catch (e: Exception) {
            Log.w(TAG, "Initial appointments fetch fallback: ${e.message}")
            trySend(emptyList())
        }

        val pollJob = scope.launch {
            while (isActive) {
                delay(8000)
                try {
                    val latest = fetchAppointmentsFromCloud()
                    if (latest.isNotEmpty()) {
                        trySend(latest)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error in periodic Supabase appointments poll: ${e.message}")
                }
            }
        }

        val manualJob = scope.launch {
            _manualRefreshSignal.collect {
                try {
                    trySend(fetchAppointmentsFromCloud())
                } catch (e: Exception) {
                    Log.w(TAG, "Error in manual appointments refresh: ${e.message}")
                }
            }
        }

        awaitClose {
            pollJob.cancel()
            manualJob.cancel()
        }
    }

    /**
     * Lists reservations (table reserva). Other customers' contact data comes masked.
     */
    suspend fun fetchAppointmentsFromCloud(): List<Appointment> {
        return try {
            val dtos = callRpc("api_listar_reservas", buildJsonObject {
                put("p_formato", FORMAT)
                sessionEmail?.let { put("p_email", it) }
                sessionPasswordHash?.let { put("p_password_sha256", it) }
            }).decodeList<AppointmentDto>()
            Log.d(TAG, "Fetched ${dtos.size} reservations from Supabase")
            dtos.map { it.toAppointment() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reservations from Supabase", e)
            emptyList()
        }
    }

    /**
     * Creates a reservation (cliente, reserva and notificacion are written by api_crear_reserva).
     * Returns false if the slot is already taken (HORARIO_OCUPADO) or the call fails.
     */
    suspend fun publishAppointmentToCloud(appointment: Appointment): Boolean {
        return try {
            val isoDateTime = toIsoDateTime(appointment.dateTime)
            callRpc("api_crear_reserva", buildJsonObject {
                put("p", buildJsonObject {
                    put("id", System.currentTimeMillis())
                    put("canal", CHANNEL)
                    put("barber_id", appointment.barberId)
                    put("location_id", appointment.locationId)
                    put("service_name", appointment.serviceName.ifBlank { "Corte" })
                    put("date_time", isoDateTime)
                    put("status", appointment.status.ifBlank { "Confirmada" })
                    put("customer_name", appointment.customerName.ifBlank { "Cliente" })
                    put("customer_phone", appointment.customerPhone)
                    put("customer_email", appointment.customerEmail)
                })
            })
            Log.d(TAG, "Reservation inserted into Supabase: $isoDateTime, barberId: ${appointment.barberId}")
            triggerCloudRefresh()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert reservation into Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Updates a reservation status (e.g. "Cancelada", "Completada", "No-Show").
     * The reservation is found by id or, for local copies, by barber and date/time.
     * cancelledBy distinguishes customer cancellations (deposit rule) from staff ones.
     */
    suspend fun updateAppointmentStatusInCloud(appointment: Appointment, newStatus: String, cancelledBy: String? = null): Boolean {
        return try {
            callRpc("api_actualizar_reserva", buildJsonObject {
                put("p", buildJsonObject {
                    put("id", appointment.id)
                    put("canal", CHANNEL)
                    put("barber_id", appointment.barberId)
                    put("date_time", toIsoDateTime(appointment.dateTime))
                    put("status", newStatus)
                    cancelledBy?.let { put("cancelled_by", it) }
                })
            })
            Log.d(TAG, "Reservation status updated in Supabase: $newStatus")
            triggerCloudRefresh()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update reservation status in Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Reschedules a reservation; the database enforces >= 24 h notice and at most 2 changes
     * and records the change in reagendamiento.
     */
    suspend fun rescheduleAppointmentInCloud(appointment: Appointment, oldDateTime: String): Boolean {
        return try {
            callRpc("api_actualizar_reserva", buildJsonObject {
                put("p", buildJsonObject {
                    put("id", appointment.id)
                    put("canal", CHANNEL)
                    put("barber_id", appointment.barberId)
                    put("date_time", toIsoDateTime(oldDateTime))
                    put("new_date_time", toIsoDateTime(appointment.dateTime))
                })
            })
            triggerCloudRefresh()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reservation in Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Checks if an account already exists (table usuario). RUT is not stored (data minimization).
     */
    suspend fun checkCloudUserExists(email: String): Pair<Boolean, String?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val exists = callRpc("api_usuario_existe", buildJsonObject {
                put("p_email", cleanEmail)
            }).decodeAs<Boolean>()
            if (exists) {
                Pair(true, "El correo '$cleanEmail' ya se encuentra registrado. Inicia sesión con tus credenciales.")
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence in Supabase: ${e.message}", e)
            Pair(false, null)
        }
    }

    /**
     * Validates the credentials in the database (api_login). The stored password never leaves Supabase;
     * the returned profile carries the same SHA-256 hash the app already computed.
     */
    suspend fun fetchCloudUser(email: String, passwordHash: String): UserDto? {
        return try {
            val cleanEmail = email.trim().lowercase()
            val result = callRpc("api_login", buildJsonObject {
                put("p_email", cleanEmail)
                put("p_password_sha256", passwordHash)
            })
            if (result.data.isBlank() || result.data.trim() == "null") {
                null
            } else {
                result.decodeAs<UserDto>().copy(passwordHash = passwordHash)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating user in Supabase: ${e.message}", e)
            null
        }
    }

    /**
     * Registers a customer account (usuario, rol CLIENTE, cliente and consentimiento).
     */
    suspend fun syncUserToCloud(
        email: String,
        fullName: String,
        phone: String,
        passwordHash: String
    ): Boolean {
        return try {
            callRpc("api_registrar_cliente", buildJsonObject {
                put("p_email", email.trim().lowercase())
                put("p_nombre_completo", fullName)
                put("p_telefono", phone)
                put("p_password_sha256", passwordHash)
                put("p_canal", CHANNEL)
            })
            Log.d(TAG, "Customer registered in Supabase: $email")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register customer in Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches branches (table sucursal).
     */
    suspend fun fetchLocations(): List<LocationDto> {
        return try {
            callRpc("api_sucursales").decodeList<LocationDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching branches from Supabase", e)
            emptyList()
        }
    }

    /**
     * Fetches barbers (tables barbero, horario_barbero and resena for the rating).
     */
    suspend fun fetchBarbers(): List<BarberDto> {
        return try {
            callRpc("api_barberos").decodeList<BarberDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching barbers from Supabase", e)
            emptyList()
        }
    }
}
