package com.spendlist.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class RenewalHistory(
    val id: Long = 0,
    val subscriptionId: Long,
    val previousRenewalDate: LocalDate,
    val newRenewalDate: LocalDate,
    val amount: BigDecimal? = null,
    val note: String? = null,
    val renewedAt: Long = System.currentTimeMillis()
)