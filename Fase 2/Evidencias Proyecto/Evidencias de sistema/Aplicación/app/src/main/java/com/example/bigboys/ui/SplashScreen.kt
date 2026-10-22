package com.example.bigboys.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bigboys.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    var statusText by remember { mutableStateOf("Conectando con Big Boys Cloud...") }
    var progress by remember { mutableStateOf(0.15f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // Entrance animation phase 1
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        scale.animateTo(1.0f, animationSpec = tween(200))
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700))
        rotation.animateTo(360f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        delay(600)
        statusText = "Sincronizando APIs & Agenda de Barberos..."
        progress = 0.55f
        delay(700)
        statusText = "Cargando servicios y tarifas oficiales..."
        progress = 0.85f
        delay(600)
        statusText = "¡Bienvenido a Big Boys!"
        progress = 1.0f
        delay(500)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        DarkBackground,
                        Color(0xFF1A1608),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Animated Logo Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GoldLight.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                GoldPrimary,
                                GoldLight,
                                Color(0xFF8A6D1C),
                                GoldPrimary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Glow circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant.copy(alpha = glowAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Big Boys Logo",
                            tint = GoldPrimary,
                            modifier = Modifier
                                .size(48.dp)
                                .rotate(rotation.value)
                        )
                        Text(
                            text = "BB",
                            color = GoldLight,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Titles
            Text(
                text = "BIG BOYS",
                color = GoldPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BARBERÍA & SPA",
                    color = OnDarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estilo Urbano • Afeitado Clásico • Master Fades",
                color = OnDarkMuted,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress bar and status indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(220.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GoldPrimary,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = statusText,
                color = GoldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Skip button at the top corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            TextButton(
                onClick = onAnimationFinished,
                colors = ButtonDefaults.textButtonColors(contentColor = OnDarkMuted)
            ) {
                Text("Saltar >", fontSize = 12.sp)
            }
        }
    }
}
