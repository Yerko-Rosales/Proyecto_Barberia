package com.example.bigboys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.bigboys.model.User
import com.example.bigboys.ui.theme.*

@Composable
fun ClientBookingScreen(
    repository: BarberRepository,
    currentUser: User?,
    onRequestLogin: () -> Unit,
    onBookingSuccess: () -> Unit
) {
    var selectedService by remember { mutableStateOf(repository.services.first()) }
    var selectedBarber by remember { mutableStateOf(repository.barbers.first()) }
    val availableHours = listOf("10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00", "18:00")
    var selectedHour by remember { mutableStateOf(availableHours.first()) }

    var customerName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var customerEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var customerPhone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var customerRut by remember { mutableStateOf(currentUser?.rut ?: "") }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            customerName = it.fullName
            customerEmail = it.email
            customerPhone = it.phone
            customerRut = it.rut
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBookingConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BIG BOYS BARBERÍA",
                                color = GoldPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Estilo, Tradición & Vanguardia",
                                color = OnDarkMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (currentUser != null) {
                            TextButton(onClick = onRequestLogin) {
                                Text("Cambiar cuenta", color = GoldLight, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = onRequestLogin,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("Iniciar Sesión", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "Hola, ${currentUser.fullName}", color = OnDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "RUT: ${currentUser.rut} • ${currentUser.email}", color = OnDarkMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Services section
        item {
            Text(
                text = "1. Selecciona tu Servicio",
                color = GoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repository.services.forEach { service ->
                    val isSelected = selectedService.id == service.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedService = service }
                            .then(
                                if (isSelected) Modifier.border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    color = if (isSelected) GoldLight else OnDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = service.description,
                                    color = OnDarkMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏱ ${service.durationMinutes} minutos • Abono: $5.000 CLP",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "$${String.format("%,d", service.price)}",
                                color = GoldPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // Barber selection
        item {
            Text(
                text = "2. Selecciona tu Barbero",
                color = GoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repository.barbers) { barber ->
                    val isSelected = selectedBarber.id == barber.id
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { selectedBarber = barber }
                            .then(
                                if (isSelected) Modifier.border(2.dp, GoldPrimary, RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = barber.name,
                                color = if (isSelected) GoldLight else OnDarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = barber.specialty,
                                color = OnDarkMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${barber.rating} (${barber.totalCuts} clientes)",
                                    color = GoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hours selection
        item {
            Text(
                text = "3. Horario Disponible para Hoy (${repository.todayDate})",
                color = GoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableHours) { hour ->
                    val isSelected = selectedHour == hour
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedHour = hour },
                        label = { Text("$hour hrs") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = OnDarkText
                        )
                    )
                }
            }
        }

        // Customer Details Form
        item {
            Text(
                text = "4. Confirmación de Datos del Cliente",
                color = GoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = OnDarkMuted,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )
                    OutlinedTextField(
                        value = customerEmail,
                        onValueChange = { customerEmail = it },
                        label = { Text("Correo Electrónico (Registrado)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = OnDarkMuted,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )
                    OutlinedTextField(
                        value = customerRut,
                        onValueChange = { customerRut = it },
                        label = { Text("RUT (Ej: 12.345.678-9)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = OnDarkMuted,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Teléfono / WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = OnDarkMuted,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )
                }
            }
        }

        // Summary & Submit Button
        item {
            errorMessage?.let {
                Text(text = it, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Servicio:", color = OnDarkMuted, fontSize = 13.sp)
                        Text("$${String.format("%,d", selectedService.price)}", color = OnDarkText, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Abono Requerido (Hoy):", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("$5.000 CLP", color = GoldPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo a pagar en el local:", color = OnDarkMuted, fontSize = 13.sp)
                        Text("$${String.format("%,d", selectedService.price - 5000)}", color = GoldLight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (customerName.isBlank() || customerEmail.isBlank() || customerRut.isBlank() || customerPhone.isBlank()) {
                        errorMessage = "Por favor completa todos los campos del cliente."
                    } else if (!customerEmail.contains("@")) {
                        errorMessage = "Ingresa un correo electrónico válido."
                    } else {
                        errorMessage = null
                        repository.bookAppointment(
                            customerName = customerName,
                            customerEmail = customerEmail,
                            customerPhone = customerPhone,
                            customerRut = customerRut,
                            barberName = selectedBarber.name,
                            serviceName = selectedService.name,
                            date = repository.todayDate,
                            time = selectedHour,
                            totalPrice = selectedService.price
                        )
                        showBookingConfirmation = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Confirmar Reserva (Pagar Abono $5.000)",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }

    if (showBookingConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showBookingConfirmation = false
                onBookingSuccess()
            },
            title = {
                Text(text = "¡Hora Reservada con Éxito!", color = GoldPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Hola $customerName, tu cita con ${selectedBarber.name} está confirmada para hoy a las $selectedHour hrs.",
                        color = OnDarkText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📧 Se envió un comprobante oficial de Big Boys Barbería a $customerEmail.",
                        color = GoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tu abono de $5.000 CLP ha sido registrado. Recuerda pagar el saldo restante en el local al momento de tu atención.",
                        color = OnDarkMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBookingConfirmation = false
                        onBookingSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Ver Mis Citas", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}
