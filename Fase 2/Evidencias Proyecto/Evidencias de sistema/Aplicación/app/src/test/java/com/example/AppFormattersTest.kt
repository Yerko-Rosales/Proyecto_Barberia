package com.example

import com.example.ui.util.AppFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppFormattersTest {

    private fun chileFormat(pattern: String) =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = AppFormatters.CHILE_TIME_ZONE }

    private fun inChile(minutesFromNow: Int, pattern: String = "yyyy-MM-dd HH:mm"): String =
        chileFormat(pattern).format(Date(System.currentTimeMillis() + minutesFromNow * 60_000L))

    @Test
    fun isoAndChileanFormatsGiveTheSameInstant() {
        // Antes «2026-10-31 20:00» se leía como dd-MM-yyyy (año 31)
        val iso = AppFormatters.parseAppointmentDate("2026-10-31 20:00")!!
        val cl = AppFormatters.parseAppointmentDate("31-10-2026 20:00")!!
        assertEquals(cl.time, iso.time)
        val cal = Calendar.getInstance(AppFormatters.CHILE_TIME_ZONE).apply { time = iso }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun parsesOtherKnownFormats() {
        val base = AppFormatters.parseAppointmentDate("2026-10-31 20:00")!!.time
        assertEquals(base, AppFormatters.parseAppointmentDate("2026-10-31T20:00:00")!!.time)
        assertEquals(base, AppFormatters.parseAppointmentDate("2026-10-31 20:00:00")!!.time)
        assertEquals(base, AppFormatters.parseAppointmentDate("31-10-2026 20:00 hrs")!!.time)
    }

    @Test
    fun rejectsInvalidDates() {
        assertNull(AppFormatters.parseAppointmentDate(""))
        assertNull(AppFormatters.parseAppointmentDate("31-13-2026 10:00"))
        assertNull(AppFormatters.parseAppointmentDate("31-02-2026 10:00"))
        assertNull(AppFormatters.parseAppointmentDate("sin fecha"))
    }

    @Test
    fun futureCloudAppointmentIsNotPast() {
        assertFalse(AppFormatters.isAppointmentPast(inChile(60 * 24)))
        assertTrue(AppFormatters.isAppointmentPast(inChile(-60)))
        assertTrue(AppFormatters.hoursUntilAppointment(inChile(60 * 48)) > 47.0)
    }

    @Test
    fun displayFormatIsAlwaysDayMonthYear() {
        assertEquals("31-10-2026 20:00", AppFormatters.toDisplayDateTime("2026-10-31 20:00"))
        assertEquals("31-10-2026 20:00", AppFormatters.toDisplayDateTime("2026-10-31T20:00:00"))
        assertEquals("31-10-2026 09:00", AppFormatters.toDisplayDateTime("31-10-2026 9:00 hrs"))
        assertEquals("02-11-2026", AppFormatters.toDisplayDateTime("2026-11-02"))
        assertEquals("Vence el 02-12-2026 (ingreso 02-11-2026)",
            AppFormatters.isoDatesToDisplay("Vence el 2026-12-02 (ingreso 2026-11-02)"))
    }

    @Test
    fun slotNeedsNinetyMinutesOfLeadTime() {
        val day = inChile(0, "yyyy-MM-dd")
        assertFalse(AppFormatters.isSlotBookable(inChile(-30, "dd-MM-yyyy"), inChile(-30, "HH:mm")))
        assertFalse(AppFormatters.isSlotBookable(inChile(60, "dd-MM-yyyy"), inChile(60, "HH:mm")))
        assertTrue(AppFormatters.isSlotBookable(inChile(120, "dd-MM-yyyy"), inChile(120, "HH:mm")))
        assertTrue(AppFormatters.isSlotBookable(inChile(60 * 24 * 3, "yyyy-MM-dd"), "10:00"))
        assertFalse(AppFormatters.isSlotBookable(day, ""))
    }
}
