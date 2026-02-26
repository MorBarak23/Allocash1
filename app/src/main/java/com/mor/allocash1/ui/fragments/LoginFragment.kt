package com.mor.allocash1.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mor.allocash1.R
import com.mor.allocash1.data.cloud.FireStoreManager
import com.mor.allocash1.ui.activities.MainActivity

// Login UI handling authentication and password recovery links.
class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var lblError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputEmail = view.findViewById<EditText>(R.id.input_login_email)
        val inputPass = view.findViewById<EditText>(R.id.input_login_password)
        lblError = view.findViewById(R.id.lbl_login_error)
        val btnLogin = view.findViewById<Button>(R.id.btn_login_submit)

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val pass = inputPass.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                FireStoreManager.loginUser(email, pass,
                    onSuccess = { navigateToMain() },
                    onFailure = { error ->
                        lblError.visibility = View.VISIBLE
                        lblError.text = "Invalid email or password"
                    }
                )
            }
        }

        setupAuthNavigation(view, inputEmail)
    }

    // Configures forgotten password and registration navigation.
    private fun setupAuthNavigation(view: View, inputEmail: EditText) {
        // Handle Password Reset
        view.findViewById<TextView>(R.id.btn_forgot_password).setOnClickListener {
            val email = inputEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                FireStoreManager.sendPasswordReset(email,
                    onSuccess = { Toast.makeText(context, "Reset email sent!", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            } else {
                Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to Register Fragment
        view.findViewById<TextView>(R.id.btn_go_to_register).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.auth_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun navigateToMain() {
        FireStoreManager.fetchAndSyncUserProfile {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }
}