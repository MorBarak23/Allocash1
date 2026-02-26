package com.mor.allocash1.data.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("allocash_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SMART_LOGIN = "smart_login_enabled"
    }

    //Saves the biometric login state. In the future, this can be updated to sync with a server.
    fun setSmartLoginEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SMART_LOGIN, enabled) }
    }

    //Returns whether smart login is enabled by the user.
    fun isSmartLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_LOGIN, false)
    }

    // Saves the dark mode preference locally
    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    // Retrieves the saved dark mode preference
    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean("dark_mode_enabled", false)
    }
}