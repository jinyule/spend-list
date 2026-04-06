package com.spendlist.app.domain.usecase.subscription

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(
        categoryId: Long? = null,
        status: SubscriptionStatus? = null
    ): Flow<List<Subscription>> {
        return when {
            categoryId != null -> repository.getByCategory(categoryId)
            status != null -> repository.getByStatus(status)
            else -> repository.getAll()
        }
    }
}
