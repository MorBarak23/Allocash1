package com.mor.allocash1

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Main Application class for global initializations and UI configuration.
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply the saved theme immediately on app startup to prevent flickering
        val settingsRepository = com.mor.allocash1.data.repositories.SettingsRepository(this)
        val mode = if (settingsRepository.isDarkModeEnabled()) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)

        // Setup automatic UI configuration for every activity created
        registerGlobalLifecycleCallbacks()
    }

    // Registers callbacks to apply global UI settings whenever an activity is created.
    private fun registerGlobalLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applyGlobalUiSettings(activity.window)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        // Configures the window for edge-to-edge display and hides navigation bars.
        fun applyGlobalUiSettings(window: Window) {
            // Enable edge-to-edge content rendering
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Disable contrast enforcement for a cleaner immersive look on Android Q+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            setupSystemBarsController(window)
        }

        // Manages system bar visibility and interaction behavior.
        private fun setupSystemBarsController(window: Window) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)

            // Hide the system navigation bars globally
            controller.hide(WindowInsetsCompat.Type.navigationBars())

            // Configure bars to reappear temporarily on swipe
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}