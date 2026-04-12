package com.spendlist.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("next_renewal_date"),
        Index("status")
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    val amount: String,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    @ColumnInfo(name = "billing_cycle_type")
    val billingCycleType: String,
    @ColumnInfo(name = "billing_cycle_days")
    val billingCycleDays: Int?,
    @ColumnInfo(name = "billing_day_of_month")
    val billingDayOfMonth: Int? = null,
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    @ColumnInfo(name = "next_renewal_date")
    val nextRenewalDate: Long,
    val note: String?,
    @ColumnInfo(name = "manage_url")
    val manageUrl: String?,
    @ColumnInfo(name = "icon_uri")
    val iconUri: String?,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
