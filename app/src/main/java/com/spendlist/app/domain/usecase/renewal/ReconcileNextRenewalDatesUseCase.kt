package com.spendlist.app.domain.usecase.renewal

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Self-heals subscriptions whose nextRenewalDate was reverted by the legacy
 * AddEdit bug (editing certain fields used to recompute from startDate, losing
 * the progress of Renew actions). For each subscription, if the renewal history
 * table records a newRenewalDate later than what's currently stored on the
 * subscription, the stored date is pushed forward to match the history.
 * When the recovered date is in the future, EXPIRED is rolled back to ACTIVE.
 *
 * Safe to run repeatedly — a no-op when data is consistent.
 */
class ReconcileNextRenewalDatesUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val renewalHistoryRepository: RenewalHistoryRepository
) {
    suspend operator fun invoke(): Int {
        val today = LocalDate.now()
        val subs = subscriptionRepository.getAllOnce()
        var fixed = 0
        for (sub in subs) {
            val latest = renewalHistoryRepository.getLatestNewRenewalDate(sub.id) ?: continue
            if (!latest.isAfter(sub.nextRenewalDate)) continue

            val recoveredStatus = if (
                sub.status == SubscriptionStatus.EXPIRED &&
                !latest.isBefore(today)
            ) {
                SubscriptionStatus.ACTIVE
            } else {
                sub.status
            }
            subscriptionRepository.update(
                sub.copy(
                    nextRenewalDate = latest,
                    status = recoveredStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
            fixed++
        }
        return fixed
    }
}
