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

// Registration screen performing Firestore writes and email verification.
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPass: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnBack: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)
        setupListeners()
    }

    private fun initUI(view: View) {
        inputName = view.findViewById(R.id.input_reg_name)
        inputEmail = view.findViewById(R.id.input_reg_email)
        inputPass = view.findViewById(R.id.input_reg_password)
        btnRegister = view.findViewById(R.id.btn_register_submit)
        btnBack = view.findViewById(R.id.btn_reg_back)

        // Ensure consistency with MyProfile name length
        inputName.filters = arrayOf(InputFilter.LengthFilter(16))
    }

    private fun setupListeners() {
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

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
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