package com.spendlist.app.domain.usecase.subscription

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetTotalSpentUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var convertCurrency: ConvertCurrencyUseCase
    private lateinit var useCase: GetTotalSpentUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk()
        convertCurrency = mockk()
        useCase = GetTotalSpentUseCase(subscriptionRepository, convertCurrency)
        // Default: same-currency passthrough
        coEvery { convertCurrency(any(), any(), any()) } answers {
            ConvertCurrencyUseCase.Result.Success(firstArg())
        }
    }

    private fun sub(
        id: Long = 1,
        amount: BigDecimal = BigDecimal("10"),
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        nextRenewalDate: LocalDate = LocalDate.of(2024, 4, 1), // 3 months paid
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        billingCycle: BillingCycle = BillingCycle.Monthly
    ) = Subscription(
        id = id,
        name = "Sub $id",
        amount = amount,
        currency = Currency.USD,
        billingCycle = billingCycle,
        startDate = startDate,
        nextRenewalDate = nextRenewalDate,
        status = status
    )

    @Test
    fun activeSubscription_countsPaidCycles() = runTest {
        // 3 monthly cycles paid × 10 = 30
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub())

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("30.00"))
    }

    @Test
    fun expiredSubscription_stillCountsPaidHistory() = runTest {
        // EXPIRED means user stopped paying — but the 3 cycles before nextRenewalDate
        // were actually paid. They must remain in the cumulative total.
        val expired = sub(status = SubscriptionStatus.EXPIRED)
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(expired)

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("30.00"))
    }

    @Test
    fun cancelledSubscription_stillCountsPaidHistory() = runTest {
        // CANCELLED means user manually cancelled — already-paid cycles stay.
        val cancelled = sub(status = SubscriptionStatus.CANCELLED)
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(cancelled)

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("30.00"))
    }

    @Test
    fun mixedStatuses_allCounted() = runTest {
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(
            sub(id = 1, amount = BigDecimal("10"), status = SubscriptionStatus.ACTIVE),   // 30
            sub(id = 2, amount = BigDecimal("20"), status = SubscriptionStatus.EXPIRED),  // 60
            sub(id = 3, amount = BigDecimal("5"), status = SubscriptionStatus.CANCELLED)  // 15
        )

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("105.00"))
    }

    @Test
    fun renewAfterExpiry_totalGrowsByOneCycle() = runTest {
        // Simulate: EXPIRED with 3 cycles paid (total = 30); then user renews one more.
        val beforeRenewal = sub(
            status = SubscriptionStatus.EXPIRED,
            nextRenewalDate = LocalDate.of(2024, 4, 1) // 3 cycles
        )
        val afterRenewal = sub(
            status = SubscriptionStatus.EXPIRED, // still EXPIRED if new date still past
            nextRenewalDate = LocalDate.of(2024, 5, 1) // 4 cycles
        )

        coEvery { subscriptionRepository.getAllOnce() } returns listOf(beforeRenewal)
        val before = useCase(Currency.USD)

        coEvery { subscriptionRepository.getAllOnce() } returns listOf(afterRenewal)
        val after = useCase(Currency.USD)

        assertThat(before).isEqualTo(BigDecimal("30.00"))
        assertThat(after).isEqualTo(BigDecimal("40.00"))
    }

    @Test
    fun noPaidCycles_excludedFromTotal() = runTest {
        // Subscription just created today with nextRenewalDate = today (0 paid cycles)
        val brandNew = sub(
            startDate = LocalDate.now(),
            nextRenewalDate = LocalDate.now()
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(brandNew)

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun emptyList_returnsZero() = runTest {
        coEvery { subscriptionRepository.getAllOnce() } returns emptyList()

        val total = useCase(Currency.USD)

        assertThat(total).isEqualTo(BigDecimal("0.00"))
    }
}
