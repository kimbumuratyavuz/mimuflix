package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OnboardingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MimuFlix", appName)
    }

    @Test
    fun `onboarding manager persists completion state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OnboardingManager(context)
        assertFalse(manager.isOnboardingCompleted)

        manager.completeOnboarding()
        assertTrue(manager.isOnboardingCompleted)

        val manager2 = OnboardingManager(context)
        assertTrue(manager2.isOnboardingCompleted)
    }

    @Test
    fun `preferences manager persists theme mode and keep screen on`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.PreferencesManager(context)

        // Default theme is dark
        assertEquals(com.example.ui.ThemeMode.DARK, prefs.themeMode)

        // Change to light theme
        prefs.themeMode = com.example.ui.ThemeMode.LIGHT
        assertEquals(com.example.ui.ThemeMode.LIGHT, prefs.themeMode)

        // Change to system theme
        prefs.themeMode = com.example.ui.ThemeMode.SYSTEM
        assertEquals(com.example.ui.ThemeMode.SYSTEM, prefs.themeMode)

        // Keep screen on preference
        prefs.keepScreenOn = false
        assertFalse(prefs.keepScreenOn)
        prefs.keepScreenOn = true
        assertTrue(prefs.keepScreenOn)
    }
}
