package com.spendlist.app.domain.usecase.export

import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class SubscriptionExportDto(
    val name: String,
    val categoryId: Long? = null,
    val amount: String,
    val currency: String,
    val billingCycleType: String,
    val billingCycleDays: Int? = null,
    val billingDayOfMonth: Int? = null,
    val startDate: String,
    val nextRenewalDate: String,
    val note: String? = null,
    val manageUrl: String? = null,
    val status: String
)

class ExportDataUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportJson(): String {
        val subs = repository.getAll().first()
        val dtos = subs.map { it.toExportDto() }
        return json.encodeToString(dtos)
    }

    suspend fun exportCsv(): String {
        val subs = repository.getAll().first()
        val header = "name,amount,currency,billingCycleType,billingCycleDays,billingDayOfMonth,startDate,nextRenewalDate,status,categoryId,note,manageUrl"
        val rows = subs.map { sub ->
            val dto = sub.toExportDto()
            listOf(
                dto.name.csvEscape(),
                dto.amount,
                dto.currency,
                dto.billingCycleType,
                dto.billingCycleDays?.toString() ?: "",
                dto.billingDayOfMonth?.toString() ?: "",
                dto.startDate,
                dto.nextRenewalDate,
                dto.status,
                dto.categoryId?.toString() ?: "",
                dto.note?.csvEscape() ?: "",
                dto.manageUrl ?: ""
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun Subscription.toExportDto() = SubscriptionExportDto(
        name = name,
        categoryId = categoryId,
        amount = amount.toPlainString(),
        currency = currency.code,
        billingCycleType = when (billingCycle) {
            is BillingCycle.Monthly -> "MONTHLY"
            is BillingCycle.Quarterly -> "QUARTERLY"
            is BillingCycle.Yearly -> "YEARLY"
            is BillingCycle.Custom -> "CUSTOM"
        },
        billingCycleDays = (billingCycle as? BillingCycle.Custom)?.days,
        billingDayOfMonth = billingDayOfMonth,
        startDate = startDate.toString(),
        nextRenewalDate = nextRenewalDate.toString(),
        note = note,
        manageUrl = manageUrl,
        status = status.name
    )

    private fun String.csvEscape(): String {
        return if (contains(",") || contains("\"") || contains("\n")) {
            "\"${replace("\"", "\"\"")}\""
        } else this
    }
}
