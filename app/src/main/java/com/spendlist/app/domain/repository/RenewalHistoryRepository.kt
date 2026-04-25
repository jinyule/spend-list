package com.spendlist.app.domain.repository

import com.spendlist.app.domain.model.RenewalHistory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RenewalHistoryRepository {
    fun getBySubscriptionId(subscriptionId: Long): Flow<List<RenewalHistory>>
    suspend fun getCountBySubscriptionId(subscriptionId: Long): Int
    suspend fun getLatestNewRenewalDate(subscriptionId: Long): LocalDate?
    suspend fun insert(history: RenewalHistory): Long
    suspend fun deleteBySubscriptionId(subscriptionId: Long)
}