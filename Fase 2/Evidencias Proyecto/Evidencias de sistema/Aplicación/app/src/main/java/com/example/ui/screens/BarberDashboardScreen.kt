package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Appointment
import com.example.data.model.Barber
import com.example.ui.theme.*
import com.example.ui.util.AppFormatters
import com.example.ui.viewmodel.BarberiaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberDashboardScreen(
    viewModel: BarberiaViewModel,
    barber: Barber,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val allAppointments by viewModel.allAppointmentsFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Location name of this barber
    val branch = remember(locations, barber) {
        locations.find { it.id == barber.locationId }
    }
    val branchName = branch?.name ?: "Big boys Chile"

    // Calendar & Day Navigation
    val sdfDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }
    var selectedDateString by remember { mutableStateOf(sdfDate.format(Date())) }
    val todayString = remember { sdfDate.format(Date()) }
    val isToday = selectedDateString == todayString

    // Filter appointments for this barber and selected date
    val barberDayAppointments = remember(allAppointments, barber, selectedDateString) {
        allAppointments.filter { appt ->
            appt.barberId == barber.id && com.example.ui.util.AppFormatters.isSameDay(appt.dateTime, selectedDateString)
        }.sortedBy { it.dateTime }
    }

    // Filter tab
    var selectedFilter by remember { mutableStateOf("TODAS") } // "TODAS", "PENDIENTES", "COMPLETADAS", "CANCELADAS"
    val displayedAppointments = remember(barberDayAppointments, selectedFilter) {
        when (selectedFilter) {
            "PENDIENTES" -> barberDayAppointments.filter { it.status == "Confirmada" || it.status == "Pendiente" }
            "COMPLETADAS" -> barberDayAppointments.filter { it.status == "Completada" }
            "CANCELADAS" -> barberDayAppointments.filter { it.status == "Cancelada" }
            else -> barberDayAppointments
        }
    }

    // Dialog states
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGeneralEmergencyDialog by remember { mutableStateOf(false) }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }
    var emergencySuccessMessage by remember { mutableStateOf<String?>(null) }

    // Intercept back button to confirm logout
    BackHandler {
        showLogoutDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(barber.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = barber.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    barber.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(GreenConfirm.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("EN TURNO", color = GreenConfirm, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "$branchName · ⭐ ${barber.rating}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("barber_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = GoldPrimary
                        )
                    }
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("barber_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Cerrar turno",
                            tint = RedCancel
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextIvory
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Success Feedback Banner
            if (emergencySuccessMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GreenConfirm.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, GreenConfirm),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenConfirm, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Acción Ejecutada Correctamente", color = GreenConfirm, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(emergencySuccessMessage ?: "", color = TextIvory, fontSize = 12.sp)
                            }
                            IconButton(onClick = { emergencySuccessMessage = null }) {
                                Icon(Icons.Filled.Close, "Cerrar", tint = TextIvory, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 1. DATE SELECTOR BAR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val current = sdfDate.parse(selectedDateString) ?: Date()
                                    calendar.time = current
                                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                                    selectedDateString = sdfDate.format(calendar.time)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            modifier = Modifier.testTag("barber_prev_day_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Día anterior", tint = GoldPrimary)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedDateString = todayString
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isToday) "HOY" else selectedDateString,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isToday) GoldPrimary else TextIvory
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("($selectedDateString)", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                            Text(
                                AppFormatters.formatSpanishFullDate(selectedDateString),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val current = sdfDate.parse(selectedDateString) ?: Date()
                                    calendar.time = current
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    selectedDateString = sdfDate.format(calendar.time)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            modifier = Modifier.testTag("barber_next_day_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Día siguiente", tint = GoldPrimary)
                        }
                    }
                }
            }

            // 2. EMERGENCY CANCELLATION PROTOCOL BANNER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barber_emergency_banner"),
                    colors = CardDefaults.cardColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                    border = BorderStroke(1.2.dp, RedCancel.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(RedCancel.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = RedCancel,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "🚨 PROTOCOLO DE EMERGENCIA",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = RedCancel
                                )
                                Text(
                                    "Cancelación inmediata de citas por fuerza mayor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextIvory,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "En caso de accidente, problema médico o imprevisto mayor, puedes suspender citas agendadas. Se enviará de inmediato un aviso al cliente con instrucciones para el reembolso del 100% de su abono.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showGeneralEmergencyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_declare_emergency"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ACTIVAR PROTOCOLO DE EMERGENCIA",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. DAILY METRICS KPI TILES
            item {
                val totalToday = barberDayAppointments.size
                val pendingToday = barberDayAppointments.count { it.status == "Confirmada" || it.status == "Pendiente" }
                val completedToday = barberDayAppointments.count { it.status == "Completada" }
                val cancelledToday = barberDayAppointments.count { it.status == "Cancelada" }
                val estimatedRevenue = barberDayAppointments
                    .filter { it.status == "Completada" || it.status == "Confirmada" }
                    .sumOf { it.servicePrice }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "RESUMEN DEL DÍA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = GoldPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Agendadas",
                            value = "$totalToday",
                            icon = Icons.Filled.Event,
                            iconColor = GoldPrimary
                        )
                        MetricKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Por Atender",
                            value = "$pendingToday",
                            icon = Icons.Filled.HourglassTop,
                            iconColor = Color(0xFFF59E0B)
                        )
                        MetricKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Atendidas",
                            value = "$completedToday",
                            icon = Icons.Filled.CheckCircle,
                            iconColor = GreenConfirm
                        )
                        MetricKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Canceladas",
                            value = "$cancelledToday",
                            icon = Icons.Filled.Cancel,
                            iconColor = RedCancel
                        )
                    }

                    // Revenue Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Payments, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Proyección Servicios del Día:", color = TextIvory, fontSize = 12.sp)
                            }
                            Text(
                                "${AppFormatters.formatClp(estimatedRevenue)} CLP",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 4. FILTER CHIPS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val countAll = barberDayAppointments.size
                    val countPending = barberDayAppointments.count { it.status == "Confirmada" || it.status == "Pendiente" }
                    val countDone = barberDayAppointments.count { it.status == "Completada" }
                    val countCancelled = barberDayAppointments.count { it.status == "Cancelada" }

                    FilterChip(
                        selected = selectedFilter == "TODAS",
                        onClick = { selectedFilter = "TODAS" },
                        label = { Text("Todas ($countAll)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDIENTES",
                        onClick = { selectedFilter = "PENDIENTES" },
                        label = { Text("Pendientes ($countPending)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "COMPLETADAS",
                        onClick = { selectedFilter = "COMPLETADAS" },
                        label = { Text("Atendidas ($countDone)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenConfirm,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "CANCELADAS",
                        onClick = { selectedFilter = "CANCELADAS" },
                        label = { Text("Canceladas ($countCancelled)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedCancel,
                            selectedLabelColor = Color.White,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        )
                    )
                }
            }

            // 5. APPOINTMENTS LIST FOR THE DAY
            if (displayedAppointments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = GoldPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Sin citas registradas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextIvory
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No hay horas en la lista con el filtro seleccionado para el $selectedDateString.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedAppointments, key = { it.id }) { appt ->
                    BarberAppointmentCard(
                        appointment = appt,
                        barber = barber,
                        branchName = branchName,
                        onComplete = {
                            viewModel.completeAppointmentByBarber(appt)
                            emergencySuccessMessage = "Cita con ${appt.customerName} marcada como completada con éxito."
                        },
                        onEmergencyCancel = {
                            appointmentToCancel = appt
                        },
                        onNoShow = {
                            viewModel.markAppointmentNoShow(appt)
                            emergencySuccessMessage = "Cita de ${appt.customerName} marcada como No-Show. Abono de $3.000 retenido."
                        }
                    )
                }
            }

            // 6. BARBER DAILY WORKING HOURS STATUS OVERVIEW
            item {
                val availableHoursList = remember(barber) {
                    barber.availableHours.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "HORARIOS DE MI TURNO",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Visualización rápida del estado de cada bloque de atención:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableHoursList) { hour ->
                                val bookedAppt = barberDayAppointments.find {
                                    com.example.ui.util.AppFormatters.extractHourSlot(it.dateTime) == hour
                                }
                                val status = bookedAppt?.status

                                val (bgColor, borderColor, textColor, label) = when (status) {
                                    "Confirmada", "Pendiente" -> Quadruple(Color(0xFF1E3A8A).copy(alpha = 0.4f), Color(0xFF3B82F6), Color(0xFF93C5FD), "Ocupada")
                                    "Completada" -> Quadruple(GreenConfirm.copy(alpha = 0.2f), GreenConfirm, GreenConfirm, "Atendida")
                                    "Cancelada" -> Quadruple(RedCancel.copy(alpha = 0.2f), RedCancel, RedCancel, "Cancelada")
                                    else -> Quadruple(DarkBackground, GoldPrimary.copy(alpha = 0.3f), TextIvory, "Libre")
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    border = BorderStroke(1.dp, borderColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(90.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(hour, fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(label, color = textColor.copy(alpha = 0.8f), fontSize = 10.sp)
                                        if (bookedAppt != null) {
                                            Text(
                                                bookedAppt.customerName.split(" ").firstOrNull() ?: "",
                                                color = TextIvory,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // --- MODAL: SINGLE APPOINTMENT EMERGENCY CANCELLATION ---
    if (appointmentToCancel != null) {
        val appt = appointmentToCancel!!
        EmergencyCancellationDialog(
            appointment = appt,
            barberName = barber.name,
            branchName = branchName,
            onDismiss = { appointmentToCancel = null },
            onConfirm = { reason ->
                viewModel.cancelAppointmentByBarber(
                    appointment = appt,
                    barberName = barber.name,
                    locationName = branchName,
                    emergencyReason = reason
                ) {
                    appointmentToCancel = null
                    emergencySuccessMessage = "Cita de las ${appt.dateTime.substringAfter(" ")} con ${appt.customerName} cancelada por emergencia. Se notificó al cliente para el reembolso de su abono."
                }
            }
        )
    }

    // --- MODAL: FULL DAY EMERGENCY SUSPENSION ---
    if (showGeneralEmergencyDialog) {
        GeneralDayEmergencyDialog(
            barber = barber,
            branchName = branchName,
            selectedDate = selectedDateString,
            pendingAppointments = barberDayAppointments.filter { it.status == "Confirmada" || it.status == "Pendiente" },
            onDismiss = { showGeneralEmergencyDialog = false },
            onConfirmSuspendAll = { reason ->
                viewModel.cancelAllTodayAppointmentsByBarber(
                    barberId = barber.id,
                    date = selectedDateString,
                    barberName = barber.name,
                    locationName = branchName,
                    reason = reason
                ) { count ->
                    showGeneralEmergencyDialog = false
                    emergencySuccessMessage = "Protocolo de emergencia ejecutado. Se cancelaron $count cita(s) agendadas y se notificó la devolución bancaria de los abonos."
                }
            }
        )
    }

    // --- MODAL: LOGOUT CONFIRMATION ---
    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(RedCancel.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, RedCancel.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Logout, null, tint = RedCancel, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "¿Cerrar Turno de Barbero?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextIvory,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Saldrás de tu panel diario de atención. Podrás volver a ingresar en cualquier momento con tus credenciales de staff.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, TextMuted)
                        ) {
                            Text("Permanecer", color = TextIvory, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_barber_logout_btn")
                        ) {
                            Text("Cerrar Turno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: BARBER APPOINTMENT CARD
// -------------------------------------------------------------
@Composable
private fun BarberAppointmentCard(
    appointment: Appointment,
    barber: Barber,
    branchName: String,
    onComplete: () -> Unit,
    onEmergencyCancel: () -> Unit,
    onNoShow: () -> Unit
) {
    val context = LocalContext.current
    val timeSlot = appointment.dateTime.substringAfter(" ")

    val (statusBg, statusBorder, statusText, statusColor) = when (appointment.status) {
        "Confirmada" -> Quadruple(GoldPrimary.copy(alpha = 0.15f), GoldPrimary, "CONFIRMADA", GoldPrimary)
        "Pendiente" -> Quadruple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B), "PENDIENTE", Color(0xFFF59E0B))
        "Completada" -> Quadruple(GreenConfirm.copy(alpha = 0.15f), GreenConfirm, "ATENDIDA", GreenConfirm)
        "Caducada", "Vencida" -> Quadruple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444), "VENCIDA / CADUCADA", Color(0xFFEF4444))
        "No-Show" -> Quadruple(Color(0xFFF97316).copy(alpha = 0.15f), Color(0xFFF97316), "NO-SHOW (RETENIDO)", Color(0xFFF97316))
        "Cancelada (Abono Retenido)" -> Quadruple(RedCancel.copy(alpha = 0.15f), RedCancel, "ANULADA (<24H)", RedCancel)
        "Cancelada" -> Quadruple(RedCancel.copy(alpha = 0.15f), RedCancel, "CANCELADA", RedCancel)
        else -> Quadruple(CardBackground, TextMuted, appointment.status.uppercase(), TextMuted)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("barber_appt_${appointment.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, if (appointment.status == "Cancelada") RedCancel.copy(alpha = 0.5f) else GoldPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Time and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⏰ $timeSlot",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cita #${appointment.id}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .border(1.dp, statusBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Name & Service
            Text(
                text = appointment.customerName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextIvory
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "✂️ ${appointment.serviceName} · ${AppFormatters.formatClpPrice(appointment.servicePrice)}",
                color = GoldAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "💳 Abono recibido: ${AppFormatters.formatClpPrice(appointment.prepayAmount)} (Restante en caja: ${AppFormatters.formatClpPrice(appointment.servicePrice - appointment.prepayAmount)})",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = DarkBackground, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Customer Contact Info & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (appointment.customerPhone.isBlank()) "Sin teléfono" else appointment.customerPhone,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (appointment.customerEmail.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                appointment.customerEmail,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Quick WhatsApp / Call actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (appointment.customerPhone.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                val cleanNum = appointment.customerPhone.replace("+", "").replace(" ", "").replace("-", "")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNum&text=Hola%20${Uri.encode(appointment.customerName)},%20te%20escribo%20de%20Big%20boys%20Barber%C3%ADas%20por%20tu%20cita%20de%20hoy."))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = GreenConfirm.copy(alpha = 0.2f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", tint = GreenConfirm, modifier = Modifier.size(18.dp))
                        }
                        FilledIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${appointment.customerPhone}"))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Llamar", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ACTIONS ROW
            if (appointment.status == "Confirmada" || appointment.status == "Pendiente") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Complete Appointment Action
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp)
                            .testTag("btn_complete_${appointment.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, tint = DarkBackground, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ATENDIDA", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // No-Show Action
                    OutlinedButton(
                        onClick = onNoShow,
                        border = BorderStroke(1.dp, Color(0xFFF97316)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF97316).copy(alpha = 0.08f)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(42.dp)
                            .testTag("btn_noshow_${appointment.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("NO-SHOW", color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Emergency Cancellation Action
                    OutlinedButton(
                        onClick = onEmergencyCancel,
                        border = BorderStroke(1.dp, RedCancel),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = RedCancel.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(42.dp)
                            .testTag("btn_cancel_emergency_${appointment.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = RedCancel, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("EMERGENCIA", color = RedCancel, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            } else if (appointment.status == "No-Show") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF97316).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "🚫 INASISTENCIA (NO-SHOW) · Abono de $3.000 retenido según política de bloqueo de agenda.",
                        color = Color(0xFFF97316),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (appointment.status.contains("Retenido")) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedCancel.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "⚠️ ANULACIÓN TARDÍA (< 24 HORAS) · Abono de $3.000 retenido por costo de agenda.",
                        color = RedCancel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (appointment.status == "Cancelada") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedCancel.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "⚠️ Cita cancelada por protocolo de emergencia. Notificación y solicitud de reembolso enviadas.",
                        color = RedCancel,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (appointment.status == "Completada") {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenConfirm.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "✨ Servicio finalizado con éxito por $barber.",
                        color = GreenConfirm,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: SINGLE APPOINTMENT EMERGENCY CANCELLATION
// -------------------------------------------------------------
@Composable
private fun EmergencyCancellationDialog(
    appointment: Appointment,
    barberName: String,
    branchName: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val emergencyReasons = listOf(
        "Emergencia médica repentina",
        "Problema de salud / Incapacidad temporal",
        "Urgencia familiar de fuerza mayor",
        "Falla de suministro eléctrico o agua en el local",
        "Imprevisto vial / Desplazamiento impedido"
    )
    var selectedReason by remember { mutableStateOf(emergencyReasons.first()) }
    var customReason by remember { mutableStateOf("") }
    var isCustom by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.5.dp, RedCancel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(RedCancel.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Emergency, null, tint = RedCancel, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Cancelar Cita por Emergencia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextIvory
                        )
                        Text(
                            "Cita de las ${appointment.dateTime.substringAfter(" ")} con ${appointment.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cliente: ${appointment.customerName}", color = TextIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Servicio: ${appointment.serviceName}", color = TextMuted, fontSize = 11.sp)
                        Text("Abono a reembolsar: ${AppFormatters.formatClpPrice(appointment.prepayAmount)}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Selecciona el motivo de la emergencia:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextIvory
                )
                Spacer(modifier = Modifier.height(6.dp))

                emergencyReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustom = false
                                selectedReason = reason
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isCustom && selectedReason == reason,
                            onClick = {
                                isCustom = false
                                selectedReason = reason
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = RedCancel)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(reason, color = TextIvory, fontSize = 12.sp)
                    }
                }

                // Notice Box
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedCancel.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = RedCancel, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Al confirmar, la hora quedará libre y se enviará un correo formal solicitando los datos bancarios del cliente para la devolución.",
                            color = TextIvory,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TextMuted)
                    ) {
                        Text("Volver", color = TextIvory, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val finalReason = if (isCustom && customReason.isNotBlank()) customReason else selectedReason
                            onConfirm(finalReason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_emergency_cancel_btn")
                    ) {
                        Text("Confirmar Cancelación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: FULL DAY EMERGENCY SUSPENSION
// -------------------------------------------------------------
@Composable
private fun GeneralDayEmergencyDialog(
    barber: Barber,
    branchName: String,
    selectedDate: String,
    pendingAppointments: List<Appointment>,
    onDismiss: () -> Unit,
    onConfirmSuspendAll: (reason: String) -> Unit
) {
    val emergencyReasons = listOf(
        "Emergencia médica repentina",
        "Problema de salud / Incapacidad temporal",
        "Urgencia familiar de fuerza mayor",
        "Falla de suministro eléctrico o agua en la sucursal"
    )
    var selectedReason by remember { mutableStateOf(emergencyReasons.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(2.dp, RedCancel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(RedCancel.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WarningAmber, null, tint = RedCancel, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Emergencia General del Día",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = RedCancel
                        )
                        Text(
                            "Barbero: ${barber.name} · $selectedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextIvory,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "Esta acción cancelará TODAS tus citas agendadas (${pendingAppointments.size} cita(s) pendiente(s)) para el día de hoy debido a una emergencia justificada.",
                    color = TextIvory,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Motivo de la emergencia:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                emergencyReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = RedCancel)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(reason, color = TextIvory, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedCancel.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "⚠️ Cada cliente afectado recibirá un correo de disculpas institucionales con el detalle para la transferencia de reembolso del 100% de su abono.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TextMuted)
                    ) {
                        Text("Cancelar", color = TextIvory, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onConfirmSuspendAll(selectedReason) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCancel),
                        enabled = pendingAppointments.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_confirm_suspend_all")
                    ) {
                        Text("Suspender Turno", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER: METRIC KPI CARD
// -------------------------------------------------------------
@Composable
private fun MetricKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 16.sp)
            Text(title, color = TextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
