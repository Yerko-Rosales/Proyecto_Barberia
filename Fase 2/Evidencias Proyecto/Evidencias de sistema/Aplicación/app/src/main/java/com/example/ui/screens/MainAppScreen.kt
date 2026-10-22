package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import com.example.ui.util.AppFormatters
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.BranchesMapScreen
import com.example.ui.components.RealTimeAppointmentCalendar
import com.example.ui.components.openGoogleMaps
import com.example.ui.theme.*
import com.example.ui.util.ValidationUtils
import com.example.ui.viewmodel.BarberiaViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val chileLocale = Locale("es", "CL")
private val clpFormatter: ThreadLocal<NumberFormat> = ThreadLocal.withInitial {
    NumberFormat.getNumberInstance(chileLocale)
}

fun formatClp(amount: Number): String {
    return clpFormatter.get()?.format(amount.toLong()) ?: amount.toLong().toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: BarberiaViewModel,
    modifier: Modifier = Modifier,
    initialShowSplash: Boolean = false
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val loggedInBarber by viewModel.loggedInBarber.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showSplashScreen by rememberSaveable { mutableStateOf(initialShowSplash) }
    var activeTab by remember { mutableStateOf("inicio") }
    var showBookingWizard by remember { mutableStateOf(false) }
    var showNotificationsDropdown by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var isGuestBypass by remember { mutableStateOf(false) }
    var startInRegisterMode by remember { mutableStateOf(false) }
    var showGuestPromoDialog by remember { mutableStateOf(false) }
    var showBarberQuickLoginDialog by remember { mutableStateOf(false) }

    if (showSplashScreen) {
        BarbershopSplashScreen(
            onFinish = { showSplashScreen = false }
        )
    } else if (loggedInBarber != null) {
        BarberDashboardScreen(
            viewModel = viewModel,
            barber = loggedInBarber!!,
            onLogout = {
                viewModel.logoutBarber()
                isGuestBypass = false
                startInRegisterMode = false
                showGuestPromoDialog = false
                activeTab = "inicio"
            }
        )
    } else if (loggedInUser == null && !isAdmin && !isGuestBypass) {
        LoginRegisterScreen(
            viewModel = viewModel,
            onGuestAccess = { 
                isGuestBypass = true 
                startInRegisterMode = false
                showGuestPromoDialog = true
            },
            initialRegisterMode = startInRegisterMode
        )
    } else if (isAdmin) {
        AdminDashboardScreen(
            viewModel = viewModel,
            onLogout = {
                viewModel.logout()
                isGuestBypass = false
                startInRegisterMode = false
                showGuestPromoDialog = false
                activeTab = "inicio"
            }
        )
    } else {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "CHILE · SUCURSAL PROVIDENCIA",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.8.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BIG BOYS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 24.sp,
                                    letterSpacing = (-1).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ".",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Theme Toggle Quick Button (Light / Dark Mode)
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("app_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkMode) "Activar Modo Claro" else "Activar Modo Oscuro",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Notification Bell with badge
                    IconButton(
                        onClick = { showNotificationsDropdown = !showNotificationsDropdown },
                        modifier = Modifier.testTag("bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = if (isDarkMode) DarkBackground else Color.White
                                    ) {
                                        Text(notifications.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alertas",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Barber Mode Fast Switcher for Testing
                    IconButton(
                        onClick = { showBarberQuickLoginDialog = true },
                        modifier = Modifier.testTag("app_bar_barber_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = "Acceso Barberos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // User Status Profile Chip
                    UserStatusChip(
                        user = loggedInUser,
                        isAdmin = isAdmin,
                        onOpenProfile = { showProfileDialog = true },
                        onLogout = {
                            viewModel.logout()
                            isGuestBypass = false
                            startInRegisterMode = false
                            showGuestPromoDialog = true
                            activeTab = "inicio"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, "Inicio") },
                    label = { Text("Inicio", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) },
                    selected = activeTab == "inicio",
                    onClick = { activeTab = "inicio" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.People, "Barberos") },
                    label = { Text("Barberos", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) },
                    selected = activeTab == "barberos",
                    onClick = { activeTab = "barberos" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Place, "Sucursales") },
                    label = { Text("Sucursales", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) },
                    selected = activeTab == "sucursales",
                    onClick = { activeTab = "sucursales" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.RateReview, "Reseñas") },
                    label = { Text("Reseñas", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) },
                    selected = activeTab == "reseñas",
                    onClick = { activeTab = "reseñas" },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (loggedInUser != null) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.History, "Historial") },
                        label = { Text("Historial", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) },
                        selected = activeTab == "historial",
                        onClick = { activeTab = "historial" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab != "reseñas") {
                FloatingActionButton(
                    onClick = { showBookingWizard = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = if (isDarkMode) DarkBackground else Color.White,
                    modifier = Modifier.testTag("book_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CalendarMonth, "Cita", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AGENDAR", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content based on Tab
            Crossfade(
                targetState = activeTab,
                animationSpec = tween(durationMillis = 300),
                label = "tab_fade"
            ) { tab ->
                when (tab) {
                    "inicio" -> HomeScreen(
                        viewModel = viewModel,
                        onGoToRegister = {
                            isGuestBypass = false
                            startInRegisterMode = true
                        },
                        onRequestBooking = { showBookingWizard = true },
                        onNavigateToMap = { activeTab = "sucursales" },
                        onOpenProfile = { showProfileDialog = true }
                    )
                    "barberos" -> BarbersScreen(
                        viewModel = viewModel,
                        onBookWithBarber = { barber ->
                            viewModel.selectBarber(barber)
                            // Prefill location as well
                            viewModel.locationsFlow.value.find { it.id == barber.locationId }?.let {
                                viewModel.selectLocation(it)
                            }
                            showBookingWizard = true
                        }
                    )
                    "sucursales" -> {
                        val locations by viewModel.locationsFlow.collectAsState()
                        val barbers by viewModel.allBarbersFlow.collectAsState()
                        BranchesMapScreen(
                            locations = locations,
                            barbers = barbers,
                            onSelectBranchForBooking = { loc ->
                                viewModel.selectLocation(loc)
                                showBookingWizard = true
                            }
                        )
                    }
                    "reseñas" -> ReviewsScreen(
                        viewModel = viewModel,
                        onNavigateToBooking = { showBookingWizard = true }
                    )
                    "historial" -> {
                        if (loggedInUser != null) {
                            HistoryScreen(
                                viewModel = viewModel,
                                onGoToBooking = { showBookingWizard = true },
                                onOpenProfile = { showProfileDialog = true }
                            )
                        } else {
                            HomeScreen(
                                viewModel = viewModel,
                                onGoToRegister = {
                                    isGuestBypass = false
                                    startInRegisterMode = true
                                },
                                onRequestBooking = { showBookingWizard = true },
                                onNavigateToMap = { activeTab = "sucursales" },
                                onOpenProfile = { showProfileDialog = true }
                            )
                        }
                    }
                    "admin" -> AdminScreen(viewModel = viewModel)
                }
            }

            // Dropdown overlay for Notifications Alert Box
            if (showNotificationsDropdown) {
                Dialog(onDismissRequest = { showNotificationsDropdown = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.NotificationsActive, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buzón de Alertas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                TextButton(onClick = { viewModel.clearNotifications() }) {
                                    Text("Limpiar", color = GoldPrimary, fontSize = 12.sp)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = GoldPrimary.copy(alpha = 0.3f))

                            if (notifications.isEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Outlined.NotificationsNone, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No tienes notificaciones por ahora", color = TextMuted, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(notifications) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    GoldPrimary.copy(alpha = 0.05f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    GoldPrimary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Notifications,
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldAccent
                                                        )
                                                    )
                                                    Text(
                                                        text = item.timestamp,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                        color = TextMuted
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.message,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = TextIvory)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showNotificationsDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("Entendido", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Booking Wizard Modal Dialog (Reservas)
            if (showBookingWizard) {
                BookingWizardDialog(
                    viewModel = viewModel,
                    onDismiss = {
                        showBookingWizard = false
                        viewModel.resetBookingDraft()
                    }
                )
            }

            // Beautiful Guest Welcome Promo Dialog for free eyebrow grooming!
            if (isGuestBypass && showGuestPromoDialog && loggedInUser == null) {
                Dialog(onDismissRequest = { showGuestPromoDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("guest_promo_card"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.5.dp, GoldPrimary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Close button in top-right
                            IconButton(
                                onClick = { showGuestPromoDialog = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextMuted
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Gift & Barber scissors Badge Design
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(GoldPrimary.copy(alpha = 0.12f), CircleShape)
                                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCut,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "¡REGALO DE BIENVENIDA! 🎁",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Perfilado de Cejas Gratis",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = TextIvory,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Regístrate hoy en la app y, cuando reserves tu primer corte de pelo con nosotros, te llevarás de regalo un perfilado de cejas profesional totalmente gratis. ¡No te pierdas de esta increíble experiencia Big Boys!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 20.sp,
                                        fontSize = 13.sp
                                    ),
                                    color = TextIvory.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Main Call-To-Action Button
                                Button(
                                    onClick = {
                                        showGuestPromoDialog = false
                                        isGuestBypass = false
                                        startInRegisterMode = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("guest_promo_register_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = DarkBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Registrarme y Obtener Regalo ⚡",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DarkBackground
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                TextButton(
                                    onClick = { showGuestPromoDialog = false },
                                    modifier = Modifier.testTag("guest_promo_not_now_button")
                                ) {
                                    Text(
                                        text = "Más tarde, explorar como invitado",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profile Dialog
            if (showProfileDialog) {
                ProfileDialog(
                    viewModel = viewModel,
                    onDismiss = { showProfileDialog = false }
                )
            }

            // Quick Barber Switcher Dialog
            if (showBarberQuickLoginDialog) {
                BarberQuickLoginDialog(
                    viewModel = viewModel,
                    onDismiss = { showBarberQuickLoginDialog = false }
                )
            }
        }
    }
}
}

@Composable
fun UserStatusChip(
    user: User?,
    isAdmin: Boolean,
    onOpenProfile: () -> Unit = {},
    onLogout: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            onClick = { showMenu = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, if (isAdmin) MaterialTheme.colorScheme.primary else if (user != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .padding(end = 4.dp)
                .testTag("profile_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user?.avatarUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = if (isAdmin) "AD" else if (user != null) {
                            user.fullName.trim().split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                        } else "IN"
                        Text(
                            text = initials.uppercase(),
                            color = DarkBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAdmin) "Admin 🛡️" else user?.fullName?.split(" ")?.firstOrNull() ?: "Invitado",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 80.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            if (user != null || isAdmin) {
                DropdownMenuItem(
                    text = { Text("Personalizar Mi Perfil 👤", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        showMenu = false
                        onOpenProfile()
                    },
                    leadingIcon = { Icon(Icons.Outlined.ManageAccounts, null, tint = MaterialTheme.colorScheme.primary) }
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                DropdownMenuItem(
                    text = { Text("Cerrar Sesión", color = RedCancel) },
                    onClick = {
                        showMenu = false
                        onLogout()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Logout, null, tint = RedCancel) }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Registrar / Iniciar Sesión", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showMenu = false
                        onLogout()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Login, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(
    viewModel: BarberiaViewModel,
    onGoToRegister: () -> Unit,
    onRequestBooking: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val user = loggedInUser
    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    val activeUserCoupon by viewModel.activeUserCoupon.collectAsState()

    val userAppointments = remember(appointments, user) {
        if (user == null) emptyList()
        else appointments.filter { appt ->
            val matchesId = (appt.userId != null && appt.userId == user.id)
            val matchesEmail = user.email.isNotBlank() && appt.customerEmail.trim().equals(user.email.trim(), ignoreCase = true)
            val cleanApptPhone = appt.customerPhone.replace(" ", "").replace("+56", "").replace("-", "")
            val cleanUserPhone = user.phone.replace(" ", "").replace("+56", "").replace("-", "")
            val matchesPhone = cleanUserPhone.isNotBlank() && cleanApptPhone.isNotBlank() && cleanApptPhone == cleanUserPhone
            (matchesId || matchesEmail || matchesPhone) && (
                appt.status == "Confirmada" || 
                appt.status == "Pendiente" || 
                appt.status == "Caducada" || 
                appt.status == "Vencida"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GoldPrimary)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (user != null) "👋 ¡Hola, ${user.fullName}!" else "💈 Bienvenidos a Big boys",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = DarkBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Donde el estilo se junta con la distinción. Agenda tu corte de manera ágil con prepagos garantizados.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = DarkBackground.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            // Column 1 height: 42.dp, width: 6.dp
                            Box(modifier = Modifier.size(width = 6.dp, height = 42.dp).clip(CircleShape).background(DarkBackground.copy(alpha = 0.15f))) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).align(Alignment.BottomCenter).background(DarkBackground))
                            }
                            // Column 2 height: 42.dp, width: 6.dp
                            Box(modifier = Modifier.size(width = 6.dp, height = 42.dp).clip(CircleShape).background(DarkBackground.copy(alpha = 0.15f))) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.50f).align(Alignment.BottomCenter).background(DarkBackground))
                            }
                            // Column 3 height: 42.dp, width: 6.dp
                            Box(modifier = Modifier.size(width = 6.dp, height = 42.dp).clip(CircleShape).background(DarkBackground.copy(alpha = 0.15f))) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(1.0f).align(Alignment.BottomCenter).background(DarkBackground))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRequestBooking,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary)
                        ) {
                            Text("AGENDAR CITAS ONLINE", fontWeight = FontWeight.Bold)
                        }

                        if (loggedInUser == null) {
                            OutlinedButton(
                                onClick = onGoToRegister,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBackground),
                                border = BorderStroke(1.dp, DarkBackground)
                            ) {
                                Text("REGISTRARME", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Active user's golden gift coupon for free eyebrow grooming
        if (user != null && activeUserCoupon != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_gift_coupon_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B18)), // Rich deep warm charcoal
                    border = BorderStroke(1.5.dp, if (activeUserCoupon!!.isRedeemed) TextMuted.copy(alpha = 0.4f) else GoldAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (activeUserCoupon!!.isRedeemed) Color(0xFF27272A) else GoldAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeUserCoupon!!.isRedeemed) Icons.Filled.CheckCircle else Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (activeUserCoupon!!.isRedeemed) GreenConfirm else GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeUserCoupon!!.isRedeemed) "REGALO CANJEADO 🏅" else "¡TU REGALO DE BIENVENIDA! 🎁",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (activeUserCoupon!!.isRedeemed) TextMuted else GoldPrimary
                                )
                                Text(
                                    text = "Perfilado de Cejas Profesional Gratis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (activeUserCoupon!!.isRedeemed) TextMuted else TextIvory
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = GoldPrimary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CÓDIGO DE CANJE (PIN):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeUserCoupon!!.pin,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 4.sp,
                                    color = if (activeUserCoupon!!.isRedeemed) TextMuted else GoldAccent
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (activeUserCoupon!!.isRedeemed) Color(0xFF27272A) else GoldPrimary.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (activeUserCoupon!!.isRedeemed) Color.Transparent else GoldPrimary.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = if (activeUserCoupon!!.isRedeemed) "CANJEADO" else "DISPONIBLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeUserCoupon!!.isRedeemed) TextMuted else GoldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (activeUserCoupon!!.isRedeemed) {
                                "Este cupón ya fue validado en caja el ${activeUserCoupon!!.createdAt}. ¡Esperamos que disfrutes de tu estilo Big Boys!"
                            } else {
                                "Muestra este PIN exclusivo de 4 dígitos a tu barbero al momento de tu primer servicio para activar tu perfilado de cejas de regalo."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Sucursales Summary (Chile locations with Google Maps)
        item {
            val context = LocalContext.current
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUCURSALES CHILE (MAPA)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp
                        ),
                        color = TextIvory,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Ver en Mapa 🗺️",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = GoldPrimary,
                        modifier = Modifier.clickable { onNavigateToMap() }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    LocationBannerCard(
                        name = "TALAGANTE",
                        address = "Av. Libertador 450",
                        phone = "+56 2 2489 1234",
                        lat = -33.6635,
                        lng = -70.9272,
                        onClickCard = { onNavigateToMap() }
                    )
                    LocationBannerCard(
                        name = "PEÑAFLOR 1",
                        address = "Av. 21 de Mayo 1282",
                        phone = "+56 2 2631 5678",
                        lat = -33.6062,
                        lng = -70.8765,
                        onClickCard = { onNavigateToMap() }
                    )
                    LocationBannerCard(
                        name = "PEÑAFLOR 2",
                        address = "Miraflores 3560",
                        phone = "+56 2 2912 3456",
                        lat = -33.5980,
                        lng = -70.8920,
                        onClickCard = { onNavigateToMap() }
                    )
                }
            }
        }

        // My Bookings list
        item {
            Text(
                text = "Tus Agendamientos y Reservas",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GoldPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        if (loggedInUser == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Info, null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Inicia sesión para ver tu historial de agendamientos y recibir los correos electrónicos",
                            textAlign = TextAlign.Center,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else if (userAppointments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Event, null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aún no tienes ninguna reserva agendada.",
                            textAlign = TextAlign.Center,
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestBooking,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Text("Reserva tu primera hora", color = DarkBackground)
                        }
                    }
                }
            }
        } else {
            items(userAppointments) { appt ->
                AppointmentRowItem(appt = appt, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LocationBannerCard(
    name: String,
    address: String,
    phone: String,
    lat: Double = -33.6635,
    lng: Double = -70.9272,
    onClickCard: () -> Unit = {}
) {
    val context = LocalContext.current
    Card(
        onClick = onClickCard,
        modifier = Modifier
            .width(230.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(name, fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 13.sp)
                }
                IconButton(
                    onClick = {
                        openGoogleMaps(context, lat, lng, "Big Boys - $name")
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Directions,
                        contentDescription = "Abrir Google Maps",
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(address, color = TextMuted, fontSize = 11.sp, maxLines = 1)
            Text("Tel: $phone", color = TextIvory, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CHILE 🇨🇱", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Ver en Maps →", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppointmentRowItem(
    appt: Appointment,
    viewModel: BarberiaViewModel
) {
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var policyNoticeMessage by remember { mutableStateOf<String?>(null) }

    val hoursLeft = remember(appt.dateTime) { AppFormatters.hoursUntilAppointment(appt.dateTime) }
    val isMoreThan24h = hoursLeft >= 24.0
    val canReschedule = (appt.status == "Confirmada" || appt.status == "Pendiente") && isMoreThan24h && appt.rescheduleCount < 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header Row: Service Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ReceiptLong, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(appt.serviceName, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 14.sp)
                }

                val isExpired = appt.status == "Caducada" || appt.status == "Vencida"
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                appt.status == "Confirmada" -> GreenConfirm.copy(alpha = 0.15f)
                                appt.status == "Completada" -> GoldPrimary.copy(alpha = 0.15f)
                                isExpired -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                appt.status == "No-Show" -> Color(0xFFF97316).copy(alpha = 0.2f)
                                appt.status.contains("Retenido") -> RedCancel.copy(alpha = 0.2f)
                                else -> RedCancel.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            isExpired -> "VENCIDA / CADUCADA"
                            appt.status == "No-Show" -> "NO-SHOW (ABONO RETENIDO)"
                            appt.status == "Cancelada (Abono Retenido)" -> "ANULADA (<24H ABONO RETENIDO)"
                            else -> appt.status.uppercase()
                        },
                        color = when {
                            appt.status == "Confirmada" -> GreenConfirm
                            appt.status == "Completada" -> GoldPrimary
                            isExpired -> Color(0xFFEF4444)
                            appt.status == "No-Show" -> Color(0xFFF97316)
                            else -> RedCancel
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date and Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${appt.dateTime} hrs",
                    color = TextIvory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (appt.status == "Confirmada") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMoreThan24h) "(${String.format(java.util.Locale.US, "%.0f", hoursLeft)}h restantes)" else "(< 24h para atención)",
                        color = if (isMoreThan24h) GreenConfirm else Color(0xFFF97316),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (appt.status == "Caducada" || appt.status == "Vencida") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(Horario ya transcurrido)",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Breakdown: Abono descontado y saldo en caja
            val remainingInShop = (appt.servicePrice - appt.prepayAmount).coerceAtLeast(0.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B1F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Servicio: $${formatClp(appt.servicePrice)} CLP", fontSize = 11.sp, color = TextMuted)
                    Text("💳 Abono pagado: -$${formatClp(appt.prepayAmount)} CLP (Descontado)", fontSize = 11.sp, color = GreenConfirm, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo a pagar en local:", fontSize = 10.sp, color = TextMuted)
                    Text("$${formatClp(remainingInShop)} CLP", fontSize = 13.sp, color = GoldPrimary, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Reschedule counter
            if (appt.rescheduleCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔄 Reagendamientos utilizados: ${appt.rescheduleCount}/2 permitidos con este abono",
                    color = GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Interactive Actions for active appointments
            if (appt.status == "Confirmada" || appt.status == "Pendiente") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button Reagendar
                    Button(
                        onClick = {
                            if (canReschedule) {
                                showRescheduleDialog = true
                            } else {
                                val reason = if (!isMoreThan24h) {
                                    "No es posible reagendar: Faltan menos de 24 horas para tu servicio. Para liberar la agenda comercial, la política exige reagendar con al menos 24h de anticipación."
                                } else {
                                    "Límite alcanzado: Ya utilizaste el máximo de 2 reagendamientos permitidos con tu abono inicial de $3.000."
                                }
                                policyNoticeMessage = reason
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canReschedule) GoldPrimary else Color(0xFF27272A)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_reschedule_${appt.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            null,
                            tint = if (canReschedule) DarkBackground else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Reagendar",
                            color = if (canReschedule) DarkBackground else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Button Cancelar
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        border = BorderStroke(1.dp, RedCancel),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_cancel_client_${appt.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Close, null, tint = RedCancel, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Anular Cita", color = RedCancel, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            } else if (appt.status == "Caducada" || appt.status == "Vencida") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TimerOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Cita caducada: La fecha y hora de la cita ya transcurrió sin ser atendida o cancelada previamente.",
                        fontSize = 11.sp,
                        color = TextIvory
                    )
                }
            }
        }
    }

    // Policy Notice Dialog
    if (policyNoticeMessage != null) {
        AlertDialog(
            onDismissRequest = { policyNoticeMessage = null },
            title = { Text("Política de Reserva y Abonos", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 15.sp) },
            text = { Text(policyNoticeMessage!!, color = TextIvory, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { policyNoticeMessage = null }) {
                    Text("Entendido", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }

    // Cancel Appointment Dialog (Checks 24h rule)
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    if (isMoreThan24h) "¿Anular cita con anticipación?" else "⚠️ Anulación dentro de las 24 horas",
                    fontWeight = FontWeight.Bold,
                    color = if (isMoreThan24h) TextIvory else RedCancel,
                    fontSize = 15.sp
                )
            },
            text = {
                Column {
                    Text("Cita: ${appt.serviceName} el ${appt.dateTime} hrs", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isMoreThan24h) {
                        Text(
                            text = "Faltan más de 24 horas para tu servicio. Tu agenda quedará liberada y mantendrás tu saldo a favor de $${formatClp(appt.prepayAmount)} CLP para tu próxima atención en Big boys.",
                            color = TextIvory,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Faltan menos de 24 horas para tu servicio. Según la política oficial de no-show y bloqueo de agenda, al anular ahora perderás tu abono de $${formatClp(appt.prepayAmount)} CLP o saldo a favor.",
                            color = RedCancel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelAppointmentByClient(appt) { _, _ -> }
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Confirmar Anulación", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Volver", color = TextMuted)
                }
            },
            containerColor = Color(0xFF1E1E24)
        )
    }

    // Reschedule Dialog
    if (showRescheduleDialog) {
        RescheduleDialog(
            appointment = appt,
            viewModel = viewModel,
            onDismiss = { showRescheduleDialog = false }
        )
    }
}

@Composable
fun RescheduleDialog(
    appointment: Appointment,
    viewModel: BarberiaViewModel,
    onDismiss: () -> Unit
) {
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val allAppointments by viewModel.allAppointmentsFlow.collectAsState()
    val barber = remember(barbers, appointment.barberId) { barbers.find { it.id == appointment.barberId } }

    var selectedDate by remember { mutableStateOf(AppFormatters.currentDateString()) }
    var selectedHour by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val hoursAvailable = remember(barber) {
        barber?.availableHours?.split(",")?.map { it.trim() } ?: listOf("10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00", "18:00")
    }

    val takenHours = remember(selectedDate, appointment.barberId, allAppointments) {
        allAppointments.filter { appt ->
            appt.barberId == appointment.barberId &&
            AppFormatters.isSameDay(appt.dateTime, selectedDate) &&
            !appt.status.equals("Cancelada", ignoreCase = true) &&
            appt.id != appointment.id
        }.map { AppFormatters.extractHourSlot(it.dateTime) }.toSet()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Reagendar Cita 🗓️", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 16.sp)
                Text(
                    text = "Abono de $3.000 CLP conservado (Reagendamiento ${appointment.rescheduleCount + 1} de 2)",
                    fontSize = 11.sp,
                    color = GreenConfirm,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker
                Text("Selecciona Nueva Fecha:", fontSize = 12.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                RealTimeAppointmentCalendar(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hour Picker
                Text("Horas Disponibles para $selectedDate:", fontSize = 12.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                val hourRows = remember(hoursAvailable) { hoursAvailable.chunked(3) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    hourRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { hour ->
                                val isTaken = takenHours.contains(hour)
                                val isSelected = selectedHour == hour
                                Card(
                                    onClick = { if (!isTaken) selectedHour = hour },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isTaken -> Color(0xFF2B1D1D)
                                            isSelected -> GoldPrimary
                                            else -> DarkBackground
                                        }
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isTaken -> RedCancel.copy(alpha = 0.3f)
                                            isSelected -> GoldPrimary
                                            else -> Color.White.copy(alpha = 0.15f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isTaken) "🔒 $hour" else hour,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isTaken -> RedCancel
                                                isSelected -> DarkBackground
                                                else -> TextIvory
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = RedCancel, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedHour.isBlank()) {
                                errorMessage = "Por favor selecciona un horario disponible."
                                return@Button
                            }
                            isSubmitting = true
                            viewModel.rescheduleAppointment(appointment, selectedDate, selectedHour) { success, msg ->
                                isSubmitting = false
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = !isSubmitting && selectedHour.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Confirmar Cambio", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- BARBERS SCREEN ---
@Composable
fun BarbersScreen(
    viewModel: BarberiaViewModel,
    onBookWithBarber: (Barber) -> Unit
) {
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLocationFilter by remember { mutableStateOf<Int?>(null) }

    val filteredBarbers = remember(barbers, searchQuery, selectedLocationFilter) {
        barbers.filter { barber ->
            val matchesSearch = barber.name.contains(searchQuery, ignoreCase = true) ||
                    barber.specialty.contains(searchQuery, ignoreCase = true)
            val matchesLocation = selectedLocationFilter == null || barber.locationId == selectedLocationFilter
            matchesSearch && matchesLocation
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Equipo de Barberos Big boys 💈",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
            color = GoldPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Filters UI
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por nombre o especialidad") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = GoldPrimary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedLabelColor = GoldPrimary,
                unfocusedLabelColor = TextMuted,
                focusedIndicatorColor = GoldPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Location Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLocationFilter == null,
                onClick = { selectedLocationFilter = null },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = DarkBackground,
                    containerColor = CardBackground,
                    labelColor = TextIvory
                )
            )

            locations.forEach { loc ->
                FilterChip(
                    selected = selectedLocationFilter == loc.id,
                    onClick = { selectedLocationFilter = loc.id },
                    label = { Text(loc.name.replace("Big Boys - ", "")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = DarkBackground,
                        containerColor = CardBackground,
                        labelColor = TextIvory
                    )
                )
            }
        }

        if (filteredBarbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PersonOff, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No coinciden barberos con tu criterio", color = TextMuted)
                }
            }
        } else {
            var selectedBarberForDetail by remember { mutableStateOf<Barber?>(null) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredBarbers) { barber ->
                    BarberCardItem(
                        barber = barber,
                        locationName = locations.find { it.id == barber.locationId }?.name?.replace("Big Boys - ", "") ?: "Chile",
                        onBook = { onBookWithBarber(barber) },
                        onRate = { rating -> viewModel.rateBarber(barber.id, rating) },
                        onClickDetails = { selectedBarberForDetail = barber }
                    )
                }
            }

            val barberDetail = selectedBarberForDetail
            if (barberDetail != null) {
                BarberDetailsDialog(
                    barber = barberDetail,
                    locationName = locations.find { it.id == barberDetail.locationId }?.name ?: "Big Boys Chile",
                    onDismiss = { selectedBarberForDetail = null },
                    onBook = {
                        onBookWithBarber(barberDetail)
                        selectedBarberForDetail = null
                    }
                )
            }
        }
    }
}

@Composable
fun BarberDetailsDialog(
    barber: Barber,
    locationName: String,
    onDismiss: () -> Unit,
    onBook: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, "Cerrar", tint = TextMuted)
                    }
                }

                Box(modifier = Modifier.size(96.dp)) {
                    AsyncImage(
                        model = barber.avatarUrl,
                        contentDescription = "Foto de ${barber.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, GoldPrimary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF2ECC71), CircleShape)
                            .border(2.dp, CardBackground, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = barber.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = TextIvory,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = barber.specialty,
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Divider(color = GoldPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "🚀 TRAYECTORIA Y BIOGRAFÍA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = barber.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📍 UBICACIÓN PRINCIPAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(locationName, color = TextIvory, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⭐ CALIFICACIÓN GLOBAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${barber.rating} de 5.0",
                            fontWeight = FontWeight.Bold,
                            color = TextIvory,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("(Basado en las últimas 45 opiniones con toalla caliente)", color = TextMuted, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🕒 HORARIOS DISPONIBLES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val hours = barber.availableHours.split(",").take(4)
                        hours.forEach { hour ->
                            Box(
                                modifier = Modifier
                                    .background(CardBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            ) {
                                Text(hour, color = TextIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (barber.availableHours.split(",").size > 4) {
                            Box(
                                modifier = Modifier
                                    .background(CardBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            ) {
                                Text("+${barber.availableHours.split(",").size - 4}", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBook,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESERVAR SESIÓN EN ESTE LOCAL", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun BarberCardItem(
    barber: Barber,
    locationName: String,
    onBook: () -> Unit,
    onRate: (Float) -> Unit,
    onClickDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetails() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp)
            ) {
                AsyncImage(
                    model = barber.avatarUrl,
                    contentDescription = barber.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(0xFF2ECC71), CircleShape)
                        .border(2.dp, CardBackground, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = barber.name,
                fontWeight = FontWeight.ExtraBold,
                color = TextIvory,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = locationName.uppercase(),
                    color = GoldAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = barber.specialty,
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, "Rating", tint = GoldAccent, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(barber.rating.toString(), fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 11.sp)
                Text(" | ", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = "${barber.availableHours.split(",").size} HORAS",
                    color = GoldPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    // Prevent Clickable from invoking parent detail trigger
                    onBook()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("RESERVAR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Calificar: ",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Nota $star",
                            tint = if (star <= barber.rating.toInt()) GoldAccent else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRate(star.toFloat()) }
                                .padding(horizontal = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- HISTORY & PROFILE SCREEN ---
@Composable
fun HistoryScreen(
    viewModel: BarberiaViewModel,
    onGoToBooking: () -> Unit,
    onOpenProfile: () -> Unit = {}
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val user = loggedInUser
    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    val allBarbers by viewModel.allBarbersFlow.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var reviewingAppointment by remember { mutableStateOf<Appointment?>(null) }

    val userActiveAppointments = remember(appointments, user) {
        if (user == null) emptyList()
        else appointments.filter { appt ->
            val matchesId = (appt.userId != null && appt.userId == user.id)
            val matchesEmail = user.email.isNotBlank() && appt.customerEmail.trim().equals(user.email.trim(), ignoreCase = true)
            val cleanApptPhone = appt.customerPhone.replace(" ", "").replace("+56", "").replace("-", "")
            val cleanUserPhone = user.phone.replace(" ", "").replace("+56", "").replace("-", "")
            val matchesPhone = cleanUserPhone.isNotBlank() && cleanApptPhone.isNotBlank() && cleanApptPhone == cleanUserPhone
            (matchesId || matchesEmail || matchesPhone) && (appt.status == "Confirmada" || appt.status == "Pendiente")
        }
    }

    val userHistory = remember(appointments, user) {
        if (user == null) emptyList()
        else appointments.filter { appt ->
            val matchesId = (appt.userId != null && appt.userId == user.id)
            val matchesEmail = user.email.isNotBlank() && appt.customerEmail.trim().equals(user.email.trim(), ignoreCase = true)
            val cleanApptPhone = appt.customerPhone.replace(" ", "").replace("+56", "").replace("-", "")
            val cleanUserPhone = user.phone.replace(" ", "").replace("+56", "").replace("-", "")
            val matchesPhone = cleanUserPhone.isNotBlank() && cleanApptPhone.isNotBlank() && cleanApptPhone == cleanUserPhone
            (matchesId || matchesEmail || matchesPhone) && (
                appt.status == "Completada" || 
                appt.status == "Cancelada" || 
                appt.status.contains("Cancelada") || 
                appt.status == "Caducada" || 
                appt.status == "Vencida" || 
                appt.status == "No-Show"
            )
        }
    }

    if (user != null) {
        // Logged-in profile view
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (user.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        user.fullName.firstOrNull()?.toString()?.uppercase() ?: "",
                                        color = if (isDarkMode) DarkBackground else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Cliente Exclusivo Big Boys", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onOpenProfile,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("history_edit_profile_btn")
                            ) {
                                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Perfil", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text("📞 Contacto: ${user.phone.ifBlank { "Sin número registrado" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("✉️ Correo: ${user.email}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🇨🇱 Membresía Activa en Sucursales Chile", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active Appointments Section in Profile/History
            if (userActiveAppointments.isNotEmpty()) {
                item {
                    Text(
                        "Tus Citas Confirmadas Activas 💈",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(userActiveAppointments) { appt ->
                    AppointmentRowItem(appt = appt, viewModel = viewModel)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tus Servicios Históricos Realizados 🕰️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (userHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No registras ningún servicio completado históricamente en Big boys.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(userHistory) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barberName = allBarbers.find { it.id == item.barberId }?.name ?: "Barbero Big Boys"
                                val locationName = locations.find { it.id == item.locationId }?.name ?: "Sucursal Chile"
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.serviceName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                    Text("Barbero: $barberName · $locationName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Fecha de atención: ${item.dateTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Anticipo abonado: $${formatClp(item.prepayAmount)} CLP", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$${formatClp(item.servicePrice)} CLP",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val isExpired = item.status == "Caducada" || item.status == "Vencida"
                                    val isNoShow = item.status == "No-Show"
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            item.status == "Completada" -> GreenConfirm.copy(alpha = 0.15f)
                                            isExpired -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                            isNoShow -> Color(0xFFF97316).copy(alpha = 0.2f)
                                            else -> RedCancel.copy(alpha = 0.15f)
                                        },
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                item.status == "Completada" -> "Completada ✓"
                                                isExpired -> "Caducada / Vencida ⏱"
                                                isNoShow -> "No-Show (Abono Retenido)"
                                                else -> "Cancelada ✕"
                                            },
                                            color = when {
                                                item.status == "Completada" -> GreenConfirm
                                                isExpired -> Color(0xFFEF4444)
                                                isNoShow -> Color(0xFFF97316)
                                                else -> RedCancel
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Review Section for completed appointments
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.hasReview) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "¡Ya dejaste tu reseña por este servicio!",
                                            fontSize = 12.sp,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Text(
                                        "¿Cómo estuvo tu corte?",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { reviewingAppointment = item },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = if (isDarkMode) DarkBackground else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("review_service_btn_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.Star, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Dejar Reseña", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Review Appointment Dialog
        if (reviewingAppointment != null) {
            val appt = reviewingAppointment!!
            ReviewAppointmentDialog(
                appointment = appt,
                onDismiss = { reviewingAppointment = null },
                onSubmit = { rating, comment, tags ->
                    viewModel.submitAppointmentReview(appt, rating, comment, tags) { success, _ ->
                        if (success) {
                            reviewingAppointment = null
                        }
                    }
                }
            )
        }
    } else {
        // Since they aren't logged in as client but are in the app, they must be Admin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, "Admin", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Navegando como Administrador",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Estás en modo administrativo (🛡️). Puedes gestionar citas, barberos, sucursales y personalizar tu perfil de administrador con PIN y Foto.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onOpenProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = if (isDarkMode) DarkBackground else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.ManageAccounts, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Personalizar Perfil de Administrador", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- BOOKING WIZARD DIALOG ---
@Composable
fun BookingWizardDialog(
    viewModel: BarberiaViewModel,
    onDismiss: () -> Unit
) {
    val locations by viewModel.locationsFlow.collectAsState()
    val allBarbers by viewModel.allBarbersFlow.collectAsState()
    val allAppointments by viewModel.allAppointmentsFlow.collectAsState()

    val bookingLocation by viewModel.bookingLocation.collectAsState()
    val bookingBarber by viewModel.bookingBarber.collectAsState()
    val bookingDate by viewModel.bookingDate.collectAsState()
    val bookingHour by viewModel.bookingHour.collectAsState()
    val bookingService by viewModel.bookingService.collectAsState()
    val bookingPrice by viewModel.bookingPrice.collectAsState()
    val prepayVal by viewModel.prepaymentAmount.collectAsState()

    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val user = loggedInUser

    val guestName by viewModel.guestName.collectAsState()
    val guestPhone by viewModel.guestPhone.collectAsState()
    val guestEmail by viewModel.guestEmail.collectAsState()

    // Elegant initial step detection depending on whether a location or barber was pre-selected
    var step by remember {
        mutableIntStateOf(
            when {
                bookingBarber != null -> 3
                bookingLocation != null -> 2
                else -> 1
            }
        )
    }

    // Fake Payment Platform screen overlay triggers
    var isSimulatingPaymentWindow by remember { mutableStateOf(false) }

    val servicesAvailable = listOf(
        Triple("Corte de Pelo Degrado", 15000.0, "45 min"),
        Triple("Arreglo de Barba Royale", 10000.0, "30 min"),
        Triple("Combo Big Boys (Pelo + Barba)", 22000.0, "60 min"),
        Triple("Servicio Completo VIP", 28000.0, "80 min"),
        Triple("Corte Clásico Militar", 13500.0, "40 min"),
        Triple("Ritual de Barba & Barbería", 22000.0, "50 min")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (!isSimulatingPaymentWindow) {
                    // Title Header with Step description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agendar Cita (Paso $step de 5)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = GoldAccent
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Cerrar", tint = TextMuted)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { step.toFloat() / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        color = GoldPrimary,
                        trackColor = DarkBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        when (step) {
                            1 -> {
                                // Step 1: Select Location (Sucursal)
                                Column {
                                    Text("Paso 1: Selecciona la Sucursal", fontWeight = FontWeight.Bold, color = TextIvory)
                                    Text("Elige la sucursal de Big boys donde deseas atenderte", fontSize = 12.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(locations) { loc ->
                                            val branchBarbersCount = remember(loc.id, allBarbers) {
                                                allBarbers.count { it.locationId == loc.id }
                                            }
                                            val isSelected = bookingLocation?.id == loc.id
                                            Card(
                                                onClick = {
                                                    viewModel.selectLocation(loc)
                                                    step = 2
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("wizard_location_card_${loc.id}"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkBackground
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.15f)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .background(CardBackground, CircleShape)
                                                            .border(1.dp, if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.4f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Filled.Storefront, "Sucursal", tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(loc.name, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 14.sp)
                                                        Text("📍 ${loc.address}", fontSize = 11.sp, color = TextMuted)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("💈 $branchBarbersCount barberos disponibles", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                                            if (loc.phone.isNotBlank()) {
                                                                Text("📞 ${loc.phone}", fontSize = 10.sp, color = TextMuted)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Step 2: Select Barber STRICTLY filtered by the chosen location
                                val branchBarbers = remember(bookingLocation, allBarbers) {
                                    if (bookingLocation != null) {
                                        allBarbers.filter { it.locationId == bookingLocation?.id }
                                    } else {
                                        allBarbers
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Paso 2: Selecciona tu Barbero",
                                        fontWeight = FontWeight.Bold,
                                        color = TextIvory
                                    )
                                    Text(
                                        text = "Barberos disponibles en ${bookingLocation?.name ?: "la sucursal elegida"}",
                                        fontSize = 12.sp,
                                        color = GoldAccent
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (branchBarbers.isEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Filled.PersonOff, null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "No hay barberos asignados a esta sucursal actualmente.",
                                                    fontSize = 12.sp,
                                                    color = TextMuted,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(branchBarbers) { barber ->
                                                val isSelected = bookingBarber?.id == barber.id
                                                Card(
                                                    onClick = {
                                                        viewModel.selectBarber(barber)
                                                        step = 3
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("wizard_barber_card_${barber.id}"),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkBackground
                                                    ),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.1f)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(42.dp)
                                                                .background(CardBackground, CircleShape)
                                                                .border(1.dp, if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.4f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Filled.Person, null, tint = GoldPrimary)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(barber.name, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 13.sp)
                                                            Text(barber.specialty, fontSize = 11.sp, color = TextMuted)
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Filled.Star, null, tint = GoldAccent, modifier = Modifier.size(11.dp))
                                                                Spacer(modifier = Modifier.width(2.dp))
                                                                Text(barber.rating.toString(), fontSize = 11.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text("🕒 Horarios: ${barber.availableHours}", fontSize = 10.sp, color = TextMuted, maxLines = 1)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // Step 3: Select Service
                                Column {
                                    Text("Paso 3: Selecciona el Servicio", fontWeight = FontWeight.Bold, color = TextIvory)
                                    Text("Elige el tratamiento o corte que deseas agendar", fontSize = 12.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(servicesAvailable) { svc ->
                                            val isSelected = bookingService == svc.first
                                            Card(
                                                onClick = {
                                                    viewModel.selectService(svc.first, svc.second)
                                                    step = 4
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkBackground
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.1f)
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = 10.dp)
                                                    ) {
                                                        Text(
                                                            text = svc.first,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextIvory
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${svc.third} • Distinción Big Boys",
                                                            fontSize = 11.sp,
                                                            color = TextMuted
                                                        )
                                                    }
                                                    Text(
                                                        text = "$${formatClp(svc.second)} CLP",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = GoldAccent,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // Step 4: DateTime selector with Real-time interactive Calendar
                                val hoursAvailable = remember(bookingBarber) {
                                    bookingBarber?.availableHours?.split(",")?.map { it.trim() } ?: emptyList()
                                }
                                val takenHours = remember(bookingBarber, bookingDate, allAppointments) {
                                    allAppointments.filter { appt ->
                                        appt.barberId == bookingBarber?.id &&
                                        AppFormatters.isSameDay(appt.dateTime, bookingDate) &&
                                        !appt.status.equals("Cancelada", ignoreCase = true)
                                    }.map { appt ->
                                        AppFormatters.extractHourSlot(appt.dateTime)
                                    }.toSet()
                                }
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text("Paso 4: Selecciona Fecha y Hora", fontWeight = FontWeight.Bold, color = TextIvory)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Real-Time Calendar Component
                                    RealTimeAppointmentCalendar(
                                        selectedDate = bookingDate,
                                        onDateSelected = { date ->
                                            viewModel.selectDate(date)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Horario para la fecha:", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                        if (takenHours.isNotEmpty()) {
                                            Text("🔒 ${takenHours.size} hora(s) ocupada(s)", fontSize = 10.sp, color = RedCancel)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val hourRows = remember(hoursAvailable) { hoursAvailable.chunked(3) }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        hourRows.forEach { rowHours ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowHours.forEach { hour ->
                                                    val isTaken = takenHours.contains(hour)
                                                    val isSelected = bookingHour == hour
                                                    Card(
                                                        onClick = {
                                                            if (!isTaken) {
                                                                viewModel.selectHour(hour)
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = when {
                                                                isTaken -> Color(0xFF201616)
                                                                isSelected -> GoldPrimary
                                                                else -> DarkBackground
                                                            }
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            when {
                                                                isTaken -> RedCancel.copy(alpha = 0.3f)
                                                                isSelected -> GoldPrimary
                                                                else -> GoldPrimary.copy(alpha = 0.4f)
                                                            }
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    text = hour,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = when {
                                                                        isTaken -> TextMuted.copy(alpha = 0.6f)
                                                                        isSelected -> DarkBackground
                                                                        else -> TextIvory
                                                                    }
                                                                )
                                                                if (isTaken) {
                                                                    Text(
                                                                        text = "Ocupado",
                                                                        fontSize = 8.sp,
                                                                        color = RedCancel,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                // Fill empty spaces if row has fewer than 3 items
                                                for (emptySlot in rowHours.size until 3) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            5 -> {
                                // Step 5: Contact details (Guest mode check) & Prepayment recap WITH complete reservation summary
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text("Paso 5: Detalles de Reserva & Pago", fontWeight = FontWeight.Bold, color = TextIvory)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // --- CRITICAL RESUMEN DE LA RESERVA (USER REQUIREMENT) ---
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A16)), // warm slate custom styling
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.List,
                                                    contentDescription = "Resumen",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "RESUMEN DE TU AGENDAMIENTO",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = GoldAccent
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))

                                            // 1. Día y Hora de Agenda
                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("🕰️  ", fontSize = 13.sp)
                                                Column {
                                                    Text("DÍA Y HORA AGENDADA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                    Text("${bookingDate} a las ${bookingHour} hrs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // 2. Barbero
                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("👤  ", fontSize = 13.sp)
                                                Column {
                                                    Text("BARBERO MULTIDISCIPLINAR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                    Text(bookingBarber?.name ?: "No seleccionado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                                    Text(bookingBarber?.specialty ?: "", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // 3. Local (Sucursal)
                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("📍  ", fontSize = 13.sp)
                                                Column {
                                                    Text("LOCAL / SUCURSAL:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                    Text(bookingLocation?.name ?: "No seleccionado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                                    Text(bookingLocation?.address ?: "", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            // 4. Servicio
                                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                                Text("✂️  ", fontSize = 13.sp)
                                                Column {
                                                    Text("SERVICIO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                    Text(bookingService, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                                    Text("Costo comercial: $${formatClp(bookingPrice)} CLP", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    if (loggedInUser == null) {
                                        // Guest required inputs
                                        Text("Estás agendando en modo Invitado. Por favor ingresa tus datos:", fontSize = 11.sp, color = GoldAccent)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        OutlinedTextField(
                                            value = guestName,
                                            onValueChange = { viewModel.updateGuestDetails(it, guestPhone, guestEmail) },
                                            label = { Text("Nombre Completo") },
                                            colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                                .testTag("guest_name_field")
                                        )

                                        OutlinedTextField(
                                            value = guestPhone,
                                            onValueChange = { viewModel.updateGuestDetails(guestName, it, guestEmail) },
                                            label = { Text("Número Telefónico") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                                .testTag("guest_phone_field")
                                        )
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Cliente: ${user?.fullName ?: ""}", fontWeight = FontWeight.Bold)
                                                Text("Fono: ${user?.phone ?: ""}", color = TextMuted, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Prepayment warning
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1815)),
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Payment, "Abono", tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("POLÍTICA DE ABONO ($3.000 CLP)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = GoldAccent)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "• Abono de $3.000 CLP: Se descuenta del total del servicio al pagar en el local (saldo en caja: $${formatClp((bookingPrice - prepayVal).coerceAtLeast(0.0))} CLP).\n• Reagendamiento: Solo podrás reagendar con al menos 24 horas de anticipación para liberar la agenda, usando este mismo abono hasta un máximo de 2 veces.\n• Anulaciones / No-Show: Si anulas con menos de 24 horas o no asistes (no-show), perderás tu bonificación o saldo a favor.",
                                                fontSize = 11.sp,
                                                color = TextIvory,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog bottom Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step-- },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Text("Atrás")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (step < 5) {
                            Button(
                                onClick = {
                                    if (step == 1 && bookingLocation != null) step++
                                    else if (step == 2 && bookingBarber != null) step++
                                    else if (step == 3 && bookingService.isNotBlank()) step++
                                    else if (step == 4 && bookingDate.isNotBlank() && bookingHour.isNotBlank()) step++
                                },
                                enabled = (step == 1 && bookingLocation != null) ||
                                        (step == 2 && bookingBarber != null) ||
                                        (step == 3 && bookingService.isNotBlank()) ||
                                        (step == 4 && bookingDate.isNotBlank() && bookingHour.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("Continuar", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val readyToPay = if (loggedInUser != null) true
                            else guestName.isNotBlank() && guestPhone.isNotBlank()

                            Column(horizontalAlignment = Alignment.End) {
                                if (!readyToPay) {
                                    Text(
                                        text = "⚠️ Ingresa tu nombre y teléfono para agendar",
                                        color = RedCancel,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            if (readyToPay) {
                                                isSimulatingPaymentWindow = true
                                            }
                                        },
                                        enabled = readyToPay,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                                        border = BorderStroke(1.dp, if (readyToPay) GoldPrimary else TextMuted.copy(alpha = 0.3f))
                                    ) {
                                        Text("Webpay", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (readyToPay) {
                                                viewModel.confirmAndBook {
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        enabled = readyToPay,
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                        modifier = Modifier.testTag("book_submit_final")
                                    ) {
                                        Text("CONFIRMAR CITA 💈", color = DarkBackground, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // WEBPAY / PAYMENTS WINDOW SIMULATOR
                    WebpaySimulationScreen(
                        serviceName = bookingService,
                        amountToPay = prepayVal,
                        onPaymentSuccess = {
                            viewModel.confirmAndBook {
                                onDismiss()
                            }
                        },
                        onPaymentCancel = {
                            isSimulatingPaymentWindow = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WebpaySimulationScreen(
    serviceName: String,
    amountToPay: Double,
    onPaymentSuccess: () -> Unit,
    onPaymentCancel: () -> Unit
) {
    // State indicators
    var currentWebpayStep by remember { mutableStateOf(1) } // 1: Select Bank/RUT, 2: Redirect, 3: Bank Login Portal, 4: Transbank Processing, 5: Success Ticket
    
    // Bank & form details
    val banks = remember {
        listOf(
            Pair("BancoEstado", Color(0xFF003366)), // Traditional Orange/Blue
            Pair("Banco de Chile / Edwards", Color(0xFF0E1E38)), // Navy
            Pair("Santander", Color(0xFFCC0000)), // Red
            Pair("BCI / TBanc", Color(0xFF1E351A)), // Dark Green
            Pair("Itaú", Color(0xFFEC7000)), // Orange
            Pair("Banco Falabella", Color(0xFFCAD822)), // Lime
            Pair("Scotiabank Chile", Color(0xFFE51D24)), // Bright Red
            Pair("Banco BICE", Color(0xFF121824)), // Slate Dark
            Pair("Coopeuch", Color(0xFF007FA3)) // Blueish green
        )
    }
    
    var selectedBankOrdinal by remember { mutableIntStateOf(0) }
    var userRut by remember { mutableStateOf("") }
    var paymentMethodType by remember { mutableStateOf("Débito (Redcompra)") } // Débito, Crédito, Prepago
    
    // Gateway security credentials
    var bankPassword by remember { mutableStateOf("") }
    var securityToken3rdFactor by remember { mutableStateOf("") }
    
    // Automated transaction identifiers
    val transactionToken = remember { "TSB_" + UUID.randomUUID().toString().substring(0, 8).uppercase() }
    val authCode = remember { (100000 + Random().nextInt(899999)).toString() }
    val buyOrder = remember { "BO-" + (20000 + Random().nextInt(79999)).toString() }
    
    val selectedBankName = banks[selectedBankOrdinal].first
    val selectedBankColor = banks[selectedBankOrdinal].second

    // Async simulated delay triggers
    if (currentWebpayStep == 2) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            currentWebpayStep = 3
        }
    }
    if (currentWebpayStep == 4) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            currentWebpayStep = 5
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Red Top Header Representing official Webpay Plus transbank
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, "Secure", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "webpayplus",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    " | Transbank",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "100% SEGURO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // STATE MACHINE
        when (currentWebpayStep) {
            1 -> {
                // STEP 1: SELECT BANK, RUT & PAYMENT TYPE
                Column {
                    // Summary card in transbank
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Orden de Compra:", fontSize = 11.sp, color = TextMuted)
                                Text(buyOrder, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Comercio:", fontSize = 11.sp, color = TextMuted)
                                Text("Club Big Boys Chile S.A.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Servicio:", fontSize = 11.sp, color = TextMuted)
                                Text(serviceName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Valor Abono:", fontSize = 12.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                                Text(
                                    "$${formatClp(amountToPay)} CLP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }

                    // Select Payment Method Tab
                    Text("Medio de Pago:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Débito (Redcompra)", "Crédito", "Prepago").forEach { mode ->
                            val isSelected = paymentMethodType == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) GoldPrimary else Color(0xFF27272A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) GoldAccent else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { paymentMethodType = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode,
                                    color = if (isSelected) DarkBackground else TextIvory,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input RUT
                    val isWebpayRutValid = ValidationUtils.isValidChileanRut(userRut)
                    OutlinedTextField(
                        value = userRut,
                        onValueChange = { input ->
                            // Clean entry and allow only digits, 'k', 'K', '.', '-' and spaces
                            val cleanInput = input.filter { it.isDigit() || it == 'k' || it == 'K' || it == '-' || it == '.' || it == ' ' }
                            userRut = cleanInput
                        },
                        label = { Text("Ingresa tu RUT (Ej: 19.342.110-3)") },
                        trailingIcon = {
                            if (userRut.isNotBlank()) {
                                Icon(
                                    imageVector = if (isWebpayRutValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (isWebpayRutValid) GreenConfirm else RedCancel
                                )
                            }
                        },
                        supportingText = {
                            if (userRut.isNotBlank() && !isWebpayRutValid) {
                                Text("RUT inválido (debe cumplir Módulo 11)", color = RedCancel, fontSize = 10.sp)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = if (userRut.isNotBlank() && !isWebpayRutValid) RedCancel else GoldPrimary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedLabelColor = GoldPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )

                    // Grid of Chilean Banks
                    Text("Selecciona tu Banco de Origen:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(modifier = Modifier.height(180.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(banks.size) { idx ->
                                val bName = banks[idx].first
                                val bColor = banks[idx].second
                                val isSelected = selectedBankOrdinal == idx
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) GoldPrimary.copy(alpha = 0.15f) else Color(0xFF18181B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldPrimary else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedBankOrdinal = idx }
                                        .padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(bColor, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            bName.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        bName,
                                        color = if (isSelected) GoldAccent else TextIvory,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, "Selected", tint = GoldAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPaymentCancel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                            border = BorderStroke(1.dp, RedCancel)
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (userRut.isNotBlank()) {
                                    currentWebpayStep = 2 // Move to redirecting
                                }
                            },
                            enabled = userRut.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("webpay_proceed_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm)
                        ) {
                            Text("Pagar", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            2 -> {
                // STEP 2: REDIRECTING ANIMATED SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "Redireccionando al portal seguro de su banco emisor...",
                        color = TextIvory,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        selectedBankName.uppercase(),
                        fontWeight = FontWeight.Black,
                        color = GoldAccent,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Servidor de Pago Encriptado SSL (HTTPS)", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
            3 -> {
                // STEP 3: INTERACTIVE BRANDED CHILEAN BANK AUTHORIZATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(1.dp, selectedBankColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        // Header color based on Selected Bank
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(selectedBankColor)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PORTAL SEGURO: $selectedBankName",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("SSL ACTIVADO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Para proceder con la autorización del cargo bancario de $${formatClp(amountToPay)} CLP, ingresa tus credenciales bancarias de seguridad.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("RUT Cliente:", fontSize = 10.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = userRut,
                                onValueChange = { userRut = it },
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = selectedBankColor, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                enabled = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Clave Internet / Pin de Acceso:", fontSize = 10.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = bankPassword,
                                onValueChange = { bankPassword = it },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = selectedBankColor, focusedContainerColor = DarkBackground).copy(
                                    focusedIndicatorColor = selectedBankColor
                                ),
                                placeholder = { Text("Mínimo 4 dígitos", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3rd Factor Token (e.g. BE Pass or Coord)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = selectedBankColor.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, selectedBankColor.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    val factorName = when {
                                        selectedBankName.contains("Estado") -> "Clave de Transferencias o BE Pass App"
                                        selectedBankName.contains("Santander") -> "SuperClave Dinámica de Santander"
                                        selectedBankName.contains("Chile") -> "Dispositivo Pinpass / Mi Pass"
                                        else -> "Código Electrónico / SMS PIN"
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Lock, null, tint = selectedBankColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(factorName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ingrese su token de confirmación de 4-6 dígitos enviado a su celular o pin dinámico:", fontSize = 9.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = securityToken3rdFactor,
                                            onValueChange = { securityToken3rdFactor = it },
                                            placeholder = { Text("Ej: 47820", fontSize = 11.sp) },
                                            colors = TextFieldDefaults.colors(focusedIndicatorColor = selectedBankColor, focusedContainerColor = DarkBackground),
                                            modifier = Modifier.weight(1f).height(46.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )

                                        Button(
                                            onClick = {
                                                securityToken3rdFactor = (10000 + Random().nextInt(89999)).toString()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = selectedBankColor)
                                        ) {
                                            Text("SMS Token", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentWebpayStep = 1 },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                    border = BorderStroke(1.dp, RedCancel)
                                ) {
                                    Text("Rechazar", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (bankPassword.isNotBlank() && securityToken3rdFactor.isNotBlank()) {
                                            currentWebpayStep = 4 // Move to Transbank processing success
                                        }
                                    },
                                    enabled = bankPassword.isNotBlank() && securityToken3rdFactor.isNotBlank(),
                                    modifier = Modifier.weight(1f).testTag("bank_auth_agree_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = selectedBankColor)
                                ) {
                                    Text("Autorizar Pago", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                // STEP 4: TRANSBANK CONCURRENT AUTHORIZATION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 35.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD32F2F), modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "Procesando transacción segura con Transbank...",
                        color = TextIvory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Validando con $selectedBankName en tiempo real...",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Token ID: $transactionToken",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            5 -> {
                // STEP 5: DETAILED RECEIPT AND SUCCESS VOUCHER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(GreenConfirm.copy(alpha = 0.2f), CircleShape)
                            .border(1.5.dp, GreenConfirm, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = GreenConfirm, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("¡TRANSACCIÓN EXITOSA!", fontWeight = FontWeight.Black, color = GreenConfirm, fontSize = 16.sp, letterSpacing = 0.5.sp)
                    Text("Tu reserva ha sido pre-pagada con éxito", fontSize = 11.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Comercio:", fontSize = 10.sp, color = TextMuted)
                                Text("Club Big Boys Chile Ltda.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RUT Comercio:", fontSize = 10.sp, color = TextMuted)
                                Text("RT-76.438.291-K", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Orden Compra:", fontSize = 10.sp, color = TextMuted)
                                Text(buyOrder, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Código Autorización:", fontSize = 10.sp, color = TextMuted)
                                Text(authCode, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Medio Pago:", fontSize = 10.sp, color = TextMuted)
                                Text("$paymentMethodType (banco: $selectedBankName)", fontSize = 10.sp, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ID Transacción:", fontSize = 10.sp, color = TextMuted)
                                Text(transactionToken, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextIvory)
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("MONTO RECAUDADO:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                Text(
                                    "$${formatClp(amountToPay)} CLP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GreenConfirm
                                )
                            }
                            Text("Fijación sujeta a contrato. Factura exenta de IVA.", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onPaymentSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("final_ticket_agree_button")
                    ) {
                        Text("COMPLETAR Y FINALIZAR AGENDAMIENTO ✂️", color = DarkBackground, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("🔐 Conexión segura encriptada por Transbank S.A. Chile", fontSize = 9.sp, color = TextMuted)
    }
}

// --- ADMINISTRATIVE SECTION MAIN VIEW & DEDICATED ADMIN DASHBOARD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: BarberiaViewModel,
    onLogout: () -> Unit
) {
    var adminSubTab by remember { mutableStateOf("agenda") }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    val adminName by viewModel.adminName.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allAppointments by viewModel.allAppointmentsFlow.collectAsState()
    val allBarbers by viewModel.allBarbersFlow.collectAsState()
    val gmailPass by viewModel.gmailAppPassword.collectAsState()

    val totalAppointmentsCount = remember(allAppointments) { allAppointments.size }
    val pendingOrConfirmedCount = remember(allAppointments) {
        allAppointments.count { it.status == "Confirmada" || it.status == "Pendiente" }
    }
    val activeBarbersCount = remember(allBarbers) { allBarbers.count { it.active } }

    if (showLogoutConfirmDialog) {
        Dialog(onDismissRequest = { showLogoutConfirmDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
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
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = RedCancel,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "¿Cerrar Sesión de Administrador?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextIvory,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Saldrás del panel de gestión y volverás a la pantalla principal de acceso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Text("Cancelar", color = TextIvory, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                showLogoutConfirmDialog = false
                                onLogout()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                        ) {
                            Text("Salir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BIG BOYS · ",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        fontSize = 14.sp
                                    ),
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        fontSize = 14.sp
                                    ),
                                    color = GoldAccent,
                                    modifier = Modifier.clickable { /* no-op */ }
                                )
                            }
                            Text(
                                text = "Panel Exclusivo · $adminName",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextIvory
                            )
                        }
                    }
                },
                actions = {
                    // Dark Mode Toggle
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = GoldPrimary
                        )
                    }

                    // Logout Button
                    Button(
                        onClick = { showLogoutConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = RedCancel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salir", color = RedCancel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = GoldPrimary,
                    actionIconContentColor = GoldPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Quick Status Highlights Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(GreenConfirm, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Activas: $pendingOrConfirmedCount citas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextIvory
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💈 Barberos: $activeBarbersCount",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (gmailPass.isNotBlank()) GreenConfirm else GoldAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (gmailPass.isNotBlank()) "Gmail SMTP Listo" else "Gmail Pendiente",
                            fontSize = 11.sp,
                            color = if (gmailPass.isNotBlank()) GreenConfirm else GoldAccent
                        )
                    }
                }
            }

            // Exclusive Admin Sub Tabs selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubTabChip("Agenda Reservas", adminSubTab == "agenda") { adminSubTab = "agenda" }
                SubTabChip("Gestionar Barberos", adminSubTab == "barberos_mgr") { adminSubTab = "barberos_mgr" }
                SubTabChip("Registro de Clientes", adminSubTab == "clientes_mgr") { adminSubTab = "clientes_mgr" }
                SubTabChip("Cupones", adminSubTab == "cupones") { adminSubTab = "cupones" }
                SubTabChip("Reportes & Gráficos", adminSubTab == "reportes") { adminSubTab = "reportes" }
                SubTabChip("Correo Gmail Real", adminSubTab == "correo_real") { adminSubTab = "correo_real" }
                SubTabChip("Corte Manual (Walk-in)", adminSubTab == "corte_manual") { adminSubTab = "corte_manual" }
                SubTabChip("Planillas Excel", adminSubTab == "gestion_datos") { adminSubTab = "gestion_datos" }
                SubTabChip("Reseñas Clientes", adminSubTab == "resenas") { adminSubTab = "resenas" }
                SubTabChip("Mi Perfil & PIN", adminSubTab == "perfil") { adminSubTab = "perfil" }
            }

            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (adminSubTab) {
                    "agenda" -> AppointmentsManagerView(viewModel = viewModel)
                    "barberos_mgr" -> BarberManagerView(viewModel = viewModel)
                    "clientes_mgr" -> ClientsManagerView(viewModel = viewModel)
                    "cupones" -> GiftCouponsManagerView(viewModel = viewModel)
                    "reportes" -> SalesReportsView(viewModel = viewModel, onNavigateToAgenda = { adminSubTab = "agenda" })
                    "correo_real" -> GmailSenderConfigView(viewModel = viewModel)
                    "corte_manual" -> ManualAppointmentWalkInView(viewModel = viewModel)
                    "gestion_datos" -> DataSincView(viewModel = viewModel)
                    "resenas" -> ReviewsScreen(viewModel = viewModel)
                    "perfil" -> ProfileScreenContent(viewModel = viewModel, onClose = { adminSubTab = "agenda" })
                }
            }
        }
    }
}

@Composable
fun AdminScreen(viewModel: BarberiaViewModel) {
    AdminDashboardScreen(
        viewModel = viewModel,
        onLogout = { viewModel.logout() }
    )
}

@Composable
fun SubTabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) GoldPrimary else CardBackground
        ),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (selected) DarkBackground else TextIvory
        )
    }
}

// Model for Pie / Donut Chart Slices
data class PieSliceItem(
    val label: String,
    val value: Double,
    val count: Int,
    val color: Color,
    val subtitle: String? = null,
    val avatarUrl: String? = null
)

@Composable
fun BigBoysDonutPieChart(
    title: String,
    subtitle: String,
    slices: List<PieSliceItem>,
    valuePrefix: String = "$",
    valueSuffix: String = " CLP",
    isCurrency: Boolean = true,
    centerLabel: String = "TOTAL",
    modifier: Modifier = Modifier
) {
    val totalValue = remember(slices) { slices.sumOf { it.value } }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        fontSize = 15.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🥧 GRÁFICO TORTA",
                        color = GoldPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (slices.isEmpty() || totalValue <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay datos suficientes para graficar en este periodo.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                // Donut Visual Canvas with Center Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.size(190.dp)
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweepAngle = ((slice.value / totalValue) * 360f).toFloat()
                            if (sweepAngle > 0f) {
                                // Draw arc with small gap
                                val gap = if (slices.size > 1) 2.5f else 0f
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle + (gap / 2f),
                                    sweepAngle = (sweepAngle - gap).coerceAtLeast(0.5f),
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    // Donut Center Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = centerLabel,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isCurrency) {
                                "$valuePrefix${formatClp(totalValue.toLong())}$valueSuffix"
                            } else {
                                "${totalValue.toInt()} $valueSuffix"
                            },
                            color = GoldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = GoldPrimary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown Legend Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slices.forEach { slice ->
                        val percentage = if (totalValue > 0.0) (slice.value / totalValue * 100.0) else 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141311), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Color Dot or Barber Avatar
                                if (slice.avatarUrl != null) {
                                    AsyncImage(
                                        model = slice.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, slice.color, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(slice.color, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = slice.label,
                                        color = TextIvory,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (slice.subtitle != null) {
                                        Text(
                                            text = slice.subtitle,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isCurrency) {
                                        "$${formatClp(slice.value.toLong())} CLP"
                                    } else {
                                        "${slice.value.toInt()} cortes"
                                    },
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", percentage)}%",
                                    color = slice.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 1. Sales reports view with Independent Pie Charts and Direct Agenda Link
@Composable
fun SalesReportsView(
    viewModel: BarberiaViewModel,
    onNavigateToAgenda: () -> Unit = {}
) {
    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    val barbers by viewModel.allBarbersFlow.collectAsState()

    var selectedLocIdForReports by remember { mutableStateOf<Int?>(null) }
    var chartFilterType by remember { mutableStateOf("todos") } // "todos", "locales", "barberos"

    val selectedLocName = remember(locations, selectedLocIdForReports) {
        locations.find { it.id == selectedLocIdForReports }?.name?.replace("Big Boys - ", "")
    }

    val completedAppts = remember(appointments, selectedLocIdForReports) {
        appointments.filter { 
            it.status == "Completada" && (selectedLocIdForReports == null || it.locationId == selectedLocIdForReports)
        }
    }

    val totalEarnings = remember(completedAppts) {
        completedAppts.sumOf { it.servicePrice }
    }

    val totalSecnas = remember(completedAppts) {
        completedAppts.sumOf { it.prepayAmount }
    }

    val totalCitasCount = remember(appointments, selectedLocIdForReports) {
        appointments.count { selectedLocIdForReports == null || it.locationId == selectedLocIdForReports }
    }

    // Pie Chart Colors Palette
    val sliceColors = remember {
        listOf(
            Color(0xFFD4AF37), // Gold
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFF34D399), // Emerald
            Color(0xFFF472B6), // Pink
            Color(0xFFA78BFA), // Purple
            Color(0xFFFB923C), // Orange
            Color(0xFFFACC15), // Yellow
            Color(0xFF2DD4BF)  // Teal
        )
    }

    // Pie Chart Data: By Local / Branch
    val locationPieSlices = remember(appointments, locations) {
        locations.mapIndexed { index, loc ->
            val locAppointments = appointments.filter { it.locationId == loc.id && it.status == "Completada" }
            val locRevenue = locAppointments.sumOf { it.servicePrice }
            val allLocAppts = appointments.filter { it.locationId == loc.id }
            PieSliceItem(
                label = loc.name.replace("Big Boys - ", "Sucursal "),
                value = locRevenue,
                count = allLocAppts.size,
                color = sliceColors[index % sliceColors.size],
                subtitle = "${locAppointments.size} completadas · ${allLocAppts.size} agendadas"
            )
        }.filter { it.value > 0 || it.count > 0 }
    }

    // Pie Chart Data: By Barber
    val filteredBarbers = remember(barbers, selectedLocIdForReports) {
        barbers.filter { selectedLocIdForReports == null || it.locationId == selectedLocIdForReports }
    }

    val barberPieSlices = remember(appointments, filteredBarbers) {
        filteredBarbers.mapIndexed { index, barber ->
            val barberAppointments = appointments.filter { it.barberId == barber.id && it.status == "Completada" }
            val barberRevenue = barberAppointments.sumOf { it.servicePrice }
            val allBarberAppts = appointments.filter { it.barberId == barber.id }
            PieSliceItem(
                label = barber.name,
                value = barberRevenue,
                count = allBarberAppts.size,
                color = sliceColors[(index + 2) % sliceColors.size],
                subtitle = "${barberAppointments.size} cortes completados",
                avatarUrl = barber.avatarUrl
            )
        }.filter { it.value > 0 || it.count > 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Direct Action Banner to Agenda
        item {
            Card(
                onClick = onNavigateToAgenda,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A11)),
                border = BorderStroke(1.5.dp, GoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EventNote, null, tint = DarkBackground, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "📅 APARTADO DIRECTO DE AGENDA",
                                fontWeight = FontWeight.Black,
                                color = GoldAccent,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Gestionar, reprogramar y modificar citas en vivo",
                                color = TextIvory,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = onNavigateToAgenda,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Ir a Agenda ⚡", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Branch Selector Filter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storefront, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "FILTRAR REPORTES Y GRÁFICOS",
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedLocIdForReports == null,
                            onClick = { selectedLocIdForReports = null },
                            label = { Text("Todas las Sucursales") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = DarkBackground,
                                containerColor = CardBackground,
                                labelColor = TextIvory
                            )
                        )
                        locations.forEach { loc ->
                            FilterChip(
                                selected = selectedLocIdForReports == loc.id,
                                onClick = { selectedLocIdForReports = loc.id },
                                label = { Text(loc.name.replace("Big Boys - ", "")) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = CardBackground,
                                    labelColor = TextIvory
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chart View Mode Filter
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("todos", "📊 Todos los Gráficos", Icons.Filled.Dashboard),
                            Triple("locales", "🥧 Torta por Sucursales", Icons.Filled.PieChart),
                            Triple("barberos", "🥧 Torta por Barberos", Icons.Filled.DonutLarge)
                        ).forEach { (id, label, icon) ->
                            val isSelected = chartFilterType == id
                            FilterChip(
                                selected = isSelected,
                                onClick = { chartFilterType = id },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent.copy(alpha = 0.25f),
                                    selectedLabelColor = GoldAccent,
                                    selectedLeadingIconColor = GoldAccent,
                                    containerColor = Color(0xFF141311),
                                    labelColor = TextMuted,
                                    iconColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }

        // High Level KPI Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ventas Totales", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$${formatClp(totalEarnings)} CLP",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GoldAccent
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Abonos Webpay", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$${formatClp(totalSecnas)} CLP",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = GreenConfirm
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(0.9f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Citas", color = TextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$totalCitasCount",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = TextIvory
                        )
                    }
                }
            }
        }

        // CHART 1: INDEPENDENT PIE CHART BY LOCAL / SUCURSAL
        if (chartFilterType == "todos" || chartFilterType == "locales") {
            item {
                BigBoysDonutPieChart(
                    title = "🥧 GRÁFICO DE TORTA: VENTAS POR SUCURSAL",
                    subtitle = "Participación y recaudación independiente por cada local",
                    slices = locationPieSlices,
                    centerLabel = "TOTAL LOCALES"
                )
            }
        }

        // CHART 2: INDEPENDENT PIE CHART BY BARBER
        if (chartFilterType == "todos" || chartFilterType == "barberos") {
            item {
                BigBoysDonutPieChart(
                    title = if (selectedLocName == null) "🥧 GRÁFICO DE TORTA: DESEMPEÑO POR BARBERO" else "🥧 TORTA: BARBEROS ($selectedLocName)",
                    subtitle = "Distribución de ingresos generados por cada profesional",
                    slices = barberPieSlices,
                    centerLabel = "TOTAL EQUIPO"
                )
            }
        }

        // Bar Chart: Volume of bookings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BarChart, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedLocName == null) "VOLUMEN DE CITAS POR BARBERO" else "VOLUMEN DE CITAS ($selectedLocName)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldPrimary
                        )
                    }
                    Text(
                        text = "Representación lineal del volumen total de clientes atendidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val maxBookings = remember(appointments, filteredBarbers, selectedLocIdForReports) {
                        val filtered = appointments.filter { selectedLocIdForReports == null || it.locationId == selectedLocIdForReports }
                        val counts = filteredBarbers.map { b -> filtered.count { it.barberId == b.id } }
                        counts.maxOrNull() ?: 1
                    }

                    if (filteredBarbers.isEmpty()) {
                        Text("No hay barberos registrados en esta sucursal.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        filteredBarbers.forEach { barber ->
                            val filtered = appointments.filter { selectedLocIdForReports == null || it.locationId == selectedLocIdForReports }
                            val bookingsCount = filtered.count { it.barberId == barber.id }
                            val pct = if (maxBookings > 0) bookingsCount.toFloat() / maxBookings.toFloat() else 0f

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = barber.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, GoldPrimary, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(barber.name, color = TextIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$bookingsCount citas", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .background(DarkBackground, RoundedCornerShape(5.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((if (pct > 0.05f) pct else 0.05f).coerceIn(0f, 1f))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(GoldAccent, GoldPrimary)
                                                ),
                                                RoundedCornerShape(5.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceBarRow(title: String, sales: Double, totalSales: Double) {
    val percentage = if (totalSales > 0f) (sales / totalSales).toFloat() else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = TextIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("$${formatClp(sales)} CLP", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(DarkBackground, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((if (percentage > 0.1f) percentage else 0.1f).coerceIn(0f, 1f))
                    .background(GoldPrimary, RoundedCornerShape(4.dp))
            )
        }
    }
}

// 2. Manage Appointments view with Direct Editing & Filtering
// 2. Manage Appointments view with Direct Editing, Filtering, Monthly Calendar, Chronological Sorting, KPIs & Table/Cards Switch
@Composable
fun AppointmentsManagerView(viewModel: BarberiaViewModel) {
    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    val barbers by viewModel.allBarbersFlow.collectAsState()

    var appointmentToModify by remember { mutableStateOf<Appointment?>(null) }
    var appointmentToCancel by remember { mutableStateOf<Triple<Appointment, String, String>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TODAS") } // "TODAS", "Confirmada", "Pendiente", "Completada", "Cancelada"
    var viewMode by remember { mutableStateOf("CARDS") } // "CARDS" or "TABLE"
    var sortAscending by remember { mutableStateOf(true) } // Chronological sort: Asc = earliest first, Desc = latest first
    var selectedCalendarDate by remember { mutableStateOf<String?>(null) }
    var showMonthlyCalendar by remember { mutableStateOf(false) }
    var calendarMonthOffset by remember { mutableIntStateOf(0) }

    // KPI Metrics calculation
    val todayDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    val totalAgendamientos = remember(appointments) { appointments.size }
    val recaudacionTotal = remember(appointments) {
        appointments.filter { it.status != "Cancelada" }.sumOf { it.servicePrice }
    }
    val prepagosRecibidos = remember(appointments) {
        // Prepagos recibidos ($3.000 CLP fijos por cita agendada)
        appointments.count { it.status != "Cancelada" } * 3000.0
    }
    val citasHoy = remember(appointments, todayDateStr) {
        appointments.count { AppFormatters.isSameDay(it.dateTime, todayDateStr) && it.status != "Cancelada" }
    }

    if (appointmentToModify != null) {
        ModifyAppointmentDialog(
            appointment = appointmentToModify!!,
            viewModel = viewModel,
            onDismiss = { appointmentToModify = null }
        )
    }

    if (appointmentToCancel != null) {
        val (apptToCancel, barberName, locationName) = appointmentToCancel!!
        Dialog(onDismissRequest = { appointmentToCancel = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.5.dp, RedCancel),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(RedCancel.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, RedCancel.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = "Alerta",
                            tint = RedCancel,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "¿Desea cancelar la cita agendada?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        ),
                        color = TextIvory,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail summary card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cliente:", fontSize = 12.sp, color = TextMuted)
                                Text(apptToCancel.customerName, fontSize = 12.sp, color = TextIvory, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Servicio:", fontSize = 12.sp, color = TextMuted)
                                Text(apptToCancel.serviceName, fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fecha y Hora:", fontSize = 12.sp, color = TextMuted)
                                Text(apptToCancel.dateTime, fontSize = 12.sp, color = TextIvory, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Barbero:", fontSize = 12.sp, color = TextMuted)
                                Text(barberName, fontSize = 12.sp, color = TextIvory)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sucursal:", fontSize = 12.sp, color = TextMuted)
                                Text(locationName, fontSize = 12.sp, color = TextIvory)
                            }
                            if (apptToCancel.prepayAmount > 0) {
                                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Abono a devolver:", fontSize = 12.sp, color = GreenConfirm, fontWeight = FontWeight.Bold)
                                    Text("$${formatClp(apptToCancel.prepayAmount)} CLP", fontSize = 12.sp, color = GreenConfirm, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { appointmentToCancel = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_dialog_cancel_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextIvory)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val toCancel = apptToCancel
                                val bName = barberName
                                val locName = locationName
                                appointmentToCancel = null
                                viewModel.cancelAppointmentAdmin(toCancel, bName, locName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_dialog_confirm_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                        ) {
                            Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    val sdfDateTime = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }
    val filteredAppointments = remember(appointments, searchQuery, statusFilter, selectedCalendarDate, sortAscending) {
        appointments.filter { appt ->
            val matchesStatus = if (statusFilter == "TODAS") true else appt.status.equals(statusFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    appt.customerName.contains(searchQuery, ignoreCase = true) ||
                    appt.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    appt.customerEmail.contains(searchQuery, ignoreCase = true) ||
                    appt.serviceName.contains(searchQuery, ignoreCase = true) ||
                    appt.dateTime.contains(searchQuery, ignoreCase = true)
            val matchesDate = selectedCalendarDate == null || AppFormatters.isSameDay(appt.dateTime, selectedCalendarDate!!)
            matchesStatus && matchesSearch && matchesDate
        }.sortedWith { a, b ->
            val timeA = try { sdfDateTime.parse(a.dateTime)?.time ?: 0L } catch (_: Exception) { 0L }
            val timeB = try { sdfDateTime.parse(b.dateTime)?.time ?: 0L } catch (_: Exception) { 0L }
            if (sortAscending) timeA.compareTo(timeB) else timeB.compareTo(timeA)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- KPI Cards Grid (4 Key Metrics) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // KPI 1: Total Agendamientos
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EventNote, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agendamientos", fontSize = 10.sp, color = TextMuted, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalAgendamientos",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TextIvory
                    )
                    Text("Total registradas", fontSize = 9.sp, color = TextMuted)
                }
            }

            // KPI 2: Recaudación Total CLP
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AttachMoney, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Recaudación", fontSize = 10.sp, color = TextMuted, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${formatClp(recaudacionTotal)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = GoldAccent,
                        maxLines = 1
                    )
                    Text("Total servicios CLP", fontSize = 9.sp, color = TextMuted)
                }
            }

            // KPI 3: Prepagos Recibidos ($3.000 por cita)
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GreenConfirm.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Paid, null, tint = GreenConfirm, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prepagos", fontSize = 10.sp, color = TextMuted, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${formatClp(prepagosRecibidos)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = GreenConfirm,
                        maxLines = 1
                    )
                    Text("$3.000 CLP / cita", fontSize = 9.sp, color = GreenConfirm.copy(alpha = 0.8f))
                }
            }

            // KPI 4: Citas de Hoy
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Today, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Citas Hoy", fontSize = 10.sp, color = TextMuted, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$citasHoy",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = TextIvory
                    )
                    Text("En agenda activa", fontSize = 9.sp, color = TextMuted)
                }
            }
        }

        // --- Search, Filter & View Controls Header Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row with Title and Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AGENDA DE CITAS",
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        "${filteredAppointments.size} de ${appointments.size} citas",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por cliente, fono, servicio o fecha...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = GoldPrimary,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_agenda_search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Chips Filter
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TODAS", "Confirmada", "Pendiente", "Completada", "Cancelada", "Caducada").forEach { st ->
                        val isSelected = statusFilter == st
                        FilterChip(
                            selected = isSelected,
                            onClick = { statusFilter = st },
                            label = { Text(st, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (st) {
                                    "Confirmada" -> GreenConfirm
                                    "Completada" -> GoldPrimary
                                    "Cancelada" -> RedCancel
                                    "Caducada" -> Color(0xFFEF4444)
                                    else -> GoldAccent
                                },
                                selectedLabelColor = DarkBackground,
                                containerColor = Color(0xFF161513),
                                labelColor = TextIvory
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Secondary Controls: Calendar Toggle, Chronological Sort, and Table/Cards Switch ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Monthly Calendar Filter Button
                    Card(
                        onClick = { showMonthlyCalendar = !showMonthlyCalendar },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCalendarDate != null || showMonthlyCalendar) GoldPrimary.copy(alpha = 0.2f) else DarkBackground
                        ),
                        border = BorderStroke(1.dp, if (selectedCalendarDate != null) GoldPrimary else GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("calendar_filter_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CalendarToday, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedCalendarDate != null) "Filtrando: $selectedCalendarDate" else "Filtrar por Día",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCalendarDate != null) GoldAccent else TextIvory
                            )
                        }
                    }

                    if (selectedCalendarDate != null) {
                        Card(
                            onClick = { selectedCalendarDate = null },
                            colors = CardDefaults.cardColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Close, null, tint = RedCancel, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Ver Todas las Fechas", fontSize = 11.sp, color = RedCancel, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Chronological Sort Button (Asc / Desc)
                    Card(
                        onClick = { sortAscending = !sortAscending },
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sort_order_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortAscending) "Cronológico: Próximas (Asc)" else "Cronológico: Recientes (Desc)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextIvory
                            )
                        }
                    }

                    // View Mode Switch: Tarjetas vs Tabla
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (viewMode == "CARDS") GoldPrimary else Color.Transparent)
                                    .clickable { viewMode = "CARDS" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("view_mode_cards_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ViewAgenda, null, tint = if (viewMode == "CARDS") DarkBackground else TextMuted, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Tarjetas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (viewMode == "CARDS") DarkBackground else TextIvory)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (viewMode == "TABLE") GoldPrimary else Color.Transparent)
                                    .clickable { viewMode = "TABLE" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("view_mode_table_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.TableChart, null, tint = if (viewMode == "TABLE") DarkBackground else TextMuted, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Tabla", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (viewMode == "TABLE") DarkBackground else TextIvory)
                                }
                            }
                        }
                    }
                }

                // --- Interactive Monthly Calendar Accordion ---
                AnimatedVisibility(visible = showMonthlyCalendar) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(DarkBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        val cal = remember(calendarMonthOffset) {
                            Calendar.getInstance().apply {
                                add(Calendar.MONTH, calendarMonthOffset)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                        }
                        val monthName = remember(cal) {
                            SimpleDateFormat("MMMM yyyy", Locale("es", "CL")).format(cal.time).replaceFirstChar { it.uppercase() }
                        }
                        val daysInMonth = remember(cal) { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
                        val firstDayOfWeek = remember(cal) {
                            val dow = cal.get(Calendar.DAY_OF_WEEK)
                            if (dow == Calendar.SUNDAY) 6 else dow - 2 // 0=Mon, 6=Sun
                        }
                        val currentMonthYear = remember(cal) {
                            SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(cal.time)
                        }

                        // Calendar Month Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { calendarMonthOffset-- }) {
                                Icon(Icons.Filled.ChevronLeft, "Mes anterior", tint = GoldPrimary)
                            }
                            Text(
                                text = monthName,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                fontSize = 13.sp
                            )
                            IconButton(onClick = { calendarMonthOffset++ }) {
                                Icon(Icons.Filled.ChevronRight, "Mes siguiente", tint = GoldPrimary)
                            }
                        }

                        // Weekday labels
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do").forEach { d ->
                                Text(d, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Days grid
                        val totalSlots = firstDayOfWeek + daysInMonth
                        val rows = (totalSlots + 6) / 7
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                for (c in 0 until 7) {
                                    val slotIndex = r * 7 + c
                                    val dayNum = slotIndex - firstDayOfWeek + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val dayString = String.format(Locale.getDefault(), "%02d-%s", dayNum, currentMonthYear)
                                        val apptsOnDay = appointments.count { AppFormatters.isSameDay(it.dateTime, dayString) && it.status != "Cancelada" }
                                        val isSelected = selectedCalendarDate == dayString

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(2.dp)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when {
                                                        isSelected -> GoldPrimary
                                                        apptsOnDay > 0 -> GoldPrimary.copy(alpha = 0.15f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) GoldAccent else if (apptsOnDay > 0) GoldPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    selectedCalendarDate = if (selectedCalendarDate == dayString) null else dayString
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected || apptsOnDay > 0) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) DarkBackground else TextIvory
                                                )
                                                if (apptsOnDay > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(if (isSelected) DarkBackground else GoldAccent, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Appointments Content (Table Mode vs Cards Mode) ---
        if (filteredAppointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EventBusy, null, tint = TextMuted, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No se encontraron citas con los filtros actuales.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else if (viewMode == "TABLE") {
            // --- Table Mode ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141311), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FECHA / HORA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.3f))
                        Text("CLIENTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.4f))
                        Text("BARBERO / LOCAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.4f))
                        Text("SERVICIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.3f))
                        Text("ABONO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1f))
                        Text("ESTADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.1f))
                        Text("ACCIONES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    }

                    Divider(color = GoldPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredAppointments, key = { it.id }) { appt ->
                            val barberName = barbers.find { it.id == appt.barberId }?.name ?: "Barbero #${appt.barberId}"
                            val locationName = locations.find { it.id == appt.locationId }?.name?.replace("Big Boys - ", "") ?: "Suc #${appt.locationId}"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBackground, RoundedCornerShape(6.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(appt.dateTime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(appt.customerName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextIvory, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(appt.customerPhone, fontSize = 9.sp, color = TextMuted)
                                }
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(barberName, fontSize = 11.sp, color = TextIvory, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(locationName, fontSize = 9.sp, color = GoldAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(appt.serviceName, fontSize = 10.sp, color = TextIvory, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$${formatClp(appt.servicePrice)} CLP", fontSize = 9.sp, color = TextMuted)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("$${formatClp(appt.prepayAmount)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenConfirm)
                                }
                                Column(modifier = Modifier.weight(1.1f)) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (appt.status) {
                                                    "Confirmada" -> GreenConfirm.copy(alpha = 0.2f)
                                                    "Completada" -> GoldPrimary.copy(alpha = 0.2f)
                                                    "Pendiente" -> Color.Yellow.copy(alpha = 0.2f)
                                                    "Caducada", "Vencida" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                    else -> RedCancel.copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (appt.status == "Caducada" || appt.status == "Vencida") "VENC" else appt.status.take(4).uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (appt.status) {
                                                "Confirmada" -> GreenConfirm
                                                "Completada" -> GoldPrimary
                                                "Pendiente" -> Color.Yellow
                                                "Caducada", "Vencida" -> Color(0xFFEF4444)
                                                else -> RedCancel
                                            }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { appointmentToModify = appt },
                                        modifier = Modifier.size(24.dp).testTag("modify_appt_tbl_${appt.id}")
                                    ) {
                                        Icon(Icons.Filled.Edit, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                    }
                                    if (appt.status != "Cancelada" && appt.status != "Completada") {
                                        IconButton(
                                            onClick = { viewModel.updateAppointmentStatusAdmin(appt.id, "Completada") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, null, tint = GreenConfirm, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { appointmentToCancel = Triple(appt, barberName, locationName) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Cancel, null, tint = RedCancel, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- Cards Mode ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAppointments, key = { it.id }) { appt ->
                    val barberName = barbers.find { it.id == appt.barberId }?.name ?: "Barbero #${appt.barberId}"
                    val locationName = locations.find { it.id == appt.locationId }?.name?.replace("Big Boys - ", "") ?: "Sucursal #${appt.locationId}"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Client Name, Guest Badge & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = appt.customerName,
                                            fontWeight = FontWeight.Black,
                                            color = TextIvory,
                                            fontSize = 15.sp
                                        )
                                        if (appt.isGuest) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("INVITADO", color = GoldAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text("📞 ${appt.customerPhone} · ✉️ ${appt.customerEmail}", fontSize = 11.sp, color = TextMuted)
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (appt.status) {
                                                "Confirmada" -> GreenConfirm.copy(alpha = 0.18f)
                                                "Completada" -> GoldPrimary.copy(alpha = 0.18f)
                                                "Pendiente" -> Color.Yellow.copy(alpha = 0.18f)
                                                "Caducada", "Vencida" -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                                else -> RedCancel.copy(alpha = 0.18f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            when (appt.status) {
                                                "Confirmada" -> GreenConfirm
                                                "Completada" -> GoldPrimary
                                                "Pendiente" -> Color.Yellow
                                                "Caducada", "Vencida" -> Color(0xFFEF4444)
                                                else -> RedCancel
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (appt.status == "Caducada" || appt.status == "Vencida") "VENCIDA / CADUCADA" else appt.status.uppercase(),
                                        color = when (appt.status) {
                                            "Confirmada" -> GreenConfirm
                                            "Completada" -> GoldPrimary
                                            "Pendiente" -> Color.Yellow
                                            "Caducada", "Vencida" -> Color(0xFFEF4444)
                                            else -> RedCancel
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = GoldPrimary.copy(alpha = 0.12f))

                            // Appointment Details Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Servicio: ${appt.serviceName}", fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 12.sp)
                                    Text("Barbero: $barberName", color = GoldAccent, fontSize = 11.sp)
                                    Text("Sucursal: $locationName", color = TextMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Fecha & Hora:", color = TextMuted, fontSize = 10.sp)
                                    Text(appt.dateTime, fontWeight = FontWeight.Black, color = GoldPrimary, fontSize = 12.sp)
                                    Text(
                                        "Valor: $${formatClp(appt.servicePrice)} CLP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextIvory
                                    )
                                    Text(
                                        "Abono: $${formatClp(appt.prepayAmount)} CLP",
                                        color = GreenConfirm,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons Row (Modificar, Cancelar, Completar)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Modify Button (Always accessible)
                                Button(
                                    onClick = { appointmentToModify = appt },
                                    modifier = Modifier.weight(1.1f).height(36.dp).testTag("modify_appt_btn_${appt.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.18f), contentColor = GoldAccent),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Filled.EditCalendar, null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Modificar Cita ✏️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (appt.status != "Cancelada" && appt.status != "Completada") {
                                    OutlinedButton(
                                        onClick = {
                                            appointmentToCancel = Triple(appt, barberName, locationName)
                                        },
                                        modifier = Modifier.weight(0.9f).height(36.dp).testTag("cancel_appt_btn_${appt.id}"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                                        border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Anular", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.updateAppointmentStatusAdmin(appt.id, "Completada") },
                                        modifier = Modifier.weight(0.9f).height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = DarkBackground, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Completar", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2b. Registered Clients Management View
@Composable
fun ClientsManagerView(viewModel: BarberiaViewModel) {
    val users by viewModel.allUsersFlow.collectAsState()
    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users else {
            users.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.rut.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.People, null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "REGISTRO DE CLIENTES",
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Text("${filteredUsers.size} clientes", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar cliente por nombre, RUT, email o teléfono...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = GoldPrimary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("clients_search_field")
                    )
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron clientes registrados.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                val userApptsCount = remember(appointments, user) {
                    appointments.count { it.userId == user.id || it.customerEmail.equals(user.email, ignoreCase = true) }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("client_card_${user.id}"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(1).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = GoldAccent,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.fullName, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 14.sp)
                            Text("✉️ ${user.email}", color = TextMuted, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (user.rut.isNotBlank()) {
                                    Text("RUT: ${user.rut}", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                if (user.phone.isNotBlank()) {
                                    Text("📞 ${user.phone}", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("$userApptsCount citas", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
// 4. Add new barber view
@Composable
fun AddNewBarberView(viewModel: BarberiaViewModel) {
    val locations by viewModel.locationsFlow.collectAsState()

    var bName by remember { mutableStateOf("") }
    var bSpecialty by remember { mutableStateOf("") }
    var bHours by remember { mutableStateOf("09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00") }
    var bLocationId by remember { mutableIntStateOf(1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Ingresar Nuevo Barbero al Equipo", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = bName,
                onValueChange = { bName = it },
                label = { Text("Nombre del Barbero") },
                colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_add_barber_name")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bSpecialty,
                onValueChange = { bSpecialty = it },
                label = { Text("Especialidad (Ej: Experto en Degradados)") },
                colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Asignar Sucursal de Trabajo:", fontSize = 11.sp, color = GoldAccent)
            locations.forEach { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { bLocationId = loc.id }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = bLocationId == loc.id,
                        onClick = { bLocationId = loc.id },
                        colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(loc.name, fontSize = 13.sp, color = TextIvory)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bHours,
                onValueChange = { bHours = it },
                label = { Text("Horarios Disponibles (Separados por coma)") },
                colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (bName.isNotBlank() && bSpecialty.isNotBlank()) {
                        viewModel.addBarberAdmin(
                            name = bName,
                            locationId = bLocationId,
                            specialty = bSpecialty,
                            initialHours = bHours
                        ) {
                            bName = ""
                            bSpecialty = ""
                            bLocationId = 1
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_add_barber_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("CONTRATAR BARBERO 💈", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ManualAppointmentWalkInView(viewModel: BarberiaViewModel) {
    val locations by viewModel.locationsFlow.collectAsState()
    val barbers by viewModel.allBarbersFlow.collectAsState()

    var selectedLocId by remember { mutableIntStateOf(1) }
    var selectedBarberId by remember { mutableIntStateOf(0) }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Corte de Pelo Degrado") }
    var selectedPrice by remember { mutableStateOf("15000") }
    var selectedHour by remember { mutableStateOf("12:00") }

    LaunchedEffect(locations) {
        if (locations.isNotEmpty() && locations.none { it.id == selectedLocId }) {
            selectedLocId = locations.first().id
        }
    }

    val services = listOf(
        "Corte de Pelo Degrado" to 15000.0,
        "Arreglo de Barba Royale" to 10000.0,
        "Combo Big Boys (Pelo + Barba)" to 22000.0,
        "Servicio Completo VIP" to 28000.0,
        "Corte Clásico Militar" to 13500.0,
        "Ritual de Barba & Barbería" to 22000.0
    )

    val activeBarbers = remember(barbers, selectedLocId) {
        barbers.filter { it.locationId == selectedLocId }
    }

    LaunchedEffect(selectedLocId, barbers) {
        if (selectedBarberId == 0 || barbers.none { it.id == selectedBarberId && it.locationId == selectedLocId }) {
            selectedBarberId = barbers.firstOrNull { it.locationId == selectedLocId }?.id ?: 0
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Ingreso Rápido de Clientes Sin Cita (Walk-in)", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Text("1. Sucursal del local:", fontSize = 11.sp, color = GoldAccent)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                locations.forEach { loc ->
                    FilterChip(
                        selected = selectedLocId == loc.id,
                        onClick = { selectedLocId = loc.id },
                        label = { Text(loc.name.replace("Big Boys - ", "")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("2. Barbero disponible:", fontSize = 11.sp, color = GoldAccent)
            if (activeBarbers.isEmpty()) {
                Text("No hay barberos contratados en este local", color = RedCancel, fontSize = 11.sp)
            } else {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    activeBarbers.forEach { barb ->
                        FilterChip(
                            selected = selectedBarberId == barb.id,
                            onClick = { selectedBarberId = barb.id },
                            label = { Text(barb.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = DarkBackground,
                                containerColor = CardBackground,
                                labelColor = TextIvory
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("3. Tipo de Registro:", fontSize = 11.sp, color = GoldPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(GoldPrimary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Person, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Cliente Presencial (Anónimo)", color = TextIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("No se requiere nombre del cliente. El servicio se registrará al barbero y local seleccionados.", color = TextMuted, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("4. Selección del Servicio prestado:", fontSize = 11.sp, color = GoldAccent)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                services.forEach { (serv, price) ->
                    FilterChip(
                        selected = selectedService == serv,
                        onClick = { 
                            selectedService = serv
                            selectedPrice = price.toInt().toString()
                        },
                        label = { Text("$serv ($${price.toInt()} CLP)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedPrice,
                    onValueChange = { selectedPrice = it },
                    label = { Text("Precio Real (CLP)") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    label = { Text("Hora del Servicio (Ej: 15:30)") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedBarberId != 0) {
                        val currentSimpleDate = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        viewModel.registerManualWalkIn(
                            locationId = selectedLocId,
                            barberId = selectedBarberId,
                            clientName = "Cliente Presencial",
                            clientPhone = "+56 9 0000 0000",
                            clientEmail = "presencial@bigboys.cl",
                            dateTimeString = "$currentSimpleDate $selectedHour",
                            serviceName = selectedService,
                            servicePrice = selectedPrice.toDoubleOrNull() ?: 15000.0
                        ) {
                            // Carga manual exitosa
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("REGISTRAR ATENCIÓN PRESENCIAL ✂️💈", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BarberManagerView(viewModel: BarberiaViewModel) {
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var barberToDelete by remember { mutableStateOf<Barber?>(null) }
    var selectedBarber by remember { mutableStateOf<Barber?>(null) }
    var filterLocationId by remember { mutableIntStateOf(0) } // 0 means all branches

    // State for Add Barber Dialog
    var newName by remember { mutableStateOf("") }
    var newSpecialty by remember { mutableStateOf("Cortes Clásicos & Degradados") }
    var newHours by remember { mutableStateOf("09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00") }
    var newBio by remember { mutableStateOf("Barbero profesional especializado en barbería tradicional y moderna.") }
    var newLocId by remember { mutableIntStateOf(locations.firstOrNull()?.id ?: 1) }
    var newImgUrl by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    // State for Edit Barber Dialog
    var editName by remember { mutableStateOf("") }
    var editSpecialty by remember { mutableStateOf("") }
    var editHours by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editLocId by remember { mutableIntStateOf(1) }
    var editImgUrl by remember { mutableStateOf("") }

    // Device photo picker for adding new barber
    val addImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            newImgUrl = uri.toString()
        }
    }

    // Device photo picker for editing existing barber
    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            editImgUrl = uri.toString()
        }
    }

    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300" to "Estilo Urbano",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300" to "Estilo Clásico",
        "https://images.unsplash.com/photo-1628157582853-a796fa650a6a?auto=format&fit=crop&q=80&w=300" to "Estilo Moderno",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=300" to "Barber Master",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&q=80&w=300" to "Estilista Top"
    )

    val filteredBarbers = if (filterLocationId == 0) {
        barbers
    } else {
        barbers.filter { it.locationId == filterLocationId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Gestión de Barberos & Sucursales 💈",
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    fontSize = 15.sp
                )
                Text(
                    "Agrega barberos, asígnalos a sucursales y personaliza sus fotos y horarios.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Button(
                onClick = {
                    newName = ""
                    newSpecialty = "Cortes Clásicos & Degradados"
                    newHours = "09:00,10:00,11:00,12:00,14:00,15:00,16:00,17:00,18:00"
                    newBio = "Barbero profesional de Big Boys Barberías Chile."
                    newLocId = if (filterLocationId != 0) filterLocationId else (locations.firstOrNull()?.id ?: 1)
                    newImgUrl = ""
                    addError = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_add_barber_btn")
            ) {
                Icon(Icons.Filled.PersonAdd, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ AGREGAR", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Branch filter chips
        Text("Filtrar por Sucursal:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterLocationId == 0,
                onClick = { filterLocationId = 0 },
                label = { Text("Todas (${barbers.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = DarkBackground,
                    containerColor = CardBackground,
                    labelColor = TextIvory
                )
            )
            locations.forEach { loc ->
                val count = barbers.count { it.locationId == loc.id }
                val shortName = loc.name.replace("Big Boys - ", "")
                FilterChip(
                    selected = filterLocationId == loc.id,
                    onClick = { filterLocationId = loc.id },
                    label = { Text("$shortName ($count)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = DarkBackground,
                        containerColor = CardBackground,
                        labelColor = TextIvory
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredBarbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PeopleOutline, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay barberos registrados en esta sucursal.", color = TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            newLocId = if (filterLocationId != 0) filterLocationId else (locations.firstOrNull()?.id ?: 1)
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Agregar barbero a esta sucursal", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredBarbers, key = { it.id }) { b ->
                    val branchName = locations.find { it.id == b.locationId }?.name?.replace("Big Boys - ", "") ?: "Sucursal #${b.locationId}"
                    val isAvailable = b.availableHours.isNotBlank() && b.availableHours != "NO_DISPONIBLE"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, if (isAvailable) Color(0xFF27272A) else RedCancel.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = if (b.avatarUrl.isNotBlank()) b.avatarUrl else "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=200",
                                    contentDescription = b.name,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, GoldPrimary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(b.name, fontWeight = FontWeight.Black, color = TextIvory, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isAvailable) GreenConfirm.copy(alpha = 0.2f) else RedCancel.copy(alpha = 0.2f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isAvailable) "ACTIVO" else "NO ASISTE",
                                                color = if (isAvailable) GreenConfirm else RedCancel,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text("Especialidad: ${b.specialty}", color = TextMuted, fontSize = 11.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Store, null, tint = GoldAccent, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sucursal: $branchName", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = if (isAvailable) "Horas: ${b.availableHours}" else "Día desactivado para reservas",
                                        color = if (isAvailable) TextMuted else RedCancel,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF27272A))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleBarberAvailability(b) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isAvailable) RedCancel else GreenConfirm
                                    ),
                                    border = BorderStroke(1.dp, if (isAvailable) RedCancel.copy(alpha = 0.6f) else GreenConfirm.copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (isAvailable) "Desactivar" else "Activar",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        selectedBarber = b
                                        editName = b.name
                                        editSpecialty = b.specialty
                                        editHours = b.availableHours
                                        editBio = b.bio
                                        editLocId = b.locationId
                                        editImgUrl = b.avatarUrl
                                        showEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, null, tint = DarkBackground, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EDITAR / ASIGNAR", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { barberToDelete = b },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Eliminar barbero", tint = RedCancel, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD BARBER DIALOG ---
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.5.dp, GoldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("add_barber_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "💈 Nuevo Barbero & Asignación",
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showAddDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = GoldPrimary.copy(alpha = 0.3f))

                    // Sucursal Assignment (MANDATORY SELECTION)
                    Text(
                        "Asignar a Sucursal:",
                        fontSize = 12.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        locations.forEach { loc ->
                            val isSelected = newLocId == loc.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newLocId = loc.id }
                                    .padding(vertical = 3.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { newLocId = loc.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    loc.name,
                                    color = if (isSelected) GoldPrimary else TextIvory,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            addError = null
                        },
                        label = { Text("Nombre Completo del Barbero") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("new_barber_name_input")
                    )

                    OutlinedTextField(
                        value = newSpecialty,
                        onValueChange = { newSpecialty = it },
                        label = { Text("Especialidad (Ej: Cortes Clásicos, Barba)") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Trayectoria / Bio") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    // Photo selector from device & presets
                    Text("Foto de Perfil del Barbero:", fontSize = 11.sp, color = GoldAccent, modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = if (newImgUrl.isNotBlank()) newImgUrl else "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=200",
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { addImagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_barber_pick_device_photo_btn")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir desde Dispositivo", color = TextIvory, fontSize = 11.sp)
                        }
                    }

                    // Avatar preset selection
                    Text("O elige un avatar sugerido:", fontSize = 10.sp, color = TextMuted, modifier = Modifier.align(Alignment.Start))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(sampleAvatars) { (url, label) ->
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (newImgUrl == url) 2.dp else 1.dp,
                                        color = if (newImgUrl == url) GoldPrimary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { newImgUrl = url },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newHours,
                        onValueChange = { newHours = it },
                        label = { Text("Horarios Disponibles (Separados por coma)") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    if (addError != null) {
                        Text(addError ?: "", color = RedCancel, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancelar", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newName.isBlank()) {
                                    addError = "Por favor ingresa el nombre del barbero"
                                    return@Button
                                }
                                viewModel.addBarberAdmin(
                                    name = newName,
                                    locationId = newLocId,
                                    specialty = newSpecialty,
                                    bio = newBio,
                                    avatarUrl = newImgUrl,
                                    initialHours = newHours,
                                    initialRating = 5.0f
                                ) {
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.testTag("confirm_add_barber_btn")
                        ) {
                            Text("Guardar Barbero", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- EDIT BARBER & ASSIGN BRANCH DIALOG ---
    if (showEditDialog && selectedBarber != null) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("edit_barber_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Editar Barbero & Sucursal ✂️", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 15.sp)
                        IconButton(onClick = { showEditDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = GoldPrimary.copy(alpha = 0.2f))

                    // Sucursal Reassignment
                    Text("Sucursal Asignada:", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        locations.forEach { loc ->
                            val isSelected = editLocId == loc.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editLocId = loc.id }
                                    .padding(vertical = 3.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { editLocId = loc.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    loc.name,
                                    color = if (isSelected) GoldPrimary else TextIvory,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = editSpecialty,
                        onValueChange = { editSpecialty = it },
                        label = { Text("Especialidad") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Trayectoria / Bio") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Text("Foto de Perfil:", fontSize = 11.sp, color = GoldAccent, modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = editImgUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, GoldPrimary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { editImagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(16.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar Foto Dispositivo", color = TextIvory, fontSize = 11.sp)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(sampleAvatars) { (url, label) ->
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (editImgUrl == url) 2.dp else 1.dp,
                                        color = if (editImgUrl == url) GoldPrimary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { editImgUrl = url },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editHours,
                        onValueChange = { editHours = it },
                        label = { Text("Horarios Disponibles (Separados por coma)") },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancelar", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val barber = selectedBarber
                                if (barber != null) {
                                    val updated = barber.copy(
                                        name = editName,
                                        specialty = editSpecialty,
                                        bio = editBio,
                                        availableHours = editHours,
                                        avatarUrl = editImgUrl,
                                        locationId = editLocId
                                    )
                                    viewModel.updateBarberAdmin(updated) {
                                        showEditDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Text("Guardar Cambios", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    val targetBarber = barberToDelete
    if (targetBarber != null) {
        AlertDialog(
            onDismissRequest = { barberToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = RedCancel, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar Barbero", color = TextIvory, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar al barbero \"${targetBarber.name}\"? Esta acción no se puede deshacer.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBarberAdmin(targetBarber) {
                            barberToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { barberToDelete = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
fun GiftCouponsManagerView(viewModel: BarberiaViewModel) {
    val coupons by viewModel.allCouponsFlow.collectAsState()
    
    var redeemPinInput by remember { mutableStateOf("") }
    var redeemMessage by remember { mutableStateOf("") }
    var isRedeemSuccess by remember { mutableStateOf(true) }

    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newCustomPin by remember { mutableStateOf("") }
    var manualMessage by remember { mutableStateOf("") }
    var isManualSuccess by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var showManualCreator by remember { mutableStateOf(false) }

    val filteredCoupons = remember(coupons, searchQuery) {
        if (searchQuery.isBlank()) {
            coupons
        } else {
            coupons.filter {
                it.userName.contains(searchQuery, ignoreCase = true) ||
                it.userEmail.contains(searchQuery, ignoreCase = true) ||
                it.pin.contains(searchQuery)
            }
        }
    }

    val totalCount = coupons.size
    val redeemedCount = coupons.count { it.isRedeemed }
    val activeCount = totalCount - redeemedCount

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards & Progress
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ESTADO DE CAMPAÑA DE BIENVENIDA 🎁",
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Emitidos", fontSize = 10.sp, color = TextMuted)
                            Text("$totalCount / 100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                        }
                        Column {
                            Text("Canjeados", fontSize = 10.sp, color = TextMuted)
                            Text("$redeemedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenConfirm)
                        }
                        Column {
                            Text("Activos", fontSize = 10.sp, color = TextMuted)
                            Text("$activeCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Límite máximo: 100 cupones en total.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { totalCount.toFloat() / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = GoldPrimary,
                        trackColor = Color(0xFF27272A)
                    )
                }
            }
        }

        // Section 1: Quick Redeem Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REGISTRAR CANJE POR PIN (CEJAS) 🛡️",
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Ingresa el PIN de 4 dígitos para validar y descontar el perfilado de cejas de regalo en caja.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = redeemPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) redeemPinInput = it },
                            label = { Text("PIN de 4 dígitos") },
                            placeholder = { Text("Ej. 4829") },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 16.sp, color = TextIvory),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("admin_redeem_pin_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = TextMuted
                            )
                        )

                        Button(
                            onClick = {
                                if (redeemPinInput.length != 4) {
                                    redeemMessage = "El PIN debe tener exactamente 4 dígitos."
                                    isRedeemSuccess = false
                                    return@Button
                                }
                                viewModel.redeemCoupon(redeemPinInput) { success, msg ->
                                    isRedeemSuccess = success
                                    redeemMessage = msg
                                    if (success) {
                                        redeemPinInput = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            modifier = Modifier.height(56.dp).testTag("admin_redeem_pin_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Canje", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (redeemMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = redeemMessage,
                            color = if (isRedeemSuccess) GreenConfirm else RedCancel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 2: Manual Coupon Generator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMITIR CUPÓN MANUALMENTE 🏷️",
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { showManualCreator = !showManualCreator },
                            modifier = Modifier.testTag("toggle_manual_creator_button")
                        ) {
                            Text(
                                text = if (showManualCreator) "Ocultar" else "Mostrar Formulario",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showManualCreator) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = newUserName,
                            onValueChange = { newUserName = it },
                            label = { Text("Nombre del Cliente") },
                            placeholder = { Text("Ej. Mauricio Solís") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_manual_coupon_name_field"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF334155))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newUserEmail,
                            onValueChange = { newUserEmail = it },
                            label = { Text("Email del Cliente") },
                            placeholder = { Text("Ej. mauricio@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_manual_coupon_email_field"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF334155))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCustomPin,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newCustomPin = it },
                            label = { Text("PIN de 4 dígitos (Opcional, se genera auto si se vacía)") },
                            placeholder = { Text("Dejar vacío para PIN aleatorio") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_manual_coupon_pin_field"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF334155))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newUserName.isBlank() || newUserEmail.isBlank()) {
                                    manualMessage = "Por favor ingresa nombre y email del cliente."
                                    isManualSuccess = false
                                    return@Button
                                }
                                viewModel.registerManualCoupon(newUserName, newUserEmail, newCustomPin.ifBlank { null }) { success, msg ->
                                    isManualSuccess = success
                                    manualMessage = msg
                                    if (success) {
                                        newUserName = ""
                                        newUserEmail = ""
                                        newCustomPin = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_manual_coupon_submit_button")
                        ) {
                            Text("Emitir Cupón de Regalo ⚡", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (manualMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = manualMessage,
                                color = if (isManualSuccess) GreenConfirm else RedCancel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Coupons Directory
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DIRECTORIO DE CUPONES EMITIDOS 📋",
                        fontWeight = FontWeight.Bold,
                        color = TextIvory,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar por nombre, email o PIN") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = GoldPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_coupon_search_field"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color(0xFF334155))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredCoupons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se encontraron cupones emitidos.", color = TextMuted, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredCoupons.forEach { coupon ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161513)),
                                    border = BorderStroke(1.dp, if (coupon.isRedeemed) Color(0xFF27272A) else GoldPrimary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(coupon.userName, fontWeight = FontWeight.Bold, color = TextIvory, fontSize = 13.sp)
                                                Text(coupon.userEmail, color = TextMuted, fontSize = 11.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Emitido: ${coupon.createdAt}", color = TextMuted, fontSize = 10.sp)
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "PIN: ",
                                                        fontSize = 10.sp,
                                                        color = TextMuted
                                                    )
                                                    Text(
                                                        text = coupon.pin,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (coupon.isRedeemed) TextMuted else GoldAccent,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (coupon.isRedeemed) Color(0xFF27272A) else GoldPrimary.copy(alpha = 0.12f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(vertical = 2.dp, horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (coupon.isRedeemed) "CANJEADO" else "DISPONIBLE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (coupon.isRedeemed) TextMuted else GoldPrimary
                                                    )
                                                }
                                            }
                                        }

                                        if (!coupon.isRedeemed) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.redeemCoupon(coupon.pin) { _, _ -> }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("coupon_redeem_quick_btn_${coupon.pin}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.15f), contentColor = GoldPrimary),
                                                contentPadding = PaddingValues(0.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Registrar Canje (1-Click)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun saveExcelFileToDevice(context: android.content.Context, filename: String, content: String) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(context, "Archivo guardado en carpeta Descargas: $filename 📂", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }
        
        // Fallback for older devices/simulated environment
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = java.io.File(downloadsDir, filename)
        file.writeText(content, Charsets.UTF_8)
        android.widget.Toast.makeText(context, "Archivo guardado en Descargas: $filename 📂", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        // Sharing fallback in case write permission is not given or some sandbox exception occurs
        shareExcelFile(context, filename, content)
    }
}

fun shareExcelFile(context: android.content.Context, filename: String, content: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, filename)
            putExtra(android.content.Intent.EXTRA_TEXT, content)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Compartir Planilla Excel"))
    } catch (ex: Exception) {
        android.widget.Toast.makeText(context, "Error al generar archivo: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DataSincView(viewModel: BarberiaViewModel) {
    val context = LocalContext.current
    val users = remember {
        mutableStateOf(
            listOf(
                User(id = 1, fullName = "Felipe Andrés", email = "felipe@gmail.com", phone = "+56 9 8452 1122"),
                User(id = 2, fullName = "Gonzalo Ignacio", email = "gonzalo@gmail.com", phone = "+56 9 7411 9900"),
                User(id = 3, fullName = "Sebastián José", email = "seba@gmail.com", phone = "+56 9 9500 4422"),
                User(id = 4, fullName = "Marcela Paz", email = "marcela@contacto.cl", phone = "+56 9 6321 0055")
            )
        )
    }

    val appointments by viewModel.allAppointmentsFlow.collectAsState()
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()

    var activeExportPeriod by remember { mutableStateOf("Mensual") }
    var displayedResultCsv by remember { mutableStateOf("") }
    var inputImportCsvText by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Gestión de Planillas Excel 📊", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Text("📥 DESCARGAR REPORTES FINANCIEROS EXCEL (.XLS)", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
            Text("Exporta un libro de Excel con la recaudación, recargos y comisiones de manera 100% compatible con Microsoft Excel o Google Sheets.", fontSize = 10.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Diario", "Semanal", "Mensual", "Anual").forEach { period ->
                    FilterChip(
                        selected = activeExportPeriod == period,
                        onClick = { activeExportPeriod = period },
                        label = { Text(period) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val contentStr = viewModel.generateExportMovementCsv(activeExportPeriod, appointments, barbers, locations)
                    displayedResultCsv = contentStr
                    saveExcelFileToDevice(context, "reporte_movimientos_$activeExportPeriod.xls", contentStr)
                    viewModel.addNotification(
                        title = "Reporte Descargado Exitosamente 📗",
                        message = "Se descargó planilla de movimientos financieros en formato Excel compatible ($activeExportPeriod)"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp), tint = DarkBackground)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Descargar Planilla Excel de Movimientos", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Divider(color = GoldPrimary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
            Text("📂 BASE DE DATOS Y ENLACE EXCEL RÁPIDO", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
            Text("Descarga el listado de clientes en formato Excel o copia y pega filas directamente de tu Excel o Google Sheets.", fontSize = 10.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val contentStr = viewModel.generateUsersCsv(users.value)
                        displayedResultCsv = contentStr
                        saveExcelFileToDevice(context, "planilla_clientes_excel.xls", contentStr)
                        viewModel.addNotification(
                            title = "Base de datos exportada 📥",
                            message = "Se descargó listado de clientes en formato Excel (.xls)."
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Descargar Clientes Excel", color = TextIvory, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        inputImportCsvText = "ID\tNombre_Completo\tCorreo_Electronico\tCelular_Contacto\n5\tEduardo Valenzuela\teduval@outlook.cl\t+56952843322\n6\tConstanza Reyes\tconi@gmail.com\t+56965121477"
                        viewModel.addNotification(
                            title = "Plantilla Precargada",
                            message = "Se cargaron filas de ejemplo con formato Excel (Tabulación). Copia tus celdas de Excel y pégalas aquí."
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pegar Ejemplo Excel", color = TextIvory, fontSize = 11.sp)
                }
            }

            if (displayedResultCsv.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = GreenConfirm, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Planilla listada para guardar como archivo:", fontSize = 11.sp, color = GoldAccent)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(displayedResultCsv, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextIvory, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
                TextButton(
                    onClick = { displayedResultCsv = "" },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Limpiar Pantalla", color = RedCancel, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = inputImportCsvText,
                onValueChange = { inputImportCsvText = it },
                label = { Text("Pegue celdas copiadas desde Excel o CSV aquí...") },
                colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            if (inputImportCsvText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.importUsersFromCsv(inputImportCsvText) { count ->
                            inputImportCsvText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenConfirm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SINCRO / ACTUALIZAR DE EXCEL DIRECTO ⚡", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GmailSenderConfigView(viewModel: BarberiaViewModel) {
    val currentGmail by viewModel.gmailSenderAddress.collectAsState()
    val currentPass by viewModel.gmailAppPassword.collectAsState()
    val sendingStatus by viewModel.emailSendingStatus.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var emailInput by remember(currentGmail) { mutableStateOf(currentGmail) }
    var passwordInput by remember(currentPass) { mutableStateOf(currentPass) }
    var testRecipientInput by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Configuración de Envíos Reales (Gmail)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Guardado permanente en memoria del dispositivo.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Persistence Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentPass.isNotBlank()) GreenConfirm.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (currentPass.isNotBlank()) GreenConfirm.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (currentPass.isNotBlank()) GreenConfirm else GoldAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (currentPass.isNotBlank()) "Guardada Permanente" else "Sin Configurar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentPass.isNotBlank()) GreenConfirm else GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instructions Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "💡 ¿Por qué Google pide Contraseña de Aplicación?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Google NO permite usar tu contraseña habitual por seguridad. En su lugar, te genera una clave especial de 16 letras exclusiva para esta aplicación:\n\n" +
                        "1. Asegúrate de tener la 'Verificación en dos pasos' activa en tu cuenta Google.\n" +
                        "2. Abre el generador de 'Contraseñas de aplicaciones' de Google.\n" +
                        "3. Escribe un nombre (ej: 'Big Boys App') y presiona 'Crear'.\n" +
                        "4. Copia las 16 letras generadas y pégalas en el campo de abajo.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Abrir 'Contraseñas de Aplicaciones' de Google ↗️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gmail Address Field
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Correo Gmail Remitente") },
                placeholder = { Text("ejemplo.barberia@gmail.com") },
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App Password Field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Contraseña de Aplicación de Gmail (16 letras)") },
                placeholder = { Text("ej: abcd efgh ijkl mnop") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.updateGmailConfiguration(emailInput, passwordInput) { success, msg ->
                        resultMessage = Pair(success, msg)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = if (isDarkMode) DarkBackground else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Cuenta de Envío", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "🧪 Probar Envío a un Correo Real",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp
            )
            Text(
                "Ingresa tu propio correo personal para verificar que el mensaje llegue en tiempo real.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testRecipientInput,
                onValueChange = { testRecipientInput = it },
                label = { Text("Enviar correo de prueba a...") },
                placeholder = { Text("tu.correo.personal@gmail.com") },
                leadingIcon = { Icon(Icons.Outlined.Send, null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (testRecipientInput.isBlank() || !testRecipientInput.contains("@")) {
                        resultMessage = Pair(false, "Por favor ingresa un correo destinatario válido.")
                    } else {
                        viewModel.sendTestRealEmail(testRecipientInput.trim()) { success, msg ->
                            resultMessage = Pair(success, msg)
                        }
                    }
                },
                enabled = sendingStatus == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE2E8F0),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sendingStatus != null) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviando Correo Real...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar Correo de Prueba Real 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (resultMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (resultMessage!!.first) GreenConfirm.copy(alpha = 0.15f) else RedCancel.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (resultMessage!!.first) GreenConfirm else RedCancel),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (resultMessage!!.first) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (resultMessage!!.first) GreenConfirm else RedCancel,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = resultMessage!!.second,
                            color = if (resultMessage!!.first) GreenConfirm else RedCancel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UrbanBigBoysLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 4.dp)
                    .background(GoldPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 4.dp)
                    .background(GoldPrimary)
            )
        }

        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "BIG BOYS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.offset(x = 2.dp, y = 2.dp)
            )
            Text(
                text = "BIG BOYS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center
                ),
                color = TextIvory
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = "CLUB DE CABALLEROS CHILE",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                ),
                color = GoldPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GoldPrimary,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun BarbershopSplashScreen(
    onFinish: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2400)
        onFinish()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )

    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = LinearEasing),
        label = "splash_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF222228),
                        Color(0xFF121215),
                        Color(0xFF08080A)
                    ),
                    radius = 1200f
                )
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                onFinish()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                }
        ) {
            // Barber Emblem Crest with rotating outer ring and pulsing center
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Outer rotating dashed decorative gold circle
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotationAnim }) {
                    drawCircle(
                        color = GoldPrimary.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(20f, 15f), 0f
                            )
                        )
                    )
                }

                // Glowing background circle
                Box(
                    modifier = Modifier
                        .size(130.dp * pulseGlow)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldPrimary.copy(alpha = 0.25f),
                                    GoldAccent.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Inner solid gold border badge
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181D))
                        .border(BorderStroke(2.dp, GoldPrimary), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Barber Crown / Stars
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Shears Icon
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = "Logo Big Boys",
                            tint = GoldPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "EST. 2024",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                letterSpacing = 1.5.sp
                            ),
                            color = GoldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Brand Typography
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .background(GoldPrimary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "BIG BOYS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = (-1.5).sp,
                        fontSize = 38.sp
                    ),
                    color = TextIvory
                )
                Text(
                    text = ".",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp
                    ),
                    color = GoldPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .background(GoldPrimary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "BARBERSHOP & GENTLEMEN'S CLUB",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp
                ),
                color = GoldPrimary
            )

            Text(
                text = "CHILE · SANTIAGO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                ),
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Bottom loading bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(200.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF27272A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GoldAccent, GoldPrimary, Color(0xFFB8860B))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cargando estilo y tradición...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun LoginRegisterScreen(
    viewModel: BarberiaViewModel,
    onGuestAccess: () -> Unit,
    initialRegisterMode: Boolean = false
) {
    // Login form state
    var userRoleTab by remember { mutableStateOf("CLIENTE") } // "CLIENTE", "BARBERO"
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    var selectedBarberForLogin by remember(barbers) { mutableStateOf(barbers.firstOrNull()) }
    var barberPinInput by remember { mutableStateOf("1234") }
    var barberErrorMessage by remember { mutableStateOf<String?>(null) }
    var isBarberLoggingIn by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf(viewModel.getSavedRememberedEmail()) }
    var loginPasswordInput by remember { mutableStateOf(viewModel.getSavedRememberedPassword()) }
    var showLoginPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(viewModel.isRememberMeEnabled()) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    // Register form state
    var userFullnameInput by remember { mutableStateOf("") }
    var userRutInput by remember { mutableStateOf("") }
    var userPhoneInput by remember { mutableStateOf("") }
    var userEmailRegisterInput by remember { mutableStateOf("") }
    var userPasswordRegisterInput by remember { mutableStateOf("") }
    var userConfirmPasswordInput by remember { mutableStateOf("") }
    var userAvatarUri by remember { mutableStateOf<String?>(null) }
    var showRegisterPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var registerErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    val registerImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            userAvatarUri = uri.toString()
        }
    }

    var isRegisterMode by remember { mutableStateOf(initialRegisterMode) }

    var showAdminPinDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Security and identity validation calculations for registration
    val isRutValid = ValidationUtils.isValidChileanRut(userRutInput)
    val isEmailValid = ValidationUtils.isValidEmail(userEmailRegisterInput)
    val hasMinLength = userPasswordRegisterInput.length >= 6
    val hasLetter = userPasswordRegisterInput.any { it.isLetter() }
    val hasDigit = userPasswordRegisterInput.any { it.isDigit() }
    val passwordsMatch = userPasswordRegisterInput.isNotEmpty() && userPasswordRegisterInput == userConfirmPasswordInput
    val isPasswordValid = hasMinLength && hasLetter && hasDigit && passwordsMatch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(36.dp))
                UrbanBigBoysLogo()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Role Selector: Cliente vs Barbero
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = userRoleTab == "CLIENTE",
                        onClick = {
                            userRoleTab = "CLIENTE"
                            loginErrorMessage = null
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clientes", fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        ),
                        modifier = Modifier.weight(1f).testTag("tab_role_cliente")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = userRoleTab == "BARBERO",
                        onClick = {
                            userRoleTab = "BARBERO"
                            barberErrorMessage = null
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ContentCut, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Barberos 💈", fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = DarkBackground,
                            containerColor = CardBackground,
                            labelColor = TextIvory
                        ),
                        modifier = Modifier.weight(1f).testTag("tab_role_barbero")
                    )
                }
            }

            if (userRoleTab == "BARBERO") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("barber_portal_welcome_card"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.ContentCut, null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("ACCESO STAFF · BARBEROS", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Dashboard Diario y Gestión de Emergencias", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Accede a tu panel para ver tus citas diarias agendadas, atender clientes o suspender turnos por fuerza mayor con reembolso automático.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Quick 1-click test barber logins:
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FlashOn, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accesos Rápidos de Prueba (1 toque):", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val b = barbers.find { it.id == 1 } ?: barbers.firstOrNull()
                                        if (b != null) {
                                            viewModel.loginAsBarber(b, "1234") { _, _ -> }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("quick_barber_mateo"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Mateo S. (Talagante)", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        val b = barbers.find { it.id == 2 } ?: barbers.getOrNull(1)
                                        if (b != null) {
                                            viewModel.loginAsBarber(b, "1234") { _, _ -> }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                                    border = BorderStroke(1.dp, GoldPrimary),
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("quick_barber_carlos"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Carlos M. (Talagante)", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Barber List Selection
                item {
                    Text(
                        "O selecciona tu nombre de la lista del equipo:",
                        color = TextIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(barbers.take(6)) { b ->
                    val branch = locations.find { it.id == b.locationId }?.name ?: "Big boys"
                    val isSelected = selectedBarberForLogin?.id == b.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBarberForLogin = b }
                            .testTag("login_select_barber_${b.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.18f) else CardBackground
                        ),
                        border = BorderStroke(1.dp, if (isSelected) GoldPrimary else TextMuted.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(b.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = b.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, GoldPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.name, color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("$branch · ${b.specialty}", color = TextMuted, fontSize = 10.sp, maxLines = 1)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedBarberForLogin = b },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                            )
                        }
                    }
                }

                // Staff PIN
                item {
                    OutlinedTextField(
                        value = barberPinInput,
                        onValueChange = {
                            barberPinInput = it
                            barberErrorMessage = null
                        },
                        label = { Text("PIN de Seguridad Staff (Por defecto: 1234)") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = GoldPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = GoldPrimary,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barber_login_pin_input")
                    )
                }

                if (barberErrorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RedCancel),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = RedCancel, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(barberErrorMessage ?: "", color = RedCancel, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Submit Button
                item {
                    Button(
                        onClick = {
                            val b = selectedBarberForLogin ?: barbers.firstOrNull()
                            if (b == null) {
                                barberErrorMessage = "Selecciona un barbero para ingresar"
                                return@Button
                            }
                            isBarberLoggingIn = true
                            barberErrorMessage = null
                            viewModel.loginAsBarber(b, barberPinInput) { success, err ->
                                isBarberLoggingIn = false
                                if (!success) {
                                    barberErrorMessage = err ?: "PIN incorrecto"
                                }
                            }
                        },
                        enabled = !isBarberLoggingIn,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_barber_login_submit"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBarberLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.ContentCut, null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INICIAR TURNO DE BARBERO 💈", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // CLIENTE ROLE SECTION
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { 
                                isRegisterMode = false 
                                registerErrorMessage = null
                                loginErrorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterMode) GoldPrimary else CardBackground
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("INGRESAR", color = if (!isRegisterMode) DarkBackground else TextIvory, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                isRegisterMode = true 
                                registerErrorMessage = null
                                loginErrorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterMode) GoldPrimary else CardBackground
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("REGISTRARSE", color = if (isRegisterMode) DarkBackground else TextIvory, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            if (isRegisterMode) {
                item {
                    Text(
                        "Crea tu cuenta personalizada con contraseña segura",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Avatar Picker in Registration Form
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { registerImagePickerLauncher.launch("image/*") }
                        ) {
                            if (!userAvatarUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(userAvatarUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de perfil seleccionada",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, GoldPrimary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(CardBackground, CircleShape)
                                        .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AddAPhoto,
                                        contentDescription = "Agregar foto de perfil",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(GoldPrimary, CircleShape)
                                    .border(1.5.dp, DarkBackground, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (userAvatarUri.isNullOrBlank()) Icons.Filled.Add else Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (userAvatarUri.isNullOrBlank()) "Toca para agregar foto desde tu dispositivo" else "Foto seleccionada (Toca para cambiar)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (userAvatarUri.isNullOrBlank()) TextMuted else GreenConfirm,
                            fontSize = 11.sp
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = userFullnameInput,
                        onValueChange = { 
                            userFullnameInput = it
                            registerErrorMessage = null
                        },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = GoldPrimary) },
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_name")
                    )
                }

                item {
                    OutlinedTextField(
                        value = userRutInput,
                        onValueChange = { input ->
                            // Allow digits, k, K, dots, hyphens and spaces
                            val filtered = input.filter { it.isDigit() || it == 'k' || it == 'K' || it == '-' || it == '.' || it == ' ' }
                            userRutInput = filtered
                            registerErrorMessage = null
                        },
                        label = { Text("RUT Chileno (Ej: 19.342.110-3)") },
                        leadingIcon = { Icon(Icons.Filled.Badge, null, tint = GoldPrimary) },
                        trailingIcon = {
                            if (userRutInput.isNotBlank()) {
                                Icon(
                                    imageVector = if (isRutValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = if (isRutValid) "RUT Válido" else "RUT Inválido",
                                    tint = if (isRutValid) GreenConfirm else RedCancel
                                )
                            }
                        },
                        supportingText = {
                            if (userRutInput.isNotBlank()) {
                                if (isRutValid) {
                                    Text("✓ RUT válido (Módulo 11: ${ValidationUtils.formatChileanRut(userRutInput)})", color = GreenConfirm, fontSize = 11.sp)
                                } else {
                                    Text("✕ RUT inválido (debe cumplir Módulo 11, ej: 19.342.110-3)", color = RedCancel, fontSize = 11.sp)
                                }
                            } else {
                                Text("Requerido para verificación de cliente y reservas", color = TextMuted, fontSize = 11.sp)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = if (userRutInput.isNotBlank() && !isRutValid) RedCancel else GoldPrimary,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_rut")
                    )
                }

                item {
                    OutlinedTextField(
                        value = userEmailRegisterInput,
                        onValueChange = { 
                            userEmailRegisterInput = it
                            registerErrorMessage = null
                        },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = GoldPrimary) },
                        trailingIcon = {
                            if (userEmailRegisterInput.isNotBlank()) {
                                Icon(
                                    imageVector = if (isEmailValid) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = if (isEmailValid) "Correo Válido" else "Correo Inválido",
                                    tint = if (isEmailValid) GreenConfirm else RedCancel
                                )
                            }
                        },
                        supportingText = {
                            if (userEmailRegisterInput.isNotBlank()) {
                                if (isEmailValid) {
                                    Text("✓ Formato de correo válido", color = GreenConfirm, fontSize = 11.sp)
                                } else {
                                    Text("✕ Ingresa un correo válido (ej: usuario@dominio.cl)", color = RedCancel, fontSize = 11.sp)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = if (userEmailRegisterInput.isNotBlank() && !isEmailValid) RedCancel else GoldPrimary,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email")
                    )
                }

                item {
                    OutlinedTextField(
                        value = userPhoneInput,
                        onValueChange = { 
                            userPhoneInput = it
                            registerErrorMessage = null
                        },
                        label = { Text("Teléfono de Contacto (Ej: +569...)") },
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = GoldPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_phone")
                    )
                }

                item {
                    OutlinedTextField(
                        value = userPasswordRegisterInput,
                        onValueChange = { 
                            userPasswordRegisterInput = it
                            registerErrorMessage = null
                        },
                        label = { Text("Contraseña a tu gusto") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = GoldPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showRegisterPassword = !showRegisterPassword }) {
                                Icon(
                                    imageVector = if (showRegisterPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showRegisterPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = GoldPrimary
                                )
                            }
                        },
                        visualTransformation = if (showRegisterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password")
                    )
                }

                item {
                    OutlinedTextField(
                        value = userConfirmPasswordInput,
                        onValueChange = { 
                            userConfirmPasswordInput = it
                            registerErrorMessage = null
                        },
                        label = { Text("Confirmar Contraseña") },
                        leadingIcon = { Icon(Icons.Filled.LockReset, null, tint = GoldPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showConfirmPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = GoldPrimary
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_confirm_password")
                    )
                }

                // Identity & Security Requirements Checklist Box
                item {
                    val allRequirementsMet = isRutValid && isEmailValid && isPasswordValid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, if (allRequirementsMet) GreenConfirm.copy(alpha = 0.6f) else GoldPrimary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, null, tint = if (allRequirementsMet) GreenConfirm else GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Requisitos de Validación e Identidad:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (allRequirementsMet) GreenConfirm else TextIvory
                                )
                            }

                            SecurityRequirementRow(
                                title = "RUT chileno válido (Módulo 11)",
                                isMet = isRutValid
                            )
                            SecurityRequirementRow(
                                title = "Correo electrónico válido y único",
                                isMet = isEmailValid
                            )
                            SecurityRequirementRow(
                                title = "Mínimo 6 caracteres",
                                isMet = hasMinLength
                            )
                            SecurityRequirementRow(
                                title = "Al menos una letra (a-z / A-Z)",
                                isMet = hasLetter
                            )
                            SecurityRequirementRow(
                                title = "Al menos un número (0-9)",
                                isMet = hasDigit
                            )
                            SecurityRequirementRow(
                                title = "Las contraseñas coinciden",
                                isMet = passwordsMatch
                            )
                        }
                    }
                }

                if (registerErrorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = RedCancel, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(registerErrorMessage ?: "", color = RedCancel, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (userFullnameInput.isBlank() || userFullnameInput.trim().length < 3) {
                                registerErrorMessage = "Por favor ingresa tu nombre completo (mínimo 3 caracteres)"
                                return@Button
                            }
                            if (userRutInput.isBlank()) {
                                registerErrorMessage = "Por favor ingresa tu RUT chileno"
                                return@Button
                            }
                            if (!isRutValid) {
                                registerErrorMessage = "El RUT ingresado no es válido (debe cumplir verificación Módulo 11, ej: 19.342.110-3)"
                                return@Button
                            }
                            if (userEmailRegisterInput.isBlank()) {
                                registerErrorMessage = "Por favor ingresa tu correo electrónico"
                                return@Button
                            }
                            if (!isEmailValid) {
                                registerErrorMessage = "Por favor ingresa un correo electrónico válido (ej: usuario@dominio.cl)"
                                return@Button
                            }
                            if (userPhoneInput.isBlank()) {
                                registerErrorMessage = "Por favor ingresa tu teléfono"
                                return@Button
                            }
                            if (!hasMinLength) {
                                registerErrorMessage = "La contraseña debe tener al menos 6 caracteres"
                                return@Button
                            }
                            if (!hasLetter) {
                                registerErrorMessage = "La contraseña debe contener al menos una letra"
                                return@Button
                            }
                            if (!hasDigit) {
                                registerErrorMessage = "La contraseña debe contener al menos un número"
                                return@Button
                            }
                            if (!passwordsMatch) {
                                registerErrorMessage = "Las contraseñas no coinciden"
                                return@Button
                            }

                            isRegistering = true
                            registerErrorMessage = null
                            viewModel.register(
                                fullName = userFullnameInput,
                                email = userEmailRegisterInput,
                                phone = userPhoneInput,
                                rut = userRutInput,
                                password = userPasswordRegisterInput,
                                avatarUrl = userAvatarUri ?: ""
                            ) { success, errorMsg ->
                                isRegistering = false
                                if (!success) {
                                    registerErrorMessage = errorMsg ?: "Error al registrar la cuenta."
                                }
                            }
                        },
                        enabled = !isRegistering,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_submit")
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CREAR CUENTA Y OBTENER REGALO", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // LOGIN MODE
                item {
                    Text(
                        "Ingresa con tu correo y contraseña registrados",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // 🧪 REGISTERED TEST USER ACCESS CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_user_quick_access_card"),
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.12f)),
                        border = BorderStroke(1.2.dp, GoldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(GoldPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Science, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "USUARIO REGISTRADO PARA PRUEBAS",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Email: cliente@bigboys.cl  ·  Clave: Bigboys2026",
                                color = TextIvory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Toca el botón para iniciar sesión inmediatamente con esta cuenta:",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    emailInput = "cliente@bigboys.cl"
                                    loginPasswordInput = "Bigboys2026"
                                    loginErrorMessage = null
                                    isLoggingIn = true
                                    viewModel.login("cliente@bigboys.cl", "Bigboys2026") { success, errorMsg ->
                                        isLoggingIn = false
                                        if (!success) {
                                            loginErrorMessage = errorMsg ?: "Error al acceder con usuario de prueba"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("btn_quick_login_test_user"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.FlashOn, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INGRESAR CON USUARIO DE PRUEBA ⚡", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            loginErrorMessage = null
                        },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = GoldPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email")
                    )
                }

                item {
                    OutlinedTextField(
                        value = loginPasswordInput,
                        onValueChange = {
                            loginPasswordInput = it
                            loginErrorMessage = null
                        },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = GoldPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                Icon(
                                    imageVector = if (showLoginPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showLoginPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = GoldPrimary
                                )
                            }
                        },
                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = CardBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password")
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp)
                            .testTag("remember_me_checkbox_row")
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldPrimary,
                                uncheckedColor = TextMuted,
                                checkmarkColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recordarme para iniciar sesión",
                            color = TextIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (loginErrorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RedCancel.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null, tint = RedCancel, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(loginErrorMessage ?: "", color = RedCancel, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (emailInput.isBlank()) {
                                loginErrorMessage = "Por favor ingresa tu correo electrónico"
                                return@Button
                            }
                            if (loginPasswordInput.isBlank()) {
                                loginErrorMessage = "Por favor ingresa tu contraseña"
                                return@Button
                            }

                            isLoggingIn = true
                            loginErrorMessage = null
                            viewModel.login(emailInput, loginPasswordInput, rememberMe = rememberMe) { success, errorMsg ->
                                isLoggingIn = false
                                if (!success) {
                                    loginErrorMessage = errorMsg ?: "Credenciales incorrectas"
                                }
                            }
                        },
                        enabled = !isLoggingIn,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit")
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Login, null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INICIAR SESIÓN", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onGuestAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, TextMuted),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("guest_login_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PersonOutline, null, tint = TextIvory, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INGRESAR COMO INVITADO 👤", color = TextIvory, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                emailInput = "cliente@bigboys.cl"
                                loginPasswordInput = "Bigboys2026"
                                loginErrorMessage = null 
                            },
                        color = Color.Transparent
                    ) {
                        Text(
                            "Protip: Si acabas de registrarte, ingresa tu correo y contraseña elegida",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Divider(color = GoldPrimary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Security, null, tint = GoldPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "⚙️ SECCIÓN ADMINISTRATIVA SECRETA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Requiere un PIN de seguridad del local para gestionar citas y visualizar reportes.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAdminPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            border = BorderStroke(1.dp, GoldPrimary)
                        ) {
                            Text("INGRESAR PIN DE ADMIN", color = GoldPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
            } // Close else for userRoleTab == "BARBERO"
        }

        if (showAdminPinDialog) {
            Dialog(onDismissRequest = { showAdminPinDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, GoldPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.LockOpen, null, tint = GoldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Ingresa PIN de Administrador", fontWeight = FontWeight.Bold, color = TextIvory)
                        Text("PIN por defecto: 1234", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = adminPinInput,
                            onValueChange = {
                                adminPinInput = it
                                pinError = false
                            },
                            label = { Text("Código PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pin_field")
                        )

                        if (pinError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("PIN de local incorrecto", color = RedCancel, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAdminPinDialog = false }) {
                                Text("Cancelar", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.loginAsAdminWithPin(adminPinInput) { success ->
                                        if (success) {
                                            showAdminPinDialog = false
                                            adminPinInput = ""
                                        } else {
                                            pinError = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                            ) {
                                Text("Verificar", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarberQuickLoginDialog(
    viewModel: BarberiaViewModel,
    onDismiss: () -> Unit
) {
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    var selectedBarber by remember(barbers) { mutableStateOf(barbers.firstOrNull()) }
    var pinInput by remember { mutableStateOf("1234") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.5.dp, GoldPrimary),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ContentCut, null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Acceso Rápido Barbero",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextIvory
                        )
                        Text(
                            "Ingresa a tu dashboard diario de atención",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Selecciona Barbero:", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(barbers) { b ->
                        val branch = locations.find { it.id == b.locationId }?.name ?: "Big boys"
                        val isSelected = selectedBarber?.id == b.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { selectedBarber = b },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkBackground),
                            border = BorderStroke(1.dp, if (isSelected) GoldPrimary else TextMuted.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(b.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = b.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(28.dp).clip(CircleShape).border(1.dp, GoldPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.name, color = TextIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("$branch · ${b.specialty}", color = TextMuted, fontSize = 10.sp, maxLines = 1)
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedBarber = b },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = null
                    },
                    label = { Text("PIN de Barbero (Por defecto: 1234)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = GoldPrimary,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("quick_barber_pin_field")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage ?: "", color = RedCancel, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, TextMuted)) {
                        Text("Cancelar", color = TextIvory, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val b = selectedBarber ?: barbers.firstOrNull()
                            if (b == null) {
                                errorMessage = "Selecciona un barbero"
                                return@Button
                            }
                            isSubmitting = true
                            viewModel.loginAsBarber(b, pinInput) { success, err ->
                                isSubmitting = false
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMessage = err ?: "PIN incorrecto"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1.3f).testTag("quick_barber_submit_btn")
                    ) {
                        Text("Iniciar Turno", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityRequirementRow(
    title: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) GreenConfirm else TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (isMet) TextIvory else TextMuted
        )
    }
}

@Composable
fun ModifyAppointmentDialog(
    appointment: Appointment,
    viewModel: BarberiaViewModel,
    onDismiss: () -> Unit
) {
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()

    var name by remember { mutableStateOf(appointment.customerName) }
    var phone by remember { mutableStateOf(appointment.customerPhone) }
    var email by remember { mutableStateOf(appointment.customerEmail) }
    var service by remember { mutableStateOf(appointment.serviceName) }
    var priceStr by remember { mutableStateOf(appointment.servicePrice.toString()) }
    var dateTimeStr by remember { mutableStateOf(appointment.dateTime) }
    var selectedBarberId by remember { mutableStateOf(appointment.barberId) }
    var selectedLocationId by remember { mutableStateOf(appointment.locationId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "✍️ Modificar Agenda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextMuted)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 10.dp), color = GoldPrimary.copy(alpha = 0.2f))

                // Customer Fields
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Cliente") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Date Time Field
                OutlinedTextField(
                    value = dateTimeStr,
                    onValueChange = { dateTimeStr = it },
                    label = { Text("Fecha y Hora (Ej: 2026-06-09 10:00)") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Service Details
                OutlinedTextField(
                    value = service,
                    onValueChange = { service = it },
                    label = { Text("Servicio") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Precio CLP") },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = GoldPrimary, focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                // Location Selector
                Text("Sucursal:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    locations.forEach { loc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLocationId = loc.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedLocationId == loc.id,
                                onClick = { selectedLocationId = loc.id },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(loc.name, color = TextIvory, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barber Selector
                Text("Barbero:", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                val relevantBarbers = barbers.filter { it.locationId == selectedLocationId }
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (relevantBarbers.isEmpty()) {
                        Text("No hay barberos asignados a esta sucursal.", color = RedCancel, fontSize = 11.sp)
                    } else {
                        relevantBarbers.forEach { barb ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBarberId = barb.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedBarberId == barb.id,
                                    onClick = { selectedBarberId = barb.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(barb.name, color = TextIvory, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextIvory),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: appointment.servicePrice
                            val updated = appointment.copy(
                                customerName = name,
                                customerPhone = phone,
                                customerEmail = email,
                                dateTime = dateTimeStr,
                                serviceName = service,
                                servicePrice = price,
                                locationId = selectedLocationId,
                                barberId = selectedBarberId
                            )
                            viewModel.updateAppointmentAdmin(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar", color = DarkBackground)
                    }
                }
            }
        }
    }
}
