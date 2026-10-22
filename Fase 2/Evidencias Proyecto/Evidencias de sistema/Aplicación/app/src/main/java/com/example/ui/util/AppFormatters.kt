package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-performance, centralized formatting utilities.
 * Thread-safe formatters avoiding expensive repeated allocations during UI recompositions and background flows.
 */
object AppFormatters {
    val CHILE_LOCALE = Locale("es", "CL")

    private val numberFormatThreadLocal = ThreadLocal.withInitial {
        NumberFormat.getNumberInstance(CHILE_LOCALE)
    }

    private val dateFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("dd-MM-yyyy", Locale.US)
    }

    private val dateTimeFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
    }

    private val timeFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.US)
    }

    private val fullSpanishDateFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", CHILE_LOCALE)
    }

    private val monthYearFormatThreadLocal = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM yyyy", CHILE_LOCALE)
    }

    /**
     * Formats an integer / long / double amount as standard Chilean Peso string without decimals.
     * Example: formatClp(12000) -> "12.000"
     */
    fun formatClp(amount: Number): String {
        return numberFormatThreadLocal.get()?.format(amount) ?: amount.toString()
    }

    /**
     * Formats an amount with full currency label.
     * Example: formatClpPrice(12000) -> "$12.000 CLP"
     */
    fun formatClpPrice(amount: Number): String {
        return "$${formatClp(amount)} CLP"
    }

    /**
     * Returns today's date formatted as "dd-MM-yyyy"
     */
    fun currentDateString(): String {
        return dateFormatThreadLocal.get()?.format(Date()) ?: ""
    }

    /**
     * Returns current date & time formatted as "dd-MM-yyyy HH:mm"
     */
    fun currentDateTimeString(): String {
        return dateTimeFormatThreadLocal.get()?.format(Date()) ?: ""
    }

    /**
     * Returns current time formatted as "HH:mm"
     */
    fun currentTimeString(): String {
        return timeFormatThreadLocal.get()?.format(Date()) ?: ""
    }

    /**
     * Formats a date string (dd-MM-yyyy) into full human-friendly Spanish.
     */
    fun formatSpanishFullDate(dateString: String): String {
        return try {
            val date = dateFormatThreadLocal.get()?.parse(dateString) ?: return dateString
            val formatted = fullSpanishDateFormatThreadLocal.get()?.format(date) ?: dateString
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(CHILE_LOCALE) else it.toString() }
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Formats month and year for calendar header.
     */
    fun formatMonthYear(date: Date): String {
        val formatted = monthYearFormatThreadLocal.get()?.format(date) ?: ""
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(CHILE_LOCALE) else it.toString() }
    }

    /**
     * Parses an appointment date/time string from any known format into java.util.Date
     */
    fun parseAppointmentDate(dateTimeString: String): Date? {
        val trimmed = dateTimeString.trim()
        if (trimmed.isEmpty()) return null
        return try {
            dateTimeFormatThreadLocal.get()?.parse(trimmed)
                ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(trimmed)
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(trimmed)
                ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(trimmed)
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(trimmed)
                ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(trimmed)
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trimmed)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Determines whether an appointment's scheduled date and time has already passed.
     */
    fun isAppointmentPast(dateTimeString: String): Boolean {
        val date = parseAppointmentDate(dateTimeString) ?: return false
        return date.time < System.currentTimeMillis()
    }

    /**
     * Returns the hours difference until an appointment (e.g. "15-06-2026 14:00" or ISO "2026-10-07 15:00").
     * If positive >= 24.0, more than 24 hours remain.
     */
    fun hoursUntilAppointment(dateTimeString: String): Double {
        val parsedDate = parseAppointmentDate(dateTimeString) ?: return 0.0
        val diffMillis = parsedDate.time - System.currentTimeMillis()
        return diffMillis.toDouble() / (1000.0 * 60.0 * 60.0)
    }

    /**
     * Determines if two date strings represent the exact same calendar day,
     * supporting any combination of formats between Android app and Supabase/Web:
     * - "dd-MM-yyyy" (e.g. "07-10-2026")
     * - "yyyy-MM-dd" (e.g. "2026-10-07")
     * - "yyyy-MM-dd HH:mm:ss" (e.g. "2026-10-07 15:00:00")
     * - "yyyy-MM-ddTHH:mm:ss" (e.g. "2026-10-07T15:00:00")
     * - "dd/MM/yyyy" or "yyyy/MM/dd"
     */
    fun isSameDay(dt1: String?, dt2: String?): Boolean {
        if (dt1.isNullOrBlank() || dt2.isNullOrBlank()) return false
        val s1 = dt1.trim()
        val s2 = dt2.trim()
        if (s1 == s2) return true

        val p1 = parseDayMonthYear(s1)
        val p2 = parseDayMonthYear(s2)

        if (p1 != null && p2 != null) {
            return p1.first == p2.first && p1.second == p2.second && p1.third == p2.third
        }

        return s1.startsWith(s2) || s2.startsWith(s1)
    }

    private fun parseDayMonthYear(str: String): Triple<Int, Int, Int>? {
        return try {
            val datePart = str.substringBefore(' ').substringBefore('T').trim().replace('/', '-')
            val tokens = datePart.split('-')
            if (tokens.size != 3) return null

            if (tokens[0].length == 4) {
                // yyyy-MM-dd
                val y = tokens[0].toIntOrNull() ?: return null
                val m = tokens[1].toIntOrNull() ?: return null
                val d = tokens[2].toIntOrNull() ?: return null
                Triple(y, m, d)
            } else if (tokens[2].length == 4) {
                // dd-MM-yyyy
                val d = tokens[0].toIntOrNull() ?: return null
                val m = tokens[1].toIntOrNull() ?: return null
                val y = tokens[2].toIntOrNull() ?: return null
                Triple(y, m, d)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts normalized "HH:mm" slot hour from any date/time string:
     * - "2026-10-07 15:00:00" -> "15:00"
     * - "2026-10-07T15:00:00.000Z" -> "15:00"
     * - "07-10-2026 15:00" -> "15:00"
     * - "15:00" -> "15:00"
     * - "9:00" -> "09:00"
     */
    fun extractHourSlot(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return ""
        val trimmed = dateTimeString.trim()

        val timePart = when {
            trimmed.contains('T') -> trimmed.substringAfter('T').substringBefore('Z').substringBefore('+').trim()
            trimmed.contains(' ') -> trimmed.substringAfterLast(' ').trim()
            else -> trimmed
        }

        if (!timePart.contains(':')) return timePart

        return try {
            val tokens = timePart.split(':')
            val h = tokens[0].toIntOrNull() ?: return timePart
            val m = if (tokens.size > 1) {
                tokens[1].take(2).toIntOrNull() ?: 0
            } else 0
            String.format(Locale.US, "%02d:%02d", h, m)
        } catch (e: Exception) {
            timePart
        }
    }

    /**
     * Converts a Chilean dd-MM-yyyy date into ISO yyyy-MM-dd for cloud compatibility
     */
    fun toIsoDate(dateString: String): String {
        val triple = parseDayMonthYear(dateString) ?: return dateString
        return String.format(Locale.US, "%04d-%02d-%02d", triple.first, triple.second, triple.third)
    }

    /**
     * Converts an ISO or standard date/time string into display "dd-MM-yyyy HH:mm"
     */
    fun toDisplayDateTime(dateTimeString: String): String {
        if (dateTimeString.isBlank()) return ""
        val triple = parseDayMonthYear(dateTimeString) ?: return dateTimeString
        val hour = extractHourSlot(dateTimeString)
        val datePart = String.format(Locale.US, "%02d-%02d-%04d", triple.third, triple.second, triple.first)
        return if (hour.isNotBlank()) "$datePart $hour" else datePart
    }
}
