package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.Appointment
import com.example.data.supabase.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Supabase PostgreSQL & Realtime Manager for Big Boys Barbershop Chile.
 * Replaces Firebase Cloud Firestore with relational tables and WebSockets.
 */
object SupabaseManager {

    private const val TAG = "SupabaseManager"
    private const val TABLE_APPOINTMENTS = "appointments"
    private const val TABLE_USERS = "users"
    private const val TABLE_LOCATIONS = "locations"
    private const val TABLE_BARBERS = "barbers"
    private const val TABLE_COUPONS = "coupons"
    private const val TABLE_REVIEWS = "reviews"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client by lazy { SupabaseClientProvider.client }
    private val _manualRefreshSignal = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun init(context: Context) {
        Log.d(TAG, "Supabase Client initialized with Project URL: ${SupabaseClientProvider.SUPABASE_URL}")
    }

    /**
     * Triggers an immediate refresh of appointments from Supabase PostgREST
     */
    fun triggerCloudRefresh() {
        _manualRefreshSignal.tryEmit(Unit)
    }

    /**
     * Listens in real-time to all appointments from Supabase PostgreSQL via Realtime WebSocket,
     * combined with automatic background polling and manual refresh triggers.
     * Whenever an appointment is booked on the Web, this flow emits the updated list
     * to immediately block hours in the app.
     */
    fun observeAppointmentsRealtime(): Flow<List<Appointment>> = callbackFlow {
        // 1. Initial query from PostgREST
        try {
            val initialList = fetchAppointmentsFromCloud()
            trySend(initialList)
        } catch (e: Exception) {
            Log.w(TAG, "Initial appointments fetch fallback: ${e.message}")
            trySend(emptyList())
        }

        // 2. Realtime Channel Subscription
        val channel = client.channel("public-appointments-channel")
        val realtimeJob = scope.launch {
            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = TABLE_APPOINTMENTS
                }
                channel.subscribe()

                changeFlow.collect { action ->
                    Log.d(TAG, "Supabase Realtime event on $TABLE_APPOINTMENTS: $action")
                    try {
                        val updated = fetchAppointmentsFromCloud()
                        trySend(updated)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error fetching updated appointments after realtime action", ex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase Realtime subscription error: ${e.message}", e)
            }
        }

        // 3. Periodic Auto-poll fallback (every 8 seconds) to guarantee Web sync
        // even if Realtime publication is not enabled in the Supabase Dashboard
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

        // 4. Manual on-demand refresh signal
        val manualJob = scope.launch {
            _manualRefreshSignal.collect {
                try {
                    val latest = fetchAppointmentsFromCloud()
                    trySend(latest)
                } catch (e: Exception) {
                    Log.w(TAG, "Error in manual appointments refresh: ${e.message}")
                }
            }
        }

