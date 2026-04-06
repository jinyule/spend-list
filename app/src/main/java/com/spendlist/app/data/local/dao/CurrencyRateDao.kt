package com.spendlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendlist.app.data.local.entity.CurrencyRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyRateDao {

    @Query("SELECT * FROM currency_rates")
    fun getAllFlow(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE base_code = :baseCode AND target_code = :targetCode")
    suspend fun getRate(baseCode: String, targetCode: String): CurrencyRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rate: CurrencyRateEntity)
}
