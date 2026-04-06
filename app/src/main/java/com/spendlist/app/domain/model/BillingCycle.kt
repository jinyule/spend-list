package com.spendlist.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class BillingCycle {
    data object Monthly : BillingCycle()
    data object Yearly : BillingCycle()
    data class Custom(val days: Int) : BillingCycle()

    fun calculateNextRenewalDate(fromDate: LocalDate): LocalDate {
        return when (this) {
            is Monthly -> fromDate.plusMonths(1)
            is Yearly -> fromDate.plusYears(1)
            is Custom -> fromDate.plusDays(days.toLong())
        }
    }

    fun toDays(): Int {
        return when (this) {
            is Monthly -> 30
            is Yearly -> 365
            is Custom -> days
        }
    }

    /**
     * Calculate the monthly equivalent amount for normalization.
     * Returns a multiplier to convert from per-cycle to per-month.
     */
    fun monthlyFactor(): Double {
        return when (this) {
            is Monthly -> 1.0
            is Yearly -> 1.0 / 12.0
            is Custom -> 30.0 / days.toDouble()
        }
    }
}
