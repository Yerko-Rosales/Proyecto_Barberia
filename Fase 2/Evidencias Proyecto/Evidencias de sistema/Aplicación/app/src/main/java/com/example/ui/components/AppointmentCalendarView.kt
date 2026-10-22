package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GreenConfirm
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Real-time dynamic appointment calendar widget.
 * Features:
 * - Current real-time calendar (shows current month, year, and days).
 * - Strictly disables past dates (no selecting dates before today).
 * - Month navigation with back-lock (cannot navigate to past months).
 * - Quick-select upcoming days carousel (Hoy, Mañana, + próximas fechas).
 * - Formatted selected date preview with full Spanish date representation.
 */
@Composable
fun RealTimeAppointmentCalendar(
    selectedDate: String, // Format "dd-MM-yyyy"
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localeEs = remember { Locale("es", "CL") }
    val todayCal = remember { Calendar.getInstance() }
    val currentRealYear = todayCal.get(Calendar.YEAR)
    val currentRealMonth = todayCal.get(Calendar.MONTH) // 0-11
    val currentRealDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val todayFormatted = remember {
        String.format(Locale.US, "%02d-%02d-%04d", currentRealDay, currentRealMonth + 1, currentRealYear)
    }

    // Auto-select today if no date is selected or if selected date is in the past
    LaunchedEffect(selectedDate) {
        if (selectedDate.isBlank()) {
            onDateSelected(todayFormatted)
        } else {
            try {
                val parts = selectedDate.split("-")
                if (parts.size == 3) {
                    val sDay = parts[0].toInt()
                    val sMonth = parts[1].toInt() - 1
                    val sYear = parts[2].toInt()
                    val isPast = (sYear < currentRealYear) ||
                            (sYear == currentRealYear && sMonth < currentRealMonth) ||
                            (sYear == currentRealYear && sMonth == currentRealMonth && sDay < currentRealDay)
                    if (isPast) {
                        onDateSelected(todayFormatted)
                    }
                }
            } catch (e: Exception) {
                onDateSelected(todayFormatted)
            }
        }
    }

    // View state for displayed month and year in calendar
    var displayedYear by remember { mutableStateOf(currentRealYear) }
    var displayedMonth by remember { mutableStateOf(currentRealMonth) }

    // Check if the displayed month is the current real month
    val isViewingCurrentMonth = (displayedYear == currentRealYear && displayedMonth == currentRealMonth)

    // Month name formatted in Spanish
    val monthName = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val name = SimpleDateFormat("MMMM yyyy", localeEs).format(cal.time)
        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
    }

    // Friendly description of currently selected date
    val selectedDateDescription = remember(selectedDate) {
        if (selectedDate.isBlank()) {
            "Selecciona un día en el calendario"
        } else {
            try {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val date = sdf.parse(selectedDate)
                if (date != null) {
                    val outSdf = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", localeEs)
                    val formatted = outSdf.format(date)
                    val capitalized = formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
                    if (selectedDate == todayFormatted) "$capitalized (HOY)" else capitalized
                } else {
                    selectedDate
                }
            } catch (e: Exception) {
                selectedDate
            }
        }
    }

    // Days of week header in Spanish (Monday to Sunday)
    val weekDays = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")

    // Calculate grid numbers for displayed month
    val monthCal = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, displayedYear)
        cal.set(Calendar.MONTH, displayedMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Day of week: Sunday is 1, Monday is 2, ..., Saturday is 7. Convert to Monday = 0, ..., Sunday = 6
    val dayOfWeekSundayBased = monthCal.get(Calendar.DAY_OF_WEEK)
    val startOffset = (dayOfWeekSundayBased + 5) % 7 // Monday becomes 0, Sunday becomes 6

    // Precomputed upcoming days carousel data (computed once per day)
    val upcomingDays = remember {
        val sdfDayOfWeek = SimpleDateFormat("EEE", localeEs)
        val sdfMonthShort = SimpleDateFormat("MMM", localeEs)
        (0..11).map { i ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, i)
            }
            val dYear = dayCal.get(Calendar.YEAR)
            val dMonth = dayCal.get(Calendar.MONTH)
            val dDay = dayCal.get(Calendar.DAY_OF_MONTH)
            val dateCode = String.format(Locale.US, "%02d-%02d-%04d", dDay, dMonth + 1, dYear)
            val labelDayOfWeek = when (i) {
                0 -> "HOY"
                1 -> "MAÑANA"
                else -> sdfDayOfWeek.format(dayCal.time).uppercase().replace(".", "")
            }
            val labelDayNum = String.format(Locale.US, "%02d", dDay)
            val labelMonthShort = sdfMonthShort.format(dayCal.time).uppercase().replace(".", "")
            QuickDayItem(
                index = i,
                year = dYear,
                month = dMonth,
                dateCode = dateCode,
                dayOfWeek = labelDayOfWeek,
                dayNum = labelDayNum,
                monthShort = labelMonthShort
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // --- 1. QUICK SELECT HORIZONTAL STRIP (Next 10 days) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Días Próximos Disponibles:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GoldAccent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            upcomingDays.forEach { item ->
                val isSelected = selectedDate == item.dateCode

                Card(
                    onClick = {
                        onDateSelected(item.dateCode)
                        displayedYear = item.year
                        displayedMonth = item.month
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) GoldPrimary else CardBackground
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) GoldPrimary else Color(0xFF2A2A30)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.dayOfWeek,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) DarkBackground else if (item.index == 0) GreenConfirm else GoldAccent
                        )
                        Text(
                            text = item.dayNum,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) DarkBackground else TextIvory
                        )
                        Text(
                            text = item.monthShort,
                            fontSize = 8.sp,
                            color = if (isSelected) DarkBackground.copy(alpha = 0.8f) else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = GoldPrimary.copy(alpha = 0.2f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. MONTH & YEAR NAVIGATION HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isViewingCurrentMonth) {
                        if (displayedMonth == 0) {
                            displayedMonth = 11
                            displayedYear -= 1
                        } else {
                            displayedMonth -= 1
                        }
                    }
                },
                enabled = !isViewingCurrentMonth,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Mes anterior",
                    tint = if (!isViewingCurrentMonth) GoldAccent else TextMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextIvory
                )
            }

            IconButton(
                onClick = {
                    if (displayedMonth == 11) {
                        displayedMonth = 0
                        displayedYear += 1
                    } else {
                        displayedMonth += 1
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Mes siguiente",
                    tint = GoldAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. WEEKDAYS HEADER (LUN, MAR, MIÉ, JUE, VIE, SÁB, DOM) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { wDay ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wDay,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (wDay == "SÁB" || wDay == "DOM") GoldAccent.copy(alpha = 0.8f) else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 4. CALENDAR DAYS GRID (Strictly disabling past dates) ---
        val totalGridSlots = startOffset + daysInMonth
        val rows = (totalGridSlots + 6) / 7

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0..6) {
                        val slotIndex = row * 7 + col
                        if (slotIndex < startOffset || slotIndex >= totalGridSlots) {
                            // Empty slot outside month
                            Box(modifier = Modifier.weight(1f).aspectRatio(1.1f))
                        } else {
                            val dayNumber = slotIndex - startOffset + 1
                            val dateCode = String.format(Locale.US, "%02d-%02d-%04d", dayNumber, displayedMonth + 1, displayedYear)

                            // Check if this date is in the PAST
                            val isPast = (displayedYear < currentRealYear) ||
                                    (displayedYear == currentRealYear && displayedMonth < currentRealMonth) ||
                                    (displayedYear == currentRealYear && displayedMonth == currentRealMonth && dayNumber < currentRealDay)

                            val isToday = (displayedYear == currentRealYear && displayedMonth == currentRealMonth && dayNumber == currentRealDay)
                            val isSelected = (selectedDate == dateCode)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> GoldPrimary
                                            isToday -> Color(0xFF1F2B20)
                                            isPast -> Color(0xFF141416)
                                            else -> CardBackground
                                        }
                                    )
                                    .border(
                                        width = if (isSelected || isToday) 1.dp else 0.5.dp,
                                        color = when {
                                            isSelected -> GoldPrimary
                                            isToday -> GreenConfirm
                                            isPast -> Color.Transparent
                                            else -> Color(0xFF2E2E35)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isPast) {
                                        onDateSelected(dateCode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal,
                                        color = when {
                                            isSelected -> DarkBackground
                                            isPast -> TextMuted.copy(alpha = 0.28f)
                                            isToday -> GreenConfirm
                                            else -> TextIvory
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                    if (isToday && !isSelected) {
                                        Text(
                                            text = "HOY",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GreenConfirm,
                                            lineHeight = 7.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 5. FULL SELECTED DATE CALLOUT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191712)),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (selectedDate.isNotBlank()) GoldAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FECHA DE SERVICIO ELEGIDA:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = selectedDateDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }
            }
        }
    }
}

private data class QuickDayItem(
    val index: Int,
    val year: Int,
    val month: Int,
    val dateCode: String,
    val dayOfWeek: String,
    val dayNum: String,
    val monthShort: String
)
