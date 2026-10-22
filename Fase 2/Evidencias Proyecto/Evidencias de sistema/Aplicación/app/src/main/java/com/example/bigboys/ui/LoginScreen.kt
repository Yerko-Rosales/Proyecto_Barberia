package com.example.bigboys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigboys.data.BarberRepository
import com.example.bigboys.model.User
import com.example.bigboys.model.UserRole
import com.example.bigboys.ui.theme.*

@Composable
fun LoginScreen(
    repository: BarberRepository,
    onLoginSuccess: (User) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    
    // Register fields
    var rut by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CLIENT) }
    var selectedBarberName by remember { mutableStateOf("Carlos 'El Patrón'") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        item {
            // Brand Logo & Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(GoldPrimary.copy(alpha = 0.3f), DarkSurfaceVariant)
                        )
                    )
                    .border(2.dp, GoldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Big Boys",
                        tint = GoldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "BB",
                        color = GoldLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BIG BOYS BARBERÍA",
                color = GoldPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = if (isRegisterMode) "Crea tu cuenta única para App y Web" else "Inicia sesión para gestionar tus citas y turnos",
                color = OnDarkMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // API Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "APIs & Base de Datos Web Conectadas",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Demo Access Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Acceso Rápido de Prueba (1-Tap):",
                            color = OnDarkText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                email = "cliente.test@bigboys.cl"
                                isRegisterMode = false
                                val res = repository.login(email)
                                res.onSuccess { onLoginSuccess(it) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("👤 Cliente (Juan)", color = GoldLight, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                email = "carlos@bigboys.cl"
                                isRegisterMode = false
                                val res = repository.login(email)
                                res.onSuccess { onLoginSuccess(it) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("💈 Barbero (Carlos)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                email = "matias@bigboys.cl"
                                isRegisterMode = false
                                val res = repository.login(email)
                                res.onSuccess { onLoginSuccess(it) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkText),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("💈 Matías (Barbero)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                email = "diego@bigboys.cl"
                                isRegisterMode = false
                                val res = repository.login(email)
                                res.onSuccess { onLoginSuccess(it) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkText),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("💈 Diego (Barbero)", fontSize = 10.sp)
                        }
                    }
                }
            }

            // Tab Selector: Login vs Register
            TabRow(
                selectedTabIndex = if (isRegisterMode) 1 else 0,
                containerColor = DarkSurface,
                contentColor = GoldPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = !isRegisterMode,
                    onClick = {
                        isRegisterMode = false
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Iniciar Sesión",
                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRegisterMode) GoldPrimary else OnDarkMuted
                        )
                    }
                )
                Tab(
                    selected = isRegisterMode,
                    onClick = {
                        isRegisterMode = true
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Crear Cuenta",
                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRegisterMode) GoldPrimary else OnDarkMuted
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Form Fields
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isRegisterMode) {
                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )

                    // RUT
                    OutlinedTextField(
                        value = rut,
                        onValueChange = { rut = it },
                        label = { Text("RUT (Ej: 12.345.678-9)") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (+56 9 1234 5678)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldLight) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = OnDarkText,
                            unfocusedTextColor = OnDarkText
                        )
                    )

                    // Role Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = UserRole.CLIENT },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedRole == UserRole.CLIENT) GoldPrimary else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedRole == UserRole.CLIENT) GoldPrimary else DarkSurfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.CLIENT) Color.Black else GoldLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Soy Cliente",
                                    color = if (selectedRole == UserRole.CLIENT) Color.Black else OnDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = UserRole.BARBER },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedRole == UserRole.BARBER) GoldPrimary else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedRole == UserRole.BARBER) GoldPrimary else DarkSurfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = if (selectedRole == UserRole.BARBER) Color.Black else GoldLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Soy Barbero Staff",
                                    color = if (selectedRole == UserRole.BARBER) Color.Black else OnDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldLight) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = OnDarkText,
                        unfocusedTextColor = OnDarkText
                    )
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldLight) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Ocultar" else "Ver",
                                color = GoldLight,
                                fontSize = 11.sp
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = OnDarkText,
                        unfocusedTextColor = OnDarkText
                    )
                )

                if (!isRegisterMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldPrimary,
                                uncheckedColor = OnDarkMuted,
                                checkmarkColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recordarme para iniciar sesión",
                            color = OnDarkText,
                            fontSize = 13.sp
                        )
                    }
                }

                // Error Message box
                errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ErrorRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Submit Button
                Button(
                    onClick = {
                        errorMessage = null
                        if (isRegisterMode) {
                            if (email.isBlank() || rut.isBlank() || fullName.isBlank() || phone.isBlank()) {
                                errorMessage = "Por favor completa todos los campos del registro."
                                return@Button
                            }
                            isLoading = true
                            val result = repository.register(
                                email = email,
                                rut = rut,
                                fullName = fullName,
                                phone = phone,
                                role = selectedRole,
                                barberName = if (selectedRole == UserRole.BARBER) fullName else null
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = { user -> onLoginSuccess(user) },
                                onFailure = { ex -> errorMessage = ex.message }
                            )
                        } else {
                            if (email.isBlank()) {
                                errorMessage = "Ingresa tu correo registrado."
                                return@Button
                            }
                            isLoading = true
                            val result = repository.login(email)
                            isLoading = false
                            result.fold(
                                onSuccess = { user -> onLoginSuccess(user) },
                                onFailure = { ex -> errorMessage = ex.message }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isRegisterMode) "Crear Cuenta en Big Boys" else "Iniciar Sesión",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Guest / Direct explore
                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continuar como Invitado (Modo Cliente) →",
                        color = OnDarkMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
