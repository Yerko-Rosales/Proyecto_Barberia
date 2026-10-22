package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Barber
import com.example.data.model.Location
import com.example.ui.theme.*

// Data representation for branch coordinates and details
data class BranchMapData(
    val id: Int,
    val name: String,
    val address: String,
    val phone: String,
    val schedule: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val rating: Float = 4.9f,
    val totalChairs: Int = 4,
    val isPrimary: Boolean = false
)

val BIG_BOYS_BRANCHES = listOf(
    BranchMapData(
        id = 1,
        name = "Big Boys - Talagante",
        address = "Boulevard del centro, local 3, Talagante",
        phone = "+56 2 2489 1234",
        schedule = "Lun a Sáb: 09:00 - 20:00 | Dom: 10:00 - 16:00",
        lat = -33.6631,
        lng = -70.9286,
        description = "Sucursal Central (4 Sillones VIP). Nuestra sede insignia en Talagante con bar de cortesía, afeitados a navaja y sillones hidráulicos de cuero.",
        rating = 4.9f,
        totalChairs = 4,
        isPrimary = true
    ),
    BranchMapData(
        id = 2,
        name = "Big Boys - Peñaflor (21 de Mayo)",
        address = "21 de mayo 4566, Peñaflor",
        phone = "+56 2 2631 5678",
        schedule = "Lun a Sáb: 09:00 - 20:00 | Dom: Cerrado",
        lat = -33.6080,
        lng = -70.8993,
        description = "Sucursal Regional (4 Sillones VIP). Ubicada en 21 de mayo. Especialistas en afeitado tradicional a navaja y degradados fade milimétricos.",
        rating = 4.8f,
        totalChairs = 4
    ),
    BranchMapData(
        id = 3,
        name = "Big Boys - Peñaflor (Alcalde Luis Araya Cereceda)",
        address = "Alcalde Luis Araya Cereceda 4387, Peñaflor",
        phone = "+56 2 2912 3456",
        schedule = "Lun a Sáb: 10:00 - 20:30 | Dom: 10:00 - 15:00",
        lat = -33.6077,
        lng = -70.9001,
        description = "Sucursal Regional (4 Sillones VIP). A solo 1 cuadra (81 m) de la sucursal de 21 de Mayo. Zona lounge climatizada y servicios VIP de visagismo.",
        rating = 5.0f,
        totalChairs = 4
    )
)

/**
 * Interactive Branches Map Screen & Google Maps Navigation component.
 */
