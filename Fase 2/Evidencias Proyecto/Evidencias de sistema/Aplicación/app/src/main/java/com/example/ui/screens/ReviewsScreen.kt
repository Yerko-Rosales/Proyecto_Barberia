package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Appointment
import com.example.data.model.Barber
import com.example.data.model.Location
import com.example.data.model.Review
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberiaViewModel

val QUICK_REVIEW_TAGS = listOf(
    "Puntualidad 10/10",
    "Corte Impecable",
    "Excelente Atención",
    "Ambiente VIP & Café",
    "Barbero Profesional",
    "Productos de Calidad",
    "Recomendado 100%",
    "Perfilado Perfecto"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    viewModel: BarberiaViewModel,
    onNavigateToBooking: () -> Unit = {}
) {
    val context = LocalContext.current
    val reviews by viewModel.allReviewsFlow.collectAsState()
    val locations by viewModel.locationsFlow.collectAsState()
    val barbers by viewModel.allBarbersFlow.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    var selectedFilter by remember { mutableStateOf("all") }
    var selectedLocationId by remember { mutableStateOf<Int?>(null) }
    var selectedBarberId by remember { mutableStateOf<Int?>(null) }
    var showCreateReviewDialog by remember { mutableStateOf(false) }

    val filteredReviews = remember(reviews, selectedFilter, selectedLocationId, selectedBarberId) {
        when (selectedFilter) {
            "location" -> if (selectedLocationId == null) reviews else reviews.filter { it.locationId == selectedLocationId }
            "barber" -> if (selectedBarberId == null) reviews else reviews.filter { it.barberId == selectedBarberId }
            "service" -> reviews.filter { it.reviewType == "SERVICE" }
            "venue" -> reviews.filter { it.reviewType == "LOCATION" || it.reviewType == "GENERAL" }
            else -> reviews
        }
    }

    val averageRating = remember(reviews) {
        if (reviews.isEmpty()) 5.0
        else (reviews.map { it.rating }.average() * 10).toInt() / 10.0
    }

    val fiveStarCount = remember(reviews) { reviews.count { it.rating == 5 } }
    val fourStarCount = remember(reviews) { reviews.count { it.rating == 4 } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateReviewDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = if (isDarkMode) DarkBackground else Color.White,
                icon = { Icon(Icons.Filled.RateReview, contentDescription = null) },
                text = { Text("DEJAR RESEÑA", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("write_review_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER HERO SUMMARY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Experiencias & Reseñas ⭐",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Opiniones de clientes sobre nuestros locales y barberos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Big Rating Number
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", averageRating),
                                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row {
                                    repeat(5) { index ->
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = if (index < averageRating.toInt()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${reviews.size} calificaciones",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick metrics
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("5 ★★★★★", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("$fiveStarCount clientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("4 ★★★★☆", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("$fourStarCount clientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = GreenConfirm, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("98% Recomendado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenConfirm)
                                }
                            }
                        }
                    }
                }
            }

            // 2. FILTER CHIPS ROW
            item {
                Column {
                    Text(
                        "Filtrar Opiniones",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "all",
                                onClick = {
                                    selectedFilter = "all"
                                    selectedLocationId = null
                                    selectedBarberId = null
                                },
                                label = { Text("Todas (${reviews.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = if (isDarkMode) DarkBackground else Color.White
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedFilter == "venue",
                                onClick = { selectedFilter = "venue" },
                                label = { Text("Sobre el Local") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = if (isDarkMode) DarkBackground else Color.White
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = selectedFilter == "service",
                                onClick = { selectedFilter = "service" },
                                label = { Text("Por Servicio") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = if (isDarkMode) DarkBackground else Color.White
                                )
                            )
                        }

                        items(locations, key = { it.id }) { loc ->
                            FilterChip(
                                selected = selectedFilter == "location" && selectedLocationId == loc.id,
                                onClick = {
                                    selectedFilter = "location"
                                    selectedLocationId = loc.id
                                },
                                label = { Text(loc.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = if (isDarkMode) DarkBackground else Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. REVIEWS LIST
            if (filteredReviews.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No hay reseñas para este filtro actualmente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showCreateReviewDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Sé el primero en calificar", color = if (isDarkMode) DarkBackground else Color.White)
                            }
                        }
                    }
                }
            } else {
                items(filteredReviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        locations = locations,
                        barbers = barbers,
                        isDarkMode = isDarkMode,
                        isAdmin = isAdmin,
                        onDeleteReview = {
                            viewModel.deleteReviewAdmin(review) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Modal to create general review
    if (showCreateReviewDialog) {
        CreateGeneralReviewDialog(
            viewModel = viewModel,
            locations = locations,
            barbers = barbers,
            onDismiss = { showCreateReviewDialog = false }
        )
    }
}

@Composable
fun ReviewCard(
    review: Review,
    locations: List<Location>,
    barbers: List<Barber>,
    isDarkMode: Boolean,
    isAdmin: Boolean = false,
    onDeleteReview: () -> Unit = {}
) {
    val context = LocalContext.current
    var showConfirmDelete by remember { mutableStateOf(false) }
    val locationName = remember(review, locations) {
        locations.find { it.id == review.locationId }?.name
    }
    val barberName = remember(review, barbers) {
        barbers.find { it.id == review.barberId }?.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Author & Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (review.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(review.authorAvatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = review.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = review.authorName.firstOrNull()?.toString()?.uppercase() ?: "C",
                                color = if (isDarkMode) DarkBackground else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = review.authorName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = "Cliente Verificado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = review.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stars display
                Row {
                    repeat(5) { idx ->
                        Icon(
                            imageVector = if (idx < review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (idx < review.rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject / Badge Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = review.serviceOrSubject,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                if (!locationName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "📍 $locationName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!barberName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "✂️ $barberName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            // Tags
            if (review.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                val tagList = review.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tagList) { tag ->
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "✨ $tag",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Admin Review Management Controls
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Modo Admin",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { showConfirmDelete = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                        border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("delete_review_btn_${review.id}")
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Eliminar Reseña",
                            modifier = Modifier.size(14.dp),
                            tint = RedCancel
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Eliminar Reseña",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedCancel
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            icon = {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = RedCancel, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    "¿Eliminar esta reseña?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "¿Deseas eliminar permanentemente la reseña de \"${review.authorName}\" (${review.rating}⭐)? Esta acción es irreversible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDeleteReview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Text("Sí, Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

// Dialog to review a completed appointment
@Composable
fun ReviewAppointmentDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, tags: List<String>) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("review_appointment_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Calificar Servicio Realizado ⭐",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Tu opinión ayuda a mejorar la experiencia en Big boys",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = appointment.serviceName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Fecha: ${appointment.dateTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Star Picker
                Text(
                    "¿Cómo calificarías la atención?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick tags
                Text(
                    "Aspectos destacados (opcional):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(QUICK_REVIEW_TAGS) { tag ->
                        val isSelected = selectedTags.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedTags.remove(tag)
                                else selectedTags.add(tag)
                            },
                            label = { Text(tag, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = DarkBackground
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Comment input
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Escribe tu experiencia...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("review_comment_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (comment.isBlank()) {
                                errorMessage = "Por favor escribe un breve comentario."
                                return@Button
                            }
                            onSubmit(rating, comment, selectedTags.toList())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_review_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PUBLICAR", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog to create general review about venue or services
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGeneralReviewDialog(
    viewModel: BarberiaViewModel,
    locations: List<Location>,
    barbers: List<Barber>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reviewType by remember { mutableStateOf("LOCATION") } // LOCATION, SERVICE, BARBER
    var selectedLocation by remember { mutableStateOf(locations.firstOrNull()) }
    var selectedBarber by remember { mutableStateOf(barbers.firstOrNull()) }
    var serviceSubject by remember { mutableStateOf("Corte de Pelo & Barba") }
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("general_review_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dejar Reseña / Opinión ⭐",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            "¿Sobre qué deseas opinar?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = reviewType == "LOCATION",
                                onClick = { reviewType = "LOCATION" },
                                label = { Text("Local / Sucursal") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                            FilterChip(
                                selected = reviewType == "SERVICE",
                                onClick = { reviewType = "SERVICE" },
                                label = { Text("Servicio") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                            FilterChip(
                                selected = reviewType == "BARBER",
                                onClick = { reviewType = "BARBER" },
                                label = { Text("Barbero") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                        }
                    }

                    // Dynamic selection based on type
                    when (reviewType) {
                        "LOCATION" -> {
                            item {
                                Text("Selecciona la Sucursal:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(locations) { loc ->
                                        FilterChip(
                                            selected = selectedLocation?.id == loc.id,
                                            onClick = { selectedLocation = loc },
                                            label = { Text(loc.name) }
                                        )
                                    }
                                }
                            }
                        }
                        "BARBER" -> {
                            item {
                                Text("Selecciona el Barbero:", style = MaterialTheme.typography.labelMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(barbers) { b ->
                                        FilterChip(
                                            selected = selectedBarber?.id == b.id,
                                            onClick = { selectedBarber = b },
                                            label = { Text(b.name) }
                                        )
                                    }
                                }
                            }
                        }
                        "SERVICE" -> {
                            item {
                                OutlinedTextField(
                                    value = serviceSubject,
                                    onValueChange = { serviceSubject = it },
                                    label = { Text("Nombre del Servicio (ej. Degradado, Barba, Tintura)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Rating Stars
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Calificación General",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(onClick = { rating = star }) {
                                        Icon(
                                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "$star estrellas",
                                            tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tags
                    item {
                        Text("Aspectos destacados:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(QUICK_REVIEW_TAGS) { tag ->
                                val isSelected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedTags.remove(tag)
                                        else selectedTags.add(tag)
                                    },
                                    label = { Text(tag, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Comment box
                    item {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Tu comentario u opinión detallada...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("general_review_comment_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5
                        )
                    }

                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (comment.isBlank()) {
                                    errorMessage = "Por favor ingresa un comentario para tu reseña."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null

                                val subjectText = when (reviewType) {
                                    "LOCATION" -> "Sucursal ${selectedLocation?.name ?: "Big boys"}"
                                    "BARBER" -> "Atención con ${selectedBarber?.name ?: "Barbero"}"
                                    else -> serviceSubject
                                }

                                viewModel.submitGeneralReview(
                                    locationId = if (reviewType == "LOCATION") selectedLocation?.id else null,
                                    barberId = if (reviewType == "BARBER") selectedBarber?.id else null,
                                    serviceOrSubject = subjectText,
                                    rating = rating,
                                    comment = comment,
                                    tags = selectedTags.toList(),
                                    reviewType = reviewType
                                ) { success, msg ->
                                    isSubmitting = false
                                    if (success) {
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_general_review_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground)
                            } else {
                                Text("PUBLICAR RESEÑA", fontWeight = FontWeight.Bold, color = DarkBackground)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
