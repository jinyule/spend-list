package com.spendlist.app.domain.usecase.stats

import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
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
    val categoryNameResKey: String?, // For i18n
    val color: Long,
    val amount: BigDecimal,
    val percentage: Float
)

/**
 * Two visualization modes:
 * - CURRENT_MONTHLY: "if all ACTIVE subscriptions continue, how much does each
 *   category cost per month going forward" — a prediction.
 * - HISTORICAL_TOTAL: "across all subscriptions (including EXPIRED/CANCELLED),
 *   how much has each category cost cumulatively" — historical actual spending.
 */
enum class CategoryStatsMode { CURRENT_MONTHLY, HISTORICAL_TOTAL }

class GetSpendingByCategoryUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val convertCurrency: ConvertCurrencyUseCase
) {
    operator fun invoke(
        targetCurrency: Currency,
        mode: CategoryStatsMode = CategoryStatsMode.CURRENT_MONTHLY
    ): Flow<List<CategorySpending>> {
        return combine(
            subscriptionRepository.getAll(),
            categoryRepository.getAll()
        ) { subscriptions, categories ->
            val relevant = when (mode) {
                CategoryStatsMode.CURRENT_MONTHLY ->
                    subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
                CategoryStatsMode.HISTORICAL_TOTAL ->
                    subscriptions.filter { it.paidCycles() > 0 }
            }
            if (relevant.isEmpty()) return@combine emptyList()

            val categoryMap = categories.associateBy { it.id }

            val grouped = relevant.groupBy { it.categoryId }
            val categoryAmounts = grouped.map { (catId, subs) ->
                var total = BigDecimal.ZERO
                for (sub in subs) {
                    val baseAmount = when (mode) {
                        CategoryStatsMode.CURRENT_MONTHLY -> sub.monthlyAmount
                        CategoryStatsMode.HISTORICAL_TOTAL -> sub.totalPaidAmount
                    }
                    val converted = when (val r = convertCurrency(baseAmount, sub.currency, targetCurrency)) {
                        is ConvertCurrencyUseCase.Result.Success -> r.amount
                        is ConvertCurrencyUseCase.Result.NoRateAvailable -> baseAmount
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
                    categoryNameResKey = cat?.nameResKey,
                    color = cat?.color ?: 0xFF95A5A6,
                    amount = amount,
                    percentage = pct
                )
            }.sortedByDescending { it.amount }
        }
    }
}
