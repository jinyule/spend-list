package com.spendlist.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class BillingCycle {
    data object Monthly : BillingCycle()
    data object Quarterly : BillingCycle()
    data object Yearly : BillingCycle()
    data class Custom(val days: Int) : BillingCycle() {
        init {
            // Ensure days is at least 1 to avoid division by zero
            require(days >= 1) { "Custom billing cycle days must be at least 1" }
        }

        // Safe days getter, returns at least 1
        val safeDays: Int get() = if (days >= 1) days else 1
    }

    fun calculateNextRenewalDate(fromDate: LocalDate): LocalDate {
        return when (this) {
            is Monthly -> fromDate.plusMonths(1)
            is Quarterly -> fromDate.plusMonths(3)
            is Yearly -> fromDate.plusYears(1)
            is Custom -> fromDate.plusDays(safeDays.toLong())
        }
    }

    fun calculateNextRenewalDate(fromDate: LocalDate, billingDayOfMonth: Int?): LocalDate {
        if (billingDayOfMonth == null || this is Custom) {
            return calculateNextRenewalDate(fromDate)
        }
        val nextBase = when (this) {
            is Monthly -> fromDate.plusMonths(1)
            is Quarterly -> fromDate.plusMonths(3)
            is Yearly -> fromDate.plusYears(1)
            is Custom -> return calculateNextRenewalDate(fromDate)
        }
        val maxDay = nextBase.lengthOfMonth()
        val targetDay = billingDayOfMonth.coerceIn(1, maxDay)
        return nextBase.withDayOfMonth(targetDay)
    }

    fun toDays(): Int {
        return when (this) {
            is Monthly -> 30
            is Quarterly -> 90
            is Yearly -> 365
            is Custom -> safeDays
        }
    }

    /**
     * Calculate the monthly equivalent amount for normalization.
     * Returns a multiplier to convert from per-cycle to per-month.
     */
    fun monthlyFactor(): Double {
        return when (this) {
            is Monthly -> 1.0
            is Quarterly -> 1.0 / 3.0
            is Yearly -> 1.0 / 12.0
            is Custom -> 30.0 / safeDays.toDouble()
        }
    }
}
