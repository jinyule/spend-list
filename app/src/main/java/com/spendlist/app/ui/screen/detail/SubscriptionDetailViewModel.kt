package com.spendlist.app.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
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
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubscriptionById: GetSubscriptionByIdUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val deleteSubscription: DeleteSubscriptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    private val subscriptionId: Long = savedStateHandle.get<Long>("id") ?: 0L

    init {
        loadSubscription()
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            val sub = getSubscriptionById(subscriptionId)
            _uiState.value = DetailUiState(
                subscription = sub,
                isLoading = false,
                error = if (sub == null) "Subscription not found" else null
            )
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
