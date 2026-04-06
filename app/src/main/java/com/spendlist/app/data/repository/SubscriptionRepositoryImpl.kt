package com.spendlist.app.data.repository

import com.spendlist.app.data.local.converter.toDomain
import com.spendlist.app.data.local.converter.toEntity
import com.spendlist.app.data.local.dao.SubscriptionDao
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {

    override fun getAll(): Flow<List<Subscription>> {
        return subscriptionDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByStatus(status: SubscriptionStatus): Flow<List<Subscription>> {
        return subscriptionDao.getByStatusFlow(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getByCategory(categoryId: Long): Flow<List<Subscription>> {
        return subscriptionDao.getByCategoryFlow(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): Subscription? {
        return subscriptionDao.getById(id)?.toDomain()
    }

    override suspend fun getUpcomingRenewals(withinDays: Int): List<Subscription> {
        val now = LocalDate.now()
        val future = now.plusDays(withinDays.toLong())
        val fromMillis = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMillis = future.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return subscriptionDao.getUpcomingRenewals(fromMillis, toMillis).map { it.toDomain() }
    }

    override suspend fun insert(subscription: Subscription): Long {
        return subscriptionDao.insert(subscription.toEntity())
    }

    override suspend fun update(subscription: Subscription) {
        subscriptionDao.update(subscription.toEntity())
    }

    override suspend fun delete(subscription: Subscription) {
        subscriptionDao.delete(subscription.toEntity())
    }
}
