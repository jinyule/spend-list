package com.spendlist.app.domain.usecase.renewal

import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.time.LocalDate
import javax.inject.Inject

class RecordRenewalUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val renewalHistoryRepository: RenewalHistoryRepository
) {
    sealed class Result {
        data class Success(val subscription: Subscription) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        subscriptionId: Long,
        note: String? = null
    ): Result {
        val subscription = subscriptionRepository.getById(subscriptionId)
            ?: return Result.Error("Subscription not found")

        val previousRenewalDate = subscription.nextRenewalDate
        val newRenewalDate = subscription.billingCycle.calculateNextRenewalDate(previousRenewalDate)

        // Create renewal history record
        val history = RenewalHistory(
            subscriptionId = subscriptionId,
            previousRenewalDate = previousRenewalDate,
            newRenewalDate = newRenewalDate,
            amount = subscription.amount,
            note = note
        )
        renewalHistoryRepository.insert(history)

        // Update subscription
        val updatedSubscription = subscription.copy(
            nextRenewalDate = newRenewalDate,
            updatedAt = System.currentTimeMillis()
        )
        subscriptionRepository.update(updatedSubscription)

        return Result.Success(updatedSubscription)
    }
}