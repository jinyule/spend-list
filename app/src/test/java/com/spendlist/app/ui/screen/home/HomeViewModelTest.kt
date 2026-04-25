package com.spendlist.app.ui.screen.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import com.spendlist.app.domain.usecase.subscription.DeleteSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionsUseCase
import com.spendlist.app.domain.usecase.subscription.GetTotalSpentUseCase
import io.mockk.coEvery
import io.mockk.every
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getSubscriptions: GetSubscriptionsUseCase
    private lateinit var deleteSubscription: DeleteSubscriptionUseCase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var convertCurrency: ConvertCurrencyUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var getTotalSpent: GetTotalSpentUseCase
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var viewModel: HomeViewModel

    private val sampleSubscriptions = listOf(
        Subscription(
            id = 1, name = "Claude Pro", amount = BigDecimal("150"),
            currency = Currency.CNY, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1),
            nextRenewalDate = LocalDate.of(2024, 4, 1),
            categoryId = 1, status = SubscriptionStatus.ACTIVE
        ),
        Subscription(
            id = 2, name = "ChatGPT", amount = BigDecimal("20"),
            currency = Currency.USD, billingCycle = BillingCycle.Monthly,
            startDate = LocalDate.of(2024, 1, 1),
            nextRenewalDate = LocalDate.of(2024, 4, 15),
            categoryId = 1, status = SubscriptionStatus.ACTIVE
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getSubscriptions = mockk()
        deleteSubscription = mockk()
        categoryRepository = mockk()
        convertCurrency = mockk()
        userPreferences = mockk()
        getTotalSpent = mockk()
        subscriptionRepository = mockk()
        every { categoryRepository.getAll() } returns flowOf(emptyList())
        every { userPreferences.primaryCurrencyCode } returns flowOf("CNY")
        every { userPreferences.expiredBannerDismissedAt } returns flowOf(0L)
        coEvery { getTotalSpent(any()) } returns BigDecimal.ZERO
        coEvery { subscriptionRepository.getAllOnce() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        every { getSubscriptions(any(), any()) } returns flowOf(sampleSubscriptions)
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal("145.00"))
        return HomeViewModel(getSubscriptions, deleteSubscription, categoryRepository, convertCurrency, userPreferences, getTotalSpent, subscriptionRepository)
    }

    @Test
    fun initialState_isLoading() = runTest {
        every { getSubscriptions(any(), any()) } returns flowOf(emptyList())
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal.ZERO)
        viewModel = HomeViewModel(getSubscriptions, deleteSubscription, categoryRepository, convertCurrency, userPreferences, getTotalSpent, subscriptionRepository)

        // Before collection starts, isLoading is true
        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun afterLoad_subscriptionsArePopulated() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.subscriptions).hasSize(2)
            assertThat(state.subscriptions[0].name).isEqualTo("Claude Pro")
        }
    }

    @Test
    fun afterLoad_emptyList_showsEmptyState() = runTest {
        every { getSubscriptions(any(), any()) } returns flowOf(emptyList())
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal.ZERO)
        viewModel = HomeViewModel(getSubscriptions, deleteSubscription, categoryRepository, convertCurrency, userPreferences, getTotalSpent, subscriptionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.subscriptions).isEmpty()
        }
    }

    @Test
    fun deleteSubscription_callsUseCase() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { deleteSubscription(any()) } returns Unit

        viewModel.onDeleteSubscription(sampleSubscriptions[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify no crash and state still valid
        val state = viewModel.uiState.value
        assertThat(state.error).isNull()
    }

    @Test
    fun totalMonthlySpend_calculatedWithConversion() = runTest {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.totalMonthlySpend).isNotNull()
        // 150 CNY (same currency) + 145 CNY (converted from 20 USD) = 295
        assertThat(state.totalMonthlySpend).isEqualTo(BigDecimal("295.00"))
    }

    @Test
    fun expiredCount_excludesSubscriptionsDismissedBeforeCurrentExpiration() = runTest {
        // User has viewed the expired subscriptions already (dismissedAt = T2).
        // Any EXPIRED sub whose updatedAt <= dismissedAt must not contribute to expiredCount.
        val expiredOld = sampleSubscriptions[0].copy(
            status = SubscriptionStatus.EXPIRED,
            updatedAt = 1_000L
        )
        every { getSubscriptions(any(), any()) } returns flowOf(listOf(expiredOld))
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal("145.00"))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(expiredOld)
        every { userPreferences.expiredBannerDismissedAt } returns flowOf(2_000L)

        viewModel = HomeViewModel(
            getSubscriptions, deleteSubscription, categoryRepository,
            convertCurrency, userPreferences, getTotalSpent, subscriptionRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.expiredCount).isEqualTo(0)
    }

    @Test
    fun expiredCount_includesSubscriptionsExpiredAfterDismiss() = runTest {
        // A fresh expiration (updatedAt > dismissedAt) must reappear in expiredCount.
        val freshlyExpired = sampleSubscriptions[0].copy(
            status = SubscriptionStatus.EXPIRED,
            updatedAt = 5_000L
        )
        every { getSubscriptions(any(), any()) } returns flowOf(listOf(freshlyExpired))
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal("145.00"))
        coEvery { subscriptionRepository.getAllOnce() } returns listOf(freshlyExpired)
        every { userPreferences.expiredBannerDismissedAt } returns flowOf(2_000L)

        viewModel = HomeViewModel(
            getSubscriptions, deleteSubscription, categoryRepository,
            convertCurrency, userPreferences, getTotalSpent, subscriptionRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.expiredCount).isEqualTo(1)
    }

    @Test
    fun onlyExpiredSubscriptions_totalSpentStillReflectsHistory() = runTest {
        // Cumulative spent must not be wiped when no ACTIVE sub remains —
        // the historical GetTotalSpentUseCase result should pass through.
        val expiredOnly = listOf(
            sampleSubscriptions[0].copy(status = SubscriptionStatus.EXPIRED),
            sampleSubscriptions[1].copy(status = SubscriptionStatus.EXPIRED)
        )
        every { getSubscriptions(any(), any()) } returns flowOf(expiredOnly)
        coEvery { convertCurrency(any(), any(), any()) } returns
            ConvertCurrencyUseCase.Result.Success(BigDecimal("145.00"))
        coEvery { getTotalSpent(any()) } returns BigDecimal("500.00")

        viewModel = HomeViewModel(
            getSubscriptions, deleteSubscription, categoryRepository,
            convertCurrency, userPreferences, getTotalSpent, subscriptionRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Monthly forecast = 0 (no ACTIVE) but cumulative preserves history
        assertThat(state.totalMonthlySpend).isEqualTo(BigDecimal.ZERO)
        assertThat(state.totalSpent).isEqualTo(BigDecimal("500.00"))
    }
}
