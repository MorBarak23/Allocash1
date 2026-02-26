package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mor.allocash1.R
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for inviting new family member via email.
class AddUserFragment : Fragment(R.layout.fragment_add_user) {

    private lateinit var layoutEmailInput: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var btnSendInvite: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupNavigation(view)
    }

    // Maps local variables to XML layout views.
    private fun initViews(view: View) {
        layoutEmailInput = view.findViewById(R.id.layout_email_input)
        inputEmail = view.findViewById(R.id.input_invite_email)
        btnSendInvite = view.findViewById(R.id.btn_send_invite)
    }

    // Sets up button listeners for back navigation and sending invites.
    private fun setupNavigation(view: View) {
        view.findViewById<ImageView>(R.id.btn_back_add_user).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSendInvite.setOnClickListener {
            validateAndAttemptInvite()
        }
    }

    // Validates the email format before proceeding with the invitation.
    private fun validateAndAttemptInvite() {
        layoutEmailInput.error = null // Clear previous errors
        val email = inputEmail.text.toString().trim()

        if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            finalizeInvitation(email)
        } else {
            layoutEmailInput.error = "Please enter a valid email address"
        }
    }

    // Handles the final invitation logic and user feedback.
    private fun finalizeInvitation(email: String) {
        // Perform real cloud invitation
        com.mor.allocash1.data.cloud.FireStoreManager.inviteUserToFamily(email,
            onSuccess = {
                Toast.makeText(context, "Invitation sent to $email", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            },
            onFailure = { error ->
                Toast.makeText(context, "Invite failed: $error", Toast.LENGTH_LONG).show()
            }
        )
    }



    override fun onStart() {
        super.onStart()
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}