package com.spendlist.app.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, languageCode: String) {
        if (languageCode.isEmpty()) {
            // Follow system
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java)
                    .applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java)
                    .applicationLocales = LocaleList(locale)
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
            }
        }
    }

    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales[0] ?: Locale.getDefault()
        } else {
            AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        }
    }

    fun getCurrentLanguageCode(context: Context): String {
        val locale = getCurrentLocale(context)
        return when (locale.language) {
            "zh" -> "zh-CN"
            "en" -> "en"
            else -> ""
        }
    }
}