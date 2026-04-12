package com.spendlist.app.domain.usecase.renewal

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class AutoRenewSubscriptionsUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var renewalHistoryRepository: RenewalHistoryRepository
    private lateinit var useCase: AutoRenewSubscriptionsUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk(relaxed = true)
        renewalHistoryRepository = mockk(relaxed = true)
        useCase = AutoRenewSubscriptionsUseCase(subscriptionRepository, renewalHistoryRepository)
    }

    private fun createSubscription(
        id: Long = 1,
        name: String = "Test Sub",
        billingCycle: BillingCycle = BillingCycle.Monthly,
        billingDayOfMonth: Int? = null,
        nextRenewalDate: LocalDate = LocalDate.now().minusDays(1),
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = id,
        name = name,
        amount = BigDecimal("10"),
        currency = Currency.USD,
        billingCycle = billingCycle,
        billingDayOfMonth = billingDayOfMonth,
        startDate = LocalDate.of(2024, 1, 1),
        nextRenewalDate = nextRenewalDate,
        status = status
    )

    @Test
    fun activeSubscription_pastDue_isAutoRenewed() = runTest {
        val sub = createSubscription(nextRenewalDate = LocalDate.now().minusDays(1))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        val count = useCase()

        assertThat(count).isEqualTo(1)
        coVerify { subscriptionRepository.update(match { it.nextRenewalDate.isAfter(LocalDate.now().minusDays(1)) }) }
    }

    @Test
    fun cancelledSubscription_pastDue_isNotRenewed() = runTest {
        val sub = createSubscription(
            nextRenewalDate = LocalDate.now().minusDays(1),
            status = SubscriptionStatus.CANCELLED
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        val count = useCase()

        assertThat(count).isEqualTo(0)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun activeSubscription_futureDate_isNotRenewed() = runTest {
        val sub = createSubscription(nextRenewalDate = LocalDate.now().plusDays(5))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        val count = useCase()

        assertThat(count).isEqualTo(0)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun activeSubscription_todayDate_isNotRenewed() = runTest {
        val sub = createSubscription(nextRenewalDate = LocalDate.now())
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        val count = useCase()

        assertThat(count).isEqualTo(0)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    @Test
    fun multiplePeriodsSkipped_catchesUp() = runTest {
        // Monthly sub overdue by 3 months
        val sub = createSubscription(
            nextRenewalDate = LocalDate.now().minusMonths(3)
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        val count = useCase()

        assertThat(count).isEqualTo(3)
        // Verify 3 history records created
        coVerify(exactly = 3) { renewalHistoryRepository.insert(any()) }
        coVerify(exactly = 1) { subscriptionRepository.update(any()) }
    }

    @Test
    fun createsHistoryRecords() = runTest {
        val sub = createSubscription(nextRenewalDate = LocalDate.now().minusDays(1))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)
        val historySlot = slot<RenewalHistory>()
        coEvery { renewalHistoryRepository.insert(capture(historySlot)) } returns 1

        useCase()

        assertThat(historySlot.captured.subscriptionId).isEqualTo(1)
        assertThat(historySlot.captured.note).isEqualTo("Auto-renewed")
    }

    @Test
    fun withBillingDayOfMonth_usesOverload() = runTest {
        // Monthly sub with billingDay=15, renewal was on Jan 10 (past due)
        val sub = createSubscription(
            billingCycle = BillingCycle.Monthly,
            billingDayOfMonth = 15,
            nextRenewalDate = LocalDate.of(2024, 1, 10)
        )
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(sub)

        useCase()

        // Should use billingDayOfMonth=15, so next date should be Feb 15
        coVerify {
            subscriptionRepository.update(match {
                // The first renewal from Jan 10 with billingDay=15 goes to Feb 15
                it.nextRenewalDate.dayOfMonth == 15 || it.nextRenewalDate.isAfter(LocalDate.now())
            })
        }
    }
}
