package com.spendlist.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendlist.app.domain.model.RenewalHistory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(
    tableName = "renewal_history",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscription_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subscription_id")]
)
data class RenewalHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "subscription_id")
    val subscriptionId: Long,
    @ColumnInfo(name = "previous_renewal_date")
    val previousRenewalDate: Long,
    @ColumnInfo(name = "new_renewal_date")
    val newRenewalDate: Long,
    val amount: String?,
    val note: String?,
    @ColumnInfo(name = "renewed_at")
    val renewedAt: Long
) {
    fun toDomain(): RenewalHistory {
        return RenewalHistory(
            id = id,
            subscriptionId = subscriptionId,
            previousRenewalDate = Instant.ofEpochMilli(previousRenewalDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate(),
            newRenewalDate = Instant.ofEpochMilli(newRenewalDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate(),
            amount = amount?.let { java.math.BigDecimal(it) },
            note = note,
            renewedAt = renewedAt
        )
    }

    companion object {
        fun fromDomain(domain: RenewalHistory): RenewalHistoryEntity {
            return RenewalHistoryEntity(
                id = domain.id,
                subscriptionId = domain.subscriptionId,
                previousRenewalDate = domain.previousRenewalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                newRenewalDate = domain.newRenewalDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                amount = domain.amount?.toPlainString(),
                note = domain.note,
                renewedAt = domain.renewedAt
            )
        }
    }
}