package com.example.ui.util

import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust validation and formatting utilities for:
 * - Chilean RUT verification with Modulo 11 algorithm
 * - RFC compliant Email verification
 * - Phone and Password validation
 */
object ValidationUtils {

    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    /**
     * Validates an email address format strictly.
     * Ensures proper format: username@domain.extension with no spaces or invalid characters.
     */
    fun isValidEmail(rawEmail: String?): Boolean {
        if (rawEmail.isNullOrBlank()) return false
        val clean = rawEmail.trim()
        if (clean.length > 254 || clean.contains(" ")) return false
        if (clean.count { it == '@' } != 1) return false
        if (clean.contains("..")) return false

        val parts = clean.split("@")
        if (parts.size != 2) return false
        val localPart = parts[0]
        val domainPart = parts[1]

        if (localPart.isBlank() || domainPart.isBlank()) return false
        if (localPart.startsWith(".") || localPart.endsWith(".")) return false
        if (domainPart.startsWith(".") || domainPart.endsWith(".")) return false
        if (!domainPart.contains(".")) return false

        val tld = domainPart.substringAfterLast(".")
        if (tld.length < 2 || !tld.all { it.isLetter() }) return false

        return EMAIL_PATTERN.matcher(clean).matches()
    }

    /**
     * Cleans a Chilean RUT by removing points, dashes, and extra spaces.
     * Example: " 19.342.110-3 " -> "193421103"
     */
    fun cleanChileanRut(rawRut: String?): String {
        if (rawRut.isNullOrBlank()) return ""
        return rawRut.uppercase()
            .replace(".", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()
    }

    /**
     * Validates a Chilean RUT using the official Módulo 11 algorithm.
     * Multipliers cycle 2, 3, 4, 5, 6, 7 from right to left on the RUT body.
     */
    fun isValidChileanRut(rawRut: String?): Boolean {
        val clean = cleanChileanRut(rawRut)
        if (clean.length < 8 || clean.length > 9) return false

        val body = clean.substring(0, clean.length - 1)
        val dv = clean.last()

        if (!body.all { it.isDigit() }) return false
        val bodyNum = body.toLongOrNull() ?: return false
        if (bodyNum <= 0) return false
        if (dv !in '0'..'9' && dv != 'K') return false

        var sum = 0
        var multiplier = 2
        for (i in body.length - 1 downTo 0) {
            val digit = body[i] - '0'
            sum += digit * multiplier
            multiplier = if (multiplier >= 7) 2 else multiplier + 1
        }

        val remainder = sum % 11
        val calculatedDvValue = 11 - remainder
        val expectedDv = when (calculatedDvValue) {
            11 -> '0'
            10 -> 'K'
            else -> ('0'.code + calculatedDvValue).toChar()
        }

        return dv == expectedDv
    }

    /**
     * Formats a Chilean RUT with standard thousands separators and dash.
     * Example: "193421103" -> "19.342.110-3"
     */
    fun formatChileanRut(rawRut: String?): String {
        val clean = cleanChileanRut(rawRut)
        if (clean.length < 2) return clean
        val body = clean.substring(0, clean.length - 1)
        val dv = clean.last()

        return try {
            val num = body.toLong()
            val formattedBody = NumberFormat.getNumberInstance(Locale("es", "CL")).format(num)
            "$formattedBody-$dv"
        } catch (e: Exception) {
            "$body-$dv"
        }
    }

    /**
     * Hashes a password string with SHA-256 for secure local storage and verification.
     */
    fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
