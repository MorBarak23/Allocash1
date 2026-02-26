package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mor.allocash1.R
import com.mor.allocash1.data.cloud.FireStoreManager

// Registration screen featuring O(1) Firestore writes and email verification.
class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputName = view.findViewById<EditText>(R.id.input_reg_name)
        val inputEmail = view.findViewById<EditText>(R.id.input_reg_email)
        val inputPass = view.findViewById<EditText>(R.id.input_reg_password)
        val btnRegister = view.findViewById<Button>(R.id.btn_register_submit)

        // Ensure consistency with MyProfile name length
        inputName.filters = arrayOf(InputFilter.LengthFilter(16))

        btnRegister.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val pass = inputPass.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                FireStoreManager.registerUser(email, pass, name,
                    onSuccess = { sendVerificationAndNotify() },
                    onFailure = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                )
            }
        }
    }

    // Sends the email link and returns user to Login
    private fun sendVerificationAndNotify() {
        FireStoreManager.sendEmailVerification(
            onSuccess = {
                Toast.makeText(context, "Registration successful! Please verify your email.", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            },
            onFailure = { error ->
                Toast.makeText(context, "Account created, but verification failed.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}