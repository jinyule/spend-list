package com.spendlist.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

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
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextRenewalDate)
    }

    val monthlyAmount: BigDecimal
        get() = amount.multiply(BigDecimal.valueOf(billingCycle.monthlyFactor()))
}
