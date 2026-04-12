package com.spendlist.app.data.local.converter

import com.spendlist.app.data.local.entity.CategoryEntity
import com.spendlist.app.data.local.entity.SubscriptionEntity
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun SubscriptionEntity.toDomain(): Subscription {
    return Subscription(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = BigDecimal(amount),
        currency = Currency.fromCode(currencyCode) ?: Currency.CNY,
        billingCycle = when (billingCycleType) {
            "MONTHLY" -> BillingCycle.Monthly
            "QUARTERLY" -> BillingCycle.Quarterly
            "YEARLY" -> BillingCycle.Yearly
            "CUSTOM" -> BillingCycle.Custom((billingCycleDays ?: 30).coerceAtLeast(1))
            else -> BillingCycle.Monthly
        },
        billingDayOfMonth = billingDayOfMonth,
        startDate = millisToLocalDate(startDate),
        nextRenewalDate = millisToLocalDate(nextRenewalDate),
        note = note,
        manageUrl = manageUrl,
        iconUri = iconUri,
        status = try {
            SubscriptionStatus.valueOf(status)
        } catch (_: Exception) {
            SubscriptionStatus.ACTIVE
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Subscription.toEntity(): SubscriptionEntity {
    return SubscriptionEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = amount.toPlainString(),
        currencyCode = currency.code,
        billingCycleType = when (billingCycle) {
            is BillingCycle.Monthly -> "MONTHLY"
            is BillingCycle.Quarterly -> "QUARTERLY"
            is BillingCycle.Yearly -> "YEARLY"
            is BillingCycle.Custom -> "CUSTOM"
        },
        billingCycleDays = when (billingCycle) {
            is BillingCycle.Custom -> billingCycle.days
            else -> null
        },
        billingDayOfMonth = billingDayOfMonth,
        startDate = localDateToMillis(startDate),
        nextRenewalDate = localDateToMillis(nextRenewalDate),
        note = note,
        manageUrl = manageUrl,
        iconUri = iconUri,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        nameResKey = nameResKey,
        iconName = iconName,
        color = color,
        isPreset = isPreset,
        sortOrder = sortOrder
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        nameResKey = nameResKey,
        iconName = iconName,
        color = color,
        isPreset = isPreset,
        sortOrder = sortOrder
    )
}

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun localDateToMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
