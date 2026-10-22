package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val rut: String = "",
    val passwordHash: String = "",
    val isRegistered: Boolean = true,
    val avatarUrl: String = ""
)

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey val id: Int,
    val name: String,
    val address: String,
    val phone: String
)

@Entity(tableName = "barbers")
data class Barber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationId: Int,
    val name: String,
    val specialty: String,
    val rating: Float,
    val avatarUrl: String, // Can be simulated raw resource ID string or drawable name
    val availableHours: String, // Comma-separated: "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00"
    val bio: String = "Profesional destacado con más de 5 años de trayectoria en barbería clásica, cortes modernos con degradados impecables y afeitados rústicos a navaja.",
    val active: Boolean = true
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationId: Int,
    val barberId: Int,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String = "",
    val dateTime: String, // ISO representation or simple: "15-06-2026 14:00"
    val serviceName: String,
    val servicePrice: Double,
    val prepayAmount: Double = 3000.0, // Fixed abono $3.000 (descontable del servicio)
    val status: String, // "Pendiente", "Confirmada", "Completada", "Cancelada", "No-Show"
    val isGuest: Boolean = false,
    val userId: Int? = null, // refers to User
    val hasReview: Boolean = false,
    val rescheduleCount: Int = 0, // Máximo 2 reagendamientos con el mismo abono
    val depositRetained: Boolean = false // Abono retenido si anula < 24h o No-Show
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appointmentId: Int? = null,
    val locationId: Int? = null,
    val barberId: Int? = null,
    val authorName: String,
    val authorAvatarUrl: String = "",
    val rating: Int, // 1 to 5
    val comment: String,
    val serviceOrSubject: String, // e.g. "Corte Degradado Moderno", "Sucursal Talagante"
    val date: String,
    val tags: String = "", // comma-separated e.g. "Puntualidad, Atención VIP"
    val reviewType: String = "SERVICE" // "SERVICE" (Barber/Service) or "BRANCH" (Local/Sucursal)
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val imageUrl: String? = null // simulated local device storage or drawable name
)

@Entity(tableName = "supplies")
data class Supply(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val locationId: Int,
    val name: String,
    val stock: Int,
    val unit: String,
    val minStock: Int
)

@Entity(tableName = "gift_coupons")
data class GiftCoupon(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pin: String,
    val userEmail: String,
    val userName: String,
    val isRedeemed: Boolean = false,
    val createdAt: String
)

