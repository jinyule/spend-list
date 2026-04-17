package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class GetTotalSpentUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val convertCurrency: ConvertCurrencyUseCase
) {
    // Historical actual spending: sum of each subscription's totalPaidAmount
    // across ALL statuses. A subscription that was ACTIVE and got marked EXPIRED
    // (or was manually CANCELLED) keeps the money the user already paid before
    // stopping — only future unpaid cycles are excluded.
    // Renewing an EXPIRED subscription advances nextRenewalDate, which grows
    // paidCycles() and thus restores that cycle to the total.
    suspend operator fun invoke(targetCurrency: Currency): BigDecimal {
        val subscriptions = subscriptionRepository.getAllOnce()
        var total = BigDecimal.ZERO

        for (sub in subscriptions) {
            val totalSpentOnSub = sub.totalPaidAmount
            if (totalSpentOnSub.signum() == 0) continue

            val converted = when (val result = convertCurrency(totalSpentOnSub, sub.currency, targetCurrency)) {
                is ConvertCurrencyUseCase.Result.Success -> result.amount
                is ConvertCurrencyUseCase.Result.NoRateAvailable -> totalSpentOnSub
            }

            total = total.add(converted)
        }

        return total.setScale(2, RoundingMode.HALF_UP)
    }
}