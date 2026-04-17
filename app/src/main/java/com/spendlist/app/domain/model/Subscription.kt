package com.spendlist.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Subscription(
    val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val amount: BigDecimal,
    val currency: Currency,
    val billingCycle: BillingCycle,
    val billingDayOfMonth: Int? = null,
    val startDate: LocalDate,
    val nextRenewalDate: LocalDate,
    val note: String? = null,
    val manageUrl: String? = null,
    val iconUri: String? = null,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = status == SubscriptionStatus.ACTIVE && nextRenewalDate.isBefore(LocalDate.now())

    fun daysUntilRenewal(): Long {
        return ChronoUnit.DAYS.between(LocalDate.now(), nextRenewalDate)
    }

    val monthlyAmount: BigDecimal
        get() = amount.multiply(BigDecimal.valueOf(billingCycle.monthlyFactor()))

    // Number of billing cycles already paid: [startDate, nextRenewalDate).
    // nextRenewalDate is the next UNPAID date, so anything before it has been paid.
    fun paidCycles(): Long {
        val cycles = when (billingCycle) {
            is BillingCycle.Monthly -> ChronoUnit.MONTHS.between(startDate, nextRenewalDate)
            is BillingCycle.Quarterly -> ChronoUnit.MONTHS.between(startDate, nextRenewalDate) / 3
            is BillingCycle.Yearly -> ChronoUnit.YEARS.between(startDate, nextRenewalDate)
            is BillingCycle.Custom -> {
                val days = ChronoUnit.DAYS.between(startDate, nextRenewalDate)
                if (billingCycle.days <= 0) 0L else days / billingCycle.days
            }
        }
        return cycles.coerceAtLeast(0L)
    }

    val totalPaidAmount: BigDecimal
        get() = amount.multiply(BigDecimal(paidCycles()))
}
