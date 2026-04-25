package com.spendlist.app.domain.usecase.renewal

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReconcileNextRenewalDatesUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var renewalHistoryRepository: RenewalHistoryRepository
    private lateinit var useCase: ReconcileNextRenewalDatesUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk(relaxed = true)
        renewalHistoryRepository = mockk()
        useCase = ReconcileNextRenewalDatesUseCase(subscriptionRepository, renewalHistoryRepository)
    }

    private fun sub(
        id: Long,
        nextRenewal: LocalDate,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = id, name = "test-$id", amount = BigDecimal("10"),
        currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
        startDate = LocalDate.of(2026, 1, 1),
        nextRenewalDate = nextRenewal,
        status = status
    )

    @Test
    fun reconcile_fixesStaleDateWhenHistoryAdvanced() = runTest {
        val stale = LocalDate.of(2026, 4, 21)
        val advanced = LocalDate.of(2026, 5, 21)
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(
            sub(4, stale, SubscriptionStatus.EXPIRED)
        )
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(4) } returns advanced

        val fixedCount = useCase()

        assertThat(fixedCount).isEqualTo(1)
        coVerify {
            subscriptionRepository.update(match {
                it.id == 4L && it.nextRenewalDate == advanced
            })
        }
    }

    @Test
    fun reconcile_recoversExpiredStatusWhenAdvancedDateInFuture() = runTest {
        val stale = LocalDate.now().minusDays(3)
        val advanced = LocalDate.now().plusDays(15)
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(
            sub(1, stale, SubscriptionStatus.EXPIRED)
        )
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1) } returns advanced

        useCase()

        coVerify {
            subscriptionRepository.update(match { it.status == SubscriptionStatus.ACTIVE })
        }
    }

    @Test
    fun reconcile_keepsExpiredWhenAdvancedDateStillPast() = runTest {
        val stale = LocalDate.now().minusDays(30)
        val advanced = LocalDate.now().minusDays(5) // later than stale but still past
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(
            sub(1, stale, SubscriptionStatus.EXPIRED)
        )
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1) } returns advanced

        useCase()

        coVerify {
            subscriptionRepository.update(match { it.status == SubscriptionStatus.EXPIRED })
        }
    }

    @Test
    fun reconcile_noopWhenHistoryNotAhead() = runTest {
        val current = LocalDate.of(2026, 6, 1)
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub(1, current))
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1) } returns LocalDate.of(2026, 5, 1)

        val fixedCount = useCase()

        assertThat(fixedCount).isEqualTo(0)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun reconcile_noopWhenNoHistory() = runTest {
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub(1, LocalDate.of(2026, 5, 1)))
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1) } returns null

        val fixedCount = useCase()

        assertThat(fixedCount).isEqualTo(0)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun reconcile_handlesMultipleSubscriptionsIndependently() = runTest {
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(
            sub(1, LocalDate.of(2026, 4, 20), SubscriptionStatus.EXPIRED), // needs fix
            sub(2, LocalDate.of(2026, 6, 1))                                // no history
        )
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1) } returns LocalDate.of(2026, 5, 20)
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(2) } returns null

        val fixedCount = useCase()

        assertThat(fixedCount).isEqualTo(1)
        coVerify { subscriptionRepository.update(match { it.id == 1L }) }
        coVerify(exactly = 0) { subscriptionRepository.update(match { it.id == 2L }) }
    }
}
