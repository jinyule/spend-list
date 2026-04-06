package com.spendlist.app.domain.usecase.stats

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
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
    operator fun invoke(targetCurrency: Currency): Flow<List<MonthlySpending>> {
        return subscriptionRepository.getAll().map { subscriptions ->
            val active = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
            val now = YearMonth.now()
            val months = (0 until 12).map { now.minusMonths(11L - it) }

            months.map { month ->
                var total = BigDecimal.ZERO
                for (sub in active) {
                    // Check if subscription was active during this month
                    val subStart = YearMonth.from(sub.startDate)
                    if (subStart <= month) {
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
