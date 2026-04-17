package com.spendlist.app.domain.usecase.renewal

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

class MarkExpiredSubscriptionsUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var useCase: MarkExpiredSubscriptionsUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk(relaxed = true)
        useCase = MarkExpiredSubscriptionsUseCase(subscriptionRepository)
    }

    private fun sub(
        id: Long = 1,
        nextRenewalDate: LocalDate = LocalDate.now().minusDays(1),
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = id,
        name = "Test $id",
        amount = BigDecimal("10"),
        currency = Currency.USD,
        billingCycle = BillingCycle.Monthly,
        startDate = LocalDate.of(2024, 1, 1),
        nextRenewalDate = nextRenewalDate,
        status = status
    )

    @Test
    fun activeSubscription_pastDue_isMarkedExpired() = runTest {
        val s = sub(nextRenewalDate = LocalDate.now().minusDays(1))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(SubscriptionStatus.EXPIRED)
        coVerify { subscriptionRepository.update(match { it.status == SubscriptionStatus.EXPIRED }) }
    }

    @Test
    fun activeSubscription_today_isNotMarkedExpired() = runTest {
        val s = sub(nextRenewalDate = LocalDate.now())
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun activeSubscription_future_isNotMarkedExpired() = runTest {
        val s = sub(nextRenewalDate = LocalDate.now().plusDays(5))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun alreadyExpired_isNotTouched() = runTest {
        val s = sub(
            nextRenewalDate = LocalDate.now().minusDays(10),
            status = SubscriptionStatus.EXPIRED
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun cancelled_isNotTouched() = runTest {
        val s = sub(
            nextRenewalDate = LocalDate.now().minusDays(1),
            status = SubscriptionStatus.CANCELLED
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).isEmpty()
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun mixedSubscriptions_onlyNewlyExpiredReturned() = runTest {
        val expiringSoon = sub(id = 1, nextRenewalDate = LocalDate.now().minusDays(1))
        val alreadyExpired = sub(
            id = 2,
            nextRenewalDate = LocalDate.now().minusDays(30),
            status = SubscriptionStatus.EXPIRED
        )
        val active = sub(id = 3, nextRenewalDate = LocalDate.now().plusDays(5))
        val cancelled = sub(
            id = 4,
            nextRenewalDate = LocalDate.now().minusDays(1),
            status = SubscriptionStatus.CANCELLED
        )
        coEvery { subscriptionRepository.getAllOnce() } returns
            listOf(expiringSoon, alreadyExpired, active, cancelled)

        val result = useCase()

        assertThat(result.map { it.id }).containsExactly(1L)
        coVerify(exactly = 1) { subscriptionRepository.update(any()) }
    }

    @Test
    fun doesNotWriteRenewalHistory() = runTest {
        // Sanity: useCase should not depend on RenewalHistoryRepository at all.
        // This is enforced structurally - constructor only takes SubscriptionRepository.
        val s = sub(nextRenewalDate = LocalDate.now().minusDays(1))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(s)

        val result = useCase()

        assertThat(result).hasSize(1)
    }
}
