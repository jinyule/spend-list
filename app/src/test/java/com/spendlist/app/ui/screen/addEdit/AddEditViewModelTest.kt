package com.spendlist.app.ui.screen.addEdit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.usecase.subscription.AddSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionByIdUseCase
import com.spendlist.app.domain.usecase.subscription.UpdateSubscriptionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var addSubscription: AddSubscriptionUseCase
    private lateinit var updateSubscription: UpdateSubscriptionUseCase
    private lateinit var getSubscriptionById: GetSubscriptionByIdUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addSubscription = mockk()
        updateSubscription = mockk()
        getSubscriptionById = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(subscriptionId: Long? = null): AddEditViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            subscriptionId?.let { set("id", it) }
        }
        return AddEditViewModel(savedStateHandle, addSubscription, updateSubscription, getSubscriptionById)
    }

    @Test
    fun newMode_initialStateIsEmpty() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEditMode).isFalse()
        assertThat(state.name).isEmpty()
        assertThat(state.amount).isEmpty()
    }

    @Test
    fun editMode_loadsExistingData() = runTest {
        val existing = Subscription(
            id = 1, name = "Claude Pro", amount = BigDecimal("150"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 12),
            nextRenewalDate = LocalDate.of(2024, 2, 12),
            status = SubscriptionStatus.ACTIVE
        )
        coEvery { getSubscriptionById(1L) } returns existing

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isEditMode).isTrue()
            assertThat(state.name).isEqualTo("Claude Pro")
            assertThat(state.amount).isEqualTo("150")
        }
    }

    @Test
    fun save_withEmptyName_showsError() = runTest {
        coEvery { addSubscription(any()) } returns
            AddSubscriptionUseCase.Result.ValidationError("Name is required")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChange("")
        viewModel.onAmountChange("150")
        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.nameError).isNotNull()
    }

    @Test
    fun save_withInvalidAmount_showsError() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onNameChange("Test")
        viewModel.onAmountChange("")
        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.amountError).isNotNull()
    }

    @Test
    fun save_withValidData_navigatesBack() = runTest {
        coEvery { addSubscription(any()) } returns AddSubscriptionUseCase.Result.Success(1L)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Must start collecting BEFORE the event is emitted (SharedFlow has no replay)
        viewModel.navigateBack.test {
            viewModel.onNameChange("Claude Pro")
            viewModel.onAmountChange("150")
            viewModel.onSave()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isTrue()
        }
    }
}