@Composable
fun BranchesMapScreen(
    locations: List<Location>,
    barbers: List<Barber>,
    onSelectBranchForBooking: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedBranch by remember { mutableStateOf<BranchMapData?>(BIG_BOYS_BRANCHES.first()) }
    var showDirectMapModal by remember { mutableStateOf(false) }
    var isSatelliteView by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = "Mapa",
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUCURSALES & GOOGLE MAPS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = GoldAccent
                    )
                }
                Text(
                    text = "Encuentra tu barbería Big Boys más cercana en Chile 🇨🇱",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Map Card / Radar View Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .testTag("branches_map_canvas_card"),
            colors = CardDefaults.cardColors(containerColor = if (isSatelliteView) Color(0xFF0F1A14) else Color(0xFF16161A)),
            border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // High Quality Vector Map Radar Canvas
                BranchesMapCanvas(
                    branches = BIG_BOYS_BRANCHES,
                    selectedBranch = selectedBranch,
                    isSatellite = isSatelliteView,
                    onSelectBranch = { branch ->
                        selectedBranch = branch
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Map controls overlay in top right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle Map type
                    Surface(
                        onClick = { isSatelliteView = !isSatelliteView },
                        color = DarkBackground.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_satellite_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSatelliteView) Icons.Filled.Layers else Icons.Outlined.Layers,
                                contentDescription = "Cambiar vista",
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSatelliteView) "Satélite" else "Vector",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextIvory
                            )
                        }
                    }

                    // Open Google Maps GPS Navigation direct button
                    Surface(
                        onClick = {
                            selectedBranch?.let { openGoogleMaps(context, it.lat, it.lng, it.name) }
                        },
                        color = GoldPrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_gmaps_external_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Directions,
                                contentDescription = "Ruta",
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cómo llegar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = DarkBackground
                            )
                        }
                    }
                }

                // GPS Center watermark chip
                Surface(
                    color = DarkBackground.copy(alpha = 0.85f),
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = GreenConfirm,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Región Metropolitana, Chile",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextIvory
                        )
                    }
                }
            }
        }

        // Branch Selection Chips Selector
        Text(
            text = "SELECCIONA UNA SUCURSAL:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = GoldPrimary,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BIG_BOYS_BRANCHES.forEach { branch ->
                val isSelected = selectedBranch?.id == branch.id
                Surface(
                    onClick = { selectedBranch = branch },
                    color = if (isSelected) GoldPrimary else CardBackground,
                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF27272A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("branch_chip_${branch.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) DarkBackground else GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = branch.name.replace("Big Boys - ", ""),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) DarkBackground else TextIvory
                            )
                            Text(
                                text = "⭐ ${branch.rating} • ${branch.totalChairs} Sillones",
                                fontSize = 10.sp,
                                color = if (isSelected) DarkBackground.copy(alpha = 0.8f) else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Selected Branch Details Showcase Card
        selectedBranch?.let { branch ->
            val branchBarbers = remember(barbers, branch.id) {
                barbers.filter { it.locationId == branch.id }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_branch_details_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = branch.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = TextIvory
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = branch.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldAccent
                            )
                        }

                        // Rating badge
                        Box(
                            modifier = Modifier
                                .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(branch.rating.toString(), color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = branch.description,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = TextIvory.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = GoldPrimary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact & Opening Info Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Phone contact item
                        Card(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${branch.phone.replace(" ", "")}"))
                                context.startActivity(intent)
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141311)),
                            border = BorderStroke(0.8.dp, Color(0xFF27272A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Phone, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Teléfono", fontSize = 10.sp, color = TextMuted)
                                    Text(branch.phone, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                }
                            }
                        }

                        // Schedule info item
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141311)),
                            border = BorderStroke(0.8.dp, Color(0xFF27272A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(GoldAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AccessTime, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Horario", fontSize = 10.sp, color = TextMuted)
                                    Text("09:00 - 20:00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextIvory)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Barbers in this branch preview
                    Text(
                        text = "BARBEROS EN ESTA SUCURSAL (${branchBarbers.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        branchBarbers.forEach { b ->
                            Surface(
                                color = Color(0xFF141311),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(GoldPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = b.name.firstOrNull()?.toString() ?: "",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBackground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = b.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextIvory
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons: 1. Abrir en Google Maps, 2. Agendar en este local
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openGoogleMaps(context, branch.lat, branch.lng, branch.name)
                            },
                            border = BorderStroke(1.2.dp, GoldPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_gmaps_directions")
                        ) {
                            Icon(Icons.Filled.DirectionsCar, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Maps", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val matchedLoc = locations.find { it.id == branch.id } ?: Location(
                                    id = branch.id,
                                    name = branch.name,
                                    address = branch.address,
                                    phone = branch.phone
                                )
                                onSelectBranchForBooking(matchedLoc)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_book_in_branch")
                        ) {
                            Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agendar Aquí", fontWeight = FontWeight.Black, fontSize = 12.sp, color = DarkBackground)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas Map Renderer that renders streets, GPS pins, branch labels, and clickable markers
 */
@Composable
fun BranchesMapCanvas(
    branches: List<BranchMapData>,
    selectedBranch: BranchMapData?,
    isSatellite: Boolean,
    onSelectBranch: (BranchMapData) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(branches) {
                detectTransformGestures { _, pan, _, _ ->
                    // Interactive touch navigation feedback
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Map Background
        if (isSatellite) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B2A1E), Color(0xFF0D140F)),
                    center = Offset(width / 2, height / 2),
                    radius = width * 0.8f
                )
            )
        } else {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1E24), Color(0xFF121214)),
                    center = Offset(width / 2, height / 2),
                    radius = width * 0.8f
                )
            )
        }

        // 2. Draw Simulated Geographic Road Network / Rivers (Santiago South-West / Talagante / Peñaflor)
        val roadColor = if (isSatellite) Color(0xFF3B483C).copy(alpha = 0.6f) else Color(0xFF2C2D35)
        val highwayColor = if (isSatellite) Color(0xFF7D6B40).copy(alpha = 0.7f) else Color(0xFF4A412A)
        val riverColor = Color(0xFF1A3848).copy(alpha = 0.5f)

        // Main Highway (Autopista del Sol / Ruta 78 connecting Peñaflor & Talagante)
        val highwayPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(
                width * 0.35f, height * 0.35f,
                width * 0.65f, height * 0.65f,
                width, height * 0.85f
            )
        }
        drawPath(highwayPath, color = highwayColor, style = Stroke(width = 8.dp.toPx()))

        // River Mapal Mapocho / Maipo canal trace
        val riverPath = Path().apply {
            moveTo(0f, height * 0.85f)
            cubicTo(
                width * 0.4f, height * 0.8f,
                width * 0.7f, height * 0.4f,
                width, height * 0.35f
            )
        }
        drawPath(riverPath, color = riverColor, style = Stroke(width = 5.dp.toPx()))

        // Secondary Avenue Grids
        val gridLines = listOf(
            Offset(0f, height * 0.5f) to Offset(width, height * 0.5f),
            Offset(width * 0.3f, 0f) to Offset(width * 0.3f, height),
            Offset(width * 0.7f, 0f) to Offset(width * 0.7f, height),
            Offset(0f, height * 0.75f) to Offset(width, height * 0.75f)
        )
        gridLines.forEach { (start, end) ->
            drawLine(
                color = roadColor,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx()
            )
        }

        // 3. Map Marker Screen Coordinates calculated to give aesthetic layout
        // Talagante is South-West, Peñaflor 1 is Center-North, Peñaflor 2 is North-West
        val markerPositions = mapOf(
            1 to Offset(width * 0.72f, height * 0.68f), // Talagante
            2 to Offset(width * 0.38f, height * 0.36f), // Peñaflor 1
            3 to Offset(width * 0.24f, height * 0.58f)  // Peñaflor 2
        )

        // Draw connecting routes between Big Boys branches
        val branchLinePath = Path().apply {
            val p3 = markerPositions[3] ?: Offset.Zero
            val p2 = markerPositions[2] ?: Offset.Zero
            val p1 = markerPositions[1] ?: Offset.Zero
            moveTo(p3.x, p3.y)
            lineTo(p2.x, p2.y)
            lineTo(p1.x, p1.y)
        }
        drawPath(
            branchLinePath,
            color = GoldPrimary.copy(alpha = 0.35f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
        )

        // Draw Interactive Markers
        branches.forEach { branch ->
            val pos = markerPositions[branch.id] ?: Offset(width * 0.5f, height * 0.5f)
            val isSelected = selectedBranch?.id == branch.id

            // Pulse radar circle if selected
            if (isSelected) {
                drawCircle(
                    color = GoldPrimary.copy(alpha = 0.25f),
                    radius = 28.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.4f),
                    radius = 20.dp.toPx(),
                    center = pos
                )
            }

            // Marker Drop Pin Base
            val pinRadius = if (isSelected) 14.dp.toPx() else 10.dp.toPx()
            drawCircle(
                color = if (isSelected) GoldPrimary else Color(0xFF2B2519),
                radius = pinRadius,
                center = pos
            )
            drawCircle(
                color = if (isSelected) DarkBackground else GoldAccent,
                radius = pinRadius * 0.55f,
                center = pos
            )

            // Inner Barber Pole dot
            drawCircle(
                color = if (isSelected) GoldPrimary else TextIvory,
                radius = pinRadius * 0.25f,
                center = pos
            )
        }
    }
}

/**
 * Launch Google Maps navigation intent with fallback to browser URL.
 */
fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    try {
        // Formulate standard Google Maps geo URI with label
        val uriStr = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to official web Google Maps navigation URL
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        // Fallback browser redirect
        try {
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(browserIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "No fue posible abrir el mapa: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
