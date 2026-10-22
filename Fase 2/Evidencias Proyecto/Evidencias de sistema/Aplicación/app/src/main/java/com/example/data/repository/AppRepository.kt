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
        val OFFICIAL_LOCATIONS = listOf(
            Location(1, "Big Boys - Talagante", "Boulevard del centro, local 3, Talagante", "+56 2 2489 1234"),
            Location(2, "Big Boys - Peñaflor (21 de Mayo)", "21 de mayo 4566, Peñaflor", "+56 2 2631 5678"),
            Location(3, "Big Boys - Peñaflor (Alcalde Luis Araya Cereceda)", "Alcalde Luis Araya Cereceda 4387, Peñaflor", "+56 2 2912 3456")
        )

        val OFFICIAL_BARBERS = listOf(
            // Sucursal 1: Big Boys - Talagante (Boulevard del centro, local 3)
            Barber(
                id = 1,
                locationId = 1,
                name = "Mateo Silva",
                specialty = "Corte Moderno & Fade",
                rating = 4.9f,
                avatarUrl = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Especialista en degradados milimétricos y texturizados urbanos. Mateo cuenta con 6 años de experiencia y es reconocido por su precisión quirúrgica con la patillera."
            ),
            Barber(
                id = 2,
                locationId = 1,
                name = "Carlos Mendoza",
                specialty = "Perfilado de Barba Premium",
                rating = 4.8f,
                avatarUrl = "https://images.unsplash.com/photo-1621605815971-fbc98d665033?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,13:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Maestro barbero enfocado en la restauración facial masculina. Ofrece un perfilado con vaporización y aceites esenciales que limpian y revitalizan cada poro."
            ),
            Barber(
                id = 3,
                locationId = 1,
                name = "Lucas Parra",
                specialty = "Estilo Clásico & Navaja",
                rating = 4.7f,
                avatarUrl = "https://images.unsplash.com/photo-1517832606299-7ae9b720a186?w=400&auto=format&fit=crop&q=80",
                availableHours = "10:00,11:00,12:00,13:00,15:00,16:00,17:00,18:00,19:00",
                bio = "Devoto del afeitado tradicional de toalla caliente. Lucas mantiene vivas las técnicas clásicas europeas de navaja libre y pompadours de vieja escuela."
            ),
            Barber(
                id = 4,
                locationId = 1,
                name = "Diego Alarcón",
                specialty = "Color Metálico & Diseño",
                rating = 4.9f,
                avatarUrl = "https://images.unsplash.com/photo-1622286342621-4bd786c2447c?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Infiltrado de la cultura urbana y las pasarelas. Diego domina coloraciones extremas (platinados, grises metálicos) y grecas tribales personalizadas."
            ),

            // Sucursal 2: Big Boys - Peñaflor (21 de Mayo) (21 de mayo 4566)
            Barber(
                id = 5,
                locationId = 2,
                name = "Sebastián Reyes",
                specialty = "Corte Clásico Militar",
                rating = 4.8f,
                avatarUrl = "https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00",
                bio = "Ex-estilista de academias navales. Sebastián diseña cortes militares, buzzcuts, y flat-tops ultra simétricos ideales para ejecutivos y profesionales."
            ),
            Barber(
                id = 6,
                locationId = 2,
                name = "Felipe Soto",
                specialty = "Afeitado con Toalla Caliente",
                rating = 4.9f,
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
                availableHours = "10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Especialista en masajes capilares relajantes y afeitado de cabeza completo a navaja. Brinda una experiencia de relajación zen absoluta."
            ),
            Barber(
                id = 7,
                locationId = 2,
                name = "Matías Rojas",
                specialty = "Fade Urbano & Diseños",
                rating = 4.6f,
                avatarUrl = "https://images.unsplash.com/photo-1605462863863-10d9e47e15ee?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Inspirado por el trap y los deportes americanos. Especialista en high-fades, drop-fades y líneas de diseño geométrico de alta complejidad técnica."
            ),
            Barber(
                id = 8,
                locationId = 2,
                name = "Misael Castro",
                specialty = "Estilo Ejecutivo",
                rating = 4.8f,
                avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,13:00,15:00,16:00,17:00,18:00",
                bio = "Su enfoque es la elegancia sobria. Especialista en cortes con tijera para cabellos largos y medios, manteniendo un estilo formal pero con personalidad."
            ),

            // Sucursal 3: Big Boys - Peñaflor (Alcalde Luis Araya Cereceda) (Alcalde Luis Araya Cereceda 4387)
            Barber(
                id = 9,
                locationId = 3,
                name = "Joaquín Guzmán",
                specialty = "Corte & Barba VIP",
                rating = 5.0f,
                avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Condecorado como barbero del año en certámenes nacionales. Su servicio VIP incluye diagnóstico de visagismo capilar y asesoramiento estético completo."
            ),
            Barber(
                id = 10,
                locationId = 3,
                name = "Andrés Muñoz",
                specialty = "Arreglo de Barba Royale",
                rating = 4.9f,
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00",
                bio = "Especialista en barbas vikingas y barbas de candado. Experto en alaciado de vello facial, hidratación profunda y moldeado con bálsamos finos."
            ),
            Barber(
                id = 11,
                locationId = 3,
                name = "Mauricio Ortiz",
                specialty = "Visagismo & Estilismo",
                rating = 4.8f,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
                availableHours = "10:00,11:00,12:00,13:00,14:00,16:00,17:00,18:00",
                bio = "Creativo apasionado del modelado y la estructura ósea craneal. Diseña tu peinado en base a la geometría de tu rostro para resaltar tus mejores rasgos."
            ),
            Barber(
                id = 12,
                locationId = 3,
                name = "Tomás Lagos",
                specialty = "Corte Joven & Tendencia",
                rating = 4.7f,
                avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
                availableHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00",
                bio = "Experto en las corrientes internacionales de K-Pop y Mullets modernos. Tomás es el favorito del público joven por su carisma y asesoramiento fresco."
            )
        )
    }

    /**
     * Mantiene las sucursales y los 12 barberos oficiales bloqueados como fuente canónica de verdad.
     * Nunca sobreescribe ni elimina los barberos oficiales con datos remotos de prueba.
     */
    suspend fun syncBarbersAndLocationsFromCloud() {
        try {
            // Aseguramos que siempre existan los 12 barberos y 3 sucursales oficiales
            OFFICIAL_LOCATIONS.forEach { appDao.insertLocation(it) }
            OFFICIAL_BARBERS.forEach { appDao.insertBarber(it) }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error syncing barbers/locations: ${e.message}")
        }
    }

    // Prepopulación y aseguramiento incondicional de los 12 barberos oficiales
    suspend fun checkAndPrepopulate() {
        // 1. Enforce official locations
        OFFICIAL_LOCATIONS.forEach { loc ->
            appDao.insertLocation(loc)
        }

        // 2. Enforce official 12 barbers and purge any unauthorized/outdated records
        val currentBarbers = appDao.getAllBarbers()
        currentBarbers.forEach { barber ->
            if (barber.id > 12 || OFFICIAL_BARBERS.none { it.id == barber.id }) {
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
            appDao.insertAppointment(Appointment(locationId = 2, barberId = 5, customerName = "Cristóbal Solís", customerPhone = "+56933445566", dateTime = "$d1 15:30", serviceName = "Corte Clásico Militar", servicePrice = 13500.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 3, barberId = 9, customerName = "Gonzalo Silva", customerPhone = "+56944556677", dateTime = "$d1 16:00", serviceName = "Ritual de Barba & Barbería", servicePrice = 22000.0, prepayAmount = 8000.0, status = "Completada"))

            // Two days ago
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d2 = sdf.format(cal.time)
            appDao.insertAppointment(Appointment(locationId = 1, barberId = 3, customerName = "Alexis Varas", customerPhone = "+56955667788", dateTime = "$d2 12:00", serviceName = "Corte Clásico Tradicional", servicePrice = 14000.0, prepayAmount = 5000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 2, barberId = 6, customerName = "Nicolás Ibáñez", customerPhone = "+56966778899", dateTime = "$d2 14:00", serviceName = "Perfilado de Barba", servicePrice = 10000.0, prepayAmount = 4000.0, status = "Completada"))
            appDao.insertAppointment(Appointment(locationId = 3, barberId = 10, customerName = "Andrés Bello", customerPhone = "+56977889900", dateTime = "$d2 17:00", serviceName = "Corte & Ritual de Afeitado VIP", servicePrice = 30000.0, prepayAmount = 10000.0, status = "Completada"))
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

