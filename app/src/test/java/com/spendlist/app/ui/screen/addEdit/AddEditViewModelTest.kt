package com.spendlist.app.ui.screen.addEdit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.subscription.AddSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionByIdUseCase
import com.spendlist.app.domain.usecase.subscription.UpdateSubscriptionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var renewalHistoryRepository: RenewalHistoryRepository
    private lateinit var subscriptionRepository: SubscriptionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        addSubscription = mockk()
        updateSubscription = mockk()
        getSubscriptionById = mockk()
        categoryRepository = mockk()
        renewalHistoryRepository = mockk()
        subscriptionRepository = mockk(relaxed = true)
        coEvery { categoryRepository.getAll() } returns flowOf(emptyList())
        coEvery { renewalHistoryRepository.getBySubscriptionId(any()) } returns flowOf(emptyList())
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(subscriptionId: Long? = null): AddEditViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            subscriptionId?.let { set("id", it) }
        }
        return AddEditViewModel(
            savedStateHandle,
            addSubscription,
            updateSubscription,
            getSubscriptionById,
            categoryRepository,
            renewalHistoryRepository,
            subscriptionRepository
        )
    }

    private fun makeExisting(
        nextRenewal: LocalDate = LocalDate.of(2024, 2, 12),
        startDate: LocalDate = LocalDate.of(2024, 1, 12),
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        cycle: BillingCycle = BillingCycle.Monthly
    ) = Subscription(
        id = 1, name = "Claude Pro", amount = BigDecimal("150"),
        currency = Currency.CNY, billingCycle = cycle,
        startDate = startDate,
        nextRenewalDate = nextRenewal,
        status = status
    )

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
        val existing = makeExisting()
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

    // ---- Regression: editing fields must not revert nextRenewalDate ----

    @Test
    fun editMode_changeBillingCycle_doesNotResetNextRenewalDate() = runTest {
        val existing = makeExisting(nextRenewal = LocalDate.of(2024, 5, 12))
        coEvery { getSubscriptionById(1L) } returns existing

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBillingCycleTypeChange("YEARLY")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate)
            .isEqualTo(LocalDate.of(2024, 5, 12))
    }

    @Test
    fun editMode_changeStartDate_doesNotResetNextRenewalDate() = runTest {
        val existing = makeExisting(nextRenewal = LocalDate.of(2024, 5, 12))
        coEvery { getSubscriptionById(1L) } returns existing

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onStartDateChange(LocalDate.of(2023, 6, 1))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate)
            .isEqualTo(LocalDate.of(2024, 5, 12))
    }

    @Test
    fun editMode_changeBillingDay_doesNotResetNextRenewalDate() = runTest {
        val existing = makeExisting(nextRenewal = LocalDate.of(2024, 5, 12))
        coEvery { getSubscriptionById(1L) } returns existing

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBillingDayChange("15")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate)
            .isEqualTo(LocalDate.of(2024, 5, 12))
    }

    @Test
    fun editMode_changeCustomDays_doesNotResetNextRenewalDate() = runTest {
        val existing = makeExisting(
            nextRenewal = LocalDate.of(2024, 5, 12),
            cycle = BillingCycle.Custom(45)
        )
        coEvery { getSubscriptionById(1L) } returns existing

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCustomDaysChange("60")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate)
            .isEqualTo(LocalDate.of(2024, 5, 12))
    }

    @Test
    fun newMode_changeBillingCycleType_recalculatesNextRenewalDate() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now()
        viewModel.onStartDateChange(today)
        viewModel.onBillingCycleTypeChange("YEARLY")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate).isEqualTo(today.plusYears(1))
    }

    // ---- Self-heal from RenewalHistory ----

    @Test
    fun editMode_loadSubscription_reconcilesFromRenewalHistory() = runTest {
        val staleNextRenewal = LocalDate.of(2024, 4, 20)  // stale (rolled back)
        val advancedByRenewal = LocalDate.of(2024, 5, 20) // truth from renewal record
        val existing = makeExisting(nextRenewal = staleNextRenewal, status = SubscriptionStatus.EXPIRED)
        coEvery { getSubscriptionById(1L) } returns existing
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1L) } returns advancedByRenewal

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate).isEqualTo(advancedByRenewal)
        coVerify { subscriptionRepository.update(any()) }
    }

    @Test
    fun editMode_reconcile_recoversExpiredToActiveWhenAdvancedDateInFuture() = runTest {
        val staleNextRenewal = LocalDate.now().minusDays(5)
        val advancedByRenewal = LocalDate.now().plusDays(20)
        val existing = makeExisting(nextRenewal = staleNextRenewal, status = SubscriptionStatus.EXPIRED)
        coEvery { getSubscriptionById(1L) } returns existing
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1L) } returns advancedByRenewal

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(SubscriptionStatus.ACTIVE)
        assertThat(viewModel.uiState.value.nextRenewalDate).isEqualTo(advancedByRenewal)
    }

    @Test
    fun editMode_reconcile_keepsExistingWhenHistoryNotAhead() = runTest {
        val nextRenewal = LocalDate.of(2024, 6, 20)
        val existing = makeExisting(nextRenewal = nextRenewal)
        coEvery { getSubscriptionById(1L) } returns existing
        coEvery { renewalHistoryRepository.getLatestNewRenewalDate(1L) } returns LocalDate.of(2024, 5, 20)

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.nextRenewalDate).isEqualTo(nextRenewal)
        coVerify(exactly = 0) { subscriptionRepository.update(any()) }
    }

    // ---- onSave must not clobber nextRenewalDate when untouched ----

    @Test
    fun editMode_save_preservesNextRenewalWhenUnchanged() = runTest {
        // DB already holds the advanced, reconciled value; user opens edit page and
        // hits save without touching the date picker — DB must remain at 5-21.
        val dbAdvanced = LocalDate.of(2026, 5, 21)
        val existing = makeExisting(nextRenewal = dbAdvanced)
        coEvery { getSubscriptionById(1L) } returns existing
        coEvery { updateSubscription(any()) } returns UpdateSubscriptionUseCase.Result.Success

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate a stale UI value leaking in somehow — save should still re-read DB.
        // Force state to a wrong value without marking manuallyChanged=true.
        val wrong = LocalDate.of(2026, 4, 20)
        // Use reflection-free approach: we can't set state directly, so this test
        // relies on the current state already being 5-21 from load, then verifies save path.
        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            updateSubscription(match { it.nextRenewalDate == dbAdvanced })
        }
    }

    @Test
    fun editMode_save_usesUserPickedDateWhenManuallyChanged() = runTest {
        val dbCurrent = LocalDate.of(2026, 5, 21)
        val existing = makeExisting(nextRenewal = dbCurrent)
        coEvery { getSubscriptionById(1L) } returns existing
        coEvery { updateSubscription(any()) } returns UpdateSubscriptionUseCase.Result.Success

        val viewModel = createViewModel(subscriptionId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val userPicked = LocalDate.of(2026, 6, 10)
        viewModel.onNextRenewalDateChange(userPicked)
        viewModel.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            updateSubscription(match { it.nextRenewalDate == userPicked })
        }
    }
}
