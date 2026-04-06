package com.spendlist.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.usecase.stats.CategorySpending
import com.spendlist.app.domain.usecase.stats.GetMonthlyTrendUseCase
import com.spendlist.app.domain.usecase.stats.GetSpendingByCategoryUseCase
import com.spendlist.app.domain.usecase.stats.MonthlySpending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val categorySpending: List<CategorySpending> = emptyList(),
    val monthlyTrend: List<MonthlySpending> = emptyList(),
    val primaryCurrency: Currency = Currency.CNY,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getSpendingByCategory: GetSpendingByCategoryUseCase,
    private val getMonthlyTrend: GetMonthlyTrendUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val currencyCode = userPreferences.primaryCurrencyCode.first()
            val currency = Currency.fromCode(currencyCode) ?: Currency.CNY
            _uiState.value = _uiState.value.copy(primaryCurrency = currency)

            launch {
                getSpendingByCategory(currency).collect { spending ->
                    _uiState.value = _uiState.value.copy(
                        categorySpending = spending,
                        isLoading = false
                    )
                }
            }

            launch {
                getMonthlyTrend(currency).collect { trend ->
                    _uiState.value = _uiState.value.copy(
                        monthlyTrend = trend,
                        isLoading = false
                    )
                }
            }
        }
    }
}
