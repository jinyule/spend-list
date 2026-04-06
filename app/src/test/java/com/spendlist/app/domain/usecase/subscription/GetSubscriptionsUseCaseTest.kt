package com.spendlist.app.domain.usecase.subscription

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetSubscriptionsUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: GetSubscriptionsUseCase

    private val sampleSubscriptions = listOf(
        Subscription(
            id = 1, name = "Claude Pro", amount = BigDecimal("150"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1),
            nextRenewalDate = LocalDate.of(2024, 2, 1),
            categoryId = 1, status = SubscriptionStatus.ACTIVE
        ),
        Subscription(
            id = 2, name = "ChatGPT", amount = BigDecimal("20"),
            currency = Currency.USD, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1),
            nextRenewalDate = LocalDate.of(2024, 2, 1),
            categoryId = 1, status = SubscriptionStatus.CANCELLED
        )
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetSubscriptionsUseCase(repository)
    }

    @Test
    fun invoke_withNoFilters_returnsAllSubscriptions() = runTest {
        every { repository.getAll() } returns flowOf(sampleSubscriptions)

        useCase().test {
            val result = awaitItem()
            assertThat(result).hasSize(2)
            awaitComplete()
        }

        verify { repository.getAll() }
    }

    @Test
    fun invoke_withCategoryFilter_returnsByCategory() = runTest {
        every { repository.getByCategory(1L) } returns flowOf(listOf(sampleSubscriptions[0]))

        useCase(categoryId = 1L).test {
            val result = awaitItem()
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("Claude Pro")
            awaitComplete()
        }

        verify { repository.getByCategory(1L) }
    }

    @Test
    fun invoke_withStatusFilter_returnsByStatus() = runTest {
        every { repository.getByStatus(SubscriptionStatus.ACTIVE) } returns
            flowOf(listOf(sampleSubscriptions[0]))

        useCase(status = SubscriptionStatus.ACTIVE).test {
            val result = awaitItem()
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(SubscriptionStatus.ACTIVE)
            awaitComplete()
        }

        verify { repository.getByStatus(SubscriptionStatus.ACTIVE) }
    }

    @Test
    fun invoke_withEmptyList_returnsEmptyFlow() = runTest {
        every { repository.getAll() } returns flowOf(emptyList())

        useCase().test {
            val result = awaitItem()
            assertThat(result).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun invoke_categoryFilterTakesPrecedence_overStatusFilter() = runTest {
        every { repository.getByCategory(1L) } returns flowOf(sampleSubscriptions)

        useCase(categoryId = 1L, status = SubscriptionStatus.ACTIVE).test {
            awaitItem()
            awaitComplete()
        }

        verify { repository.getByCategory(1L) }
        verify(exactly = 0) { repository.getByStatus(any()) }
    }
}
