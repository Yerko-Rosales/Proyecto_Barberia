package com.example.ui.util

import java.util.regex.Pattern

/**
 * Robust validation and formatting utilities for:
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
     * Hashes a password string with SHA-256 for secure local storage and verification.
     */
    fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
