package com.spendlist.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val THEME_MODE = intPreferencesKey("theme_mode") // 0=System, 1=Light, 2=Dark
        val REMINDER_DAYS = stringSetPreferencesKey("reminder_days") // {"3", "1", "0"}
        val LANGUAGE_CODE = stringPreferencesKey("language_code") // "" = System, "en", "zh-CN"
    }

    val primaryCurrencyCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PRIMARY_CURRENCY] ?: "CNY"
    }

    val reminderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REMINDER_ENABLED] ?: true
    }

    val themeMode: Flow<Int> = dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: 0
    }

    val reminderDays: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs[REMINDER_DAYS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(3, 1, 0)
    }

    val languageCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[LANGUAGE_CODE] ?: ""
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

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun setReminderDays(days: Set<Int>) {
        dataStore.edit { prefs ->
            prefs[REMINDER_DAYS] = days.map { it.toString() }.toSet()
        }
    }

    suspend fun setLanguageCode(code: String) {
        dataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = code
        }
    }
}
