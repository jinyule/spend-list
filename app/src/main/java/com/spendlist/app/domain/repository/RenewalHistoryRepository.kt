package com.spendlist.app.domain.repository

import com.spendlist.app.domain.model.RenewalHistory
import kotlinx.coroutines.flow.Flow

interface RenewalHistoryRepository {
    fun getBySubscriptionId(subscriptionId: Long): Flow<List<RenewalHistory>>
    suspend fun getCountBySubscriptionId(subscriptionId: Long): Int
    suspend fun insert(history: RenewalHistory): Long
    suspend fun deleteBySubscriptionId(subscriptionId: Long)
}