package com.spendlist.app.domain.repository

import com.spendlist.app.domain.model.CurrencyRate
import java.math.BigDecimal

interface CurrencyRepository {
    suspend fun getRate(baseCode: String, targetCode: String): CurrencyRate?
    suspend fun fetchAndCacheRates(baseCode: String)
    suspend fun setManualRate(baseCode: String, targetCode: String, rate: BigDecimal)
}
