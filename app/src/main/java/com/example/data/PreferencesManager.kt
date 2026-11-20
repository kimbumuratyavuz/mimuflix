package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.ThemeMode

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    var themeMode: ThemeMode
        get() {
            val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            return try {
                ThemeMode.valueOf(saved)
            } catch (e: Exception) {
                ThemeMode.DARK
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) {
            prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
        }

    fun completeOnboarding() {
        isOnboardingCompleted = true
    }

    companion object {
        private const val PREFS_NAME = "mimu_app_preferences"
        private const val KEY_ONBOARDING_COMPLETED = "has_completed_onboarding"
        private const val KEY_THEME_MODE = "saved_theme_mode"
        private const val KEY_KEEP_SCREEN_ON = "saved_keep_screen_on"
    }
}
