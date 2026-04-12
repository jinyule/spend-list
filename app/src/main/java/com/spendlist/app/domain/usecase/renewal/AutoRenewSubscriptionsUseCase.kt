package com.spendlist.app.domain.usecase.renewal

import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.time.LocalDate
import javax.inject.Inject

class AutoRenewSubscriptionsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val renewalHistoryRepository: RenewalHistoryRepository
) {
    suspend operator fun invoke(): Int {
        val allSubs = subscriptionRepository.getAllOnce()
        val today = LocalDate.now()
        var renewedCount = 0

        for (sub in allSubs) {
            if (sub.status != SubscriptionStatus.ACTIVE) continue

            var current = sub
            var iterations = 0
            while (current.nextRenewalDate.isBefore(today) && iterations < 365) {
                val prev = current.nextRenewalDate
                val next = current.billingCycle.calculateNextRenewalDate(
                    prev, current.billingDayOfMonth
                )

                renewalHistoryRepository.insert(
                    RenewalHistory(
                        subscriptionId = current.id,
                        previousRenewalDate = prev,
                        newRenewalDate = next,
                        amount = current.amount,
                        note = "Auto-renewed"
                    )
                )

                current = current.copy(
                    nextRenewalDate = next,
                    updatedAt = System.currentTimeMillis()
                )
                renewedCount++
                iterations++
            }

            if (current.nextRenewalDate != sub.nextRenewalDate) {
                subscriptionRepository.update(current)
            }
        }
        return renewedCount
    }
}
