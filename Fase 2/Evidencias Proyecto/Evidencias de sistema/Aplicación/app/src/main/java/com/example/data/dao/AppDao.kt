package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Users ---
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

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

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY id DESC")
    fun getAllAppointmentsFlow(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointments(): List<Appointment>

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

    /** keys = "nombre|teléfono"; borra solo las citas que calzan en ambos datos. */
    @Query("DELETE FROM appointments WHERE customerName || '|' || customerPhone IN (:keys)")
    suspend fun deleteAppointmentsByNameAndPhone(keys: List<String>)

    // --- Products ---

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Delete
    suspend fun deleteReview(review: Review)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewById(id: Int)

    @Query("UPDATE appointments SET hasReview = 1 WHERE id = :id")
    suspend fun markAppointmentReviewed(id: Int)
}
