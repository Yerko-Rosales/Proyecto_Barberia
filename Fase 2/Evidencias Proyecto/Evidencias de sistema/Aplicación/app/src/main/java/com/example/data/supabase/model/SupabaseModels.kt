package com.example.data.supabase.model

import com.example.data.model.Appointment
import com.example.data.model.Barber
import com.example.data.model.Location
import com.example.data.model.User
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the JSON returned by the Supabase api_* functions (tables sucursal, barbero, usuario, reserva).
 */

@Serializable
data class LocationDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("address") val address: String,
    @SerialName("phone") val phone: String,
    @SerialName("lat") val lat: Double = -33.6667,
    @SerialName("lng") val lng: Double = -70.9333,
    @SerialName("is_primary") val isPrimary: Boolean = false
) {
    fun toLocation(): Location = Location(
        id = id,
        name = name,
        address = address,
        phone = phone
    )
}

@Serializable
data class BarberDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("location_id") val locationId: Int,
    @SerialName("name") val name: String,
    @SerialName("specialty") val specialty: String,
    @SerialName("bio") val bio: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("available_hours") val availableHours: String = "",
    @SerialName("rating") val rating: Float = 5.0f,
    @SerialName("active") val active: Boolean = true
) {
    fun toBarber(): Barber = Barber(
        id = id,
        locationId = locationId,
        name = name,
        specialty = specialty,
        rating = rating,
        avatarUrl = avatarUrl,
        availableHours = availableHours,
        bio = bio,
        active = active
    )
}

@Serializable
data class UserDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("rut") val rut: String = "",
    @SerialName("password_hash") val passwordHash: String = ""
) {
    fun toUser(): User = User(
        id = id ?: 0,
        fullName = fullName,
        email = email,
        phone = phone,
        rut = rut,
        passwordHash = passwordHash,
        isRegistered = true
    )
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class AppointmentDto(
    @SerialName("id") @EncodeDefault val id: Long? = null,
    @SerialName("user_id") @EncodeDefault val userId: Int? = null,
    @SerialName("barber_id") @EncodeDefault val barberId: Int? = 1,
    @SerialName("location_id") @EncodeDefault val locationId: Int? = 1,
    @SerialName("service_name") @EncodeDefault val serviceName: String? = "Corte de Cabello",
    @SerialName("service_price") @EncodeDefault val servicePrice: Int? = 0,
    @SerialName("prepay_amount") @EncodeDefault val prepayAmount: Int? = 0,
    @SerialName("date_time") @EncodeDefault val dateTime: String? = "",
    @SerialName("status") @EncodeDefault val status: String? = "Confirmada",
    @SerialName("customer_name") @EncodeDefault val customerName: String? = "Cliente",
    @SerialName("customer_phone") @EncodeDefault val customerPhone: String? = "",
    @SerialName("customer_email") @EncodeDefault val customerEmail: String? = "",
    @SerialName("is_guest") @EncodeDefault val isGuest: Boolean? = false,
    @SerialName("codigo") val codigo: String? = null,
    @SerialName("reschedule_count") val rescheduleCount: Int? = 0,
    @SerialName("prepay_forfeited") val prepayForfeited: Boolean? = false,
    @SerialName("cancelled_by") val cancelledBy: String? = null
) {
    fun toAppointment(): Appointment = Appointment(
        id = (id ?: System.currentTimeMillis()).toInt(),
        locationId = locationId ?: 1,
        barberId = barberId ?: 1,
        customerName = customerName ?: "",
        customerPhone = customerPhone ?: "",
        customerEmail = customerEmail ?: "",
        dateTime = dateTime ?: "",
        serviceName = serviceName ?: "Corte",
        servicePrice = (servicePrice ?: 0).toDouble(),
        prepayAmount = (prepayAmount ?: 0).toDouble(),
        status = status ?: "Confirmada",
        isGuest = isGuest ?: false,
        userId = userId,
        rescheduleCount = rescheduleCount ?: 0,
        depositRetained = prepayForfeited ?: false
    )
}

fun Appointment.toDto(): AppointmentDto = AppointmentDto(
    id = if (id > 100000) id.toLong() else System.currentTimeMillis(),
    userId = userId,
    barberId = barberId,
    locationId = locationId,
    serviceName = serviceName,
    servicePrice = servicePrice.toInt(),
    prepayAmount = prepayAmount.toInt(),
    dateTime = dateTime,
    status = status,
    customerName = customerName,
    customerPhone = customerPhone,
    customerEmail = customerEmail,
    isGuest = isGuest
)

fun User.toDto(): UserDto = UserDto(
    id = if (id > 0) id else null,
    fullName = fullName,
    email = email,
    phone = phone,
    rut = rut,
    passwordHash = passwordHash
)

@Serializable
data class CouponDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("user_email") val userEmail: String,
    @SerialName("code") val code: String,
    @SerialName("discount_label") val discountLabel: String,
    @SerialName("is_used") val isUsed: Boolean = false
)

@Serializable
data class ReviewDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("location_id") val locationId: Int,
    @SerialName("barber_id") val barberId: Int? = null,
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("rating") val rating: Int,
    @SerialName("comment") val comment: String
)
