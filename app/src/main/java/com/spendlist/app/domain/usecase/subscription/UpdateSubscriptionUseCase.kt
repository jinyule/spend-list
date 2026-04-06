package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.math.BigDecimal
import javax.inject.Inject

class UpdateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    sealed class Result {
        data object Success : Result()
        data class ValidationError(val message: String) : Result()
    }

    suspend operator fun invoke(subscription: Subscription): Result {
        if (subscription.name.isBlank()) {
            return Result.ValidationError("Name is required")
        }
        if (subscription.amount <= BigDecimal.ZERO) {
            return Result.ValidationError("Amount must be greater than 0")
        }

        repository.update(subscription.copy(updatedAt = System.currentTimeMillis()))
        return Result.Success
    }
}
