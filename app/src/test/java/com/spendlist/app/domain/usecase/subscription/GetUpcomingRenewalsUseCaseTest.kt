package com.spendlist.app.domain.usecase.subscription

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetUpcomingRenewalsUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: GetUpcomingRenewalsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetUpcomingRenewalsUseCase(repository)
    }

    @Test
    fun invoke_defaultsToThreeDays() = runTest {
        coEvery { repository.getUpcomingRenewals(3) } returns emptyList()

        useCase()

        coVerify { repository.getUpcomingRenewals(3) }
    }

    @Test
    fun invoke_returnsUpcomingRenewals() = runTest {
        val upcoming = listOf(
            Subscription(
                id = 1, name = "Claude Pro", amount = BigDecimal("150"),
                currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
                startDate = LocalDate.now().minusMonths(1),
                nextRenewalDate = LocalDate.now().plusDays(2),
                status = SubscriptionStatus.ACTIVE
            )
        )
        coEvery { repository.getUpcomingRenewals(3) } returns upcoming

        val result = useCase()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Claude Pro")
    }

    @Test
    fun invoke_withCustomDays_passesCorrectValue() = runTest {
        coEvery { repository.getUpcomingRenewals(7) } returns emptyList()

        useCase(withinDays = 7)

        coVerify { repository.getUpcomingRenewals(7) }
    }

    @Test
    fun invoke_withNoUpcoming_returnsEmptyList() = runTest {
        coEvery { repository.getUpcomingRenewals(3) } returns emptyList()

        val result = useCase()

        assertThat(result).isEmpty()
    }
}
