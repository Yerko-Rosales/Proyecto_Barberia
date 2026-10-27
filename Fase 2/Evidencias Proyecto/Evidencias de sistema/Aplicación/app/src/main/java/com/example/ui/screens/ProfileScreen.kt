package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberiaViewModel

val AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80"
)

@Composable
fun ProfileDialog(
    viewModel: BarberiaViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("profile_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            ProfileScreenContent(
                viewModel = viewModel,
                onClose = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    viewModel: BarberiaViewModel,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val adminName by viewModel.adminName.collectAsState()
    val adminPin by viewModel.adminPin.collectAsState()
    val adminAvatarUrl by viewModel.adminAvatarUrl.collectAsState()

    // Form states
    var nameInput by remember(loggedInUser, isAdmin, adminName) {
        mutableStateOf(if (isAdmin) adminName else loggedInUser?.fullName ?: "")
    }
    var phoneInput by remember(loggedInUser) {
        mutableStateOf(loggedInUser?.phone ?: "")
    }
    var selectedAvatarUrl by remember(loggedInUser, isAdmin, adminAvatarUrl) {
        mutableStateOf(if (isAdmin) adminAvatarUrl else loggedInUser?.avatarUrl ?: "")
    }
    var customAvatarUrlInput by remember { mutableStateOf("") }
    var showCustomUrlInput by remember { mutableStateOf(false) }

    // Launcher for selecting profile photo from device gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUrl = uri.toString()
            Toast.makeText(context, "Foto seleccionada del dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    // Password / PIN states
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var adminCurrentPinInput by remember { mutableStateOf("") }
    var adminNewPinInput by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAdmin) "Perfil de Administrador" else "Mi Perfil Personal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personaliza tus datos, tema y credenciales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Theme Quick Action in Top Bar
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("profile_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkMode) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER WITH AVATAR PREVIEW & BADGE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") }
                        ) {
                            if (selectedAvatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = if (isAdmin) "AD" else {
                                        nameInput.split(" ").take(2).map { it.firstOrNull() ?: "" }.joinToString("")
                                    }.ifBlank { "BB" }
                                    Text(
                                        text = initials.uppercase(),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (isDarkMode) DarkBackground else Color.White
                                    )
                                }
                            }

                            // Camera / Edit badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = "Seleccionar Foto del Dispositivo",
                                    tint = if (isDarkMode) DarkBackground else Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = nameInput.ifBlank { if (isAdmin) "Administrador" else "Usuario Big boys" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isAdmin) "admin@bigboys.cl" else loggedInUser?.email ?: "usuario@bigboys.cl",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAdmin) "Administrador Global 🛡️" else "Cliente VIP Big boys ⭐",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. THEME CUSTOMIZATION CARD (MODO CLARO / OSCURO)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Tema de la Aplicación",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDarkMode) "Modo Oscuro (Negro Matte & Oro)" else "Modo Claro (Blanco Elegante & Ámbar)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDarkMode) DarkBackground else Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("theme_switch")
                            )
                        }
                    }
                }
            }

            // 3. AVATAR PICKER (SELECCIÓN DE FOTO DE PERFIL)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Elige tu Foto de Perfil",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            "Selecciona una foto de tu dispositivo o un avatar prediseñado:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Button to select image from device
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("pick_device_image_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Seleccionar foto desde tu dispositivo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            "O elige uno de los avatares predeterminados:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Avatar Presets Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            // Option: Default / Initials
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (selectedAvatarUrl.isBlank()) 3.dp else 1.dp,
                                            color = if (selectedAvatarUrl.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarUrl = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = "Iniciales",
                                        tint = if (selectedAvatarUrl.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(AVATAR_PRESETS) { url ->
                                val isSelected = selectedAvatarUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarUrl = url }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Preset avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Custom URL toggle
                        TextButton(
                            onClick = { showCustomUrlInput = !showCustomUrlInput },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                if (showCustomUrlInput) Icons.Filled.ExpandLess else Icons.Filled.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showCustomUrlInput) "Ocultar URL personalizada" else "Ingresar enlace de imagen personalizado")
                        }

                        if (showCustomUrlInput) {
                            OutlinedTextField(
                                value = customAvatarUrlInput,
                                onValueChange = {
                                    customAvatarUrlInput = it
                                    if (it.startsWith("http")) {
                                        selectedAvatarUrl = it
                                    }
                                },
                                label = { Text("URL de imagen (https://...)") },
                                leadingIcon = { Icon(Icons.Filled.Image, null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 4. BASIC DETAILS (NAME & PHONE)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Datos Personales",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(if (isAdmin) "Nombre de Administrador" else "Nombre Completo") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!isAdmin) {
                            if (!loggedInUser?.rut.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = loggedInUser?.rut ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("RUT Registrado (Verificado)") },
                                    leadingIcon = { Icon(Icons.Filled.Badge, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = { Icon(Icons.Filled.VerifiedUser, "RUT Verificado", tint = GreenConfirm) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_rut_display"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("Teléfono de Contacto") },
                                leadingIcon = { Icon(Icons.Filled.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_phone_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 5. SECURITY / PASSWORD / PIN CHANGE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isAdmin) "Cambiar PIN de Acceso Admin" else "Cambiar Contraseña",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (isAdmin) "Deja en blanco el nuevo PIN si no deseas modificarlo." else "Completa los campos solo si deseas cambiar tu contraseña actual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (isAdmin) {
                            OutlinedTextField(
                                value = adminCurrentPinInput,
                                onValueChange = { adminCurrentPinInput = it },
                                label = { Text("PIN Actual de Administrador (Obligatorio para guardar)") },
                                leadingIcon = { Icon(Icons.Filled.Key, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_current_pin_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = adminNewPinInput,
                                onValueChange = { adminNewPinInput = it },
                                label = { Text("Nuevo PIN (Opcional, min 4 dígitos)") },
                                leadingIcon = { Icon(Icons.Filled.VpnKey, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_new_pin_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = currentPasswordInput,
                                onValueChange = { currentPasswordInput = it },
                                label = { Text("Contraseña Actual") },
                                leadingIcon = { Icon(Icons.Filled.Key, null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_current_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = { newPasswordInput = it },
                                label = { Text("Nueva Contraseña (Opcional)") },
                                leadingIcon = { Icon(Icons.Filled.LockReset, null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_new_password_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (newPasswordInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text("Confirmar Nueva Contraseña") },
                                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_confirm_password_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FEEDBACK MESSAGES
            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (successMessage != null) {
                item {
                    Surface(
                        color = GreenConfirm.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GreenConfirm)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenConfirm)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                color = GreenConfirm,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // «Mis datos»: derechos del titular (solo clientes con cuenta)
            if (!isAdmin && loggedInUser != null) {
                item { MyDataSection(viewModel = viewModel) }
            }

            // 6. SAVE BUTTON
            item {
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        isSaving = true

                        if (isAdmin) {
                            viewModel.updateAdminProfile(
                                name = nameInput,
                                currentPin = adminCurrentPinInput,
                                newPin = adminNewPinInput.ifBlank { null },
                                avatarUrl = selectedAvatarUrl
                            ) { success, msg ->
                                isSaving = false
                                if (success) {
                                    successMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        } else {
                            if (newPasswordInput.isNotBlank() && newPasswordInput != confirmPasswordInput) {
                                errorMessage = "Las contraseñas nuevas no coinciden."
                                isSaving = false
                                return@Button
                            }

                            viewModel.updateUserProfile(
                                name = nameInput,
                                phone = phoneInput,
                                currentPassword = currentPasswordInput,
                                newPassword = newPasswordInput.ifBlank { null },
                                avatarUrl = selectedAvatarUrl
                            ) { success, msg ->
                                isSaving = false
                                if (success) {
                                    successMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = if (isDarkMode) DarkBackground else Color.White
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (isDarkMode) DarkBackground else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GUARDAR CAMBIOS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // BARBER PORTAL ACCESS QUICK SHORTCUT
            item {
                OutlinedButton(
                    onClick = {
                        val barbers = viewModel.allBarbersFlow.value
                        val firstBarber = barbers.firstOrNull()
                        if (firstBarber != null) {
                            viewModel.loginAsBarber(firstBarber, "1234") { _, _ ->
                                onClose()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_switch_to_barber_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Filled.ContentCut, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresar a Modo Barbero 💈", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
