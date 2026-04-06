package com.spendlist.app.domain.usecase.subscription

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

class DeleteSubscriptionUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: DeleteSubscriptionUseCase

    private val subscription = Subscription(
        id = 1, name = "Claude Pro", amount = BigDecimal("150"),
        currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
        startDate = LocalDate.of(2024, 1, 1),
        nextRenewalDate = LocalDate.of(2024, 2, 1),
        status = SubscriptionStatus.ACTIVE
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteSubscriptionUseCase(repository)
    }

    @Test
    fun deleteSubscription_callsRepositoryDelete() = runTest {
        coEvery { repository.delete(any()) } returns Unit
        useCase(subscription)
        coVerify(exactly = 1) { repository.delete(subscription) }
    }

    @Test
    fun deleteSubscription_withNonExistentSubscription_noException() = runTest {
        coEvery { repository.delete(any()) } returns Unit
        useCase(subscription.copy(id = 999))
        coVerify(exactly = 1) { repository.delete(any()) }
    }
}
