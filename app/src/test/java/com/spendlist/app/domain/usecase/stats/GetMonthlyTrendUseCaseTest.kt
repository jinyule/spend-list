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
import java.time.YearMonth

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
    fun activeMonthlySubscription_paidMonthsHaveAmount() = runTest {
        // Started 3 months ago, next renewal in 1 month from now
        val startDate = LocalDate.now().minusMonths(3)
        val sub = Subscription(
            id = 0, name = "Test", amount = BigDecimal("100"), currency = Currency.CNY,
            billingCycle = BillingCycle.Monthly,
            startDate = startDate,
            nextRenewalDate = LocalDate.now().plusMonths(1),
            status = SubscriptionStatus.ACTIVE
        )
        every { subscriptionRepository.getAll() } returns flowOf(listOf(sub))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        // Months from 3 months ago up to this month should be non-zero (paid)
        val nonZeroCount = result.count { it.amount.signum() > 0 }
        assertThat(nonZeroCount).isAtLeast(3)
    }

    @Test
    fun cancelledSubscription_paidMonthsStillIncluded() = runTest {
        // Cancelled after paying 1 cycle (3 months ago → 2 months ago)
        val threeMonthsAgo = LocalDate.now().minusMonths(3)
        val twoMonthsAgo = LocalDate.now().minusMonths(2)
        val sub = Subscription(
            id = 0, name = "Cancelled", amount = BigDecimal("100"), currency = Currency.CNY,
            billingCycle = BillingCycle.Monthly,
            startDate = threeMonthsAgo,
            nextRenewalDate = twoMonthsAgo,
            status = SubscriptionStatus.CANCELLED
        )
        every { subscriptionRepository.getAll() } returns flowOf(listOf(sub))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        // The paid month (3 months ago's YearMonth) should be 100
        val paidYm = YearMonth.from(threeMonthsAgo)
        val paidEntry = result.find { it.yearMonth == paidYm }
        assertThat(paidEntry).isNotNull()
        assertThat(paidEntry!!.amount.compareTo(BigDecimal("100"))).isEqualTo(0)
    }

    @Test
    fun expiredSubscription_paidMonthsIncluded_unpaidMonthsZero() = runTest {
        val twoMonthsAgo = LocalDate.now().minusMonths(2)
        val oneMonthAgo = LocalDate.now().minusMonths(1)
        val sub = Subscription(
            id = 0, name = "Expired", amount = BigDecimal("50"), currency = Currency.CNY,
            billingCycle = BillingCycle.Monthly,
            startDate = twoMonthsAgo,
            nextRenewalDate = oneMonthAgo, // 1 cycle paid, then stopped
            status = SubscriptionStatus.EXPIRED
        )
        every { subscriptionRepository.getAll() } returns flowOf(listOf(sub))
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }

        val result = useCase(Currency.CNY).first()

        // Paid month
        val paidYm = YearMonth.from(twoMonthsAgo)
        assertThat(result.find { it.yearMonth == paidYm }!!.amount.compareTo(BigDecimal("50"))).isEqualTo(0)

        // Month after nextRenewalDate = not paid → 0
        val unpaidYm = YearMonth.from(oneMonthAgo)
        // Only true when oneMonthAgo's YearMonth differs from twoMonthsAgo's YearMonth
        if (unpaidYm != paidYm) {
            val unpaidEntry = result.find { it.yearMonth == unpaidYm }
            if (unpaidEntry != null) {
                assertThat(unpaidEntry.amount.signum()).isEqualTo(0)
            }
        }
    }
}
