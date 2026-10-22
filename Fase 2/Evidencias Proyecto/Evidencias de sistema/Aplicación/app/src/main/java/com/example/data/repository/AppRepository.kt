package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import com.example.ui.util.ValidationUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(private val appDao: AppDao) {

    // Users
    val allUsersFlow: Flow<List<User>> = appDao.getAllUsersFlow()
    suspend fun getUserByEmail(email: String) = appDao.getUserByEmail(email)
    suspend fun getUserByRut(rut: String, cleanRut: String = "") = appDao.getUserByRut(rut, cleanRut.ifBlank { rut.replace(".", "").replace("-", "").replace(" ", "").uppercase() })
    suspend fun getUserById(id: Int) = appDao.getUserById(id)
    suspend fun registerUser(user: User): Int = appDao.insertUser(user).toInt()
    suspend fun updateUser(user: User) = appDao.updateUser(user)

    // Locations
    val allLocationsFlow: Flow<List<Location>> = appDao.getAllLocationsFlow()
    suspend fun getAllLocations() = appDao.getAllLocations()
    suspend fun saveLocation(location: Location) = appDao.insertLocation(location)

    // Barbers
    val allBarbersFlow: Flow<List<Barber>> = appDao.getAllBarbersFlow()
    suspend fun getAllBarbers() = appDao.getAllBarbers()
    fun getBarbersByLocation(locationId: Int): Flow<List<Barber>> = appDao.getBarbersByLocationFlow(locationId)
    suspend fun getBarberById(id: Int) = appDao.getBarberById(id)
    suspend fun addBarber(barber: Barber): Int = appDao.insertBarber(barber).toInt()
    suspend fun updateBarber(barber: Barber) = appDao.updateBarber(barber)
    suspend fun deleteBarber(barber: Barber) = appDao.deleteBarber(barber)
    suspend fun deleteBarberById(id: Int) = appDao.deleteBarberById(id)

    // Appointments
    val allAppointmentsFlow: Flow<List<Appointment>> = appDao.getAllAppointmentsFlow()
    suspend fun getAllAppointments(): List<Appointment> = appDao.getAllAppointments()
    fun getAppointmentsByUser(userId: Int): Flow<List<Appointment>> = appDao.getAppointmentsByUserFlow(userId)
    fun getAppointmentsByLocation(locationId: Int): Flow<List<Appointment>> = appDao.getAppointmentsByLocationFlow(locationId)
    fun getAppointmentsByBarber(barberId: Int): Flow<List<Appointment>> = appDao.getAppointmentsByBarberFlow(barberId)
    suspend fun insertAppointment(appointment: Appointment): Int = appDao.insertAppointment(appointment).toInt()
    suspend fun updateAppointment(appointment: Appointment) = appDao.updateAppointment(appointment)
    suspend fun updateAppointmentStatus(id: Int, status: String) = appDao.updateAppointmentStatus(id, status)

    // Products
    val allProductsFlow: Flow<List<Product>> = appDao.getAllProductsFlow()
    suspend fun getAllProducts() = appDao.getAllProducts()
    suspend fun addProduct(product: Product): Int = appDao.insertProduct(product).toInt()
    suspend fun updateProduct(product: Product) = appDao.updateProduct(product)
    suspend fun updateProductStock(id: Int, newStock: Int) = appDao.updateProductStock(id, newStock)

    // Supplies
    val allSuppliesFlow: Flow<List<Supply>> = appDao.getAllSuppliesFlow()
    suspend fun getAllSupplies() = appDao.getAllSupplies()
    fun getSuppliesByLocation(locationId: Int): Flow<List<Supply>> = appDao.getSuppliesByLocationFlow(locationId)
    suspend fun addSupply(supply: Supply): Int = appDao.insertSupply(supply).toInt()
    suspend fun updateSupply(supply: Supply) = appDao.updateSupply(supply)

    // Gift Coupons
    val allCouponsFlow: Flow<List<GiftCoupon>> = appDao.getAllCouponsFlow()
    suspend fun getAllCoupons() = appDao.getAllCoupons()
    suspend fun getCouponByPin(pin: String) = appDao.getCouponByPin(pin)
    suspend fun getCouponByUserEmail(email: String) = appDao.getCouponByUserEmail(email)
    suspend fun getCouponsCount() = appDao.getCouponsCount()
    suspend fun addCoupon(coupon: GiftCoupon): Int = appDao.insertCoupon(coupon).toInt()
    suspend fun updateCoupon(coupon: GiftCoupon) = appDao.updateCoupon(coupon)

    // Reviews
    val allReviewsFlow: Flow<List<Review>> = appDao.getAllReviewsFlow()
    suspend fun getAllReviews() = appDao.getAllReviews()
    fun getReviewsByLocation(locationId: Int): Flow<List<Review>> = appDao.getReviewsByLocationFlow(locationId)
    fun getReviewsByBarber(barberId: Int): Flow<List<Review>> = appDao.getReviewsByBarberFlow(barberId)
    suspend fun addReview(review: Review): Int {
        val id = appDao.insertReview(review).toInt()
        review.appointmentId?.let { appDao.markAppointmentReviewed(it) }
        return id
    }
    suspend fun deleteReview(review: Review) = appDao.deleteReview(review)
    suspend fun deleteReviewById(id: Int) = appDao.deleteReviewById(id)
    suspend fun markAppointmentReviewed(appointmentId: Int) = appDao.markAppointmentReviewed(appointmentId)

    companion object {
        // Sucursales reales (mismos ids que la tabla sucursal de Supabase)
        val OFFICIAL_LOCATIONS = listOf(
            Location(1, "Peñaflor – Sucursal 21 de Mayo", "21 de Mayo 4566, Peñaflor", ""),
            Location(2, "Peñaflor – Sucursal Alcalde Luis Araya Cereceda", "Alcalde Luis Araya Cereceda 4387, Peñaflor", ""),
            Location(3, "Talagante", "Boulevard del Centro, local 3, Talagante", "")
        )

        // Barberos de prueba (demo) de la tabla barbero, hasta tener la nómina real
        val OFFICIAL_BARBERS = listOf(
            Barber(id = 1, locationId = 1, name = "Mateo \"Fade Master\" Silva", specialty = "Degradados de precisión, low y mid fade",
                rating = 5.0f, avatarUrl = "", availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00", bio = ""),
            Barber(id = 2, locationId = 1, name = "Esteban \"El Maestro\" Rojas", specialty = "Corte clásico, barba spa y toalla caliente",
                rating = 5.0f, avatarUrl = "", availableHours = "10:00,11:00,12:00,13:00,15:00,16:00,17:00,18:00,19:00", bio = ""),
            Barber(id = 3, locationId = 2, name = "Cristian \"Black Gold\" Castro", specialty = "Freestyle, tintura y perfilado con cera",
                rating = 5.0f, avatarUrl = "", availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00", bio = ""),
            Barber(id = 4, locationId = 3, name = "Diego \"La Navaja\" Morales", specialty = "Afeitado tradicional y barboterapia",
                rating = 5.0f, avatarUrl = "", availableHours = "10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00", bio = "")
        )
    }

    /**
     * Carga sucursales y barberos desde Supabase (api_sucursales / api_barberos).
     * Sin conexión se mantienen los datos oficiales locales.
     */
    suspend fun syncBarbersAndLocationsFromCloud() {
        try {
            val cloudLocations = com.example.data.supabase.SupabaseManager.fetchLocations()
            val cloudBarbers = com.example.data.supabase.SupabaseManager.fetchBarbers()
            if (cloudLocations.isNotEmpty()) {
                cloudLocations.forEach { appDao.insertLocation(it.toLocation()) }
            }
            if (cloudBarbers.isNotEmpty()) {
                appDao.getAllBarbers().forEach { local ->
                    if (cloudBarbers.none { it.id == local.id }) appDao.deleteBarberById(local.id)
                }
                cloudBarbers.forEach { appDao.insertBarber(it.toBarber()) }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error syncing barbers/locations: ${e.message}")
        }
    }

    // Prepopulación de las sucursales y los barberos oficiales
    suspend fun checkAndPrepopulate() {
        // 1. Enforce official locations
        OFFICIAL_LOCATIONS.forEach { loc ->
            appDao.insertLocation(loc)
        }

        // 2. Enforce official barbers and purge any unauthorized/outdated records
        val currentBarbers = appDao.getAllBarbers()
        currentBarbers.forEach { barber ->
            if (OFFICIAL_BARBERS.none { it.id == barber.id }) {
                appDao.deleteBarberById(barber.id)
            }
        }
        OFFICIAL_BARBERS.forEach { barber ->
            appDao.insertBarber(barber)
        }

        // 3. Purge supplies
        appDao.deleteAllSupplies()

        // 4. Insert Products if empty
        if (appDao.getAllProducts().isEmpty()) {
            appDao.insertProduct(Product(name = "Pomada Matte Black Gold", description = "Fijación extra fuerte con acabado mate ultra seco. Ideal para cabellos rebeldes de corte pompadour o militar. Se retira fácilmente con agua y no deja residuos en el cuero cabelludo.", category = "Fijación", price = 12000.0, stock = 24, imageUrl = "https://images.unsplash.com/photo-1608248597481-496100c80836?w=400&auto=format&fit=crop&q=80"))
            appDao.insertProduct(Product(name = "Aceite de Eucalipto Big Boys", description = "Suaviza la barba dura y humecta los folículos pilosos. Contiene aceites esenciales de almendra dulce y jojoba con un toque refrescante de eucalipto. Hidratación premium de uso diario.", category = "Cuidado de Barba", price = 14500.0, stock = 18, imageUrl = "https://images.unsplash.com/photo-1617897903246-719242758050?w=400&auto=format&fit=crop&q=80"))
            appDao.insertProduct(Product(name = "Champú de Carbón Activado", description = "Desintoxica y limpia el cuero cabelludo de impurezas habituales de la ciudad. Su fórmula purificante con mentol estimula la circulación y aporta fuerza regenerativa al cabello fino.", category = "Cabello", price = 10990.0, stock = 30, imageUrl = "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=400&auto=format&fit=crop&q=80"))
            appDao.insertProduct(Product(name = "Aftershave de Sándalo Madera", description = "Loción astringente ideal para rehidratar la piel tras afeitados de navaja tradicionales. Alivia inmediatamente irritaciones, previene micro-infecciones y deja un distinguido aroma amaderado.", category = "Cuidado de Barba", price = 9500.0, stock = 12, imageUrl = "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=400&auto=format&fit=crop&q=80"))
            appDao.insertProduct(Product(name = "Cera Brillo Clásico Retro", description = "Aporta fijación media-alta con efecto húmedo retro para peinados con gomina impecables. Diseñada para lucir peinados elegantes con raya al lado con excelente flexibilidad y tacto suave.", category = "Fijación", price = 11000.0, stock = 15, imageUrl = "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=400&auto=format&fit=crop&q=80"))
        }
        // 4. Populate some Completed Appointments to generate immediate reports if empty
        if (appDao.getAllAppointments().isEmpty()) {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val cal = Calendar.getInstance()

            // Yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d1 = sdf.format(cal.time)
            appDao.insertAppointment(Appointment(locationId = 1, barberId = 1, customerName = "Juan Pérez", customerPhone = "+56911223344", dateTime = "$d1 10:00", serviceName = "Corte de Pelo Degrado", servicePrice = 15000.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 1, barberId = 2, customerName = "Diego Muñoz", customerPhone = "+56922334455", dateTime = "$d1 11:00", serviceName = "Arreglo de Barba Royale", servicePrice = 12000.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 2, barberId = 3, customerName = "Cristóbal Solís", customerPhone = "+56933445566", dateTime = "$d1 15:30", serviceName = "Corte Clásico Militar", servicePrice = 13500.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 3, barberId = 4, customerName = "Gonzalo Silva", customerPhone = "+56944556677", dateTime = "$d1 16:00", serviceName = "Ritual de Barba & Barbería", servicePrice = 22000.0, prepayAmount = 8000.0, status = "Completada"))

            // Two days ago
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d2 = sdf.format(cal.time)
            appDao.insertAppointment(Appointment(locationId = 1, barberId = 1, customerName = "Alexis Varas", customerPhone = "+56955667788", dateTime = "$d2 12:00", serviceName = "Corte Clásico Tradicional", servicePrice = 14000.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 2, barberId = 3, customerName = "Nicolás Ibáñez", customerPhone = "+56966778899", dateTime = "$d2 14:00", serviceName = "Perfilado de Barba", servicePrice = 10000.0, prepayAmount = 4000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 3, barberId = 4, customerName = "Andrés Bello", customerPhone = "+56977889900", dateTime = "$d2 17:00", serviceName = "Corte & Ritual de Afeitado VIP", servicePrice = 30000.0, prepayAmount = 10000.0, status = "Completada"))
        }

        // 6. Prepopulate Reviews for Branches and Services if empty
        if (appDao.getAllReviews().isEmpty()) {
            appDao.insertReview(Review(
                locationId = 1,
                authorName = "Ignacio Valdés",
                rating = 5,
                comment = "Excelente local en Talagante. La atención con café de cortesía y la música ambiente hacen que la espera valga totalmente la pena.",
                serviceOrSubject = "Sucursal Talagante",
                date = "24-08-2026",
                tags = "Ambiente VIP, Café de Grano, Puntualidad",
                reviewType = "BRANCH"
            ))
            appDao.insertReview(Review(
                locationId = 1,
                barberId = 1,
                authorName = "Juan Pérez",
                rating = 5,
                comment = "Mateo Silva es un maestro con la navaja y el fade. Quedó exactamente como lo pedí, degradado limpio y muy pulcro.",
                serviceOrSubject = "Corte Moderno & Fade",
                date = "25-08-2026",
                tags = "Degradado Perfecto, Precisión, Trato Amable",
                reviewType = "SERVICE"
            ))
            appDao.insertReview(Review(
                locationId = 2,
                authorName = "Rodrigo Fuentes",
                rating = 5,
                comment = "La sucursal de Peñaflor 1 es muy amplia y moderna. Sillones súper cómodos y todo muy higienizado.",
                serviceOrSubject = "Sucursal Peñaflor 1",
                date = "23-08-2026",
                tags = "Higiene Impecable, Sillones Cómodos, Rapidez",
                reviewType = "BRANCH"
            ))
            appDao.insertReview(Review(
                locationId = 2,
                barberId = 6,
                authorName = "Nicolás Ibáñez",
                rating = 5,
                comment = "El afeitado con toalla caliente de Felipe es otro nivel de relajación. Salí renovado.",
                serviceOrSubject = "Afeitado con Toalla Caliente",
                date = "24-08-2026",
                tags = "Relajación Total, Toalla Caliente, Profesionalismo",
                reviewType = "SERVICE"
            ))
            appDao.insertReview(Review(
                locationId = 3,
                authorName = "Claudio Santander",
                rating = 5,
                comment = "El local de Peñaflor 2 tiene una vibra increíble. Te reciben con buena onda y atienden puntual a la hora reservada.",
                serviceOrSubject = "Sucursal Peñaflor 2",
                date = "22-08-2026",
                tags = "Puntualidad, Buena Música, Estacionamiento",
                reviewType = "BRANCH"
            ))
            appDao.insertReview(Review(
                locationId = 3,
                barberId = 9,
                authorName = "Gonzalo Silva",
                rating = 5,
                comment = "Joaquín Guzmán es un genio del visagismo. Me aconsejó el corte según la forma de mi cara y el cambio fue notable.",
                serviceOrSubject = "Corte & Barba VIP",
                date = "25-08-2026",
                tags = "Visagismo, Asesoría Personalizada, Calidad Superior",
                reviewType = "SERVICE"
            ))
        }

        // --- Test Users Initialization ---
        // Ensures test user accounts are permanently registered for testing
        val testUser1 = appDao.getUserByEmail("cliente@bigboys.cl")
        val testUserId: Int = if (testUser1 == null) {
            appDao.insertUser(User(
                fullName = "Usuario de Pruebas",
                email = "cliente@bigboys.cl",
                phone = "+56 9 8765 4321",
                rut = "19.342.110-3",
                passwordHash = ValidationUtils.hashPassword("Bigboys2026"),
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80"
            )).toInt()
        } else {
            testUser1.id
        }

        val testUser2 = appDao.getUserByEmail("test@bigboys.cl")
        if (testUser2 == null) {
            appDao.insertUser(User(
                fullName = "Tester Big Boys",
                email = "test@bigboys.cl",
                phone = "+56 9 1234 5678",
                rut = "19.342.110-3",
                passwordHash = ValidationUtils.hashPassword("123"),
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80"
            ))
        } else {
            appDao.updateUser(testUser2.copy(
                rut = "19.342.110-3",
                passwordHash = ValidationUtils.hashPassword("123")
            ))
        }

        // Welcome Gift Coupon with PIN 7788
        if (appDao.getCouponByPin("7788") == null) {
            appDao.insertCoupon(GiftCoupon(
                pin = "7788",
                userEmail = "test@bigboys.cl",
                userName = "Tester Big Boys",
                isRedeemed = false,
                createdAt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            ))
        }

        // --- Today's Appointments for Barber Dashboard & Customer Testing ---
        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val existingTodayAppts = appDao.getAllAppointments().filter { it.dateTime.startsWith(todayStr) }
        if (existingTodayAppts.isEmpty()) {
            // Appointments for Mateo Silva (Barber 1, Talagante)
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 1,
                customerName = "Usuario de Pruebas",
                customerPhone = "+56 9 8765 4321",
                customerEmail = "cliente@bigboys.cl",
                dateTime = "$todayStr 10:00",
                serviceName = "Corte Moderno & Fade",
                servicePrice = 15000.0,
                prepayAmount = 5000.0,
                status = "Confirmada",
                userId = testUserId
            ))
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 1,
                customerName = "Rodrigo Morales",
                customerPhone = "+56 9 4433 2211",
                customerEmail = "rodrigo.m@gmail.com",
                dateTime = "$todayStr 11:30",
                serviceName = "Perfilado de Barba Premium",
                servicePrice = 12000.0,
                prepayAmount = 5000.0,
                status = "Confirmada"
            ))
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 1,
                customerName = "Felipe Carrasco",
                customerPhone = "+56 9 5566 7788",
                customerEmail = "felipe.c@gmail.com",
                dateTime = "$todayStr 14:00",
                serviceName = "Corte y Lavado VIP",
                servicePrice = 18000.0,
                prepayAmount = 5000.0,
                status = "Pendiente"
            ))
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 1,
                customerName = "Ignacio Valdés",
                customerPhone = "+56 9 6677 8899",
                customerEmail = "ignacio.v@gmail.com",
                dateTime = "$todayStr 16:30",
                serviceName = "Ritual de Barba a Navaja",
                servicePrice = 14000.0,
                prepayAmount = 5000.0,
                status = "Confirmada"
            ))
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 1,
                customerName = "Cristóbal Solís",
                customerPhone = "+56 9 3344 5566",
                customerEmail = "cristobal.s@gmail.com",
                dateTime = "$todayStr 09:00",
                serviceName = "Corte Clásico Militar",
                servicePrice = 13500.0,
                prepayAmount = 5000.0,
                status = "Completada"
            ))

            // Appointments for Carlos Mendoza (Barber 2, Talagante)
            appDao.insertAppointment(Appointment(
                locationId = 1,
                barberId = 2,
                customerName = "Gabriel Osorio",
                customerPhone = "+56 9 7788 9900",
                customerEmail = "gabriel.o@gmail.com",
                dateTime = "$todayStr 10:30",
                serviceName = "Perfilado de Barba Premium",
                servicePrice = 12000.0,
                prepayAmount = 5000.0,
                status = "Confirmada"
            ))

            // Appointments for Joaquín Guzmán (Barber 9, Peñaflor 2)
            appDao.insertAppointment(Appointment(
                locationId = 3,
                barberId = 9,
                customerName = "Matías Concha",
                customerPhone = "+56 9 8899 0011",
                customerEmail = "matias.c@gmail.com",
                dateTime = "$todayStr 15:00",
                serviceName = "Corte & Barba VIP",
                servicePrice = 25000.0,
                prepayAmount = 8000.0,
                status = "Confirmada"
            ))
        }
    }
}

