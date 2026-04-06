package com.spendlist.app.domain.usecase.stats

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetMonthlyTrendUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var convertCurrency: ConvertCurrencyUseCase
    private lateinit var useCase: GetMonthlyTrendUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk()
        convertCurrency = mockk()
        useCase = GetMonthlyTrendUseCase(subscriptionRepository, convertCurrency)
    }

    private fun createSub(
        amount: BigDecimal = BigDecimal("100"),
        currency: Currency = Currency.CNY,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        cycle: BillingCycle = BillingCycle.Monthly,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = 0, name = "Test", amount = amount, currency = currency,
        billingCycle = cycle, startDate = startDate,
        nextRenewalDate = startDate.plusMonths(1), status = status
    )

    @Test
    fun returns12Months() = runTest {
        every { subscriptionRepository.getAll() } returns flowOf(listOf(createSub()))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        assertThat(result).hasSize(12)
    }

    @Test
    fun noSubscriptions_allMonthsZero() = runTest {
        every { subscriptionRepository.getAll() } returns flowOf(emptyList())

        val result = useCase(Currency.CNY).first()

        assertThat(result).hasSize(12)
        result.forEach { assertThat(it.amount).isEqualTo(BigDecimal.ZERO) }
    }

    @Test
    fun monthlySubscription_appearsInActiveMonths() = runTest {
        val sub = createSub(
            amount = BigDecimal("100"),
            startDate = LocalDate.now().minusMonths(3)
        )
        every { subscriptionRepository.getAll() } returns flowOf(listOf(sub))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        // At least the recent months should have non-zero amounts
        val nonZero = result.count { it.amount > BigDecimal.ZERO }
        assertThat(nonZero).isGreaterThan(0)
    }

    @Test
    fun cancelledSubscription_excluded() = runTest {
        val sub = createSub(status = SubscriptionStatus.CANCELLED)
        every { subscriptionRepository.getAll() } returns flowOf(listOf(sub))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        result.forEach { assertThat(it.amount).isEqualTo(BigDecimal.ZERO) }
    }
}
