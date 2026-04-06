package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.math.BigDecimal
import javax.inject.Inject

class AddSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    sealed class Result {
        data class Success(val id: Long) : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(subscription: Subscription): Result {
        if (subscription.name.isBlank()) {
            return Result.ValidationError("Name is required")
        }
        if (subscription.amount <= BigDecimal.ZERO) {
            return Result.ValidationError("Amount must be greater than 0")
        }

        val toInsert = subscription.copy(
            nextRenewalDate = if (subscription.nextRenewalDate <= subscription.startDate) {
                subscription.billingCycle.calculateNextRenewalDate(subscription.startDate)
            } else {
                subscription.nextRenewalDate
            }
        )

        val id = repository.insert(toInsert)
        return Result.Success(id)
    }
}
