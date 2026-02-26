package com.mor.allocash1.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mor.allocash1.App
import com.mor.allocash1.R
import com.mor.allocash1.ui.fragments.LoginFragment

// Activity handling the authentication flow (Login/Register).
class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Hide Galaxy navigation bars
        App.applyGlobalUiSettings(window)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }
}