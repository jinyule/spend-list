package com.spendlist.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.data.local.dao.CurrencyRateDao
import com.spendlist.app.data.local.entity.CurrencyRateEntity
import com.spendlist.app.data.remote.ExchangeRateApi
import com.spendlist.app.data.remote.ExchangeRateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CurrencyRepositoryImplTest {

    private lateinit var dao: CurrencyRateDao
    private lateinit var api: ExchangeRateApi
    private lateinit var repository: CurrencyRepositoryImpl

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        api = mockk()
        repository = CurrencyRepositoryImpl(dao, api)
    }

    @Test
    fun getRate_fromLocalCache_returnsRate() = runTest {
        coEvery { dao.getRate("USD", "CNY") } returns
            CurrencyRateEntity(
                baseCode = "USD", targetCode = "CNY",
                rate = "7.25", isManualOverride = false
            )

        val rate = repository.getRate("USD", "CNY")

        assertThat(rate).isNotNull()
        assertThat(rate!!.rate).isEqualTo(BigDecimal("7.25"))
    }

    @Test
    fun getRate_noLocalCache_returnsNull() = runTest {
        coEvery { dao.getRate("USD", "KRW") } returns null

        val rate = repository.getRate("USD", "KRW")

        assertThat(rate).isNull()
    }

    @Test
    fun fetchAndCacheRates_fromApi_savesToLocal() = runTest {
        val response = ExchangeRateResponse(
            result = "success",
            baseCode = "USD",
            rates = mapOf("CNY" to 7.25, "EUR" to 0.92, "GBP" to 0.79)
        )
        coEvery { api.getLatestRates("USD") } returns response

        repository.fetchAndCacheRates("USD")

        val slot = slot<List<CurrencyRateEntity>>()
        coVerify { dao.insertAll(capture(slot)) }
        assertThat(slot.captured).hasSize(3)
        assertThat(slot.captured.find { it.targetCode == "CNY" }?.rate).isEqualTo("7.25")
    }

    @Test
    fun fetchAndCacheRates_apiFails_throwsException() = runTest {
        coEvery { api.getLatestRates("USD") } throws RuntimeException("Network error")

        var threw = false
        try {
            repository.fetchAndCacheRates("USD")
        } catch (e: RuntimeException) {
            threw = true
        }
        assertThat(threw).isTrue()
    }

    @Test
    fun setManualRate_setsOverrideFlag() = runTest {
        val slot = slot<CurrencyRateEntity>()
        coEvery { dao.insert(capture(slot)) } returns Unit

        repository.setManualRate("USD", "CNY", BigDecimal("7.50"))

        assertThat(slot.captured.isManualOverride).isTrue()
        assertThat(slot.captured.rate).isEqualTo("7.50")
    }

    @Test
    fun getRate_manualOverrideExists_returnsOverride() = runTest {
        coEvery { dao.getRate("USD", "CNY") } returns
            CurrencyRateEntity(
                baseCode = "USD", targetCode = "CNY",
                rate = "7.50", isManualOverride = true
            )

        val rate = repository.getRate("USD", "CNY")

        assertThat(rate).isNotNull()
        assertThat(rate!!.isManualOverride).isTrue()
        assertThat(rate.rate).isEqualTo(BigDecimal("7.50"))
    }
}
