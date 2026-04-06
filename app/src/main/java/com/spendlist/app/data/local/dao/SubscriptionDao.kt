package com.spendlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendlist.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY next_renewal_date ASC")
    fun getAllFlow(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE status = :status ORDER BY next_renewal_date ASC")
    fun getByStatusFlow(status: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE category_id = :categoryId ORDER BY next_renewal_date ASC")
    fun getByCategoryFlow(categoryId: Long): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Query(
        """
        SELECT * FROM subscriptions
        WHERE next_renewal_date BETWEEN :fromMillis AND :toMillis
        AND status = 'ACTIVE'
        ORDER BY next_renewal_date ASC
        """
    )
    suspend fun getUpcomingRenewals(fromMillis: Long, toMillis: Long): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubscriptionEntity): Long

    @Update
    suspend fun update(entity: SubscriptionEntity)

    @Delete
    suspend fun delete(entity: SubscriptionEntity)
}
