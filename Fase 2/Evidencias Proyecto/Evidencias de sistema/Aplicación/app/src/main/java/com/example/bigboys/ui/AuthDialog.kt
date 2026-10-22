package com.example.bigboys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigboys.data.BarberRepository
import com.example.bigboys.model.User
import com.example.bigboys.model.UserRole
import com.example.bigboys.ui.theme.*

@Composable
fun AuthDialog(
    repository: BarberRepository,
    onDismiss: () -> Unit,
    onSuccess: (User) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isRegisterMode) "Crear Cuenta en Big Boys" else "Iniciar Sesión",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isRegisterMode) "Tu cuenta será única para la Web y la App" else "Ingresa tu correo registrado",
                    color = OnDarkMuted,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick switch for demo/testing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        // Quick fill client
                        email = "j.ojedalefio@gmail.com"
                        isRegisterMode = false
                    }) {
                        Text("👤 Cliente Demo", fontSize = 11.sp, color = GoldLight)
                    }
                    TextButton(onClick = {
                        // Quick fill barber
                        email = "carlos@bigboys.cl"
                        isRegisterMode = false
                    }) {
                        Text("💈 Barbero Demo", fontSize = 11.sp, color = GoldPrimary)
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = OnDarkMuted,
                        focusedTextColor = OnDarkText,
                        unfocusedTextColor = OnDarkText
                    )
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = rut,
                        onValueChange = { rut = it },
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
                        value = fullName,
                        onValueChange = { fullName = it },
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
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono (+56 9...)") },
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

                errorMessage?.let {
                    Text(text = it, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    }) {
                        Text(
                            text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí",
                            color = GoldLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isRegisterMode) {
                        if (email.isBlank() || rut.isBlank() || fullName.isBlank() || phone.isBlank()) {
                            errorMessage = "Por favor completa todos los campos."
                        } else {
                            val result = repository.register(
                                email = email,
                                rut = rut,
                                fullName = fullName,
                                phone = phone,
                                role = UserRole.CLIENT
                            )
                            result.fold(
                                onSuccess = { user -> onSuccess(user) },
                                onFailure = { ex -> errorMessage = ex.message }
                            )
                        }
                    } else {
                        if (email.isBlank()) {
                            errorMessage = "Ingresa tu correo."
                        } else {
                            val result = repository.login(email)
                            result.fold(
                                onSuccess = { user -> onSuccess(user) },
                                onFailure = { ex -> errorMessage = ex.message }
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text(
                    text = if (isRegisterMode) "Crear Cuenta" else "Entrar",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OnDarkMuted)
            }
        },
        containerColor = DarkSurface
    )
}
