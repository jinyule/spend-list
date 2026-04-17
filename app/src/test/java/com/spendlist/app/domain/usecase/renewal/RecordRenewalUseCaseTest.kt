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

class RecordRenewalUseCaseTest {

    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var renewalHistoryRepository: RenewalHistoryRepository
    private lateinit var useCase: RecordRenewalUseCase

    @Before
    fun setup() {
        subscriptionRepository = mockk(relaxed = true)
        renewalHistoryRepository = mockk(relaxed = true)
        useCase = RecordRenewalUseCase(subscriptionRepository, renewalHistoryRepository)
    }

    private fun sub(
        id: Long = 1,
        nextRenewalDate: LocalDate,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        billingCycle: BillingCycle = BillingCycle.Monthly
    ) = Subscription(
        id = id,
        name = "Netflix",
        amount = BigDecimal("10"),
        currency = Currency.USD,
        billingCycle = billingCycle,
        startDate = LocalDate.of(2024, 1, 1),
        nextRenewalDate = nextRenewalDate,
        status = status
    )

    @Test
    fun subscriptionNotFound_returnsError() = runTest {
        coEvery { subscriptionRepository.getById(99) } returns null

        val result = useCase(99)

        assertThat(result).isInstanceOf(RecordRenewalUseCase.Result.Error::class.java)
    }

    @Test
    fun activeSubscription_renewed_staysActive() = runTest {
        val s = sub(nextRenewalDate = LocalDate.now().plusDays(3), status = SubscriptionStatus.ACTIVE)
        coEvery { subscriptionRepository.getById(1) } returns s

        val result = useCase(1)

        assertThat(result).isInstanceOf(RecordRenewalUseCase.Result.Success::class.java)
        val updated = (result as RecordRenewalUseCase.Result.Success).subscription
        assertThat(updated.status).isEqualTo(SubscriptionStatus.ACTIVE)
        coVerify { subscriptionRepository.update(match { it.status == SubscriptionStatus.ACTIVE }) }
    }

    @Test
    fun expiredSubscription_renewedPastToday_becomesActive() = runTest {
        // Monthly subscription expired 5 days ago → renew advances 1 month → now in future
        val s = sub(
            nextRenewalDate = LocalDate.now().minusDays(5),
            status = SubscriptionStatus.EXPIRED
        )
        coEvery { subscriptionRepository.getById(1) } returns s

        val result = useCase(1)

        val updated = (result as RecordRenewalUseCase.Result.Success).subscription
        assertThat(updated.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(updated.nextRenewalDate).isGreaterThan(LocalDate.now())
    }

    @Test
    fun expiredSubscription_renewedButStillPast_staysExpired() = runTest {
        // Monthly subscription expired 3 months ago → one renew advances 1 month → still past
        val s = sub(
            nextRenewalDate = LocalDate.now().minusMonths(3),
            status = SubscriptionStatus.EXPIRED
        )
        coEvery { subscriptionRepository.getById(1) } returns s

        val result = useCase(1)

        val updated = (result as RecordRenewalUseCase.Result.Success).subscription
        assertThat(updated.status).isEqualTo(SubscriptionStatus.EXPIRED)
        assertThat(updated.nextRenewalDate).isLessThan(LocalDate.now())
    }

    @Test
    fun renewalHistoryRecordWritten() = runTest {
        val s = sub(nextRenewalDate = LocalDate.now().plusDays(3))
        coEvery { subscriptionRepository.getById(1) } returns s
        val historySlot = slot<RenewalHistory>()
        coEvery { renewalHistoryRepository.insert(capture(historySlot)) } returns 1

        useCase(1, note = "manual-test")

        assertThat(historySlot.captured.subscriptionId).isEqualTo(1)
        assertThat(historySlot.captured.previousRenewalDate).isEqualTo(s.nextRenewalDate)
        assertThat(historySlot.captured.note).isEqualTo("manual-test")
    }

    @Test
    fun cancelledSubscription_renewed_staysCancelled() = runTest {
        // UI should prevent this path, but use case itself must not silently reactivate
        val s = sub(
            nextRenewalDate = LocalDate.now().plusDays(3),
            status = SubscriptionStatus.CANCELLED
        )
        coEvery { subscriptionRepository.getById(1) } returns s

        val result = useCase(1)

        val updated = (result as RecordRenewalUseCase.Result.Success).subscription
        assertThat(updated.status).isEqualTo(SubscriptionStatus.CANCELLED)
    }
}
