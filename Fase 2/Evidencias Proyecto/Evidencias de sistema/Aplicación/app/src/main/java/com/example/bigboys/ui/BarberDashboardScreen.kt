package com.example.bigboys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigboys.data.BarberRepository
import com.example.bigboys.model.Appointment
import com.example.bigboys.model.AppointmentStatus
import com.example.bigboys.ui.theme.*

@Composable
fun BarberDashboardScreen(
    repository: BarberRepository,
    selectedBarberName: String,
    onSelectBarber: (String) -> Unit
) {
    val appointments by repository.appointments.collectAsState()
    var selectedDate by remember { mutableStateOf(repository.todayDate) }
    var emergencyAptToCancel by remember { mutableStateOf<Appointment?>(null) }
    var emergencyReason by remember { mutableStateOf("") }
    var showSuccessSnackbar by remember { mutableStateOf<String?>(null) }

    val barberAppointments = appointments.filter {
        it.barberName == selectedBarberName && it.date == selectedDate
    }.sortedBy { it.time }

    val activeCount = barberAppointments.count { it.status == AppointmentStatus.CONFIRMED }
    val completedCount = barberAppointments.count { it.status == AppointmentStatus.COMPLETED }
    val pendingRevenue = barberAppointments.filter { it.status != AppointmentStatus.CANCELLED }
        .sumOf { it.totalPrice - it.depositAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Barber Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Barbero",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedBarberName,
                    color = GoldLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Dashboard Diario de Atención",
                    color = OnDarkMuted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barber switcher tabs
        Text(
            text = "Seleccionar Barbero:",
            color = OnDarkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(repository.barbers) { barber ->
                val isSelected = barber.name == selectedBarberName
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectBarber(barber.name) },
                    label = { Text(barber.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = OnDarkText
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Citas Hoy",
                value = "$activeCount",
                subtitle = "Por atender",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DateRange,
                color = GoldPrimary
            )
            MetricCard(
                title = "Atendidos",
                value = "$completedCount",
                subtitle = "Completados",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                color = SuccessGreen
            )
            MetricCard(
                title = "Por Cobrar",
                value = "$${String.format("%,d", pendingRevenue)}",
                subtitle = "Saldo pendiente",
                modifier = Modifier.weight(1.3f),
                icon = Icons.Default.ShoppingCart,
                color = GoldLight
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agenda del Día ($selectedDate)",
                color = OnDarkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${barberAppointments.size} turnos",
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Appointment List
        if (barberAppointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay horas agendadas para hoy con este barbero.",
                    color = OnDarkMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(barberAppointments) { apt ->
                    BarberAppointmentCard(
                        appointment = apt,
                        onCancelEmergency = { emergencyAptToCancel = apt },
                        onComplete = {
                            repository.markAsCompleted(apt.id)
                            showSuccessSnackbar = "Cita de ${apt.customerName} marcada como atendida."
                        }
                    )
                }
            }
        }
    }

    // Emergency Cancellation Dialog
    emergencyAptToCancel?.let { apt ->
        AlertDialog(
            onDismissRequest = {
                emergencyAptToCancel = null
                emergencyReason = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergencia",
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancelar por Emergencia",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Vas a cancelar la cita de ${apt.customerName} fijada a las ${apt.time} hrs.",
                        color = OnDarkText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "El cliente recibirá un correo inmediato con la explicación de fuerza mayor y se liberará el cupo.",
                        color = OnDarkMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emergencyReason,
                        onValueChange = { emergencyReason = it },
                        label = { Text("Motivo de la Emergencia") },
                        placeholder = { Text("Ej: Urgencia médica, corte de luz en el local...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRed,
                            unfocusedBorderColor = OnDarkMuted,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        ),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emergencyReason.isNotBlank()) {
                            repository.cancelByEmergency(apt.id, emergencyReason)
                            showSuccessSnackbar = "🚨 Cita cancelada. Correo de emergencia enviado a ${apt.customerEmail}."
                            emergencyAptToCancel = null
                            emergencyReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    enabled = emergencyReason.isNotBlank()
                ) {
                    Text("Confirmar Cancelación", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    emergencyAptToCancel = null
                    emergencyReason = ""
                }) {
                    Text("Volver", color = OnDarkMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Success Notification Banner
    showSuccessSnackbar?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(4000)
            showSuccessSnackbar = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = GoldPrimary,
            contentColor = Color.Black
        ) {
            Text(msg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = OnDarkMuted, fontSize = 11.sp)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = OnDarkMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun BarberAppointmentCard(
    appointment: Appointment,
    onCancelEmergency: () -> Unit,
    onComplete: () -> Unit
) {
    val isCancelled = appointment.status == AppointmentStatus.CANCELLED
    val isCompleted = appointment.status == AppointmentStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCancelled) Modifier.border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) Color(0xFF261818) else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCancelled) ErrorRed else GoldPrimary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${appointment.time} hrs",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Status chip
                when (appointment.status) {
                    AppointmentStatus.CONFIRMED -> {
                        Text(text = "Confirmada", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    AppointmentStatus.COMPLETED -> {
                        Text(text = "✅ Atendido", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    AppointmentStatus.CANCELLED -> {
                        Text(text = "❌ Cancelada", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appointment.customerName,
                color = OnDarkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RUT: ${appointment.customerRut} • Tel: ${appointment.customerPhone}",
                color = OnDarkMuted,
                fontSize = 12.sp
            )
            Text(
                text = "Servicio: ${appointment.serviceName}",
                color = GoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Payment Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Abono Pagado: $${String.format("%,d", appointment.depositAmount)}",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Saldo en Local: $${String.format("%,d", appointment.totalPrice - appointment.depositAmount)}",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isCancelled && appointment.cancellationReason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motivo de cancelación: \"${appointment.cancellationReason}\"",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons
            if (!isCancelled && !isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atendido", color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancelEmergency,
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Emergencia", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
