package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.SubscriptionRepository
import javax.inject.Inject

class GetSubscriptionByIdUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(id: Long): Subscription? {
        return repository.getById(id)
    }
}