        awaitClose {
            realtimeJob.cancel()
            pollJob.cancel()
            manualJob.cancel()
            scope.launch {
                try {
                    channel.unsubscribe()
                } catch (e: Exception) {
                    Log.w(TAG, "Error unsubscribing channel", e)
                }
            }
        }
    }

    /**
     * Queries all appointments using Supabase PostgREST.
     */
    suspend fun fetchAppointmentsFromCloud(): List<Appointment> {
        return try {
            val dtos = client.from(TABLE_APPOINTMENTS)
                .select()
                .decodeList<AppointmentDto>()
            Log.d(TAG, "Fetched ${dtos.size} appointments from Supabase PostgREST")
            dtos.map { it.toAppointment() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching appointments from Supabase PostgREST", e)
            emptyList()
        }
    }

    /**
     * Inserts a newly booked appointment into Supabase PostgreSQL.
     * Uses strict ISO format "yyyy-MM-dd HH:mm" for 100% compatibility with the Web platform.
     */
    suspend fun publishAppointmentToCloud(appointment: Appointment): Boolean {
        return try {
            val datePart = appointment.dateTime.substringBefore(" ").trim()
            val timePart = appointment.dateTime.substringAfter(" ").trim()
            val isoDate = com.example.ui.util.AppFormatters.toIsoDate(datePart)
            val isoHour = com.example.ui.util.AppFormatters.extractHourSlot(timePart)
            val isoDateTime = "$isoDate $isoHour"

            val cloudUser = if (appointment.customerEmail.isNotBlank()) {
                fetchCloudUser(appointment.customerEmail)
            } else null
            val validUserId = cloudUser?.id

            // Ensure barberId and locationId are valid in Supabase
            val validBarberId = if (appointment.barberId in 1..4) appointment.barberId else 1
            val validLocationId = if (appointment.locationId in 1..3) appointment.locationId else 1

            val safePhone = when {
                appointment.customerPhone.isNotBlank() -> appointment.customerPhone
                !cloudUser?.phone.isNullOrBlank() -> cloudUser!!.phone
                else -> "+56912345678"
            }
            val safeName = when {
                appointment.customerName.isNotBlank() -> appointment.customerName
                !cloudUser?.fullName.isNullOrBlank() -> cloudUser!!.fullName
                else -> "Cliente"
            }
            val safeEmail = when {
                appointment.customerEmail.isNotBlank() -> appointment.customerEmail
                !cloudUser?.email.isNullOrBlank() -> cloudUser!!.email
                else -> "cliente@bigboys.cl"
            }

            val dto = AppointmentDto(
                id = System.currentTimeMillis(),
                userId = validUserId,
                barberId = validBarberId,
                locationId = validLocationId,
                serviceName = appointment.serviceName.ifBlank { "Corte de Cabello" },
                servicePrice = appointment.servicePrice.toInt(),
                prepayAmount = appointment.prepayAmount.toInt(),
                dateTime = isoDateTime,
                status = appointment.status.ifBlank { "Confirmada" },
                customerName = safeName,
                customerPhone = safePhone,
                customerEmail = safeEmail,
                isGuest = appointment.isGuest
            )
            client.from(TABLE_APPOINTMENTS).insert(dto)
            Log.d(TAG, "Appointment successfully inserted into Supabase with ISO date: $isoDateTime, barberId: $validBarberId")
            triggerCloudRefresh()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert appointment into Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Updates appointment status (e.g. "Cancelada", "Completada") in Supabase.
     */
    suspend fun updateAppointmentStatusInCloud(appointment: Appointment, newStatus: String): Boolean {
        return try {
            val datePart = appointment.dateTime.substringBefore(" ").trim()
            val timePart = appointment.dateTime.substringAfter(" ").trim()
            val isoDate = com.example.ui.util.AppFormatters.toIsoDate(datePart)
            val isoHour = com.example.ui.util.AppFormatters.extractHourSlot(timePart)
            val isoDateTime = "$isoDate $isoHour"

            if (appointment.id > 1000) {
                val apptId = appointment.id.toLong()
                client.from(TABLE_APPOINTMENTS).update({
                    set("status", newStatus)
                }) {
                    filter {
                        eq("id", apptId)
                    }
                }
            } else {
                client.from(TABLE_APPOINTMENTS).update({
                    set("status", newStatus)
                }) {
                    filter {
                        or {
                            eq("date_time", isoDateTime)
                            eq("date_time", appointment.dateTime)
                        }
                        eq("barber_id", appointment.barberId)
                    }
                }
            }
            Log.d(TAG, "Appointment status updated in Supabase: $newStatus")
            triggerCloudRefresh()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update appointment status in Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Checks if a user already exists in Supabase by email or RUT.
     * Prevents duplicate registration across both Web and Android app.
     */
    suspend fun checkCloudUserExists(email: String, rut: String = ""): Pair<Boolean, String?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val usersByEmail = client.from(TABLE_USERS).select {
                filter {
                    eq("email", cleanEmail)
                }
            }.decodeList<UserDto>()

            if (usersByEmail.isNotEmpty()) {
                val user = usersByEmail.first()
                val name = if (user.fullName.isNotBlank()) " (${user.fullName})" else ""
                return Pair(true, "El correo '$cleanEmail' ya se encuentra registrado$name. Inicia sesión con tus credenciales.")
            }

            if (rut.isNotBlank()) {
                val cleanRut = rut.trim()
                val rutCleanDigits = cleanRut.replace(".", "").replace("-", "").lowercase()
                val allUsers = client.from(TABLE_USERS).select().decodeList<UserDto>()
                val existingByRut = allUsers.find { u ->
                    val uDigits = u.rut.replace(".", "").replace("-", "").lowercase()
                    uDigits.isNotEmpty() && uDigits == rutCleanDigits
                }

                if (existingByRut != null) {
                    val name = if (existingByRut.fullName.isNotBlank()) " (${existingByRut.fullName})" else ""
                    return Pair(true, "El RUT '$cleanRut' ya se encuentra registrado con otra cuenta$name. Inicia sesión con tus credenciales.")
                }
            }

            Pair(false, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence in Supabase: ${e.message}", e)
            Pair(false, null)
        }
    }

    /**
     * Retrieves user profile from Supabase PostgreSQL by email.
     */
    suspend fun fetchCloudUser(email: String): UserDto? {
        return try {
            val cleanEmail = email.trim().lowercase()
            val list = client.from(TABLE_USERS).select {
                filter {
                    eq("email", cleanEmail)
                }
            }.decodeList<UserDto>()
            list.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Supabase: ${e.message}", e)
            null
        }
    }

    /**
     * Syncs / Registers user profile into Supabase PostgreSQL with password hash.
     */
    suspend fun syncUserToCloud(
        email: String,
        fullName: String,
        phone: String,
        rut: String,
        passwordHash: String = ""
    ): Boolean {
        return try {
            val cleanEmail = email.trim().lowercase()
            val existing = fetchCloudUser(cleanEmail)
            if (existing == null) {
                val newUser = UserDto(
                    fullName = fullName,
                    email = cleanEmail,
                    phone = phone,
                    rut = rut,
                    passwordHash = passwordHash
                )
                client.from(TABLE_USERS).insert(newUser)
                Log.d(TAG, "User registered in Supabase users table: $cleanEmail")
            } else {
                client.from(TABLE_USERS).update({
                    set("full_name", fullName)
                    set("phone", phone)
                    set("rut", rut)
                    if (passwordHash.isNotEmpty()) {
                        set("password_hash", passwordHash)
                    }
                }) {
                    filter {
                        eq("email", cleanEmail)
                    }
                }
                Log.d(TAG, "User profile updated in Supabase users table: $cleanEmail")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user to Supabase: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches locations from Supabase PostgreSQL.
     */
    suspend fun fetchLocations(): List<LocationDto> {
        return try {
            client.from(TABLE_LOCATIONS).select().decodeList<LocationDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching locations from Supabase", e)
            emptyList()
        }
    }

    /**
     * Fetches barbers from Supabase PostgreSQL.
     */
    suspend fun fetchBarbers(): List<BarberDto> {
        return try {
            client.from(TABLE_BARBERS).select().decodeList<BarberDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching barbers from Supabase", e)
            emptyList()
        }
    }
}
