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
import com.spendlist.app.domain.repository.RenewalHistoryRepository
import com.spendlist.app.domain.repository.SubscriptionRepository
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
    val customDaysError: String? = null,
    val billingDayOfMonth: String = "",
    val billingDayError: String? = null,
    val isSaving: Boolean = false,
    val nextRenewalDateManuallyChanged: Boolean = false
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addSubscription: AddSubscriptionUseCase,
    private val updateSubscription: UpdateSubscriptionUseCase,
    private val getSubscriptionById: GetSubscriptionByIdUseCase,
    private val categoryRepository: CategoryRepository,
    private val renewalHistoryRepository: RenewalHistoryRepository,
    private val subscriptionRepository: SubscriptionRepository
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
            val resolved = reconcileNextRenewalFromHistory(sub)
            _uiState.value = _uiState.value.copy(
                isEditMode = true,
                subscriptionId = resolved.id,
                name = resolved.name,
                categoryId = resolved.categoryId,
                amount = resolved.amount.toPlainString(),
                currency = resolved.currency,
                billingCycleType = when (resolved.billingCycle) {
                    is BillingCycle.Monthly -> "MONTHLY"
                    is BillingCycle.Quarterly -> "QUARTERLY"
                    is BillingCycle.Yearly -> "YEARLY"
                    is BillingCycle.Custom -> "CUSTOM"
                },
                customDays = when (resolved.billingCycle) {
                    is BillingCycle.Custom -> resolved.billingCycle.days.toString()
                    else -> "30"
                },
                billingDayOfMonth = resolved.billingDayOfMonth?.toString() ?: "",
                startDate = resolved.startDate,
                nextRenewalDate = resolved.nextRenewalDate,
                note = resolved.note ?: "",
                manageUrl = resolved.manageUrl ?: "",
                iconUri = resolved.iconUri,
                status = resolved.status
            )
        }
    }

    // Self-heal stale data caused by an earlier bug where editing certain fields
    // reverted nextRenewalDate to the first cycle. If a renewal record advanced the
    // date past what's currently stored, trust the renewal record and rewrite.
    private suspend fun reconcileNextRenewalFromHistory(sub: Subscription): Subscription {
        val latestRenewedTo = renewalHistoryRepository.getLatestNewRenewalDate(sub.id)
            ?: return sub
        if (!latestRenewedTo.isAfter(sub.nextRenewalDate)) return sub

        val recoveredStatus = if (
            sub.status == SubscriptionStatus.EXPIRED &&
            !latestRenewedTo.isBefore(LocalDate.now())
        ) {
            SubscriptionStatus.ACTIVE
        } else {
            sub.status
        }
        val fixed = sub.copy(
            nextRenewalDate = latestRenewedTo,
            status = recoveredStatus,
            updatedAt = System.currentTimeMillis()
        )
        subscriptionRepository.update(fixed)
        return fixed
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
        val newState = if (type == "CUSTOM") {
            _uiState.value.copy(billingCycleType = type, billingDayOfMonth = "", billingDayError = null)
        } else {
            _uiState.value.copy(billingCycleType = type)
        }
        _uiState.value = newState
        if (!newState.isEditMode) updateNextRenewalDate()
    }

    fun onBillingDayChange(day: String) {
        val error = if (day.isNotBlank()) {
            val dayInt = day.toIntOrNull()
            if (dayInt == null || dayInt < 1 || dayInt > 31) "1-31" else null
        } else null
        val state = _uiState.value.copy(billingDayOfMonth = day, billingDayError = error)
        _uiState.value = state
        if (!state.isEditMode) updateNextRenewalDate()
    }

    fun onCustomDaysChange(days: String) {
        val error = if (days.isNotBlank()) {
            val daysInt = days.toIntOrNull()
            if (daysInt == null || daysInt < 1) "Days must be at least 1" else null
        } else null
        val state = _uiState.value.copy(customDays = days, customDaysError = error)
        _uiState.value = state
        if (!state.isEditMode) updateNextRenewalDate()
    }

    fun onStartDateChange(date: LocalDate) {
        val state = _uiState.value.copy(startDate = date)
        _uiState.value = state
        if (!state.isEditMode) updateNextRenewalDate()
    }

    fun onNextRenewalDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            nextRenewalDate = date,
            nextRenewalDateManuallyChanged = true
        )
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
        val billingDay = state.billingDayOfMonth.toIntOrNull()
        _uiState.value = state.copy(
            nextRenewalDate = cycle.calculateNextRenewalDate(state.startDate, billingDay)
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

        // Validate custom days
        if (state.billingCycleType == "CUSTOM") {
            val customDaysInt = state.customDays.toIntOrNull()
            if (customDaysInt == null || customDaysInt < 1) {
                _uiState.value = state.copy(customDaysError = "Days must be at least 1")
                return
            }
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            // In edit mode, never overwrite nextRenewalDate unless the user explicitly
            // picked one. This prevents a stale UI value from silently clobbering a
            // date that was just advanced by a Renew action or reconcile pass.
            val persistedNextRenewal = if (state.isEditMode && !state.nextRenewalDateManuallyChanged) {
                state.subscriptionId?.let { getSubscriptionById(it)?.nextRenewalDate }
                    ?: state.nextRenewalDate
            } else {
                state.nextRenewalDate
            }

            val subscription = Subscription(
                id = state.subscriptionId ?: 0,
                name = state.name.trim(),
                categoryId = state.categoryId,
                amount = amount,
                currency = state.currency,
                billingCycle = buildBillingCycle(state),
                billingDayOfMonth = state.billingDayOfMonth.toIntOrNull(),
                startDate = state.startDate,
                nextRenewalDate = persistedNextRenewal,
                note = state.note.ifBlank { null },
                manageUrl = state.manageUrl.ifBlank { null },
                iconUri = state.iconUri,
                status = state.status
            )

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
            "QUARTERLY" -> BillingCycle.Quarterly
            "YEARLY" -> BillingCycle.Yearly
            "CUSTOM" -> BillingCycle.Custom(state.customDays.toIntOrNull() ?: 30)
            else -> BillingCycle.Monthly
        }
    }
}
