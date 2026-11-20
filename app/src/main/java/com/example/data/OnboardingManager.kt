package com.example.data

import android.content.Context
import android.content.SharedPreferences

class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    fun completeOnboarding() {
        isOnboardingCompleted = true
    }

    companion object {
        private const val PREFS_NAME = "mimu_app_preferences"
        private const val KEY_ONBOARDING_COMPLETED = "has_completed_onboarding"
    }
}
