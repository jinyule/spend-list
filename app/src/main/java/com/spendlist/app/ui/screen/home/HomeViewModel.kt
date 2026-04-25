package com.spendlist.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import com.spendlist.app.domain.usecase.subscription.DeleteSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionsUseCase
import com.spendlist.app.domain.usecase.subscription.GetTotalSpentUseCase
import java.math.BigDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(), // subscriptionId -> converted amount
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedStatus: SubscriptionStatus? = null,
    val primaryCurrency: Currency = Currency.CNY,
    val totalMonthlySpend: BigDecimal? = null,
    val totalSpent: BigDecimal? = null, // Cumulative spent
    val expiredCount: Int = 0, // count of EXPIRED subscriptions across all categories/filters
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSubscriptions: GetSubscriptionsUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val categoryRepository: CategoryRepository,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val userPreferences: UserPreferences,
    private val getTotalSpent: GetTotalSpentUseCase,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currencyJob: Job? = null
    private var categoryJob: Job? = null
    private var bannerDismissJob: Job? = null

    private var expiredBannerDismissedAt: Long = 0L

    init {
        loadPrimaryCurrency()
        loadCategories()
        loadExpiredBannerDismissedAt()
        loadSubscriptions()
    }

    private fun loadPrimaryCurrency() {
        currencyJob?.cancel()
        currencyJob = viewModelScope.launch {
            userPreferences.primaryCurrencyCode.collect { code ->
                val currency = Currency.fromCode(code) ?: Currency.CNY
                _uiState.value = _uiState.value.copy(primaryCurrency = currency)
                calculateTotalSpend()
            }
        }
    }

    private fun loadCategories() {
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            categoryRepository.getAll()
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
        }
    }

    private fun loadExpiredBannerDismissedAt() {
        bannerDismissJob?.cancel()
        bannerDismissJob = viewModelScope.launch {
            userPreferences.expiredBannerDismissedAt.collect { ts ->
                expiredBannerDismissedAt = ts
                // Recompute the banner count whenever the dismiss timestamp moves.
                refreshExpiredCount()
            }
        }
    }

    private suspend fun refreshExpiredCount() {
        val newCount = subscriptionRepository.getAllOnce()
            .count { it.status == SubscriptionStatus.EXPIRED && it.updatedAt > expiredBannerDismissedAt }
        if (newCount != _uiState.value.expiredCount) {
            _uiState.value = _uiState.value.copy(expiredCount = newCount)
        }
    }

    private fun loadSubscriptions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getSubscriptions(
                categoryId = _uiState.value.selectedCategoryId,
                status = _uiState.value.selectedStatus
            )
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { subscriptions ->
                    val expiredCount = subscriptionRepository.getAllOnce()
                        .count { it.status == SubscriptionStatus.EXPIRED && it.updatedAt > expiredBannerDismissedAt }
                    _uiState.value = _uiState.value.copy(
                        subscriptions = subscriptions,
                        expiredCount = expiredCount,
                        isLoading = false,
                        error = null
                    )
                    calculateTotalSpend()
                }
        }
    }

    private fun calculateTotalSpend() {
        viewModelScope.launch {
            val state = _uiState.value
            var totalMonthly = BigDecimal.ZERO
            val convertedAmounts = mutableMapOf<Long, BigDecimal>()

            for (sub in state.subscriptions) {
                val monthlyAmount = sub.monthlyAmount
                val convertedAmount = if (sub.currency == state.primaryCurrency) {
                    monthlyAmount
                } else {
                    when (val result = convertCurrency(monthlyAmount, sub.currency, state.primaryCurrency)) {
                        is ConvertCurrencyUseCase.Result.Success -> result.amount
                        is ConvertCurrencyUseCase.Result.NoRateAvailable -> monthlyAmount
                    }
                }
                convertedAmounts[sub.id] = convertedAmount

                // Monthly forecast reflects only currently-paying subscriptions.
                if (sub.status == SubscriptionStatus.ACTIVE) {
                    totalMonthly = totalMonthly.add(convertedAmount)
                }
            }

            // Cumulative spent reflects historical actual spending across all
            // statuses (GetTotalSpentUseCase handles the ACTIVE/EXPIRED/CANCELLED
            // distinction internally via paidCycles()).
            val cumulativeSpent = getTotalSpent(state.primaryCurrency)

            _uiState.value = _uiState.value.copy(
                totalMonthlySpend = totalMonthly,
                totalSpent = cumulativeSpent,
                convertedAmounts = convertedAmounts
            )
        }
    }

    fun onCategoryFilterChanged(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId,
            isLoading = true
        )
        loadSubscriptions()
    }

    fun onStatusFilterChanged(status: SubscriptionStatus?) {
        _uiState.value = _uiState.value.copy(
            selectedStatus = status,
            isLoading = true
        )
        loadSubscriptions()
    }

    fun onDeleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            deleteSubscription(subscription)
        }
    }
}
