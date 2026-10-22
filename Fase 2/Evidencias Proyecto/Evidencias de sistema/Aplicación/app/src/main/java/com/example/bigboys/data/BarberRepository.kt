package com.example.bigboys.data

import com.example.bigboys.data.api.BigBoysApiService
import com.example.bigboys.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BarberRepository {

    val apiService = BigBoysApiService()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDate: String = dateFormat.format(Date())

    val services = listOf(
        ServiceItem("1", "Corte Big Boys Signature", 45, 15000, "Degradado personalizado, tijera, perfilado a navaja, lavado y fijación.", "Cortes"),
        ServiceItem("2", "Perfilado de Barba & Ritual Hot Towel", 35, 10000, "Vaporizador, toalla caliente con aceites esenciales, rebaje y navaja.", "Barba"),
        ServiceItem("3", "Combo Full VIP (Corte + Barba)", 65, 22000, "La experiencia completa de Big Boys: Corte, barba, exfoliación y tratamiento capilar.", "Combos"),
        ServiceItem("4", "Corte Clásico Tradicional", 35, 13000, "Tijera o máquina clásica sin fade extremo, ideal para estilo formal.", "Cortes"),
        ServiceItem("5", "Tratamiento Facial & Black Mask", 30, 8000, "Limpieza profunda de poros, exfoliación y mascarilla purificante.", "Spa")
    )

    val barbers = listOf(
        Barber("b1", "Carlos 'El Patrón'", "carlos@bigboys.cl", "Master Barber • Fades & Freestyle", 4.9, 1420, "10 años dominando la navaja y el estilo urbano."),
        Barber("b2", "Matías 'Beard King'", "matias@bigboys.cl", "Especialista en Barbas & Afeitado Clásico", 4.8, 980, "Experto en diseño de barba y cuidado de la piel."),
        Barber("b3", "Diego 'The Stylist'", "diego@bigboys.cl", "Cortes Clásicos & Texturizados Modernos", 5.0, 1150, "Perfección en tijera y asesoría de imagen.")
    )

    // Registered users (Email as unique key)
    private val _users = MutableStateFlow<Map<String, User>>(
        mapOf(
            "cliente.test@bigboys.cl" to User(
                email = "cliente.test@bigboys.cl",
                rut = "18.345.678-2",
                fullName = "Juan Pérez",
                phone = "+56 9 8765 4321",
                role = UserRole.CLIENT
            ),
            "carlos@bigboys.cl" to User(
                email = "carlos@bigboys.cl",
                rut = "15.432.198-7",
                fullName = "Carlos 'El Patrón'",
                phone = "+56 9 9123 4567",
                role = UserRole.BARBER,
                barberName = "Carlos 'El Patrón'"
            ),
            "matias@bigboys.cl" to User(
                email = "matias@bigboys.cl",
                rut = "16.789.012-3",
                fullName = "Matías 'Beard King'",
                phone = "+56 9 8234 5678",
                role = UserRole.BARBER,
                barberName = "Matías 'Beard King'"
            ),
            "diego@bigboys.cl" to User(
                email = "diego@bigboys.cl",
                rut = "17.890.123-4",
                fullName = "Diego 'The Stylist'",
                phone = "+56 9 7345 6789",
                role = UserRole.BARBER,
                barberName = "Diego 'The Stylist'"
            )
        )
    )

    // Current logged in user (null by default so the user sees the Splash entrance and Login flow)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(
        listOf(
            Appointment(
                id = "apt-1",
                customerName = "Juan Pérez",
                customerEmail = "cliente.test@bigboys.cl",
                customerPhone = "+56 9 8765 4321",
                customerRut = "18.345.678-2",
                barberName = "Carlos 'El Patrón'",
                serviceName = "Combo Full VIP (Corte + Barba)",
                date = todayDate,
                time = "10:00",
                depositAmount = 5000,
                totalPrice = 22000,
                status = AppointmentStatus.CONFIRMED
            ),
            Appointment(
                id = "apt-2",
                customerName = "Cristóbal Valdés",
                customerEmail = "c.valdes@gmail.com",
                customerPhone = "+56 9 7654 3210",
                customerRut = "19.876.543-K",
                barberName = "Carlos 'El Patrón'",
                serviceName = "Corte Big Boys Signature",
                date = todayDate,
                time = "11:30",
                depositAmount = 5000,
                totalPrice = 15000,
                status = AppointmentStatus.CONFIRMED
            ),
            Appointment(
                id = "apt-3",
                customerName = "Ignacio Morales",
                customerEmail = "i.morales@outlook.com",
                customerPhone = "+56 9 6543 2109",
                customerRut = "17.654.321-9",
                barberName = "Carlos 'El Patrón'",
                serviceName = "Perfilado de Barba & Ritual Hot Towel",
                date = todayDate,
                time = "14:00",
                depositAmount = 5000,
                totalPrice = 10000,
                status = AppointmentStatus.CONFIRMED
            ),
            Appointment(
                id = "apt-4",
                customerName = "Rodrigo Fuentes",
                customerEmail = "r.fuentes@gmail.com",
                customerPhone = "+56 9 5432 1098",
                customerRut = "16.543.210-4",
                barberName = "Matías 'Beard King'",
                serviceName = "Combo Full VIP (Corte + Barba)",
                date = todayDate,
                time = "11:00",
                depositAmount = 5000,
                totalPrice = 22000,
                status = AppointmentStatus.CONFIRMED
            )
        )
    )
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    fun login(email: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val user = _users.value[cleanEmail]
        return if (user != null) {
            _currentUser.value = user
            scope.launch {
                apiService.loginApi(cleanEmail)
            }
            Result.success(user)
        } else {
            apiService.logCall("POST", "/api/v1/auth/login", 404, 35, "Fallo de login: $cleanEmail no registrado")
            Result.failure(Exception("El correo '$cleanEmail' no está registrado. Por favor crea una cuenta."))
        }
    }

    fun logout() {
        val user = _currentUser.value
        apiService.logCall("POST", "/api/v1/auth/logout", 200, 20, "Sesión cerrada: ${user?.email ?: "Desconocido"}")
        _currentUser.value = null
    }

    fun register(
        email: String,
        rut: String,
        fullName: String,
        phone: String,
        role: UserRole = UserRole.CLIENT,
        barberName: String? = null
    ): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val cleanRut = rut.trim()

        if (_users.value.containsKey(cleanEmail)) {
            apiService.logCall("POST", "/api/v1/auth/register", 409, 30, "Rechazado: Correo duplicado ($cleanEmail)")
            return Result.failure(Exception("Este correo ya está registrado en Big Boys. Por favor inicia sesión."))
        }

        val rutExists = _users.value.values.any { it.rut.equals(cleanRut, ignoreCase = true) }
        if (rutExists) {
            apiService.logCall("POST", "/api/v1/auth/register", 409, 32, "Rechazado: RUT duplicado ($cleanRut)")
            return Result.failure(Exception("El RUT '$cleanRut' ya se encuentra registrado con otra cuenta."))
        }

        val newUser = User(
            email = cleanEmail,
            rut = cleanRut,
            fullName = fullName.trim(),
            phone = phone.trim(),
            role = role,
            barberName = barberName
        )

        _users.value = _users.value + (cleanEmail to newUser)
        _currentUser.value = newUser

        scope.launch {
            apiService.registerApi(cleanEmail, cleanRut, fullName)
        }

        return Result.success(newUser)
    }

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun bookAppointment(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        customerRut: String,
        barberName: String,
        serviceName: String,
        date: String,
        time: String,
        totalPrice: Int
    ): Result<Appointment> {
        val cleanEmail = customerEmail.trim().lowercase()
        val cleanRut = customerRut.trim()

        val newApt = Appointment(
            id = "apt-${System.currentTimeMillis()}",
            customerName = customerName.trim(),
            customerEmail = cleanEmail,
            customerPhone = customerPhone.trim(),
            customerRut = cleanRut,
            barberName = barberName,
            serviceName = serviceName,
            date = date,
            time = time,
            depositAmount = 5000,
            totalPrice = totalPrice,
            status = AppointmentStatus.CONFIRMED
        )
        _appointments.value = _appointments.value + newApt

        scope.launch {
            apiService.logCall(
                method = "POST",
                endpoint = "/api/v1/appointments",
                code = 201,
                durationMs = 95,
                details = "Nueva cita agendada: $serviceName con $barberName para $customerName ($date $time)"
            )
            apiService.dispatchBookingEmail(
                to = cleanEmail,
                clientName = customerName,
                barber = barberName,
                service = serviceName,
                date = date,
                time = time
            )
        }

        return Result.success(newApt)
    }

    fun cancelByEmergency(appointmentId: String, reason: String, cancelledBy: String = "Barbero"): Boolean {
        val targetApt = _appointments.value.find { it.id == appointmentId }
        _appointments.value = _appointments.value.map { apt ->
            if (apt.id == appointmentId) {
                apt.copy(
                    status = AppointmentStatus.CANCELLED,
                    cancellationReason = reason,
                    cancelledBy = cancelledBy
                )
            } else {
                apt
            }
        }

        targetApt?.let { apt ->
            scope.launch {
                apiService.logCall(
                    method = "POST",
                    endpoint = "/api/v1/appointments/$appointmentId/cancel-emergency",
                    code = 200,
                    durationMs = 88,
                    details = "Cancelación de emergencia registrada por $cancelledBy. Motivo: $reason"
                )
                apiService.dispatchEmergencyCancellationEmail(
                    to = apt.customerEmail,
                    clientName = apt.customerName,
                    barber = apt.barberName,
                    time = apt.time,
                    reason = reason
                )
            }
        }

        return true
    }

    fun markAsCompleted(appointmentId: String): Boolean {
        _appointments.value = _appointments.value.map { apt ->
            if (apt.id == appointmentId) {
                apt.copy(status = AppointmentStatus.COMPLETED)
            } else {
                apt
            }
        }
        scope.launch {
            apiService.logCall(
                method = "PATCH",
                endpoint = "/api/v1/appointments/$appointmentId/complete",
                code = 200,
                durationMs = 60,
                details = "Cita completada y cobro cerrado"
            )
        }
        return true
    }
}
