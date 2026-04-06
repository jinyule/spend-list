package com.spendlist.app.domain.usecase.stats

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String,
    val color: Long,
    val amount: BigDecimal,
    val percentage: Float
)

class GetSpendingByCategoryUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val convertCurrency: ConvertCurrencyUseCase
) {
    operator fun invoke(targetCurrency: Currency): Flow<List<CategorySpending>> {
        return combine(
            subscriptionRepository.getAll(),
            categoryRepository.getAll()
        ) { subscriptions, categories ->
            val active = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
            if (active.isEmpty()) return@combine emptyList()

            val categoryMap = categories.associateBy { it.id }

            // Group by category and sum monthly amounts
            val grouped = active.groupBy { it.categoryId }
            val categoryAmounts = grouped.map { (catId, subs) ->
                var total = BigDecimal.ZERO
                for (sub in subs) {
                    val monthly = sub.monthlyAmount
                    val converted = when (val r = convertCurrency(monthly, sub.currency, targetCurrency)) {
                        is ConvertCurrencyUseCase.Result.Success -> r.amount
                        is ConvertCurrencyUseCase.Result.NoRateAvailable -> monthly
                    }
                    total = total.add(converted)
                }
                val cat = catId?.let { categoryMap[it] }
                Triple(catId, cat, total)
            }

            val grandTotal = categoryAmounts.fold(BigDecimal.ZERO) { acc, (_, _, amt) -> acc.add(amt) }

            categoryAmounts.map { (catId, cat, amount) ->
                val pct = if (grandTotal > BigDecimal.ZERO) {
                    amount.multiply(BigDecimal(100))
                        .divide(grandTotal, 1, RoundingMode.HALF_UP)
                        .toFloat()
                } else 0f
                CategorySpending(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Other",
                    color = cat?.color ?: 0xFF95A5A6,
                    amount = amount,
                    percentage = pct
                )
            }.sortedByDescending { it.amount }
        }
    }
}
