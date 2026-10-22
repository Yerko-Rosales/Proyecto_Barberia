package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Users ---
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE (rut != '' AND rut = :rut) OR (rut != '' AND REPLACE(REPLACE(REPLACE(rut, '.', ''), '-', ''), ' ', '') = :cleanRut) LIMIT 1")
    suspend fun getUserByRut(rut: String, cleanRut: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    // --- Locations ---
    @Query("SELECT * FROM locations")
    fun getAllLocationsFlow(): Flow<List<Location>>

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<Location>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Location)

    // --- Barbers ---
    @Query("SELECT * FROM barbers ORDER BY id ASC")
    fun getAllBarbersFlow(): Flow<List<Barber>>

    @Query("SELECT * FROM barbers")
    suspend fun getAllBarbers(): List<Barber>

    @Query("SELECT * FROM barbers WHERE locationId = :locationId")
    fun getBarbersByLocationFlow(locationId: Int): Flow<List<Barber>>

    @Query("SELECT * FROM barbers WHERE id = :id LIMIT 1")
    suspend fun getBarberById(id: Int): Barber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarber(barber: Barber): Long

    @Update
    suspend fun updateBarber(barber: Barber)

    @Delete
    suspend fun deleteBarber(barber: Barber)

    @Query("DELETE FROM barbers WHERE id = :id")
    suspend fun deleteBarberById(id: Int)

    @Query("DELETE FROM barbers")
    suspend fun deleteAllBarbers()

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY id DESC")
    fun getAllAppointmentsFlow(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointments(): List<Appointment>

    @Query("SELECT * FROM appointments WHERE userId = :userId ORDER BY id DESC")
    fun getAppointmentsByUserFlow(userId: Int): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE locationId = :locationId ORDER BY id DESC")
    fun getAppointmentsByLocationFlow(locationId: Int): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE barberId = :barberId ORDER BY id DESC")
    fun getAppointmentsByBarberFlow(barberId: Int): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE barberId = :barberId")
    suspend fun getAppointmentsByBarber(barberId: Int): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateAppointmentStatus(id: Int, status: String)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET stock = :newStock WHERE id = :id")
    suspend fun updateProductStock(id: Int, newStock: Int)

    // --- Supplies ---
    @Query("SELECT * FROM supplies ORDER BY name ASC")
    fun getAllSuppliesFlow(): Flow<List<Supply>>

    @Query("SELECT * FROM supplies")
    suspend fun getAllSupplies(): List<Supply>

    @Query("SELECT * FROM supplies WHERE locationId = :locationId ORDER BY name ASC")
    fun getSuppliesByLocationFlow(locationId: Int): Flow<List<Supply>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupply(supply: Supply): Long

    @Update
    suspend fun updateSupply(supply: Supply)

    @Query("DELETE FROM supplies")
    suspend fun deleteAllSupplies()

    // --- Gift Coupons ---
    @Query("SELECT * FROM gift_coupons ORDER BY id DESC")
    fun getAllCouponsFlow(): Flow<List<GiftCoupon>>

    @Query("SELECT * FROM gift_coupons")
    suspend fun getAllCoupons(): List<GiftCoupon>

    @Query("SELECT * FROM gift_coupons WHERE pin = :pin LIMIT 1")
    suspend fun getCouponByPin(pin: String): GiftCoupon?

    @Query("SELECT * FROM gift_coupons WHERE userEmail = :email LIMIT 1")
    suspend fun getCouponByUserEmail(email: String): GiftCoupon?

    @Query("SELECT COUNT(*) FROM gift_coupons")
    suspend fun getCouponsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: GiftCoupon): Long

    @Update
    suspend fun updateCoupon(coupon: GiftCoupon)

    // --- Reviews ---
    @Query("SELECT * FROM reviews ORDER BY id DESC")
    fun getAllReviewsFlow(): Flow<List<Review>>

    @Query("SELECT * FROM reviews")
    suspend fun getAllReviews(): List<Review>

    @Query("SELECT * FROM reviews WHERE locationId = :locationId ORDER BY id DESC")
    fun getReviewsByLocationFlow(locationId: Int): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE barberId = :barberId ORDER BY id DESC")
    fun getReviewsByBarberFlow(barberId: Int): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Delete
    suspend fun deleteReview(review: Review)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewById(id: Int)

    @Query("UPDATE appointments SET hasReview = 1 WHERE id = :id")
    suspend fun markAppointmentReviewed(id: Int)
}
