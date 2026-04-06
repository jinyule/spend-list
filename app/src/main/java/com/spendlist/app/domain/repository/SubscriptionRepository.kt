package com.spendlist.app.domain.repository

import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getAll(): Flow<List<Subscription>>
    suspend fun getAllOnce(): List<Subscription>
    fun getByStatus(status: SubscriptionStatus): Flow<List<Subscription>>
    fun getByCategory(categoryId: Long): Flow<List<Subscription>>
    suspend fun getById(id: Long): Subscription?
    suspend fun getUpcomingRenewals(withinDays: Int): List<Subscription>
    suspend fun insert(subscription: Subscription): Long
    suspend fun update(subscription: Subscription)
    suspend fun delete(subscription: Subscription)
}
