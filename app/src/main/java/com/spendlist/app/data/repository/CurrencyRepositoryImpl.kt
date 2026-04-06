package com.spendlist.app.data.repository

import com.spendlist.app.data.local.dao.CurrencyRateDao
import com.spendlist.app.data.local.entity.CurrencyRateEntity
import com.spendlist.app.data.remote.ExchangeRateApi
import com.spendlist.app.domain.model.CurrencyRate
import com.spendlist.app.domain.repository.CurrencyRepository
import java.math.BigDecimal
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val dao: CurrencyRateDao,
    private val api: ExchangeRateApi
) : CurrencyRepository {

    override suspend fun getRate(baseCode: String, targetCode: String): CurrencyRate? {
        val entity = dao.getRate(baseCode, targetCode) ?: return null
        return CurrencyRate(
            baseCode = entity.baseCode,
            targetCode = entity.targetCode,
            rate = BigDecimal(entity.rate),
            isManualOverride = entity.isManualOverride,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun fetchAndCacheRates(baseCode: String) {
        val response = api.getLatestRates(baseCode)
        val entities = response.rates.map { (targetCode, rate) ->
            CurrencyRateEntity(
                baseCode = baseCode,
                targetCode = targetCode,
                rate = rate.toBigDecimal().toPlainString(),
                isManualOverride = false
            )
        }
        dao.insertAll(entities)
    }

    override suspend fun setManualRate(baseCode: String, targetCode: String, rate: BigDecimal) {
        dao.insert(
            CurrencyRateEntity(
                baseCode = baseCode,
                targetCode = targetCode,
                rate = rate.toPlainString(),
                isManualOverride = true
            )
        )
    }
}
