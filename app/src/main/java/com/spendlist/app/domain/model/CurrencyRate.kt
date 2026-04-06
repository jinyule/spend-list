package com.spendlist.app.domain.model

import java.math.BigDecimal

data class CurrencyRate(
    val baseCode: String,
    val targetCode: String,
    val rate: BigDecimal,
    val isManualOverride: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
