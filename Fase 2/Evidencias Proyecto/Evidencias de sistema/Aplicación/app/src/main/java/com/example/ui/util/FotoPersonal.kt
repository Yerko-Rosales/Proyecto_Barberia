package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import java.io.ByteArrayOutputStream

/**
 * Foto de perfil del personal (11-11-2026). La base de datos guarda la foto como data URL
 * (data:image/png;base64,...), que Coil 2 no lee por sí solo: este fetcher la decodifica.
 */
class DataUriFetcher(private val uri: Uri, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val texto = uri.toString()
        val coma = texto.indexOf(',')
        if (coma < 0 || !texto.substring(0, coma).endsWith(";base64")) return null
        val bytes = Base64.decode(texto.substring(coma + 1), Base64.DEFAULT)
        val mime = texto.substring(5, coma).substringBefore(';')
        return SourceResult(
            source = ImageSource(Buffer().write(bytes), options.context),
            mimeType = mime,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "data") DataUriFetcher(data, options) else null
    }
}

/** Lee la imagen elegida, la recorta al centro (cuadrada, 320 px) y la devuelve como data URL JPEG. */
fun imagenADataUrl(context: Context, uri: Uri): String? = try {
    val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    original?.let { bmp ->
        val lado = minOf(bmp.width, bmp.height)
        val recorte = Bitmap.createBitmap(bmp, (bmp.width - lado) / 2, (bmp.height - lado) / 2, lado, lado)
        val final = Bitmap.createScaledBitmap(recorte, 320, 320, true)
        val out = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.JPEG, 85, out)
        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
} catch (e: Exception) {
    null
}

/** Iniciales para el avatar de respaldo (sin el apodo entre comillas ni «(demo)»). */
fun inicialesDe(nombre: String): String =
    nombre.replace(Regex("\"[^\"]*\"|«[^»]*»|\\(.*?\\)"), " ")
        .trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "BB" }

/** Foto del funcionario o, si aún no tiene, un círculo con sus iniciales. */
@Composable
fun FotoPersonal(url: String?, nombre: String, size: Dp, modifier: Modifier = Modifier) {
    val oro = Color(0xFFFFD700)
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = "Foto de $nombre",
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape).border(2.dp, oro, CircleShape)
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(Color(0xFF27272A)).border(2.dp, oro, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(inicialesDe(nombre), color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.36f).sp)
        }
    }
}
