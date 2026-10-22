package com.example.bigboys

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.bigboys.model.UserRole
import com.example.bigboys.ui.*
import com.example.bigboys.ui.theme.*

class MainActivity : ComponentActivity() {
    private val repository = BarberRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BigBoysTheme {
                AppNavigator(repository = repository)
            }
        }
    }
}

@Composable
fun AppNavigator(repository: BarberRepository) {
    var showSplash by remember { mutableStateOf(true) }
    var showApiMonitor by remember { mutableStateOf(false) }
    val currentUser by repository.currentUser.collectAsState()

    AnimatedContent(
        targetState = when {
            showSplash -> AppScreenState.SPLASH
            currentUser == null -> AppScreenState.LOGIN
            else -> AppScreenState.MAIN
        },
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AppScreenTransition"
    ) { targetState ->
        when (targetState) {
            AppScreenState.SPLASH -> {
                SplashScreen(
                    onAnimationFinished = {
                        showSplash = false
                    }
                )
            }
            AppScreenState.LOGIN -> {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = { user ->
                        repository.setCurrentUser(user)
                    },
                    onContinueAsGuest = {
                        // Quick continue as default guest client
                        repository.login("cliente.test@bigboys.cl")
                    }
                )
            }
            AppScreenState.MAIN -> {
                MainScreen(
                    repository = repository,
                    onReplaySplash = { showSplash = true },
                    onOpenApiMonitor = { showApiMonitor = true }
                )
            }
        }
    }

    if (showApiMonitor) {
        ApiMonitorSheet(
            apiService = repository.apiService,
            onDismiss = { showApiMonitor = false }
        )
    }
}

enum class AppScreenState {
    SPLASH,
    LOGIN,
    MAIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: BarberRepository,
    onReplaySplash: () -> Unit,
    onOpenApiMonitor: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    var clientTab by remember { mutableStateOf(0) } // 0: Agendar, 1: Mis Citas
    var showAuthDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val isBarber = currentUser?.role == UserRole.BARBER
    var selectedBarberName by remember {
        mutableStateOf(currentUser?.barberName ?: repository.barbers.first().name)
    }

    LaunchedEffect(currentUser) {
        if (currentUser?.role == UserRole.BARBER && currentUser?.barberName != null) {
            selectedBarberName = currentUser!!.barberName!!
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BIG BOYS",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Live API indicator chip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceVariant,
                                    modifier = Modifier.clickable { onOpenApiMonitor() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(SuccessGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "APIs",
                                            color = GoldLight,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isBarber) "Dashboard de Barbero" else "Reserva Online & Citas",
                                color = OnDarkMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Right actions: User pill + Overflow menu
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBarber) GoldPrimary else DarkSurfaceVariant,
                                onClick = { showMenu = true },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isBarber) Icons.Default.Face else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isBarber) Color.Black else GoldLight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBarber) "Barbero (${currentUser?.fullName?.split(" ")?.first() ?: "Staff"})"
                                               else currentUser?.fullName?.split(" ")?.first() ?: "Invitado",
                                        color = if (isBarber) Color.Black else OnDarkText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isBarber) Color.Black else OnDarkMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📡 Monitor de APIs & Correo", color = GoldLight, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onOpenApiMonitor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("✨ Ver Intro Animada", color = OnDarkText, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GoldPrimary) },
                                    onClick = {
                                        showMenu = false
                                        onReplaySplash()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔄 Cambiar de Usuario", color = OnDarkText, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GoldLight) },
                                    onClick = {
                                        showMenu = false
                                        showAuthDialog = true
                                    }
                                )
                                Divider(color = DarkSurfaceVariant)
                                DropdownMenuItem(
                                    text = { Text("🚪 Cerrar Sesión", color = ErrorRed, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ErrorRed) },
                                    onClick = {
                                        showMenu = false
                                        repository.logout()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            if (!isBarber) {
                NavigationBar(
                    containerColor = DarkSurface
                ) {
                    NavigationBarItem(
                        selected = clientTab == 0,
                        onClick = { clientTab = 0 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Agendar") },
                        label = { Text("Agendar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = OnDarkMuted,
                            unselectedTextColor = OnDarkMuted
                        )
                    )
                    NavigationBarItem(
                        selected = clientTab == 1,
                        onClick = { clientTab = 1 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Mis Citas") },
                        label = { Text("Mis Citas") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = OnDarkMuted,
                            unselectedTextColor = OnDarkMuted
                        )
                    )
                }
            } else {
                // Barber bottom bar with return to client option
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sesión Barbero: ${currentUser?.barberName ?: "Staff"}",
                            color = OnDarkMuted,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                repository.logout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Text("Cerrar Sesión", color = GoldLight, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBarber) {
                BarberDashboardScreen(
                    repository = repository,
                    selectedBarberName = selectedBarberName,
                    onSelectBarber = { selectedBarberName = it }
                )
            } else {
                when (clientTab) {
                    0 -> ClientBookingScreen(
                        repository = repository,
                        currentUser = currentUser,
                        onRequestLogin = { showAuthDialog = true },
                        onBookingSuccess = { clientTab = 1 }
                    )
                    1 -> MyAppointmentsScreen(
                        repository = repository
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            repository = repository,
            onDismiss = { showAuthDialog = false },
            onSuccess = { user ->
                showAuthDialog = false
            }
        )
    }
}
