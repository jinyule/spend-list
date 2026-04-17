package com.spendlist.app.domain.usecase.renewal

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.time.LocalDate
import javax.inject.Inject

class MarkExpiredSubscriptionsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): List<Subscription> {
        val today = LocalDate.now()
        val allSubs = subscriptionRepository.getAllOnce()
        val newlyExpired = mutableListOf<Subscription>()

        for (sub in allSubs) {
            if (sub.status == SubscriptionStatus.ACTIVE && sub.nextRenewalDate.isBefore(today)) {
                val updated = sub.copy(
                    status = SubscriptionStatus.EXPIRED,
                    updatedAt = System.currentTimeMillis()
                )
                subscriptionRepository.update(updated)
                newlyExpired.add(updated)
            }
        }
        return newlyExpired
    }
}
