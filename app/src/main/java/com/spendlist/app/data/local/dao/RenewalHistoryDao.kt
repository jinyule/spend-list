package com.spendlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendlist.app.data.local.entity.RenewalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RenewalHistoryDao {
    @Query("SELECT * FROM renewal_history WHERE subscription_id = :subscriptionId ORDER BY renewed_at DESC")
    fun getBySubscriptionId(subscriptionId: Long): Flow<List<RenewalHistoryEntity>>

    @Query("SELECT COUNT(*) FROM renewal_history WHERE subscription_id = :subscriptionId")
    suspend fun getCountBySubscriptionId(subscriptionId: Long): Int

    @Query("SELECT MAX(new_renewal_date) FROM renewal_history WHERE subscription_id = :subscriptionId")
    suspend fun getMaxNewRenewalDateMillis(subscriptionId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: RenewalHistoryEntity): Long

    @Query("DELETE FROM renewal_history WHERE subscription_id = :subscriptionId")
    suspend fun deleteBySubscriptionId(subscriptionId: Long)
}