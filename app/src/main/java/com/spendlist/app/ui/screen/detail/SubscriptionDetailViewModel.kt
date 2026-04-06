package com.spendlist.app.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.domain.model.RenewalHistory
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.usecase.renewal.RecordRenewalUseCase
import com.spendlist.app.domain.usecase.subscription.DeleteSubscriptionUseCase
import com.spendlist.app.domain.usecase.subscription.GetSubscriptionByIdUseCase
import com.spendlist.app.domain.usecase.subscription.UpdateSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val subscription: Subscription? = null,
    val renewalHistory: List<RenewalHistory> = emptyList(),
    val renewalCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubscriptionById: GetSubscriptionByIdUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase,
    private val recordRenewal: RecordRenewalUseCase,
    private val renewalHistoryRepository: RenewalHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    private val subscriptionId: Long = savedStateHandle.get<Long>("id") ?: 0L

    init {
        loadSubscription()
        loadRenewalHistory()
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val sub = getSubscriptionById(subscriptionId)
            _uiState.value = _uiState.value.copy(
                subscription = sub,
                isLoading = false,
                error = if (sub == null) "Subscription not found" else null
            )
        }
    }

    private fun loadRenewalHistory() {
        viewModelScope.launch {
            renewalHistoryRepository.getBySubscriptionId(subscriptionId).collect { history ->
                _uiState.value = _uiState.value.copy(
                    renewalHistory = history,
                    renewalCount = history.size
                )
            }
        }
    }

    fun onRenew() {
        viewModelScope.launch {
            when (val result = recordRenewal(subscriptionId)) {
                is RecordRenewalUseCase.Result.Success -> {
                    _uiState.value = _uiState.value.copy(subscription = result.subscription)
                }
                is RecordRenewalUseCase.Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun onMarkCancelled() {
        val sub = _uiState.value.subscription ?: return
        viewModelScope.launch {
            updateSubscription(sub.copy(status = SubscriptionStatus.CANCELLED))
            loadSubscription()
        }
    }

    fun onDelete() {
        val sub = _uiState.value.subscription ?: return
        viewModelScope.launch {
            deleteSubscription(sub)
            _navigateBack.emit(true)
        }
    }
}
