package com.example.data.mail

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object DirectGmailSender {

    private const val TAG = "DirectGmailSender"
    private const val PREFS_NAME = "bigboys_gmail_config"
    private const val KEY_SENDER_EMAIL = "key_sender_gmail"
    private const val KEY_APP_PASSWORD = "key_app_password"
    private const val KEY_DISPLAY_NAME = "key_display_name"

    // Default Gmail credentials; the app password comes from .env (Secrets Gradle Plugin → BuildConfig)
    var senderGmail: String = "j.ojedalefio@gmail.com"
    var senderAppPassword: String = BuildConfig.GMAIL_APP_PASSWORD
    var senderDisplayName: String = "Big boys Barbershop Chile 💈"

    fun loadCredentials(context: Context) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedEmail = prefs.getString(KEY_SENDER_EMAIL, null)
            val savedPass = prefs.getString(KEY_APP_PASSWORD, null)
            val savedName = prefs.getString(KEY_DISPLAY_NAME, null)

            senderGmail = if (!savedEmail.isNullOrBlank() && savedEmail != "bigboysbarberia.chile@gmail.com") savedEmail.trim() else "j.ojedalefio@gmail.com"
            senderAppPassword = if (!savedPass.isNullOrBlank() && savedPass.length >= 10) savedPass.trim() else BuildConfig.GMAIL_APP_PASSWORD
            senderDisplayName = if (!savedName.isNullOrBlank()) savedName.trim() else "Big boys Barbershop Chile 💈"
            Log.d(TAG, "Loaded credentials from persistent storage: email=$senderGmail, hasPassword=${senderAppPassword.isNotBlank()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved credentials: ${e.message}")
        }
    }

    fun saveCredentials(context: Context, email: String, appPassword: String, displayName: String? = null) {
        try {
            senderGmail = email.trim()
            senderAppPassword = appPassword.replace(" ", "").trim()
            if (displayName != null) {
                senderDisplayName = displayName.trim()
            }

            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(KEY_SENDER_EMAIL, senderGmail)
                putString(KEY_APP_PASSWORD, senderAppPassword)
                if (displayName != null) {
                    putString(KEY_DISPLAY_NAME, senderDisplayName)
                }
                apply()
            }
            Log.d(TAG, "Persisted credentials successfully: email=$senderGmail, passLength=${senderAppPassword.length}")
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting credentials: ${e.message}")
        }
    }

    suspend fun sendRealEmail(
        recipientEmail: String,
        subject: String,
        htmlBody: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (senderAppPassword.isBlank()) {
            Log.w(TAG, "No app password configured for $senderGmail. Email logged locally.")
            return@withContext Result.failure(Exception("Falta configurar la Contraseña de Aplicación de Gmail en el Panel de Administración."))
        }

        try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    // Clean spaces if user copied "abcd efgh ijkl mnop"
                    val cleanPassword = senderAppPassword.replace(" ", "")
                    return PasswordAuthentication(senderGmail.trim(), cleanPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderGmail.trim(), senderDisplayName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()))
                setSubject(subject, "UTF-8")
                setContent(htmlBody, "text/html; charset=utf-8")
            }

            Transport.send(message)
            Log.d(TAG, "Email sent successfully to $recipientEmail via Gmail SMTP")
            Result.success(true)
        } catch (e: Exception) {
            val rawMsg = e.message ?: ""
            Log.e(TAG, "Error sending email via Gmail SMTP: $rawMsg", e)
            
            val friendlyError = when {
                rawMsg.contains("534") || rawMsg.contains("Application-specific password", ignoreCase = true) || rawMsg.contains("InvalidSecondFactor", ignoreCase = true) -> {
                    "Google requiere una 'Contraseña de Aplicación' de 16 letras (no tu contraseña normal de Gmail). Activa 'Verificación en 2 pasos' en Google y crea una contraseña de app en myaccount.google.com/apppasswords."
                }
                rawMsg.contains("535") || rawMsg.contains("AuthenticationFailed", ignoreCase = true) || rawMsg.contains("BadCredentials", ignoreCase = true) -> {
                    "Credenciales incorrectas. Verifica que el correo Gmail y la contraseña de aplicación de 16 letras estén bien escritos."
                }
                rawMsg.contains("UnknownHost", ignoreCase = true) || rawMsg.contains("ConnectException", ignoreCase = true) -> {
                    "Sin conexión a internet o el servidor SMTP de Gmail no responde. Revisa tu conexión."
                }
                else -> "Fallo al enviar correo: $rawMsg"
            }
            Result.failure(Exception(friendlyError))
        }
    }
}
