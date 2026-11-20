package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseClientProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.BarberiaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Asesor de estilo con IA (idea de Jonathan, 19-11-2026).
// La foto se envía a la Edge Function "asesor-ia" de Supabase, que llama a Gemini con la clave
// guardada en el servidor: la app no lleva ninguna clave de IA. La foto no se guarda (Ley 21.719).

data class AdvisorResult(
    val lookTitle: String,
    val diagnosis: String,
    val serviceName: String,
    val servicePrice: Int,
    val complement: String,
    val stylingTips: String,
    val maintenanceDays: Int,
    val faceShapeLabel: String,
    val hairTypeLabel: String,
    val beardTypeLabel: String,
    val engineName: String
)

private class AsesorException(message: String) : Exception(message)

@Composable
fun BarberAdvisorScreen(
    viewModel: BarberiaViewModel,
    onRequestBooking: (serviceName: String) -> Unit = {}
) {
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var advisorMode by remember { mutableStateOf("photo") } // "photo" o "manual"
    var acepta by remember { mutableStateOf(false) }

    var faceShape by remember { mutableStateOf("oval") }
    var beardType by remember { mutableStateOf("short") }

    var isAnalyzing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<AdvisorResult?>(null) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    fun analizarConIA(bitmap: Bitmap) {
        scope.launch {
            isAnalyzing = true
            result = null
            errorText = null
            statusText = "La IA está analizando tu rostro, cabello y barba..."
            try {
                val b64 = withContext(Dispatchers.Default) { bitmapABase64(bitmap) }
                val json = withContext(Dispatchers.IO) { llamarAsesorIA(b64) }
                result = AdvisorResult(
                    lookTitle = json.optString("recommendedLook"),
                    diagnosis = json.optString("anatomicalDiagnosis"),
                    serviceName = json.optString("officialService", "Corte"),
                    servicePrice = json.optInt("servicePrice", 11900),
                    complement = json.optString("stylingProduct", "Producto de peinado") + " · Toalla caliente y perfilado a navaja",
                    stylingTips = json.optString("stylingTips", "Aplica el producto en el cabello seco."),
                    maintenanceDays = json.optInt("maintenanceDays", 14),
                    faceShapeLabel = json.optString("morphologyLabel", "—"),
                    hairTypeLabel = json.optString("hairTexture", "—"),
                    beardTypeLabel = json.optString("beardType", "—"),
                    engineName = "Google Gemini (" + json.optString("modelo", "IA") + ")"
                )
            } catch (e: AsesorException) {
                errorText = e.message
            } catch (e: Exception) {
                android.util.Log.w("BarberAdvisor", "Asesor IA: ${e.message}")
                errorText = "No pudimos conectar con el asesor. Revisa tu conexión o usa el ajuste manual."
            }
            isAnalyzing = false
        }
    }

    fun recibirFoto(bitmap: Bitmap) {
        photo = bitmap
        result = null
        errorText = null
        analizarConIA(bitmap)
    }

    // La cámara guarda la foto a resolución completa en un archivo temporal (la miniatura
    // de TakePicturePreview es muy pequeña y la IA no distinguía bien el rostro).
    val fotoUri = remember {
        val archivo = File(File(context.cacheDir, "asesor").apply { mkdirs() }, "selfie.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
        if (ok) {
            val bitmap = runCatching { cargarFotoReducida(context, fotoUri) }.getOrNull()
            if (bitmap != null) recibirFoto(bitmap) else errorText = "No pudimos leer la foto. Intenta de nuevo."
        }
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = runCatching { cargarFotoReducida(context, uri) }.getOrNull()
            if (bitmap != null) recibirFoto(bitmap) else errorText = "No pudimos leer la imagen. Usa una foto JPG o PNG."
        }
    }

    fun pedirConsentimiento(): Boolean {
        if (!acepta) {
            Toast.makeText(context, "Primero marca la casilla de consentimiento.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado y selector de modo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                border = BorderStroke(1.5.dp, GoldPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨ ASESOR DE ESTILO CON IA", color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "BARBER ADVISOR",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Toma o elige una selfie: la IA analiza la forma de tu rostro, tu cabello y tu barba, y te recomienda el corte que mejor te queda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf("photo" to "📸 Con Foto", "manual" to "⚙️ Ajuste Manual").forEach { (id, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (advisorMode == id) GoldPrimary else Color.Transparent)
                                    .clickable { advisorMode = id; errorText = null }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    color = if (advisorMode == id) DarkBackground else Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        if (advisorMode == "photo") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        // Consentimiento (Ley 21.719)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                .clickable { acepta = !acepta }
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = acepta,
                                onCheckedChange = { acepta = it },
                                colors = CheckboxDefaults.colors(checkedColor = GoldPrimary, checkmarkColor = DarkBackground)
                            )
                            Text(
                                "Acepto que mi foto se envíe a Google (Gemini) para analizar mi rostro y recomendarme un corte. Big Boys no guarda la foto; Google puede usarla para mejorar sus productos.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Visor de la foto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF101010))
                                .border(2.dp, if (photo != null) GoldPrimary else Color.DarkGray, RoundedCornerShape(16.dp))
                                .clickable { if (pedirConsentimiento()) cameraLauncher.launch(fotoUri) },
                            contentAlignment = Alignment.Center
                        ) {
                            val actual = photo
                            if (actual != null) {
                                Image(
                                    bitmap = actual.asImageBitmap(),
                                    contentDescription = "Tu foto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(56.dp).background(GoldPrimary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Toca aquí para tomar una selfie", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    Text("De frente y con buena luz", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            if (isAnalyzing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp)
                                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("ANALIZANDO...", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { if (pedirConsentimiento()) cameraLauncher.launch(fotoUri) },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CÁMARA", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { if (pedirConsentimiento()) photoPickerLauncher.launch("image/*") },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Image, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GALERÍA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        val actual = photo
                        if (actual != null && !isAnalyzing && result == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { if (pedirConsentimiento()) analizarConIA(actual) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ANALIZAR DE NUEVO CON IA", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (advisorMode == "manual") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sin foto: elige la forma de tu rostro y tu barba. La recomendación sale de las reglas de visagismo de Big Boys (sin IA).",
                            fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("FORMA DEL ROSTRO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OpcionesChips(
                            opciones = listOf("oval" to "Ovalado", "square" to "Cuadrado", "round" to "Redondo", "diamond" to "Diamante", "long" to "Alargado"),
                            seleccion = faceShape,
                            onSeleccion = { faceShape = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("BARBA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OpcionesChips(
                            opciones = listOf("full" to "Tupida", "short" to "Media", "stubble" to "Perfilada", "clean" to "Sin barba"),
                            seleccion = beardType,
                            onSeleccion = { beardType = it }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { errorText = null; result = recomendacionPorReglas(faceShape, beardType) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("VER RECOMENDACIÓN", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (isAnalyzing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(statusText, fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GoldPrimary,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }

        errorText?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1212)),
                    border = BorderStroke(1.dp, RedCancel.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("⚠️ $msg", fontSize = 12.sp, color = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { advisorMode = "manual"; errorText = null },
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Usar el ajuste manual", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        result?.let { adv ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181510)),
                    border = BorderStroke(2.dp, GoldPrimary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .background(GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("👑 RECOMENDACIÓN LISTA · ${adv.engineName}", color = GoldPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(adv.lookTitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Rostro: ${adv.faceShapeLabel} · Cabello: ${adv.hairTypeLabel} · Barba: ${adv.beardTypeLabel}",
                            fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(adv.diagnosis, style = MaterialTheme.typography.bodySmall, color = Color(0xFFDDDDDD), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Servicio sugerido:", fontSize = 12.sp, color = Color.Gray)
                            Text("${adv.serviceName} (${clp(adv.servicePrice)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Abono Webpay:", fontSize = 12.sp, color = GreenConfirm)
                            Text("-${clp(3000)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenConfirm)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Saldo en el local:", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                            Text(clp(adv.servicePrice - 3000), fontSize = 13.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF241E15), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("💡 Tratamiento sugerido:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                Text(adv.complement, fontSize = 11.sp, color = Color.LightGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Peinado: ${adv.stylingTips}", fontSize = 11.sp, color = Color.LightGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✂️ Retoque recomendado cada ${adv.maintenanceDays} días.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onRequestBooking(adv.serviceName) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("AGENDAR ESTE LOOK", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val ficha = "💈 FICHA DE VISAGISMO BIG BOYS: ${adv.lookTitle} | Servicio: ${adv.serviceName} (${clp(adv.servicePrice)}) | ${adv.diagnosis} | ${adv.engineName}"
                                    clipboardManager.setText(AnnotatedString(ficha))
                                    Toast.makeText(context, "¡Ficha copiada!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldPrimary)
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionesChips(opciones: List<Pair<String, String>>, seleccion: String, onSeleccion: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        opciones.forEach { (id, label) ->
            FilterChip(
                selected = seleccion == id,
                onClick = { onSeleccion(id) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = DarkBackground,
                    labelColor = Color.LightGray
                )
            )
        }
    }
}

private fun clp(valor: Int): String = "$" + String.format(java.util.Locale("es", "CL"), "%,d", valor).replace(',', '.')

// Lee la foto sin cargarla entera en memoria (~1024 px) y la endereza según su EXIF,
// porque muchas cámaras guardan la selfie girada y la IA no reconocía el rostro de lado.
private fun cargarFotoReducida(context: android.content.Context, uri: Uri): Bitmap? {
    val cr = context.contentResolver
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
    var muestra = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= 1024) muestra *= 2
    val bitmap = cr.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
    } ?: return null
    val grados = runCatching {
        cr.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    }.getOrNull() ?: 0f
    if (grados == 0f) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(grados) }, true)
}

// Reduce la foto a 1024 px como máximo y la codifica en JPEG/base64
private fun bitmapABase64(bitmap: Bitmap): String {
    val escala = minOf(1f, 1024f / maxOf(bitmap.width, bitmap.height))
    val reducida = if (escala < 1f)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
    else bitmap
    val stream = ByteArrayOutputStream()
    reducida.compress(Bitmap.CompressFormat.JPEG, 88, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

// Llama a la Edge Function "asesor-ia" (la clave de Gemini está solo en Supabase)
private fun llamarAsesorIA(base64Image: String): JSONObject {
    val conn = URL("${SupabaseClientProvider.SUPABASE_URL}/functions/v1/asesor-ia").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Authorization", "Bearer ${SupabaseClientProvider.SUPABASE_ANON_KEY}")
    conn.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
    conn.doOutput = true
    conn.connectTimeout = 15_000
    conn.readTimeout = 150_000 // la función corta a los ~140 s con un mensaje claro
    val body = JSONObject()
        .put("imagen", base64Image)
        .put("mime", "image/jpeg")
        .put("consentimiento", true)
    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

    val code = conn.responseCode
    val texto = (if (code in 200..299) conn.inputStream else conn.errorStream)
        ?.bufferedReader()?.use { it.readText() } ?: ""
    val json = runCatching { JSONObject(texto) }.getOrNull()
    if (code !in 200..299 || json == null || !json.has("recommendedLook")) {
        throw AsesorException(json?.optString("error")?.takeIf { it.isNotBlank() }
            ?: "No pudimos analizar tu foto. Intenta de nuevo o usa el ajuste manual.")
    }
    return json
}

// Recomendación por reglas de visagismo (modo manual, sin IA)
private fun recomendacionPorReglas(shape: String, beard: String): AdvisorResult {
    val (titulo, diag, tips, dias) = when (shape) {
        "round" -> listOf("High Skin Fade con Pompadour Texturizado",
            "Para un rostro redondo, de facciones suaves: volumen arriba y laterales cortos alargan ópticamente el rostro.",
            "Pomada mate de fijación alta.", "14")
        "square" -> listOf("Mid Taper Fade con Textured Crop",
            "Para un rostro cuadrado, de mandíbula marcada: un degradado medio suaviza las sienes sin perder la fuerza de tus facciones.",
            "Polvo texturizador en seco en la parte superior.", "18")
        "diamond" -> listOf("Mid Fade Clásico con Desconexión",
            "Para pómulos pronunciados y mentón afilado: la transición media equilibra la zona de las sienes.",
            "Crema de peinar ligera, sin aplastar los laterales.", "18")
        "long" -> listOf("Side Part Fade con Caída Lateral",
            "Para un rostro alargado: densidad media en las sienes y peinado diagonal aportan anchura visual.",
            "Cera al agua con brillo medio y peinado diagonal.", "20")
        else -> listOf("Low Drop Fade con Quiff Ejecutivo",
            "El rostro ovalado es el más versátil: admite casi cualquier corte; el low fade realza la elegancia natural.",
            "Secado hacia atrás con bálsamo suave.", "21")
    }
    val tituloFinal = when (beard) {
        "full" -> "$titulo & Barba Esculpida"
        "short", "stubble" -> "$titulo & Perfilado"
        else -> titulo
    }
    val (servicio, precio, complemento) = when (beard) {
        "full" -> Triple("S. Completo", 23800, "Mascarilla negra purificante")
        "short", "stubble" -> Triple("Corte + Barba", 20230, "Lavado revitalizante")
        else -> Triple("Corte", 11900, "Loción after-shave y fijador mate")
    }
    val formas = mapOf("oval" to "Ovalado", "square" to "Cuadrado", "round" to "Redondo", "diamond" to "Diamante", "long" to "Alargado")
    val barbas = mapOf("full" to "Tupida", "short" to "Media", "stubble" to "Perfilada", "clean" to "Sin barba")
    return AdvisorResult(
        lookTitle = tituloFinal,
        diagnosis = diag,
        serviceName = servicio,
        servicePrice = precio,
        complement = complemento,
        stylingTips = tips,
        maintenanceDays = dias.toInt(),
        faceShapeLabel = formas[shape] ?: "—",
        hairTypeLabel = "—",
        beardTypeLabel = barbas[beard] ?: "—",
        engineName = "Reglas Big Boys (sin IA)"
    )
}
