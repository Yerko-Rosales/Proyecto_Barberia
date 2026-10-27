package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Barber
import com.example.data.supabase.text
import com.example.ui.theme.*
import com.example.ui.util.AppFormatters
import com.example.ui.viewmodel.BarberiaViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.util.*

/** Fecha de hoy en Chile (yyyy-MM-dd); minSdk 24 no tiene java.time. */
private fun todayInChile(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("America/Santiago") }.format(Date())

private fun clp(value: String?): String = AppFormatters.formatClp(value?.toDoubleOrNull() ?: 0.0)

private fun JsonObject.list(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.map { it.jsonObject } ?: emptyList()

// ---------------------------------------------------------------------
// Pago del abono con Webpay Plus (ambiente de integración de Transbank)
// ---------------------------------------------------------------------
@Composable
fun WebpayPaymentPanel(
    viewModel: BarberiaViewModel,
    serviceName: String,
    amountToPay: Double,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.webpay.collectAsState()
    val openInBrowser: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WEBPAY PLUS · TRANSBANK", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Servicio: $serviceName", color = TextIvory, fontSize = 13.sp)
                Text("Abono a pagar: ${AppFormatters.formatClp(amountToPay)}", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        when (state.stage) {
            "IDLE", "ERROR" -> {
                Text(
                    "Se abrirá el formulario seguro de Webpay en tu navegador. Big Boys no recibe ni guarda los datos de tu tarjeta. " +
                        "El horario queda reservado 15 minutos mientras pagas.",
                    color = TextMuted, fontSize = 12.sp
                )
                Text(
                    "Ambiente de pruebas: tarjeta VISA 4051 8856 0044 6623, CVV 123, fecha futura; RUT 11.111.111-1, clave 123.",
                    color = GoldPrimary.copy(alpha = 0.8f), fontSize = 11.sp
                )
                state.message?.let { Text("⚠️ $it", color = RedCancel, fontSize = 12.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Volver", color = TextIvory) }
                    Button(
                        onClick = { viewModel.startWebpayPayment(openInBrowser) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1.6f).testTag("webpay_pay_btn")
                    ) { Text("PAGAR CON WEBPAY", color = DarkBackground, fontWeight = FontWeight.Black) }
                }
            }
            "INICIANDO" -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Conectando con Webpay…", color = TextIvory)
            }
            "ESPERANDO" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Completa el pago en el navegador. Esta pantalla se actualiza sola.", color = TextIvory, fontSize = 13.sp)
                }
                Text("Código de reserva: ${state.codigo}", color = TextMuted, fontSize = 12.sp)
                state.message?.let { Text("⚠️ $it", color = RedCancel, fontSize = 12.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openInBrowser(state.paymentUrl) }, modifier = Modifier.weight(1f)) {
                        Text("Abrir Webpay", color = TextIvory, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.verifyWebpayNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.weight(1f)
                    ) { Text("Ya pagué", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
            else -> {
                val ok = state.stage == "AUTORIZADO"
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null,
                    tint = if (ok) GreenConfirm else RedCancel, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                )
                Text(
                    when (state.stage) {
                        "AUTORIZADO" -> "¡Pago aprobado! Tu reserva ${state.codigo} quedó confirmada."
                        "ANULADO" -> "Anulaste el pago. La reserva no se confirmó y el horario quedó libre."
                        else -> "El pago fue rechazado. No se hizo ningún cargo y el horario quedó libre."
                    },
                    color = TextIvory, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                state.authorizationCode?.let { Text("Código de autorización: $it", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                Button(
                    onClick = { viewModel.resetWebpay(); if (ok) onFinished() else onCancel() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (ok) "LISTO" else "VOLVER A INTENTAR", color = DarkBackground, fontWeight = FontWeight.Black) }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Fila por orden de llegada (vista pública)
// ---------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(viewModel: BarberiaViewModel) {
    val locations by viewModel.locationsFlow.collectAsState()
    var locationId by remember(locations) { mutableStateOf(locations.firstOrNull()?.id ?: 1) }
    var queue by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(locationId) {
        while (true) {
            viewModel.loadQueue(locationId) { q, e -> queue = q; error = e }
            delay(10_000)
        }
    }

    val turns = queue?.list("turnos") ?: emptyList()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("FILA POR ORDEN DE LLEGADA", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Revisa cuánta gente hay antes de ir. Al llegar, el cajero/a te da tu número.", color = TextMuted, fontSize = 12.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                locations.forEach { loc ->
                    FilterChip(
                        selected = loc.id == locationId,
                        onClick = { locationId = loc.id },
                        label = { Text(loc.name, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        }
        error?.let { item { Text("⚠️ $it", color = RedCancel) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Esperando" to (queue?.text("en_espera") ?: "–"),
                    "Espera estimada" to "${queue?.text("espera_estimada_min") ?: "–"} min"
                ).forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = TextMuted, fontSize = 11.sp)
                            Text(value, color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
                        }
                    }
                }
            }
        }
        item { Text("Llamando ahora", color = TextIvory, fontWeight = FontWeight.Bold) }
        val called = turns.filter { it.text("estado") == "LLAMADO" }
        if (called.isEmpty()) item { Text("Nadie por ahora.", color = TextMuted, fontSize = 12.sp) }
        items(called) { t -> Text("N° ${t.text("numero")}", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp) }
        item { Text("En espera", color = TextIvory, fontWeight = FontWeight.Bold) }
        val waiting = turns.filter { it.text("estado") == "EN_ESPERA" }
        if (waiting.isEmpty()) item { Text("Sin fila: ¡ven ahora!", color = TextMuted, fontSize = 12.sp) }
        item { Text(waiting.joinToString("   ") { "N° ${it.text("numero")}" }, color = TextIvory) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ---------------------------------------------------------------------
// Barbero: solo sus atenciones del día y lo ganado (CU-41)
// ---------------------------------------------------------------------
@Composable
fun BarberEarningsScreen(viewModel: BarberiaViewModel, barber: Barber, onLogout: () -> Unit) {
    val account by viewModel.barberAccount.collectAsState()
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var newPass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val mustChange = account?.mustChangePassword == true

    LaunchedEffect(mustChange) {
        while (!mustChange) {
            viewModel.loadMyEarnings(todayInChile()) { d, e -> data = d; error = e }
            delay(15_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BARBERO", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(barber.name, color = TextIvory, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            OutlinedButton(onClick = onLogout) { Text("Salir", color = TextIvory) }
        }
        if (mustChange) {
            Text("Es tu primer ingreso: crea tu contraseña personal.", color = TextIvory)
            OutlinedTextField(
                value = newPass, onValueChange = { newPass = it },
                label = { Text("Nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            message?.let { Text(it, color = RedCancel, fontSize = 12.sp) }
            Button(
                onClick = { viewModel.changeBarberPassword(newPass) { ok, err -> message = if (ok) null else err } },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("GUARDAR", color = DarkBackground, fontWeight = FontWeight.Black) }
            return@Column
        }
        error?.let { Text("⚠️ $it", color = RedCancel) }
        val attentions = data?.list("atenciones") ?: emptyList()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Atenciones" to attentions.size.toString(),
                "Total cobrado" to clp(data?.text("total")),
                "Propinas" to clp(data?.text("propinas"))
            ).forEach { (label, value) ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = TextMuted, fontSize = 10.sp)
                        Text(value, color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
        if (attentions.isEmpty()) Text("Sin atenciones registradas hoy.", color = TextMuted, fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(attentions) { a ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${a.text("hora")} · ${a.text("servicio")}", color = TextIvory, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text(clp(a.text("monto")), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Text("Por privacidad no se muestran datos de contacto de los clientes.", color = TextMuted, fontSize = 10.sp)
    }
}

// ---------------------------------------------------------------------
// «Mis datos»: derechos del titular (Ley N° 21.719)
// ---------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataSection(viewModel: BarberiaViewModel) {
    var summary by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf("RECTIFICACION") }
    var detail by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MIS DATOS · LEY N° 21.719", color = GoldPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Button(
                onClick = {
                    viewModel.loadMyData("ACCESO") { d, e ->
                        summary = d?.let {
                            val cuenta = it["cuenta"]?.jsonObject
                            "Nombre: ${cuenta?.text("nombre")} ${cuenta?.text("apellido") ?: ""}\n" +
                                "Correo: ${cuenta?.text("email")} · Teléfono: ${cuenta?.text("telefono") ?: "—"}\n" +
                                "Reservas: ${it.list("reservas").size} · Pagos: ${it.list("pagos").size} · " +
                                "Reseñas: ${it.list("resenas").size} · Solicitudes: ${it.list("solicitudes").size}"
                        }
                        message = e
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                border = BorderStroke(1.dp, GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ver mis datos", color = GoldPrimary) }
            summary?.let { Text(it, color = TextIvory, fontSize = 12.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("RECTIFICACION" to "Rectificar", "OPOSICION" to "Oponerme", "BLOQUEO" to "Bloquear").forEach { (code, label) ->
                    FilterChip(selected = type == code, onClick = { type = code }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            OutlinedTextField(
                value = detail, onValueChange = { detail = it },
                label = { Text("Cuéntanos qué necesitas") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.sendRightsRequest(type, detail) { ok, e -> message = ok ?: e; if (ok != null) detail = "" }
                },
                enabled = detail.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enviar solicitud", color = DarkBackground, fontWeight = FontWeight.Bold) }
            message?.let { Text(it, color = GoldPrimary, fontSize = 12.sp) }
        }
    }
}
