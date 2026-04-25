package com.spendlist.app.data.repository

import com.spendlist.app.data.local.dao.RenewalHistoryDao
import com.spendlist.app.data.local.entity.RenewalHistoryEntity
import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RenewalHistoryRepositoryImpl @Inject constructor(
    private val dao: RenewalHistoryDao
) : RenewalHistoryRepository {
    override fun getBySubscriptionId(subscriptionId: Long): Flow<List<RenewalHistory>> {
        return dao.getBySubscriptionId(subscriptionId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getCountBySubscriptionId(subscriptionId: Long): Int {
        return dao.getCountBySubscriptionId(subscriptionId)
    }

    override suspend fun getLatestNewRenewalDate(subscriptionId: Long): LocalDate? {
        val millis = dao.getMaxNewRenewalDateMillis(subscriptionId) ?: return null
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    override suspend fun insert(history: RenewalHistory): Long {
        return dao.insert(RenewalHistoryEntity.fromDomain(history))
    }

    override suspend fun deleteBySubscriptionId(subscriptionId: Long) {
        dao.deleteBySubscriptionId(subscriptionId)
    }
}