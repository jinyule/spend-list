package com.spendlist.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }

    val primaryCurrencyCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PRIMARY_CURRENCY] ?: "CNY"
    }

    val reminderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REMINDER_ENABLED] ?: true
    }

    suspend fun setPrimaryCurrency(code: String) {
        dataStore.edit { prefs ->
            prefs[PRIMARY_CURRENCY] = code
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMINDER_ENABLED] = enabled
        }
    }
}
