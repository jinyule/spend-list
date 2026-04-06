package com.spendlist.app.ui.screen.addEdit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.domain.model.BillingCycle
import com.spendlist.app.domain.model.Category
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.model.Subscription
import com.spendlist.app.domain.model.SubscriptionStatus
import com.spendlist.app.domain.repository.CategoryRepository
import com.spendlist.app.domain.usecase.subscription.AddSubscriptionUseCase
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
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class AddEditUiState(
    val isEditMode: Boolean = false,
    val subscriptionId: Long? = null,
    val name: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val amount: String = "",
    val currency: Currency = Currency.CNY,
    val billingCycleType: String = "MONTHLY",
    val customDays: String = "30",
    val startDate: LocalDate = LocalDate.now(),
    val nextRenewalDate: LocalDate = LocalDate.now().plusMonths(1),
    val note: String = "",
    val manageUrl: String = "",
    val iconUri: String? = null,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val nameError: String? = null,
    val amountError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addSubscription: AddSubscriptionUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val getSubscriptionById: GetSubscriptionByIdUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack: SharedFlow<Boolean> = _navigateBack.asSharedFlow()

    init {
        loadCategories()
        val id = savedStateHandle.get<Long>("id")
        if (id != null) {
            loadSubscription(id)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAll().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    private fun loadSubscription(id: Long) {
        viewModelScope.launch {
            val sub = getSubscriptionById(id) ?: return@launch
            _uiState.value = _uiState.value.copy(
                isEditMode = true,
                subscriptionId = sub.id,
                name = sub.name,
                categoryId = sub.categoryId,
                amount = sub.amount.toPlainString(),
                currency = sub.currency,
                billingCycleType = when (sub.billingCycle) {
                    is BillingCycle.Monthly -> "MONTHLY"
                    is BillingCycle.Yearly -> "YEARLY"
                    is BillingCycle.Custom -> "CUSTOM"
                },
                customDays = when (sub.billingCycle) {
                    is BillingCycle.Custom -> sub.billingCycle.days.toString()
                    else -> "30"
                },
                startDate = sub.startDate,
                nextRenewalDate = sub.nextRenewalDate,
                note = sub.note ?: "",
                manageUrl = sub.manageUrl ?: "",
                iconUri = sub.iconUri,
                status = sub.status
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onAmountChange(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount, amountError = null)
    }

    fun onCurrencyChange(currency: Currency) {
        _uiState.value = _uiState.value.copy(currency = currency)
    }

    fun onBillingCycleTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(billingCycleType = type)
        updateNextRenewalDate()
    }

    fun onCustomDaysChange(days: String) {
        _uiState.value = _uiState.value.copy(customDays = days)
        updateNextRenewalDate()
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = date)
        updateNextRenewalDate()
    }

    fun onNextRenewalDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(nextRenewalDate = date)
    }

    fun onCategoryChange(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onManageUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(manageUrl = url)
    }

    fun onIconChange(iconUri: String?) {
        _uiState.value = _uiState.value.copy(iconUri = iconUri)
    }

    fun onStatusChange(status: SubscriptionStatus) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    private fun updateNextRenewalDate() {
        val state = _uiState.value
        val cycle = buildBillingCycle(state)
        _uiState.value = state.copy(
            nextRenewalDate = cycle.calculateNextRenewalDate(state.startDate)
        )
    }

    fun onSave() {
        val state = _uiState.value

        // Client-side validation
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = "Name is required")
            return
        }

        val amount = try {
            BigDecimal(state.amount)
        } catch (_: Exception) {
            _uiState.value = state.copy(amountError = "Invalid amount")
            return
        }

        if (amount <= BigDecimal.ZERO) {
            _uiState.value = state.copy(amountError = "Amount must be greater than 0")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        val subscription = Subscription(
            id = state.subscriptionId ?: 0,
            name = state.name.trim(),
            categoryId = state.categoryId,
            amount = amount,
            currency = state.currency,
            billingCycle = buildBillingCycle(state),
            startDate = state.startDate,
            nextRenewalDate = state.nextRenewalDate,
            note = state.note.ifBlank { null },
            manageUrl = state.manageUrl.ifBlank { null },
            iconUri = state.iconUri,
            status = state.status
        )

        viewModelScope.launch {
            val result = if (state.isEditMode) {
                when (val r = updateSubscription(subscription)) {
                    is UpdateSubscriptionUseCase.Result.Success -> true
                    is UpdateSubscriptionUseCase.Result.ValidationError -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            nameError = if (r.message.contains("Name")) r.message else null,
                            amountError = if (r.message.contains("Amount")) r.message else null
                        )
                        false
                    }
                }
            } else {
                when (val r = addSubscription(subscription)) {
                    is AddSubscriptionUseCase.Result.Success -> true
                    is AddSubscriptionUseCase.Result.ValidationError -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            nameError = if (r.message.contains("Name")) r.message else null,
                            amountError = if (r.message.contains("Amount")) r.message else null
                        )
                        false
                    }
                }
            }

            if (result) {
                _navigateBack.emit(true)
            }
        }
    }

    private fun buildBillingCycle(state: AddEditUiState): BillingCycle {
        return when (state.billingCycleType) {
            "YEARLY" -> BillingCycle.Yearly
            "CUSTOM" -> BillingCycle.Custom(state.customDays.toIntOrNull() ?: 30)
            else -> BillingCycle.Monthly
        }
    }
}
