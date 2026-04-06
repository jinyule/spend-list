package com.spendlist.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.usecase.currency.ConvertCurrencyUseCase
import com.spendlist.app.domain.usecase.subscription.DeleteSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionsUseCase
import java.math.BigDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedStatus: SubscriptionStatus? = null,
    val primaryCurrency: Currency = Currency.CNY,
    val totalMonthlySpend: BigDecimal? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSubscriptions: GetSubscriptionsUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val categoryRepository: CategoryRepository,
    private val convertCurrency: ConvertCurrencyUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPrimaryCurrency()
        loadCategories()
        loadSubscriptions()
    }

    private fun loadPrimaryCurrency() {
        viewModelScope.launch {
            userPreferences.primaryCurrencyCode.collect { code ->
                val currency = Currency.fromCode(code) ?: Currency.CNY
                _uiState.value = _uiState.value.copy(primaryCurrency = currency)
                calculateTotalSpend()
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAll()
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
        }
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
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
                    _uiState.value = _uiState.value.copy(
                        subscriptions = subscriptions,
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
            val activeSubscriptions = state.subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
            if (activeSubscriptions.isEmpty()) {
                _uiState.value = state.copy(totalMonthlySpend = BigDecimal.ZERO)
                return@launch
            }

            var total = BigDecimal.ZERO
            for (sub in activeSubscriptions) {
                val monthlyAmount = sub.monthlyAmount
                if (sub.currency == state.primaryCurrency) {
                    total = total.add(monthlyAmount)
                } else {
                    when (val result = convertCurrency(monthlyAmount, sub.currency, state.primaryCurrency)) {
                        is ConvertCurrencyUseCase.Result.Success -> {
                            total = total.add(result.amount)
                        }
                        is ConvertCurrencyUseCase.Result.NoRateAvailable -> {
                            // Fall back to original amount if no rate
                            total = total.add(monthlyAmount)
                        }
                    }
                }
            }
            _uiState.value = _uiState.value.copy(totalMonthlySpend = total)
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
