package com.example.bigboys.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoldPrimary = Color(0xFFD4AF37)
val GoldVariant = Color(0xFFB8860B)
val GoldLight = Color(0xFFFFE082)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val OnDarkText = Color(0xFFF5F5F5)
val OnDarkMuted = Color(0xFFAAAAAA)
val ErrorRed = Color(0xFFCF6679)
val SuccessGreen = Color(0xFF4CAF50)

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332900),
    onPrimaryContainer = GoldLight,
    secondary = GoldVariant,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = OnDarkText,
    surface = DarkSurface,
    onSurface = OnDarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    error = ErrorRed,
    onError = Color.Black
)

@Composable
fun BigBoysTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
