package com.spendlist.app.domain.usecase.export

import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    sealed class Result {
        data class Success(val inserted: Int, val failed: Int = 0) : Result()
        data class Error(val message: String) : Result()
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importJson(jsonString: String): Result {
        val dtos = try {
            json.decodeFromString<List<SubscriptionExportDto>>(jsonString)
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Invalid JSON format")
        }
        var inserted = 0
        var failed = 0
        for (dto in dtos) {
            try {
                repository.insert(dto.toDomain())
                inserted++
            } catch (e: Exception) {
                failed++
            }
        }
        return Result.Success(inserted, failed)
    }

    suspend fun importCsv(csvString: String): Result {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Result.Error("Empty file")
        if (lines.size == 1) return Result.Success(0) // header only

        val header = try {
            lines[0].split(",").map { it.trim() }
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Invalid CSV format")
        }
        val nameIdx = header.indexOf("name")
        val amountIdx = header.indexOf("amount")
        val currencyIdx = header.indexOf("currency")
        val cycleTypeIdx = header.indexOf("billingCycleType")
        val billingDayIdx = header.indexOf("billingDayOfMonth")
        val startDateIdx = header.indexOf("startDate")
        val nextRenewalIdx = header.indexOf("nextRenewalDate")
        val statusIdx = header.indexOf("status")
        val noteIdx = header.indexOf("note")
        val manageUrlIdx = header.indexOf("manageUrl")
        val categoryIdIdx = header.indexOf("categoryId")

        if (nameIdx == -1 || amountIdx == -1) {
            return Result.Error("Missing required columns: name, amount")
        }

        var inserted = 0
        var failed = 0
        for (i in 1 until lines.size) {
            try {
                val cols = parseCsvLine(lines[i])
                val dto = SubscriptionExportDto(
                    name = cols.getOrElse(nameIdx) { "" },
                    amount = cols.getOrElse(amountIdx) { "0" },
                    currency = cols.getOrElse(currencyIdx) { "CNY" },
                    billingCycleType = cols.getOrElse(cycleTypeIdx) { "MONTHLY" },
                    billingDayOfMonth = billingDayIdx.takeIf { it >= 0 }?.let { cols.getOrElse(it) { "" }.toIntOrNull() },
                    startDate = cols.getOrElse(startDateIdx) { LocalDate.now().toString() },
                    nextRenewalDate = cols.getOrElse(nextRenewalIdx) { LocalDate.now().plusMonths(1).toString() },
                    status = cols.getOrElse(statusIdx) { "ACTIVE" },
                    categoryId = categoryIdIdx.takeIf { it >= 0 }?.let { cols.getOrElse(it) { "" }.toLongOrNull() },
                    note = noteIdx.takeIf { it >= 0 }?.let { cols.getOrElse(it) { "" }.ifBlank { null } },
                    manageUrl = manageUrlIdx.takeIf { it >= 0 }?.let { cols.getOrElse(it) { "" }.ifBlank { null } }
                )
                repository.insert(dto.toDomain())
                inserted++
            } catch (e: Exception) {
                failed++
            }
        }
        return Result.Success(inserted, failed)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private fun SubscriptionExportDto.toDomain() = Subscription(
        id = 0, // new entry, auto-generate ID
        name = name,
        categoryId = categoryId,
        amount = BigDecimal(amount),
        currency = Currency.fromCode(currency) ?: Currency.CNY,
        billingCycle = when (billingCycleType) {
            "QUARTERLY" -> BillingCycle.Quarterly
            "YEARLY" -> BillingCycle.Yearly
            "CUSTOM" -> BillingCycle.Custom(billingCycleDays ?: 30)
            else -> BillingCycle.Monthly
        },
        billingDayOfMonth = billingDayOfMonth,
        startDate = LocalDate.parse(startDate),
        nextRenewalDate = LocalDate.parse(nextRenewalDate),
        note = note,
        manageUrl = manageUrl,
        status = try {
            SubscriptionStatus.valueOf(status)
        } catch (e: Exception) {
            SubscriptionStatus.ACTIVE
        }
    )
}
