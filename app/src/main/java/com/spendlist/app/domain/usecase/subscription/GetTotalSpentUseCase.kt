package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetTotalSpentUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val convertCurrency: ConvertCurrencyUseCase
) {
    suspend operator fun invoke(targetCurrency: Currency): BigDecimal {
        val subscriptions = subscriptionRepository.getAllOnce()
        var total = BigDecimal.ZERO

        for (sub in subscriptions) {
            if (sub.status != SubscriptionStatus.ACTIVE) continue

            // Number of payments = cycles from startDate to nextRenewalDate
            // nextRenewalDate is the next UNPAID renewal, so we've paid for all cycles before it
            val cycles = when (sub.billingCycle) {
                is BillingCycle.Monthly -> {
                    ChronoUnit.MONTHS.between(sub.startDate, sub.nextRenewalDate)
                }
                is BillingCycle.Yearly -> {
                    ChronoUnit.YEARS.between(sub.startDate, sub.nextRenewalDate)
                }
                is BillingCycle.Custom -> {
                    val days = ChronoUnit.DAYS.between(sub.startDate, sub.nextRenewalDate)
                    days / sub.billingCycle.days
                }
            }

            // Ensure at least 1 payment if nextRenewalDate > startDate
            val paymentCount = cycles.coerceAtLeast(0L)
            val totalSpentOnSub = sub.amount.multiply(BigDecimal(paymentCount))

            // Convert to target currency
            val converted = when (val result = convertCurrency(totalSpentOnSub, sub.currency, targetCurrency)) {
                is ConvertCurrencyUseCase.Result.Success -> result.amount
                is ConvertCurrencyUseCase.Result.NoRateAvailable -> totalSpentOnSub
            }

            total = total.add(converted)
        }

        return total.setScale(2, RoundingMode.HALF_UP)
    }
}