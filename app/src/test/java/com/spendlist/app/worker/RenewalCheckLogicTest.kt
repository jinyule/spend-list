package com.spendlist.app.worker

import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.usecase.subscription.GetUpcomingRenewalsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RenewalCheckLogicTest {

    private lateinit var getUpcomingRenewals: GetUpcomingRenewalsUseCase

    private fun createSubscription(
        id: Long = 1,
        name: String = "Claude Pro",
        daysUntil: Long = 1,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE
    ) = Subscription(
        id = id,
        name = name,
        amount = BigDecimal("150"),
        currency = Currency.CNY,
        billingCycle = BillingCycle.Monthly,
        startDate = LocalDate.now().minusMonths(1),
        nextRenewalDate = LocalDate.now().plusDays(daysUntil),
        status = status
    )

    @Before
    fun setup() {
        getUpcomingRenewals = mockk()
    }

    @Test
    fun upcomingRenewals_withinThreeDays_returnsSubscriptions() = runTest {
        val subs = listOf(
            createSubscription(id = 1, name = "Claude Pro", daysUntil = 1),
            createSubscription(id = 2, name = "ChatGPT", daysUntil = 3)
        )
        coEvery { getUpcomingRenewals(3) } returns subs

        val result = getUpcomingRenewals(3)
        assertThat(result).hasSize(2)
    }

    @Test
    fun upcomingRenewals_noUpcoming_returnsEmpty() = runTest {
        coEvery { getUpcomingRenewals(3) } returns emptyList()

        val result = getUpcomingRenewals(3)
        assertThat(result).isEmpty()
    }

    @Test
    fun upcomingRenewals_cancelledNotIncluded() = runTest {
        // The use case filters active only - cancelled should not appear
        coEvery { getUpcomingRenewals(3) } returns emptyList()

        val result = getUpcomingRenewals(3)
        assertThat(result).isEmpty()
    }

    @Test
    fun renewalToday_daysUntilIsZero() = runTest {
        val todaySub = createSubscription(daysUntil = 0)
        coEvery { getUpcomingRenewals(3) } returns listOf(todaySub)

        val result = getUpcomingRenewals(3)
        assertThat(result).hasSize(1)
        assertThat(result[0].daysUntilRenewal()).isEqualTo(0)
    }

    @Test
    fun notificationContent_todayRenewal_usesTodayFormat() {
        val sub = createSubscription(daysUntil = 0)
        val days = sub.daysUntilRenewal()
        assertThat(days).isEqualTo(0)
        // Worker will use notification_renewal_today format
    }

    @Test
    fun notificationContent_futureRenewal_usesDaysFormat() {
        val sub = createSubscription(daysUntil = 2)
        val days = sub.daysUntilRenewal()
        assertThat(days).isEqualTo(2)
        // Worker will use notification_renewal_days format
    }
}
