package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.SubscriptionRepository
import javax.inject.Inject

class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(subscription: Subscription) {
        repository.delete(subscription)
    }
}
