package com.spendlist.app.domain.usecase.currency

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.CurrencyRate
import com.spendlist.app.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ConvertCurrencyUseCaseTest {

    private lateinit var repository: CurrencyRepository
    private lateinit var useCase: ConvertCurrencyUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ConvertCurrencyUseCase(repository)
    }

    @Test
    fun convert_sameCurrency_returnsOriginalAmount() = runTest {
        val result = useCase(BigDecimal("100"), Currency.CNY, Currency.CNY)

        assertThat(result).isInstanceOf(ConvertCurrencyUseCase.Result.Success::class.java)
        assertThat((result as ConvertCurrencyUseCase.Result.Success).amount)
            .isEqualTo(BigDecimal("100"))
    }

    @Test
    fun convert_usdToCny_calculatesCorrectly() = runTest {
        coEvery { repository.getRate("USD", "CNY") } returns
            CurrencyRate(baseCode = "USD", targetCode = "CNY", rate = BigDecimal("7.25"))

        val result = useCase(BigDecimal("20"), Currency.USD, Currency.CNY)

        assertThat(result).isInstanceOf(ConvertCurrencyUseCase.Result.Success::class.java)
        val amount = (result as ConvertCurrencyUseCase.Result.Success).amount
        assertThat(amount).isEqualTo(BigDecimal("145.00"))
    }

    @Test
    fun convert_bigDecimalPrecision_noFloatingPointError() = runTest {
        coEvery { repository.getRate("USD", "CNY") } returns
            CurrencyRate(baseCode = "USD", targetCode = "CNY", rate = BigDecimal("7.2456"))

        val result = useCase(BigDecimal("19.99"), Currency.USD, Currency.CNY)

        assertThat(result).isInstanceOf(ConvertCurrencyUseCase.Result.Success::class.java)
        val amount = (result as ConvertCurrencyUseCase.Result.Success).amount
        // 19.99 * 7.2456 = 144.8397 -> rounded to 2 decimals = 144.84
        assertThat(amount).isEqualTo(BigDecimal("144.84"))
    }

    @Test
    fun convert_noRateAvailable_returnsError() = runTest {
        coEvery { repository.getRate("USD", "KRW") } returns null

        val result = useCase(BigDecimal("20"), Currency.USD, Currency.KRW)

        assertThat(result).isInstanceOf(ConvertCurrencyUseCase.Result.NoRateAvailable::class.java)
    }

    @Test
    fun convert_manualOverrideRate_isUsed() = runTest {
        coEvery { repository.getRate("USD", "CNY") } returns
            CurrencyRate(
                baseCode = "USD", targetCode = "CNY",
                rate = BigDecimal("7.50"), isManualOverride = true
            )

        val result = useCase(BigDecimal("10"), Currency.USD, Currency.CNY)

        assertThat(result).isInstanceOf(ConvertCurrencyUseCase.Result.Success::class.java)
        assertThat((result as ConvertCurrencyUseCase.Result.Success).amount)
            .isEqualTo(BigDecimal("75.00"))
    }
}
