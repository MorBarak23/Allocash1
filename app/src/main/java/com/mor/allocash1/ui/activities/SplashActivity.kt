package com.mor.allocash1.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mor.allocash1.utils.BiometricHelper
import com.mor.allocash1.R
import com.mor.allocash1.data.cloud.FireStoreManager
import com.mor.allocash1.data.repositories.SettingsRepository

// Screen that manages app entry logic, including timing delays and security verification.
class SplashActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        settingsRepository = SettingsRepository(this)
        biometricHelper = BiometricHelper(this)

        //Wait for 1.5 seconds to show the splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 1500)
    }

    // Decisions on where to send the user based on cloud and local status.
    private fun checkAuthAndNavigate() {
        if (FireStoreManager.isUserFullyVerified()) {
            // Sync user data from cloud before performing security checks
            FireStoreManager.fetchAndSyncUserProfile {
                handleSecurityCheck()
            }
        } else {
            // User not logged in - navigate to authentication flow
            navigateToAuth()
        }
    }

    // Handles biometric verification logic if the user has enabled it.
    private fun handleSecurityCheck() {
        val isBiometricEnabled = settingsRepository.isSmartLoginEnabled()

        if (isBiometricEnabled && biometricHelper.canAuthenticate()) {
            // Request biometric ID before proceeding to home
            biometricHelper.showBiometricPrompt(
                title = "Allocash Secure Login",
                subtitle = "Please authenticate",
                onSuccess = { startMainActivity() },
                onError = { _, _ ->
                    // Fail-safe: Go to main activity
                    startMainActivity()
                }
            )
        } else {
            // No biometric enabled or supported, go straight to home
            startMainActivity()
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startMainActivity() {
        // Crucial: Tell MainActivity that biometric was already handled here
        FireStoreManager.isManualLoginSession = true
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}