package com.example.bigboys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.bigboys.model.AppointmentStatus
import com.example.bigboys.ui.theme.*

@Composable
fun MyAppointmentsScreen(
    repository: BarberRepository
) {
    val appointments by repository.appointments.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Mis Citas Agendadas",
            color = GoldPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Revisa el estado de tus reservas y notificaciones del barbero.",
            color = OnDarkMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (appointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No tienes citas registradas.", color = OnDarkMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(appointments.reversed()) { apt ->
                    val isCancelled = apt.status == AppointmentStatus.CANCELLED
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCancelled) Color(0xFF261818) else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${apt.date} • ${apt.time} hrs",
                                    color = if (isCancelled) ErrorRed else GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (apt.status) {
                                        AppointmentStatus.CONFIRMED -> "Confirmada"
                                        AppointmentStatus.COMPLETED -> "Atendido"
                                        AppointmentStatus.CANCELLED -> "Cancelada"
                                    },
                                    color = when (apt.status) {
                                        AppointmentStatus.CONFIRMED -> GoldPrimary
                                        AppointmentStatus.COMPLETED -> SuccessGreen
                                        AppointmentStatus.CANCELLED -> ErrorRed
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = apt.serviceName, color = OnDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Barbero: ${apt.barberName}", color = GoldLight, fontSize = 13.sp)
                            Text(text = "Cliente: ${apt.customerName} (${apt.customerEmail})", color = OnDarkMuted, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Abono: $${String.format("%,d", apt.depositAmount)} | Saldo a pagar: $${String.format("%,d", apt.totalPrice - apt.depositAmount)}",
                                color = OnDarkMuted,
                                fontSize = 11.sp
                            )

                            if (isCancelled && apt.cancellationReason != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🚨 Cancelado por ${apt.cancelledBy ?: "el barbero"}: \"${apt.cancellationReason}\"",
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
