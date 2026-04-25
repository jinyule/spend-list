package com.spendlist.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Single source of truth for displaying dates.
 * Uses the device locale's MEDIUM style (e.g. "Apr 17, 2024" / "2024年4月17日").
 */
object DateFormatter {
    private val medium: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun format(date: LocalDate): String = date.format(medium)
}
