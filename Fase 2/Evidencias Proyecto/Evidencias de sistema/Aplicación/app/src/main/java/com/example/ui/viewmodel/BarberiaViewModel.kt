package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.supabase.SupabaseManager
import com.example.data.model.*
import com.example.data.repository.AppRepository
import android.util.Log
import com.example.ui.util.AppFormatters
import com.example.ui.util.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BarberiaViewModel(application: Application) : AndroidViewModel(application) {

    private val authPrefs = application.getSharedPreferences("bigboys_auth_prefs", android.content.Context.MODE_PRIVATE)
    private val repository: AppRepository

    // Central state variables
    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    private val _loggedInBarber = MutableStateFlow<Barber?>(null)
    val loggedInBarber: StateFlow<Barber?> = _loggedInBarber.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Screen-level state for booking process
    private val _bookingLocation = MutableStateFlow<Location?>(null)
    val bookingLocation: StateFlow<Location?> = _bookingLocation.asStateFlow()

    private val _bookingBarber = MutableStateFlow<Barber?>(null)
    val bookingBarber: StateFlow<Barber?> = _bookingBarber.asStateFlow()

    private val _bookingDate = MutableStateFlow("")
    val bookingDate: StateFlow<String> = _bookingDate.asStateFlow()

    private val _bookingHour = MutableStateFlow("")
    val bookingHour: StateFlow<String> = _bookingHour.asStateFlow()

    private val _bookingService = MutableStateFlow("Corte de Pelo Degrado")
    val bookingService: StateFlow<String> = _bookingService.asStateFlow()

    private val _bookingPrice = MutableStateFlow(15000.0)
    val bookingPrice: StateFlow<Double> = _bookingPrice.asStateFlow()

    private val _guestName = MutableStateFlow("")
    val guestName: StateFlow<String> = _guestName.asStateFlow()

    private val _guestPhone = MutableStateFlow("")
    val guestPhone: StateFlow<String> = _guestPhone.asStateFlow()

    private val _guestEmail = MutableStateFlow("")
    val guestEmail: StateFlow<String> = _guestEmail.asStateFlow()

    // Pre-pay reservation active variables
    private val _prepaymentAmount = MutableStateFlow(3000.0) // Fixed reservation deposit $3.000 CLP (descontable del servicio)
    val prepaymentAmount: StateFlow<Double> = _prepaymentAmount.asStateFlow()

    // List trackers
    val locationsFlow: StateFlow<List<Location>>
    val allBarbersFlow: StateFlow<List<Barber>>
    val allAppointmentsFlow: StateFlow<List<Appointment>>
    val allCouponsFlow: StateFlow<List<GiftCoupon>>
    val activeUserCoupon: StateFlow<GiftCoupon?>
    val allSuppliesFlow: StateFlow<List<Supply>>
    val allUsersFlow: StateFlow<List<User>>

    // Live Notifications Feed in-app
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Light / Dark Mode State
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    // Admin Customization State
    private val _adminPin = MutableStateFlow("1234")
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    private val _adminName = MutableStateFlow("Administrador Central Big boys")
    val adminName: StateFlow<String> = _adminName.asStateFlow()

    private val _adminAvatarUrl = MutableStateFlow("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80")
    val adminAvatarUrl: StateFlow<String> = _adminAvatarUrl.asStateFlow()

    // Gmail Direct SMTP Configuration (No paid APIs required)
    private val _gmailSenderAddress = MutableStateFlow(com.example.data.mail.DirectGmailSender.senderGmail)
    val gmailSenderAddress: StateFlow<String> = _gmailSenderAddress.asStateFlow()

    private val _gmailAppPassword = MutableStateFlow(com.example.data.mail.DirectGmailSender.senderAppPassword)
    val gmailAppPassword: StateFlow<String> = _gmailAppPassword.asStateFlow()

    private val _emailSendingStatus = MutableStateFlow<String?>(null)
    val emailSendingStatus: StateFlow<String?> = _emailSendingStatus.asStateFlow()

    // Reviews Flow
    val allReviewsFlow: StateFlow<List<Review>>

    data class NotificationItem(
        val id: Long = System.currentTimeMillis() + (0..1000).random(),
        val title: String,
        val message: String,
        val timestamp: String = AppFormatters.currentTimeString()
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        // Initial launch checks
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            repository.syncBarbersAndLocationsFromCloud()
            checkAndExpirePastAppointments()
        }

        // Background loop to evaluate and persist expired appointments in Room and Supabase
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                checkAndExpirePastAppointments()
            }
        }

        // Initialize Supabase Manager
        SupabaseManager.init(application)

        // Initialize lists with reactive Flows
        locationsFlow = repository.allLocationsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allBarbersFlow = repository.allBarbersFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Merge local Room appointments with real-time Supabase PostgreSQL appointments
        allAppointmentsFlow = combine(
            repository.allAppointmentsFlow,
            SupabaseManager.observeAppointmentsRealtime()
        ) { localList, cloudList ->
            val mergedList = if (cloudList.isEmpty()) {
                localList
            } else {
                // Include all cloud appointments (from Web and App) so every booked hour is visible
                val combinedMap = linkedMapOf<String, Appointment>()
                cloudList.forEach { appt ->
                    combinedMap["cloud_${appt.id}"] = appt
                }
                // Include local appointments only if not already matched in cloud
                localList.forEach { appt ->
                    val alreadyInCloud = cloudList.any { c ->
                        (c.id == appt.id) || (
                            c.barberId == appt.barberId &&
                            AppFormatters.isSameDay(c.dateTime, appt.dateTime) &&
                            AppFormatters.extractHourSlot(c.dateTime) == AppFormatters.extractHourSlot(appt.dateTime)
                        )
                    }
                    if (!alreadyInCloud) {
                        combinedMap["local_${appt.id}"] = appt
                    }
                }
                combinedMap.values.toList()
            }

            // Detect and mark expired/caducadas appointments in memory
            mergedList.map { appt ->
                if ((appt.status == "Confirmada" || appt.status == "Pendiente") && AppFormatters.isAppointmentPast(appt.dateTime)) {
                    appt.copy(status = "Caducada")
                } else {
                    appt
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCouponsFlow = repository.allCouponsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allReviewsFlow = repository.allReviewsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeUserCoupon = combine(_loggedInUser, repository.allCouponsFlow) { user, coupons ->
            if (user == null) null else coupons.find { it.userEmail == user.email }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        allSuppliesFlow = repository.allSuppliesFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allUsersFlow = repository.allUsersFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Setup some initial welcome system notifications
        _notifications.value = listOf(
            NotificationItem(
                title = "¡Bienvenido a Big boys!",
                message = "Reserva en línea, revisa tu historial y gestiona tus citas con nuestra app."
            )
        )

        // Restore persisted Gmail credentials across app launches and restarts
        com.example.data.mail.DirectGmailSender.loadCredentials(application)
        _gmailSenderAddress.value = com.example.data.mail.DirectGmailSender.senderGmail
        _gmailAppPassword.value = com.example.data.mail.DirectGmailSender.senderAppPassword

        // Note: We do NOT auto-login on app launch so the app always starts cleanly at the login screen.
        // Remembered credentials are provided to pre-fill the login form fields via getSavedRememberedEmail().
    }

    // --- Remember Me Preferences ---
    fun isRememberMeEnabled(): Boolean {
        return authPrefs.getBoolean("remember_me", false)
    }

    fun getSavedRememberedEmail(): String {
        return if (isRememberMeEnabled()) authPrefs.getString("remembered_email", "") ?: "" else ""
    }

    fun getSavedRememberedPassword(): String {
        return if (isRememberMeEnabled()) authPrefs.getString("remembered_password", "") ?: "" else ""
    }

    fun setRememberMeCredentials(enabled: Boolean, email: String, password: String = "") {
        authPrefs.edit().apply {
            putBoolean("remember_me", enabled)
            if (enabled) {
                putString("remembered_email", email.trim().lowercase())
                putString("remembered_password", password)
            } else {
                remove("remembered_email")
                remove("remembered_password")
            }
            apply()
        }
    }

    // --- Authentication Actions ---
    fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun hashMd5(password: String): String {
        val digest = java.security.MessageDigest.getInstance("MD5")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        val cleanP = password.trim()
        val cleanStored = storedHash.trim()

        // 1. Plain text comparison
        if (cleanStored == cleanP) return true

        // 2. SHA-256 comparison (case-insensitive)
        val sha256 = hashPassword(cleanP)
        if (cleanStored.equals(sha256, ignoreCase = true)) return true

        // 3. MD5 comparison (case-insensitive)
        val md5 = hashMd5(cleanP)
        if (cleanStored.equals(md5, ignoreCase = true)) return true

        return false
    }

    fun validatePasswordRequirements(password: String): List<String> {
        val errors = mutableListOf<String>()
        if (password.length < 6) {
            errors.add("Mínimo 6 caracteres")
        }
        if (!password.any { it.isLetter() }) {
            errors.add("Debe contener al menos una letra")
        }
        if (!password.any { it.isDigit() }) {
            errors.add("Debe contener al menos un número")
        }
        return errors
    }

    fun login(email: String, password: String, rememberMe: Boolean, callback: (Boolean, String?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank()) {
            callback(false, "Por favor ingresa tu correo electrónico.")
            return
        }
        if (cleanPass.isBlank()) {
            callback(false, "Por favor ingresa tu contraseña.")
            return
        }

        viewModelScope.launch {
            var user = repository.getUserByEmail(cleanEmail)

            // Always check Supabase PostgreSQL so users registered on the Web can log in immediately
            val cloudUserDto = SupabaseManager.fetchCloudUser(cleanEmail)
            if (cloudUserDto != null) {
                val cloudUser = cloudUserDto.toUser()
                if (user == null) {
                    val genId = repository.registerUser(cloudUser)
                    user = cloudUser.copy(id = genId)
                } else {
                    // Sync updated password hash or profile data from Supabase
                    if (cloudUser.passwordHash.isNotBlank() && cloudUser.passwordHash != user.passwordHash) {
                        val updatedUser = user.copy(
                            fullName = if (cloudUser.fullName.isNotBlank()) cloudUser.fullName else user.fullName,
                            phone = if (cloudUser.phone.isNotBlank()) cloudUser.phone else user.phone,
                            rut = if (cloudUser.rut.isNotBlank()) cloudUser.rut else user.rut,
                            passwordHash = cloudUser.passwordHash
                        )
                        repository.updateUser(updatedUser)
                        user = updatedUser
                    }
                }
            }

            if (user == null) {
                callback(false, "El correo '$cleanEmail' no se encuentra registrado ni en la App ni en la Web.")
                return@launch
            }

            val isValid = verifyPassword(cleanPass, user.passwordHash)

            if (isValid) {
                _loggedInUser.value = user
                _isAdmin.value = false
                _loggedInBarber.value = null
                setRememberMeCredentials(rememberMe, cleanEmail, cleanPass)
                addNotification(
                    title = "¡Bienvenido ${user.fullName}!",
                    message = "Sesión iniciada correctamente en Big boys (sincronizado con Web & App)."
                )
                callback(true, null)
            } else {
                callback(false, "Contraseña incorrecta. Por favor verifica e intenta nuevamente.")
            }
        }
    }

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        login(email, password, isRememberMeEnabled(), callback)
    }

    fun loginAsAdminWithPin(pin: String, successCallback: (Boolean) -> Unit) {
        if (pin == _adminPin.value) {
            _isAdmin.value = true
            _loggedInBarber.value = null
            _loggedInUser.value = User(
                id = -99,
                fullName = _adminName.value,
                email = "admin@bigboys.cl",
                phone = "+56 9 9999 9999",
                avatarUrl = _adminAvatarUrl.value
            )
            successCallback(true)
        } else {
            successCallback(false)
        }
    }

    // --- Barber Authentication ---
    fun loginAsBarber(barber: Barber, pin: String = "1234", callback: (Boolean, String?) -> Unit) {
        val cleanPin = pin.trim()
        if (cleanPin.isNotBlank() && cleanPin != "1234" && cleanPin != "Barbero2026") {
            callback(false, "PIN de seguridad incorrecto (PIN de staff por defecto: 1234).")
            return
        }
        _loggedInBarber.value = barber
        _loggedInUser.value = null
        _isAdmin.value = false
        addNotification(
            title = "¡Turno de Barbero Activo! 💈",
            message = "Bienvenido ${barber.name}. Ya puedes revisar tu agenda del día y gestionar citas."
        )
        callback(true, null)
    }

    fun loginBarberByCredentials(identifier: String, pinOrPass: String, callback: (Boolean, String?) -> Unit) {
        val cleanId = identifier.trim().lowercase()
        val cleanPass = pinOrPass.trim()
        if (cleanId.isBlank()) {
            callback(false, "Por favor ingresa tu correo de barbero o tu nombre.")
            return
        }

        viewModelScope.launch {
            val barbers = allBarbersFlow.value.ifEmpty { repository.getAllBarbers() }
            val matchedBarber = barbers.firstOrNull { b ->
                val bEmail = b.name.lowercase()
                    .replace(" ", ".")
                    .replace("á","a")
                    .replace("é","e")
                    .replace("í","i")
                    .replace("ó","o")
                    .replace("ú","u") + "@bigboys.cl"
                bEmail == cleanId || b.name.lowercase().contains(cleanId) || cleanId == "barbero@bigboys.cl"
            }

            if (matchedBarber == null) {
                callback(false, "No se encontró ningún barbero registrado con el dato '$cleanId'.")
                return@launch
            }

            if (cleanPass.isNotBlank() && cleanPass != "1234" && cleanPass != "Barbero2026") {
                callback(false, "PIN de barbero incorrecto (PIN estándar: 1234).")
                return@launch
            }

            _loggedInBarber.value = matchedBarber
            _loggedInUser.value = null
            _isAdmin.value = false
            addNotification(
                title = "¡Turno de Barbero Activo! 💈",
                message = "Bienvenido ${matchedBarber.name}. Ya puedes revisar tu agenda del día y gestionar citas."
            )
            callback(true, null)
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        rut: String,
        password: String,
        avatarUrl: String = "",
        callback: (Boolean, String?) -> Unit
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPhone = phone.trim()
        val cleanRut = ValidationUtils.cleanChileanRut(rut)
        val cleanPass = password.trim()
        val cleanAvatar = avatarUrl.trim()

        if (cleanName.isBlank() || cleanName.length < 3) {
            callback(false, "Por favor ingresa tu nombre completo (mínimo 3 caracteres).")
            return
        }
        if (!ValidationUtils.isValidChileanRut(cleanRut)) {
            callback(false, "El RUT ingresado no es válido. Debe tener formato chileno con dígito verificador Módulo 11 (Ej: 19.342.110-3).")
            return
        }
        if (!ValidationUtils.isValidEmail(cleanEmail)) {
            callback(false, "Por favor ingresa un correo electrónico válido (ej: usuario@dominio.cl).")
            return
        }
        if (cleanPhone.isBlank()) {
            callback(false, "Por favor ingresa tu teléfono de contacto.")
            return
        }

        val passErrors = validatePasswordRequirements(cleanPass)
        if (passErrors.isNotEmpty()) {
            callback(false, "La contraseña no cumple con los requisitos: ${passErrors.joinToString(", ")}")
            return
        }

        val formattedRut = ValidationUtils.formatChileanRut(cleanRut)

        viewModelScope.launch {
            val existingByEmail = repository.getUserByEmail(cleanEmail)
            if (existingByEmail != null) {
                callback(false, "El correo '$cleanEmail' ya se encuentra registrado. Ingresa con tu contraseña o utiliza otro correo.")
                return@launch
            }

            val existingByRut = repository.getUserByRut(formattedRut, cleanRut)
            if (existingByRut != null) {
                callback(false, "El RUT '$formattedRut' ya se encuentra registrado con otra cuenta. Inicia sesión con tus credenciales.")
                return@launch
            }

            // Check also in Supabase to ensure cloud uniqueness across platforms
            val cloudCheck = SupabaseManager.checkCloudUserExists(cleanEmail, cleanRut)
            if (cloudCheck.first) {
                callback(false, cloudCheck.second ?: "El correo o RUT ya está registrado en el sistema.")
                return@launch
            }

            val pHash = hashPassword(cleanPass)
            val newUser = User(
                fullName = cleanName,
                email = cleanEmail,
                phone = cleanPhone,
                rut = formattedRut,
                passwordHash = pHash,
                avatarUrl = cleanAvatar
            )
            val generatedId = repository.registerUser(newUser)

            _loggedInUser.value = newUser.copy(id = generatedId)
            _isAdmin.value = false

            // Sync user profile to Supabase PostgreSQL in background with password hash
            viewModelScope.launch {
                SupabaseManager.syncUserToCloud(
                    email = cleanEmail,
                    fullName = cleanName,
                    phone = cleanPhone,
                    rut = formattedRut,
                    passwordHash = pHash
                )
            }

            // Generate 4-digit PIN coupon if within limit of 100 coupons
            val currentCount = repository.getCouponsCount()
            var couponPin = ""
            if (currentCount < 100) {
                var attemptedPin = ""
                var isUnique = false
                while (!isUnique) {
                    attemptedPin = (1000..9999).random().toString()
                    val existingCoupon = repository.getCouponByPin(attemptedPin)
                    if (existingCoupon == null) {
                        isUnique = true
                    }
                }
                couponPin = attemptedPin
                val newCoupon = GiftCoupon(
                    pin = couponPin,
                    userEmail = cleanEmail,
                    userName = cleanName,
                    isRedeemed = false,
                    createdAt = AppFormatters.currentDateTimeString()
                )
                repository.addCoupon(newCoupon)
            }

            // Send real email in background via Gmail SMTP
            if (com.example.data.mail.DirectGmailSender.senderAppPassword.isNotBlank()) {
                val realHtml = """
                    <div style="font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #0d0d11; color: #fdfbf7; padding: 30px; border-radius: 12px; max-width: 600px; margin: auto; border: 1px solid #d4af37;">
                        <div style="text-align: center; margin-bottom: 24px;">
                            <h1 style="color: #d4af37; margin: 0; font-size: 28px; letter-spacing: 2px;">BIG BOYS</h1>
                            <p style="color: #a1a1aa; margin: 4px 0 0 0; font-size: 12px; letter-spacing: 3px;">BARBERSHOP · CHILE</p>
                        </div>
                        <h2 style="color: #fdfbf7; font-size: 20px;">¡Hola $cleanName!</h2>
                        <p style="color: #d1d5db; font-size: 15px; line-height: 1.6;">
                            Te damos la más cordial bienvenida al club exclusivo de <strong>Big boys Barberías</strong>. Tu cuenta ha sido activada exitosamente con tu correo <em>$cleanEmail</em>.
                        </p>
                        ${if (couponPin.isNotEmpty()) """
                        <div style="background: linear-gradient(135deg, #27272a, #18181b); border: 2px dashed #d4af37; border-radius: 10px; padding: 20px; text-align: center; margin: 24px 0;">
                            <span style="color: #e6c875; font-size: 13px; font-weight: bold; text-transform: uppercase;">🎁 Regalo de Bienvenida Exclusivo</span>
                            <h3 style="color: #ffffff; margin: 8px 0; font-size: 18px;">1x Perfilado de Cejas Profesional GRATIS</h3>
                            <div style="background-color: #0d0d11; display: inline-block; padding: 8px 24px; border-radius: 8px; border: 1px solid #d4af37; margin-top: 10px;">
                                <span style="font-size: 24px; font-weight: bold; color: #d4af37; letter-spacing: 4px;">PIN: $couponPin</span>
                            </div>
                            <p style="color: #a1a1aa; font-size: 11px; margin-top: 8px;">Muestra este código PIN en recepción o a tu barbero al momento de tu primer servicio.</p>
                        </div>
                        """ else ""}
                        <p style="color: #9ca3af; font-size: 13px; line-height: 1.5;">
                            Ya puedes ingresar a la app para seleccionar tu sucursal favorita (Providencia, Las Condes, Santiago Centro, Viña del Mar, Talagante), elegir a tu barbero de confianza y reservar tu horario.
                        </p>
                        <hr style="border: none; border-top: 1px solid #27272a; margin: 24px 0;" />
                        <p style="color: #6b7280; font-size: 12px; text-align: center;">
                            Big boys Barberías Chile 🇨🇱 · Santiago, Chile
                        </p>
                    </div>
                """.trimIndent()

                viewModelScope.launch {
                    com.example.data.mail.DirectGmailSender.sendRealEmail(
                        recipientEmail = cleanEmail,
                        subject = "✂️ ¡Bienvenido a Big boys Barberías! Tu Cupón de Bienvenida 🎁",
                        htmlBody = realHtml
                    )
                }
            }

            addNotification(
                title = "Bienvenido $cleanName 🎁",
                message = if (couponPin.isNotEmpty()) "¡Registrado con éxito! Cupón de cejas gratis generado con el PIN: $couponPin" else "Te hemos enviado un correo de bienvenida a $cleanEmail"
            )

            callback(true, null)
        }
    }

    fun logout() {
        _loggedInUser.value = null
        _isAdmin.value = false
        _loggedInBarber.value = null
    }

    fun logoutBarber() {
        _loggedInBarber.value = null
    }

    fun cancelAppointmentByBarber(
        appointment: Appointment,
        barberName: String,
        locationName: String,
        emergencyReason: String,
        callback: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = appointment.copy(status = "Cancelada")
            repository.updateAppointment(updated)

            // Update in Supabase in background so slot is released immediately
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, "Cancelada")
            }

            val clientEmail = if (appointment.customerEmail.isBlank()) "contacto@caballero.cl" else appointment.customerEmail
            val clientName = appointment.customerName
            val formattedPrepay = AppFormatters.formatClp(appointment.prepayAmount)
            val reasonText = if (emergencyReason.isNotBlank()) emergencyReason else "Emergencia médica / fuerza mayor imprevista del barbero"

            val emergencyHtml = """
                <div style="font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #0d0d11; color: #fdfbf7; padding: 30px; border-radius: 12px; max-width: 600px; margin: auto; border: 1px solid #d4af37;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h1 style="color: #d4af37; margin: 0; font-size: 28px; letter-spacing: 2px;">BIG BOYS</h1>
                        <p style="color: #a1a1aa; margin: 4px 0 0 0; font-size: 12px; letter-spacing: 3px;">BARBERSHOP · CHILE</p>
                    </div>
                    <div style="background-color: #7f1d1d; border-left: 4px solid #ef4444; padding: 14px 18px; border-radius: 8px; margin-bottom: 20px;">
                        <h3 style="color: #fecaca; margin: 0; font-size: 16px;">🚨 SUSPENSIÓN DE HORA POR EMERGENCIA DE BARBERO</h3>
                    </div>
                    <p style="color: #d1d5db; font-size: 15px; line-height: 1.6;">
                        Estimado/a <strong>$clientName</strong>:
                    </p>
                    <p style="color: #d1d5db; font-size: 14px; line-height: 1.6;">
                        Le informamos que lamentablemente su barbero <strong>$barberName</strong> ha debido activar el protocolo de emergencia en <strong>$locationName</strong> debido a:
                    </p>
                    <div style="background-color: #27272a; padding: 12px 16px; border-radius: 8px; border-left: 3px solid #d4af37; margin: 12px 0;">
                        <p style="color: #fef08a; margin: 0; font-size: 14px; font-weight: bold;">Motivo informado: $reasonText</p>
                    </div>
                    <p style="color: #d1d5db; font-size: 14px; line-height: 1.6;">
                        Por esta razón su reserva para el <strong>${appointment.dateTime}</strong> ha sido suspendida.
                    </p>
                    <div style="background-color: #18181b; border: 1px solid #d4af37; border-radius: 10px; padding: 20px; margin: 20px 0;">
                        <h3 style="color: #d4af37; margin-top: 0; font-size: 16px;">💳 DEVOLUCIÓN DEL 100% DE SU ABONO ($$formattedPrepay CLP)</h3>
                        <p style="color: #d1d5db; font-size: 13px; line-height: 1.5; margin-bottom: 12px;">
                            Para efectuar la transferencia bancaria inmediata de su abono por <strong>$$formattedPrepay CLP</strong>, responda directamente a este correo indicando sus datos bancarios:
                        </p>
                        <table style="width: 100%; font-size: 13px; color: #fdfbf7; border-collapse: collapse; background-color: #0d0d11; padding: 12px; border-radius: 6px;">
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Nombre Titular:</td><td style="padding: 6px 10px; color: #e4e4e7;">$clientName</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Banco:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Banco de preferencia)</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Tipo de Cuenta:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Cuenta RUT / Vista / Corriente)</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• N° de Cuenta:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Número de cuenta)</td></tr>
                        </table>
                    </div>
                    <p style="color: #9ca3af; font-size: 12px; line-height: 1.5;">
                        Agradecemos profundamente su comprensión y le invitamos a reagendar cuando lo estime conveniente con un descuento especial en su próxima visita.
                    </p>
                    <hr style="border: none; border-top: 1px solid #27272a; margin: 20px 0;" />
                    <p style="color: #6b7280; font-size: 11px; text-align: center; margin: 0;">
                        Barbero: $barberName · Big boys Barberías Chile 🇨🇱
                    </p>
                </div>
            """.trimIndent()

            if (com.example.data.mail.DirectGmailSender.senderAppPassword.isNotBlank()) {
                viewModelScope.launch {
                    com.example.data.mail.DirectGmailSender.sendRealEmail(
                        recipientEmail = clientEmail,
                        subject = "🚨 Emergencia de Barbero: Cita suspendida y devolución de abono - Big Boys",
                        htmlBody = emergencyHtml
                    )
                }
            }

            addNotification(
                title = "🚨 Cita Cancelada por Emergencia",
                message = "Cita de las ${appointment.dateTime.substringAfter(" ")} con ${appointment.customerName} cancelada por: $reasonText. Se notificó al cliente para devolución de su abono."
            )
            callback(true)
        }
    }

    fun cancelAllTodayAppointmentsByBarber(
        barberId: Int,
        date: String,
        barberName: String,
        locationName: String,
        reason: String,
        callback: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val allAppts = repository.getAllAppointments()
            val toCancel = allAppts.filter { 
                it.barberId == barberId && 
                AppFormatters.isSameDay(it.dateTime, date) && 
                (it.status == "Pendiente" || it.status == "Confirmada")
            }

            toCancel.forEach { appt ->
                cancelAppointmentByBarber(appt, barberName, locationName, reason)
            }

            addNotification(
                title = "⚠️ Turno Suspendido por Emergencia",
                message = "Se cancelaron ${toCancel.size} cita(s) de hoy por fuerza mayor. Clientes notificados vía correo para devolución de abonos."
            )
            callback(toCancel.size)
        }
    }

    fun completeAppointmentByBarber(appointment: Appointment) {
        viewModelScope.launch {
            val updated = appointment.copy(status = "Completada")
            repository.updateAppointment(updated)
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, "Completada")
            }
            addNotification(
                title = "¡Cita Completada! ✨",
                message = "Servicio con ${appointment.customerName} finalizado exitosamente."
            )
        }
    }

    /**
     * Marks an appointment as No-Show (Client absence).
     * The $3.000 CLP deposit is retained for lost barber time slot.
     */
    fun markAppointmentNoShow(appointment: Appointment, callback: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                status = "No-Show",
                depositRetained = true
            )
            repository.updateAppointment(updated)
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, "No-Show")
            }
            addNotification(
                title = "🚫 Cita Marcada como No-Show",
                message = "Cliente ${appointment.customerName} no se presentó. Abono de $3.000 retenido según política de agenda."
            )
            callback(true)
        }
    }

    /**
     * Checks if the client can reschedule an appointment using their current $3.000 deposit.
     * Rule: Must be at least 24 hours before service AND maximum 2 reschedules allowed.
     */
    fun canRescheduleAppointment(appointment: Appointment): Pair<Boolean, String?> {
        if (appointment.status != "Confirmada" && appointment.status != "Pendiente") {
            return Pair(false, "Solo se pueden reagendar citas activas (Confirmadas).")
        }
        val hoursRemaining = AppFormatters.hoursUntilAppointment(appointment.dateTime)
        if (hoursRemaining < 24.0) {
            val remainingText = if (hoursRemaining <= 0) "la cita ya venció" else "quedan solo ${String.format(java.util.Locale.US, "%.1f", hoursRemaining)} hrs"
            return Pair(false, "Solo se puede reagendar con al menos 24 horas de anticipación para liberar la agenda ($remainingText).")
        }
        if (appointment.rescheduleCount >= 2) {
            return Pair(false, "Has alcanzado el límite máximo de 2 reagendamientos permitidos con tu abono inicial de $3.000.")
        }
        return Pair(true, null)
    }

    /**
     * Reschedules an appointment to a new date and hour preserving the $3.000 deposit.
     * Increments rescheduleCount (max 2).
     */
    fun rescheduleAppointment(
        appointment: Appointment,
        newDate: String,
        newHour: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val check = canRescheduleAppointment(appointment)
        if (!check.first) {
            callback(false, check.second)
            return
        }

        val newDateTime = "$newDate $newHour"
        viewModelScope.launch {
            // Check if slot is taken in all appointments
            val currentAppts = allAppointmentsFlow.value
            val isTaken = currentAppts.any {
                it.barberId == appointment.barberId &&
                it.dateTime.trim() == newDateTime.trim() &&
                it.status != "Cancelada" &&
                it.id != appointment.id
            }
            if (isTaken) {
                callback(false, "El horario $newHour para el día $newDate ya se encuentra reservado. Selecciona otro horario.")
                return@launch
            }

            val updatedCount = appointment.rescheduleCount + 1
            val updated = appointment.copy(
                dateTime = newDateTime,
                rescheduleCount = updatedCount
            )
            repository.updateAppointment(updated)
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, updated.status)
            }

            addNotification(
                title = "🗓️ Cita Reagendada con Éxito",
                message = "Tu atención fue movida para el $newDate a las $newHour hrs. Abono de $3.000 CLP conservado (Reagendamiento $updatedCount de 2)."
            )
            callback(true, "Cita reagendada exitosamente para el $newDate a las $newHour hrs. Abono conservado ($updatedCount de 2 reagendamientos utilizados).")
        }
    }

    /**
     * Client cancels appointment.
     * Rule: If cancelled with >= 24h notice -> Keeps deposit / credit balance.
     * If cancelled within 24h -> Loses the $3.000 deposit.
     */
    fun cancelAppointmentByClient(
        appointment: Appointment,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val hoursRemaining = AppFormatters.hoursUntilAppointment(appointment.dateTime)
            val isWithin24h = hoursRemaining < 24.0

            val newStatus = if (isWithin24h) "Cancelada (Abono Retenido)" else "Cancelada"
            val updated = appointment.copy(
                status = newStatus,
                depositRetained = isWithin24h
            )
            repository.updateAppointment(updated)
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, newStatus)
            }

            val message = if (isWithin24h) {
                "Cita cancelada con menos de 24 horas de anticipación. Según la política de la barbería, el abono de $3.000 CLP ha sido retenido por bloqueo tardío de agenda."
            } else {
                "Cita cancelada con más de 24 horas de anticipación. Tu cupo fue liberado a tiempo y conservas tu saldo a favor de $3.000 CLP para tu próxima atención."
            }

            addNotification(
                title = if (isWithin24h) "⚠️ Cita Cancelada (Abono Retenido)" else "✅ Cita Cancelada a Tiempo",
                message = message
            )
            callback(true, message)
        }
    }

    // --- Booking Setups ---
    fun selectLocation(location: Location) {
        if (_bookingLocation.value?.id != location.id) {
            _bookingLocation.value = location
            _bookingBarber.value = null // reset barber when location changes
        } else {
            _bookingLocation.value = location
        }
    }

    fun selectBarber(barber: Barber) {
        _bookingBarber.value = barber
        locationsFlow.value.find { it.id == barber.locationId }?.let { loc ->
            _bookingLocation.value = loc
        }
        refreshAppointmentsFromCloud()
    }

    fun selectDate(date: String) {
        _bookingDate.value = date
        refreshAppointmentsFromCloud()
    }

    fun refreshAppointmentsFromCloud() {
        viewModelScope.launch {
            SupabaseManager.triggerCloudRefresh()
        }
    }

    fun selectHour(hour: String) {
        _bookingHour.value = hour
    }

    fun selectService(serviceName: String, price: Double) {
        _bookingService.value = serviceName
        _bookingPrice.value = price
    }

    fun updateGuestDetails(name: String, phone: String, email: String) {
        _guestName.value = name
        _guestPhone.value = phone
        _guestEmail.value = email
    }

    fun resetBookingDraft() {
        _bookingLocation.value = null
        _bookingBarber.value = null
        _bookingDate.value = ""
        _bookingHour.value = ""
        _bookingService.value = ""
        _bookingPrice.value = 0.0
        _guestName.value = ""
        _guestPhone.value = ""
        _guestEmail.value = ""
    }

    // --- Make Appointment Bookings ---
    fun confirmAndBook(onCompleted: () -> Unit) {
        viewModelScope.launch {
            val location = _bookingLocation.value ?: return@launch
            val barber = _bookingBarber.value ?: return@launch
            val date = _bookingDate.value
            val hour = _bookingHour.value
            val service = _bookingService.value
            val price = _bookingPrice.value
            val user = _loggedInUser.value

            val isGuestMode = user == null
            val clientName = if (user != null) user.fullName else _guestName.value
            val clientPhone = if (user != null) user.phone else _guestPhone.value
            val clientEmail = if (user != null) user.email else _guestEmail.value

            val prepay = _prepaymentAmount.value // typical virtual CLP reservation prepay

            val newAppointment = Appointment(
                locationId = location.id,
                barberId = barber.id,
                customerName = clientName,
                customerPhone = clientPhone,
                customerEmail = clientEmail,
                dateTime = "$date $hour",
                serviceName = service,
                servicePrice = price,
                prepayAmount = prepay,
                status = "Confirmada", // auto confirms post fake prepay
                isGuest = isGuestMode,
                userId = user?.id
            )

            val generatedId = repository.insertAppointment(newAppointment)
            val finalAppointment = newAppointment.copy(id = generatedId)

            // Publish to Supabase PostgreSQL for real-time slot locking across all devices & web
            val cloudPublished = SupabaseManager.publishAppointmentToCloud(finalAppointment)

            if (cloudPublished) {
                addNotification(
                    title = "¡Cita Bloqueada en la Web y App! 💈",
                    message = "Tu reserva para el $date a las $hour con ${barber.name} en ${location.name} quedó registrada en la base de datos central de Supabase."
                )
            } else {
                addNotification(
                    title = "¡Cita Registrada!",
                    message = "Tu reserva para el $date a las $hour con ${barber.name} en ${location.name} ha sido agendada con éxito."
                )
            }

            // Send real booking confirmation email via Gmail SMTP
            if (clientEmail.isBlank() || !clientEmail.contains("@")) {
                addNotification(
                    title = "ℹ️ Correo no registrado",
                    message = "No se envió comprobante digital porque la reserva no tiene un correo electrónico válido."
                )
            } else if (com.example.data.mail.DirectGmailSender.senderAppPassword.isBlank()) {
                addNotification(
                    title = "⚠️ Correo no enviado (Falta Configurar)",
                    message = "Para que los correos automáticos se envíen a $clientEmail, el Administrador debe ingresar la 'Contraseña de Aplicación' de Gmail en la pestaña '✉️ Correo Gmail Real' del Panel Admin."
                )
            } else {
                val bookingHtml = """
                    <div style="font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #0d0d11; color: #fdfbf7; padding: 30px; border-radius: 12px; max-width: 600px; margin: auto; border: 1px solid #d4af37;">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <h1 style="color: #d4af37; margin: 0; font-size: 26px;">BIG BOYS</h1>
                            <p style="color: #a1a1aa; margin: 2px 0 0 0; font-size: 11px; letter-spacing: 2px;">COMPROBANTE OFICIAL DE RESERVA</p>
                        </div>
                        <h3 style="color: #fdfbf7; margin-bottom: 8px;">¡Tu reserva ha sido confirmada, $clientName! 💈</h3>
                        <p style="color: #d1d5db; font-size: 14px; line-height: 1.5;">
                            Tu atención ha quedado agendada en nuestro sistema. A continuación encontrarás el detalle de tu cita:
                        </p>
                        <div style="background-color: #18181b; border: 1px solid #27272a; border-radius: 8px; padding: 18px; margin: 20px 0;">
                            <table style="width: 100%; border-collapse: collapse; font-size: 14px;">
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">📅 Fecha y Hora:</td>
                                    <td style="color: #fdfbf7; font-weight: bold; text-align: right; padding: 6px 0;">$date · $hour hrs</td>
                                </tr>
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">📍 Sucursal:</td>
                                    <td style="color: #fdfbf7; font-weight: bold; text-align: right; padding: 6px 0;">${location.name} (${location.address})</td>
                                </tr>
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">✂️ Barbero:</td>
                                    <td style="color: #fdfbf7; font-weight: bold; text-align: right; padding: 6px 0;">${barber.name}</td>
                                </tr>
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">💈 Servicio:</td>
                                    <td style="color: #fdfbf7; font-weight: bold; text-align: right; padding: 6px 0;">$service</td>
                                </tr>
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">💵 Total Servicio:</td>
                                    <td style="color: #d4af37; font-weight: bold; text-align: right; padding: 6px 0;">${AppFormatters.formatClpPrice(price)}</td>
                                </tr>
                                <tr>
                                    <td style="color: #9ca3af; padding: 6px 0;">🔒 Abono Anticipado:</td>
                                    <td style="color: #10b981; font-weight: bold; text-align: right; padding: 6px 0;">${AppFormatters.formatClpPrice(prepay)} (Garantizado)</td>
                                </tr>
                            </table>
                        </div>
                        <p style="color: #9ca3af; font-size: 12px; line-height: 1.5;">
                            Te recomendamos llegar 5 minutos antes de tu hora. ¡Te esperamos para darte el mejor corte y estilo de Santiago!
                        </p>
                        <hr style="border: none; border-top: 1px solid #27272a; margin: 20px 0;" />
                        <p style="color: #6b7280; font-size: 11px; text-align: center; margin: 0;">
                            Big boys Barberías Chile 🇨🇱 · Santiago, Chile
                        </p>
                    </div>
                """.trimIndent()

                viewModelScope.launch {
                    val emailResult = com.example.data.mail.DirectGmailSender.sendRealEmail(
                        recipientEmail = clientEmail,
                        subject = "✂️ Big boys - Comprobante de Reserva Confirmada ($date $hour)",
                        htmlBody = bookingHtml
                    )
                    if (emailResult.isSuccess) {
                        addNotification(
                            title = "📧 Correo Enviado con Éxito",
                            message = "Se envió el comprobante oficial a $clientEmail"
                        )
                    } else {
                        val errMsg = emailResult.exceptionOrNull()?.message ?: "Error desconocido"
                        addNotification(
                            title = "❌ Fallo al enviar correo",
                            message = "No se pudo entregar a $clientEmail: $errMsg"
                        )
                    }
                }
            }

            // Reset booking screen inputs
            _bookingDate.value = ""
            _bookingHour.value = ""
            _guestName.value = ""
            _guestPhone.value = ""
            _guestEmail.value = ""

            onCompleted()
        }
    }

    // --- Notification Helpers ---
    fun addNotification(title: String, message: String) {
        val list = _notifications.value.toMutableList()
        list.add(0, NotificationItem(title = title, message = message))
        _notifications.value = list
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // --- Administrative Actions ---
    fun updateAppointmentStatusAdmin(appointmentId: Int, newStatus: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, newStatus)
            addNotification(
                title = "Estado Actualizado (Admin)",
                message = "Cita #$appointmentId ha sido marcada como '$newStatus'"
            )
        }
    }

    fun updateAppointmentAdmin(appointment: Appointment) {
        viewModelScope.launch {
            repository.updateAppointment(appointment)
            addNotification(
                title = "Agenda Modificada ✨",
                message = "Se guardaron correctamente los cambios de la cita para ${appointment.customerName}."
            )
        }
    }

    fun addBarberAdmin(
        name: String,
        locationId: Int,
        specialty: String,
        bio: String = "",
        avatarUrl: String = "",
        initialHours: String = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
        initialRating: Float = 5.0f,
        onCompleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val defaultAvatar = if (avatarUrl.isNotBlank()) avatarUrl else "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300"
            val newBarber = Barber(
                locationId = locationId,
                name = name.trim(),
                specialty = specialty.trim().ifBlank { "Barbero Estilista" },
                rating = initialRating,
                avatarUrl = defaultAvatar,
                availableHours = initialHours.ifBlank { "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00" },
                bio = bio.trim().ifBlank { "Barbero profesional de Big Boys Barberías Chile." },
                active = true
            )
            repository.addBarber(newBarber)
            val locName = repository.getAllLocations().find { it.id == locationId }?.name?.replace("Big Boys - ", "") ?: "Sucursal"
            addNotification(
                title = "Nuevo Barbero Agregado 💈",
                message = "${newBarber.name} ha sido incorporado a la sucursal $locName."
            )
            onCompleted()
        }
    }

    fun deleteBarberAdmin(barber: Barber, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteBarber(barber)
            addNotification(
                title = "Barbero Eliminado ✂️",
                message = "${barber.name} fue removido del equipo de barberos."
            )
            onCompleted()
        }
    }

    // --- Rate Barber ---
    fun rateBarber(barberId: Int, userRating: Float) {
        viewModelScope.launch {
            val barber = repository.getBarberById(barberId)
            if (barber != null) {
                val newRating = ((barber.rating * 4f + userRating) / 5f).let { val rounded = Math.round(it * 10) / 10f; if (rounded > 5.0f) 5.0f else rounded }
                repository.updateBarber(barber.copy(rating = newRating))
                addNotification(
                    title = "Calificación Registrada ⭐",
                    message = "Has calificado a ${barber.name} con ${userRating.toInt()} estrellas."
                )
            }
        }
    }

    fun cancelAppointmentAdmin(appointment: Appointment, barberName: String, locationName: String) {
        viewModelScope.launch {
            val updated = appointment.copy(status = "Cancelada")
            repository.updateAppointment(updated)

            // Update in Supabase in background so the slot is released on the web immediately
            viewModelScope.launch {
                SupabaseManager.updateAppointmentStatusInCloud(updated, "Cancelada")
            }

            val clientEmail = if (appointment.customerEmail.isBlank()) "contacto@caballero.cl" else appointment.customerEmail
            val clientName = appointment.customerName

            val subject = "⚠️ Suspensión de Turno: Disculpas de Club Big Boys Chile y Devolución de Abono"
            val formattedPrepay = AppFormatters.formatClp(appointment.prepayAmount)
            val realCancellationHtml = """
                <div style="font-family: 'Helvetica Neue', Arial, sans-serif; background-color: #0d0d11; color: #fdfbf7; padding: 30px; border-radius: 12px; max-width: 600px; margin: auto; border: 1px solid #d4af37;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h1 style="color: #d4af37; margin: 0; font-size: 28px; letter-spacing: 2px;">BIG BOYS</h1>
                        <p style="color: #a1a1aa; margin: 4px 0 0 0; font-size: 12px; letter-spacing: 3px;">BARBERSHOP · CHILE</p>
                    </div>
                    <div style="background-color: #7f1d1d; border-left: 4px solid #ef4444; padding: 14px 18px; border-radius: 8px; margin-bottom: 20px;">
                        <h3 style="color: #fecaca; margin: 0; font-size: 16px;">⚠️ Suspensión de Reserva de Atención</h3>
                    </div>
                    <p style="color: #d1d5db; font-size: 15px; line-height: 1.6;">
                        Estimado/a <strong>$clientName</strong>:
                    </p>
                    <p style="color: #d1d5db; font-size: 14px; line-height: 1.6;">
                        Le escribimos para ofrecerle nuestras más sinceras disculpas. Por motivos de fuerza mayor (ausencia de barbero o ajuste operacional imprevisto), hemos tenido que suspender su reserva agendada para el <strong>${appointment.dateTime}</strong> con el barbero <strong>$barberName</strong> en la sucursal de <strong>$locationName</strong>.
                    </p>
                    <div style="background-color: #18181b; border: 1px solid #d4af37; border-radius: 10px; padding: 20px; margin: 20px 0;">
                        <h3 style="color: #d4af37; margin-top: 0; font-size: 16px;">💳 DEVOLUCIÓN DEL DINERO ABONADO ($${formattedPrepay} CLP)</h3>
                        <p style="color: #d1d5db; font-size: 13px; line-height: 1.5; margin-bottom: 12px;">
                            Para efectuar la transferencia bancaria inmediata de su abono por <strong>$$formattedPrepay CLP</strong>, por favor responda directamente a este correo o vía WhatsApp indicando sus datos de cuenta:
                        </p>
                        <table style="width: 100%; font-size: 13px; color: #fdfbf7; border-collapse: collapse; background-color: #0d0d11; padding: 12px; border-radius: 6px;">
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Nombre Completo / Titular:</td><td style="padding: 6px 10px; color: #e4e4e7;">$clientName</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• RUT:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Ingresar RUT)</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Banco:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Banco de Chile / Estado / Santander / BCI / etc.)</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• Tipo de Cuenta:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Cuenta RUT / Vista / Corriente)</td></tr>
                            <tr><td style="padding: 6px 10px; color: #9ca3af;">• N° de Cuenta:</td><td style="padding: 6px 10px; color: #e4e4e7;">(Número de cuenta)</td></tr>
                        </table>
                    </div>
                    <p style="color: #9ca3af; font-size: 12px; line-height: 1.5;">
                        Nos comunicaremos a la brevedad para asistirle en lo que requiera. Muchas gracias por su comprensión.
                    </p>
                    <hr style="border: none; border-top: 1px solid #27272a; margin: 20px 0;" />
                    <p style="color: #6b7280; font-size: 11px; text-align: center; margin: 0;">
                        Equipo de Administración · Big boys Barberías Chile 🇨🇱
                    </p>
                </div>
            """.trimIndent()

            // Send real email automatically via Gmail SMTP if credentials configured
            if (com.example.data.mail.DirectGmailSender.senderAppPassword.isNotBlank()) {
                viewModelScope.launch {
                    com.example.data.mail.DirectGmailSender.sendRealEmail(
                        recipientEmail = clientEmail,
                        subject = subject,
                        htmlBody = realCancellationHtml
                    )
                }
            }

            addNotification(
                title = "Cita Cancelada y Notificación Enviada 📧",
                message = "Se anuló la cita de $clientName. Notificación y solicitud de datos bancarios enviada a $clientEmail."
            )
        }
    }

    // --- Activar / Desactivar Disponibilidad de Barbero ---
    fun toggleBarberAvailability(barber: Barber, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val defaultHours = "10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00,19:00"
            val newHours = if (barber.availableHours.isBlank() || barber.availableHours == "NO_DISPONIBLE") {
                defaultHours
            } else {
                "NO_DISPONIBLE"
            }
            val updated = barber.copy(availableHours = newHours)
            repository.updateBarber(updated)
            
            val statusMsg = if (newHours == "NO_DISPONIBLE") "DESACTIVADO (No disponible hoy)" else "ACTIVADO (Horarios habilitados)"
            addNotification(
                title = "Disponibilidad Actualizada 💈",
                message = "${barber.name} ahora está: $statusMsg"
            )
            onCompleted()
        }
    }

    // --- Update Existing Barber ---
    fun updateBarberAdmin(barber: Barber, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateBarber(barber)
            addNotification(
                title = "Barbero Modificado 💈",
                message = "El perfil de '${barber.name}' se actualizó correctamente."
            )
            onCompleted()
        }
    }

    // --- Walk-in Appointment Creation (Manual) ---
    fun registerManualWalkIn(
        locationId: Int,
        barberId: Int,
        clientName: String,
        clientPhone: String,
        clientEmail: String,
        dateTimeString: String,
        serviceName: String,
        servicePrice: Double,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            val appointment = Appointment(
                locationId = locationId,
                barberId = barberId,
                customerName = clientName,
                customerPhone = clientPhone,
                customerEmail = clientEmail,
                dateTime = dateTimeString,
                serviceName = serviceName,
                servicePrice = servicePrice,
                prepayAmount = 0.0, // walk-in clients pay directly at local, no prepaid deposit
                status = "Completada", // Instant delivery
                isGuest = true,
                userId = null
            )
            repository.insertAppointment(appointment)
            addNotification(
                title = "Cita Sin Reserva Registrada",
                message = "Servicio de $clientName cargado al barbero asignado con éxito."
            )
            onCompleted()
        }
    }

    // --- CSV & Excel Exporters / Importers ---
    fun generateExportMovementCsv(period: String, appointments: List<Appointment>, barbers: List<Barber>, locations: List<Location>): String {
        val sb = java.lang.StringBuilder()
        // Using Tab-Separated Values (TSV) which Excel reads natively as clean columns
        sb.append("ID_Cita\tFecha_Corte\tSucursal\tBarbero\tCliente\tCelular\tServicio\tTotal_Cobrado\tPrepago_Online\tEstado\n")
        
        for (app in appointments) {
            val bName = barbers.find { it.id == app.barberId }?.name ?: "Barbero #${app.barberId}"
            val lName = locations.find { it.id == app.locationId }?.name?.replace("Big Boys - ", "") ?: "Chile"
            sb.append("${app.id}\t${app.dateTime}\t$lName\t$bName\t${app.customerName}\t${app.customerPhone}\t${app.serviceName}\t${app.servicePrice}\t${app.prepayAmount}\t${app.status}\n")
        }
        return sb.toString()
    }

    fun generateUsersCsv(users: List<User>): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID\tNombre_Completo\tCorreo_Electronico\tCelular_Contacto\n")
        for (u in users) {
            sb.append("${u.id}\t${u.fullName}\t${u.email}\t${u.phone}\n")
        }
        return sb.toString()
    }

    fun importUsersFromCsv(csvText: String, onCompleted: (Int) -> Unit) {
        viewModelScope.launch {
            var count = 0
            val lines = csvText.lines()
            for (line in lines) {
                if (line.isBlank() || line.startsWith("ID", ignoreCase = true) || line.startsWith("ID_Cita", ignoreCase = true)) continue
                // Auto-detect delimiter: Excel copy-paste uses Tab (\t), CSV uses comma (,)
                val delimiter = if (line.contains("\t")) "\t" else ","
                val parts = line.split(delimiter)
                if (parts.size >= 3) {
                    val name = parts.getOrNull(1)?.replace("\"", "")?.trim() ?: ""
                    val email = parts.getOrNull(2)?.replace("\"", "")?.trim() ?: ""
                    val phone = parts.getOrNull(3)?.replace("\"", "")?.trim() ?: ""
                    if (name.isNotBlank() && email.isNotBlank()) {
                        val existing = repository.getUserByEmail(email)
                        if (existing == null) {
                            repository.registerUser(User(fullName = name, email = email, phone = phone))
                            count++
                        } else {
                            repository.registerUser(existing.copy(fullName = name, phone = phone))
                            count++
                        }
                    }
                }
            }
            if (count > 0) {
                addNotification(
                    title = "Planilla Excel Sincronizada 📊",
                    message = "Se importaron y actualizaron $count cuentas de clientes desde la planilla de Excel."
                )
            }
            onCompleted(count)
        }
    }

    fun redeemCoupon(pin: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val coupon = repository.getCouponByPin(pin)
            if (coupon == null) {
                callback(false, "El PIN ingresado no corresponde a ningún cupón activo.")
                return@launch
            }
            if (coupon.isRedeemed) {
                callback(false, "El cupón con este PIN (${coupon.pin}) ya fue canjeado por ${coupon.userName}.")
                return@launch
            }
            val updated = coupon.copy(isRedeemed = true)
            repository.updateCoupon(updated)
            addNotification(
                title = "Regalo Entregado 🎁",
                message = "Se canjeó exitosamente el perfilado de cejas para: ${coupon.userName}."
            )
            callback(true, "¡Cupón canjeado con éxito para ${coupon.userName}!")
        }
    }

    fun registerManualCoupon(userName: String, userEmail: String, customPin: String?, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentCount = repository.getCouponsCount()
            if (currentCount >= 100) {
                callback(false, "Límite alcanzado: Ya se han generado los 100 cupones de regalo permitidos.")
                return@launch
            }
            val email = userEmail.trim()
            val existing = repository.getCouponByUserEmail(email)
            if (existing != null) {
                callback(false, "Este cliente ya tiene un cupón asignado (PIN: ${existing.pin}).")
                return@launch
            }

            val finalPin = if (!customPin.isNullOrBlank()) {
                if (customPin.length != 4 || customPin.any { !it.isDigit() }) {
                    callback(false, "El PIN manual debe ser exactamente de 4 dígitos numéricos.")
                    return@launch
                }
                val checkPin = repository.getCouponByPin(customPin)
                if (checkPin != null) {
                    callback(false, "El PIN $customPin ya está en uso por otro cupón.")
                    return@launch
                }
                customPin
            } else {
                var attemptedPin = ""
                var isUnique = false
                while (!isUnique) {
                    attemptedPin = (1000..9999).random().toString()
                    val existingCoupon = repository.getCouponByPin(attemptedPin)
                    if (existingCoupon == null) {
                        isUnique = true
                    }
                }
                attemptedPin
            }

            val newCoupon = GiftCoupon(
                pin = finalPin,
                userEmail = email,
                userName = userName,
                isRedeemed = false,
                createdAt = AppFormatters.currentDateTimeString()
            )
            repository.addCoupon(newCoupon)
            addNotification(
                title = "Cupón Creado Manualmente 🏷️",
                message = "Cupón creado para $userName con el PIN: $finalPin."
            )
            callback(true, "Cupón creado exitosamente con el PIN: $finalPin")
        }
    }

    // --- Profile Customization Actions ---
    fun updateAdminProfile(
        name: String,
        currentPin: String,
        newPin: String?,
        avatarUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (currentPin != _adminPin.value) {
            callback(false, "El PIN actual de administrador es incorrecto.")
            return
        }
        if (name.isBlank()) {
            callback(false, "El nombre de administrador no puede estar vacío.")
            return
        }
        if (!newPin.isNullOrBlank()) {
            if (newPin.length < 4 || newPin.any { !it.isDigit() }) {
                callback(false, "El nuevo PIN debe contener al menos 4 dígitos numéricos.")
                return
            }
        }

        _adminName.value = name.trim()
        if (!newPin.isNullOrBlank()) {
            _adminPin.value = newPin.trim()
        }
        if (avatarUrl.isNotBlank()) {
            _adminAvatarUrl.value = avatarUrl.trim()
        }

        _loggedInUser.value = _loggedInUser.value?.copy(
            fullName = _adminName.value,
            avatarUrl = _adminAvatarUrl.value
        )
        addNotification(
            title = "Perfil de Administrador Actualizado 🛡️",
            message = "Tus cambios de perfil y credenciales han sido guardados."
        )
        callback(true, "Perfil de administrador actualizado correctamente.")
    }

    fun updateUserProfile(
        name: String,
        phone: String,
        currentPassword: String,
        newPassword: String?,
        avatarUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val user = _loggedInUser.value
        if (user == null || user.id <= 0) {
            callback(false, "No hay una sesión activa de usuario.")
            return
        }

        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank()) {
            callback(false, "Por favor ingresa tu nombre.")
            return
        }
        if (cleanPhone.isBlank()) {
            callback(false, "Por favor ingresa tu teléfono.")
            return
        }

        viewModelScope.launch {
            if (user.passwordHash.isNotBlank()) {
                val enteredHash = hashPassword(currentPassword.trim())
                if (enteredHash != user.passwordHash) {
                    callback(false, "La contraseña actual ingresada es incorrecta.")
                    return@launch
                }
            }

            var updatedPasswordHash = user.passwordHash
            if (!newPassword.isNullOrBlank()) {
                val passErrors = validatePasswordRequirements(newPassword.trim())
                if (passErrors.isNotEmpty()) {
                    callback(false, "La nueva contraseña no cumple los requisitos: ${passErrors.joinToString(", ")}")
                    return@launch
                }
                updatedPasswordHash = hashPassword(newPassword.trim())
            }

            val updatedUser = user.copy(
                fullName = cleanName,
                phone = cleanPhone,
                passwordHash = updatedPasswordHash,
                avatarUrl = avatarUrl.trim()
            )

            repository.updateUser(updatedUser)
            _loggedInUser.value = updatedUser
            addNotification(
                title = "Perfil Actualizado 👤",
                message = "Tu nombre, teléfono y foto de perfil han sido actualizados."
            )
            callback(true, "Perfil actualizado con éxito.")
        }
    }

    // --- Review Actions ---
    fun submitAppointmentReview(
        appointment: Appointment,
        rating: Int,
        comment: String,
        tags: List<String>,
        callback: (Boolean, String) -> Unit
    ) {
        if (rating < 1 || rating > 5) {
            callback(false, "Por favor selecciona una calificación entre 1 y 5 estrellas.")
            return
        }
        if (comment.isBlank()) {
            callback(false, "Por favor cuéntanos brevemente tu experiencia.")
            return
        }

        viewModelScope.launch {
            val user = _loggedInUser.value
            val authorName = user?.fullName ?: appointment.customerName.ifBlank { "Cliente Big boys" }
            val authorAvatar = user?.avatarUrl ?: ""
            val dateStr = AppFormatters.currentDateString()

            val review = Review(
                appointmentId = appointment.id,
                locationId = appointment.locationId,
                barberId = appointment.barberId,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                rating = rating,
                comment = comment.trim(),
                serviceOrSubject = appointment.serviceName,
                date = dateStr,
                tags = tags.joinToString(", "),
                reviewType = "SERVICE"
            )

            repository.addReview(review)
            addNotification(
                title = "¡Reseña Publicada! ⭐",
                message = "Muchas gracias por tu valoración para ${appointment.serviceName}."
            )
            callback(true, "¡Gracias! Tu reseña ha sido publicada con éxito.")
        }
    }

    fun submitGeneralReview(
        locationId: Int?,
        barberId: Int?,
        serviceOrSubject: String,
        rating: Int,
        comment: String,
        tags: List<String>,
        reviewType: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (rating < 1 || rating > 5) {
            callback(false, "Por favor selecciona una calificación entre 1 y 5 estrellas.")
            return
        }
        if (comment.isBlank()) {
            callback(false, "Por favor escribe tu reseña.")
            return
        }

        viewModelScope.launch {
            val user = _loggedInUser.value
            val authorName = user?.fullName ?: "Cliente Verificado"
            val authorAvatar = user?.avatarUrl ?: ""
            val dateStr = AppFormatters.currentDateString()

            val review = Review(
                locationId = locationId,
                barberId = barberId,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                rating = rating,
                comment = comment.trim(),
                serviceOrSubject = serviceOrSubject.ifBlank { "Experiencia Big boys" },
                date = dateStr,
                tags = tags.joinToString(", "),
                reviewType = reviewType
            )

            repository.addReview(review)
            addNotification(
                title = "¡Opinión Registrada! ⭐",
                message = "Tu reseña sobre $serviceOrSubject fue guardada."
            )
            callback(true, "¡Reseña agregada con éxito!")
        }
    }

    fun deleteReviewAdmin(review: Review, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            repository.deleteReview(review)
            addNotification(
                title = "Reseña Eliminada 🗑️",
                message = "La reseña de '${review.authorName}' (${review.rating}⭐) fue eliminada por administración."
            )
            callback(true, "Reseña eliminada correctamente.")
        }
    }

    // --- Gmail SMTP Configuration & Real Email Dispatch ---
    fun updateGmailConfiguration(email: String, appPassword: String, callback: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim()
        val cleanPass = appPassword.replace(" ", "").trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            callback(false, "Por favor ingresa una dirección de correo Gmail válida.")
            return
        }

        // Persist permanently in SharedPreferences so it never resets when closing the app
        com.example.data.mail.DirectGmailSender.saveCredentials(getApplication<Application>(), cleanEmail, cleanPass)
        _gmailSenderAddress.value = cleanEmail
        _gmailAppPassword.value = cleanPass

        callback(true, "Configuración guardada permanentemente en el dispositivo.")
    }

    fun sendTestRealEmail(targetEmail: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _emailSendingStatus.value = "Enviando correo de prueba a $targetEmail..."
            val html = """
                <div style="font-family: Arial, sans-serif; background-color: #0d0d11; color: #fdfbf7; padding: 24px; border-radius: 12px; max-width: 600px; margin: auto; border: 1px solid #d4af37;">
                    <h1 style="color: #d4af37; margin-bottom: 8px;">💈 Big boys Barbershop - Chile</h1>
                    <p style="font-size: 16px; color: #e4e4e7;">¡Prueba de conexión exitosa!</p>
                    <p style="color: #a1a1aa; line-height: 1.6;">Este es un correo real enviado directamente desde tu aplicación Big boys utilizando tu cuenta de Gmail particular sin necesidad de APIs externas.</p>
                    <div style="background-color: #17171d; border-left: 4px solid #d4af37; padding: 12px; margin: 16px 0; border-radius: 6px;">
                        <strong style="color: #d4af37;">Remitente configurado:</strong> ${_gmailSenderAddress.value}<br/>
                        <strong style="color: #d4af37;">Hora de envío:</strong> ${AppFormatters.currentDateTimeString()}
                    </div>
                    <p style="font-size: 12px; color: #71717a; text-align: center; margin-top: 24px;">Big boys Barbería © 2026 - Santiago, Chile</p>
                </div>
            """.trimIndent()

            val result = com.example.data.mail.DirectGmailSender.sendRealEmail(
                recipientEmail = targetEmail,
                subject = "💈 Big boys Barbershop - Prueba de Conexión de Correo Real",
                htmlBody = html
            )

            if (result.isSuccess) {
                _emailSendingStatus.value = null
                addNotification("Correo Real Enviado 📧", "Se envió un correo de prueba exitosamente a $targetEmail")
                callback(true, "¡Correo de prueba enviado con éxito a $targetEmail! Revisa tu bandeja de entrada o Spam.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido al enviar correo"
                _emailSendingStatus.value = null
                callback(false, "Fallo al enviar correo: $errorMsg")
            }
        }
    }

    /**
     * Checks all appointments in the local database. If an appointment's scheduled
     * date and time has passed and it was not completed or cancelled, its status is
     * automatically updated to "Caducada" and synchronized with Supabase.
     */
    fun checkAndExpirePastAppointments() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allAppointments = repository.getAllAppointments()
                allAppointments.forEach { appt ->
                    if ((appt.status == "Confirmada" || appt.status == "Pendiente") && AppFormatters.isAppointmentPast(appt.dateTime)) {
                        val expired = appt.copy(status = "Caducada")
                        repository.updateAppointment(expired)
                        SupabaseManager.updateAppointmentStatusInCloud(expired, "Caducada")
                        Log.d("BarberiaViewModel", "Cita ID ${appt.id} ha caducado automáticamente (fecha/hora transcurrida)")
                    }
                }
            } catch (e: Exception) {
                Log.w("BarberiaViewModel", "Error al verificar citas caducadas: ${e.message}")
            }
        }
    }
}
