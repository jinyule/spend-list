package com.spendlist.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendlist.app.data.datastore.UserPreferences
import com.spendlist.app.domain.model.Currency
import com.spendlist.app.domain.repository.CurrencyRepository
import com.spendlist.app.domain.usecase.export.ExportDataUseCase
import com.spendlist.app.domain.usecase.export.ImportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val primaryCurrency: Currency = Currency.CNY,
    val reminderEnabled: Boolean = true,
    val themeMode: Int = 0, // 0=System, 1=Light, 2=Dark
    val reminderDays: Set<Int> = setOf(3, 1, 0), // days before renewal
    val isSyncingRates: Boolean = false,
    val rateSyncMessage: String? = null,
    val exportedData: String? = null,
    val importMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val currencyRepository: CurrencyRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.primaryCurrencyCode.collect { code ->
                val currency = Currency.fromCode(code) ?: Currency.CNY
                _uiState.value = _uiState.value.copy(primaryCurrency = currency)
            }
        }
        viewModelScope.launch {
            userPreferences.reminderEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            userPreferences.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            userPreferences.reminderDays.collect { days ->
                _uiState.value = _uiState.value.copy(reminderDays = days)
            }
        }
    }

    fun onPrimaryCurrencyChanged(currency: Currency) {
        viewModelScope.launch {
            userPreferences.setPrimaryCurrency(currency.code)
        }
    }

    fun onReminderEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setReminderEnabled(enabled)
        }
    }

    fun onThemeModeChanged(mode: Int) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun onReminderDaysChanged(days: Set<Int>) {
        viewModelScope.launch {
            userPreferences.setReminderDays(days)
        }
    }

    fun onSyncRates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingRates = true, rateSyncMessage = null)
            try {
                currencyRepository.fetchAndCacheRates(_uiState.value.primaryCurrency.code)
                _uiState.value = _uiState.value.copy(
                    isSyncingRates = false,
                    rateSyncMessage = "success"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncingRates = false,
                    rateSyncMessage = e.message ?: "error"
                )
            }
        }
    }

    fun onExportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = exportDataUseCase.exportJson()
            onResult(data)
        }
    }

    fun onExportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val data = exportDataUseCase.exportCsv()
            onResult(data)
        }
    }

    fun onImportJson(jsonString: String) {
        viewModelScope.launch {
            when (val result = importDataUseCase.importJson(jsonString)) {
                is ImportDataUseCase.Result.Success ->
                    _uiState.value = _uiState.value.copy(importMessage = "import_success:${result.count}")
                is ImportDataUseCase.Result.Error ->
                    _uiState.value = _uiState.value.copy(importMessage = "import_error:${result.message}")
            }
        }
    }

    fun onImportCsv(csvString: String) {
        viewModelScope.launch {
            when (val result = importDataUseCase.importCsv(csvString)) {
                is ImportDataUseCase.Result.Success ->
                    _uiState.value = _uiState.value.copy(importMessage = "import_success:${result.count}")
                is ImportDataUseCase.Result.Error ->
                    _uiState.value = _uiState.value.copy(importMessage = "import_error:${result.message}")
            }
        }
    }

    fun onClearMessage() {
        _uiState.value = _uiState.value.copy(rateSyncMessage = null, importMessage = null)
    }
}
