package com.example.bigboys.model

enum class UserRole {
    CLIENT,
    BARBER,
    ADMIN
}

data class User(
    val email: String,
    val rut: String,
    val fullName: String,
    val phone: String,
    val role: UserRole = UserRole.CLIENT,
    val barberName: String? = null
)

data class ServiceItem(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val price: Int,
    val description: String,
    val category: String = "Cortes"
)

data class Barber(
    val id: String,
    val name: String,
    val email: String,
    val specialty: String,
    val rating: Double,
    val totalCuts: Int,
    val bio: String
)

enum class AppointmentStatus {
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

data class Appointment(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val customerRut: String,
    val barberName: String,
    val serviceName: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val depositAmount: Int = 5000,
    val totalPrice: Int,
    val status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ApiLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val method: String,
    val endpoint: String,
    val statusCode: Int = 200,
    val durationMs: Long,
    val details: String,
    val isSuccess: Boolean = true
)

data class EmailNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val recipient: String,
    val subject: String,
    val preview: String,
    val sentAt: String,
    val status: String = "Entregado (200 OK)"
)
