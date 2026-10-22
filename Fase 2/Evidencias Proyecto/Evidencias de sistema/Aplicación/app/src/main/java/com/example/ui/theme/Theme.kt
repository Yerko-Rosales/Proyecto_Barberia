package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldAccent,
    tertiary = GoldAccent,
    background = DarkBackground,
    surface = CardBackground,
    surfaceVariant = Color(0xFF27272A),
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextIvory,
    onSurface = TextIvory,
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF3F3F46)
)

private val LightColorScheme = lightColorScheme(
    primary = LightGoldPrimary,
    secondary = LightGoldPrimary,
    tertiary = LightGoldPrimary,
    background = LightBackground,
    surface = LightCardBackground,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextDark,
    onSurface = LightTextDark,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
