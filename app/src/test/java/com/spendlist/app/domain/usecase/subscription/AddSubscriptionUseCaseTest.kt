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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class AddSubscriptionUseCaseTest {

    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: AddSubscriptionUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = AddSubscriptionUseCase(repository)
    }

    private fun createSubscription(
        name: String = "Claude Pro",
        amount: BigDecimal = BigDecimal("150.00"),
        startDate: LocalDate = LocalDate.of(2024, 1, 12),
        nextRenewalDate: LocalDate = LocalDate.of(2024, 2, 12)
    ) = Subscription(
        name = name,
        amount = amount,
        currency = Currency.CNY,
        billingCycle = BillingCycle.Monthly,
        startDate = startDate,
        nextRenewalDate = nextRenewalDate,
        status = SubscriptionStatus.ACTIVE
    )

    @Test
    fun addSubscription_withValidData_returnsSuccess() = runTest {
        coEvery { repository.insert(any()) } returns 1L
        val result = useCase(createSubscription())
        assertThat(result).isInstanceOf(AddSubscriptionUseCase.Result.Success::class.java)
        assertThat((result as AddSubscriptionUseCase.Result.Success).id).isEqualTo(1L)
    }

    @Test
    fun addSubscription_withEmptyName_returnsValidationError() = runTest {
        val result = useCase(createSubscription(name = ""))
        assertThat(result).isInstanceOf(AddSubscriptionUseCase.Result.ValidationError::class.java)
        assertThat((result as AddSubscriptionUseCase.Result.ValidationError).message)
            .contains("Name")
    }

    @Test
    fun addSubscription_withBlankName_returnsValidationError() = runTest {
        val result = useCase(createSubscription(name = "   "))
        assertThat(result).isInstanceOf(AddSubscriptionUseCase.Result.ValidationError::class.java)
    }

    @Test
    fun addSubscription_withZeroAmount_returnsValidationError() = runTest {
        val result = useCase(createSubscription(amount = BigDecimal.ZERO))
        assertThat(result).isInstanceOf(AddSubscriptionUseCase.Result.ValidationError::class.java)
        assertThat((result as AddSubscriptionUseCase.Result.ValidationError).message)
            .contains("Amount")
    }

    @Test
    fun addSubscription_withNegativeAmount_returnsValidationError() = runTest {
        val result = useCase(createSubscription(amount = BigDecimal("-10")))
        assertThat(result).isInstanceOf(AddSubscriptionUseCase.Result.ValidationError::class.java)
    }

    @Test
    fun addSubscription_autoCalculatesNextRenewalDate_whenNotSet() = runTest {
        val slot = slot<Subscription>()
        coEvery { repository.insert(capture(slot)) } returns 1L

        val startDate = LocalDate.of(2024, 1, 12)
        val sub = createSubscription(
            startDate = startDate,
            nextRenewalDate = startDate // same as start = not properly set
        )

        useCase(sub)

        assertThat(slot.captured.nextRenewalDate).isEqualTo(LocalDate.of(2024, 2, 12))
    }

    @Test
    fun addSubscription_preservesExplicitNextRenewalDate() = runTest {
        val slot = slot<Subscription>()
        coEvery { repository.insert(capture(slot)) } returns 1L

        val explicitDate = LocalDate.of(2024, 5, 1)
        val sub = createSubscription(
            startDate = LocalDate.of(2024, 1, 12),
            nextRenewalDate = explicitDate
        )

        useCase(sub)

        assertThat(slot.captured.nextRenewalDate).isEqualTo(explicitDate)
    }

    @Test
    fun addSubscription_callsRepositoryInsert() = runTest {
        coEvery { repository.insert(any()) } returns 1L
        useCase(createSubscription())
        coVerify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun addSubscription_doesNotCallRepository_onValidationError() = runTest {
        useCase(createSubscription(name = ""))
        coVerify(exactly = 0) { repository.insert(any()) }
    }
}
