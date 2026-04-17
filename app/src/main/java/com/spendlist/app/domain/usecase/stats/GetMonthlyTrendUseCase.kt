package com.spendlist.app.domain.usecase.stats

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

data class MonthlySpending(
    val yearMonth: YearMonth,
    val amount: BigDecimal
)

class GetMonthlyTrendUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val convertCurrency: ConvertCurrencyUseCase
) {
    // Historical actual spending: for each month, sum monthlyAmount of every
    // subscription whose paid window [startYm, endYm) covers that month.
    // endYm = YearMonth.from(nextRenewalDate) is exclusive because nextRenewalDate
    // itself is the next UNPAID renewal. This naturally excludes months after a
    // user stopped paying (EXPIRED/CANCELLED), and re-includes them once
    // RecordRenewalUseCase advances nextRenewalDate on manual renewal.
    operator fun invoke(targetCurrency: Currency): Flow<List<MonthlySpending>> {
        return subscriptionRepository.getAll().map { subscriptions ->
            val now = YearMonth.now()
            val months = (0 until 12).map { now.minusMonths(11L - it) }

            months.map { month ->
                var total = BigDecimal.ZERO
                for (sub in subscriptions) {
                    val startYm = YearMonth.from(sub.startDate)
                    val endYm = YearMonth.from(sub.nextRenewalDate)
                    if (!month.isBefore(startYm) && month.isBefore(endYm)) {
                        val monthly = sub.monthlyAmount
                        val converted = when (val r = convertCurrency(monthly, sub.currency, targetCurrency)) {
                            is ConvertCurrencyUseCase.Result.Success -> r.amount
                            is ConvertCurrencyUseCase.Result.NoRateAvailable -> monthly
                        }
                        total = total.add(converted)
                    }
                }
                MonthlySpending(yearMonth = month, amount = total)
            }
        }
    }
}
