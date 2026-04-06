package com.spendlist.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "currency_rates",
    primaryKeys = ["base_code", "target_code"]
)
data class CurrencyRateEntity(
    @ColumnInfo(name = "base_code")
    val baseCode: String,
    @ColumnInfo(name = "target_code")
    val targetCode: String,
    val rate: String,
    @ColumnInfo(name = "is_manual_override")
    val isManualOverride: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
