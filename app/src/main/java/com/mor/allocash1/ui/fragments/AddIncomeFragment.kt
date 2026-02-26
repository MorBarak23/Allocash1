package com.mor.allocash1.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.mor.allocash1.data.classes.Action
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.data.classes.CategoryType
import com.mor.allocash1.R
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.databinding.FragmentAddIncomeBinding
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for creating new income streams and tracking incoming funds.
class AddIncomeFragment : Fragment(R.layout.fragment_add_income) {

    private lateinit var binding: FragmentAddIncomeBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddIncomeBinding.bind(view)

        // Set 14-character limit to the income title input
        binding.inputIncomeTitle.filters = arrayOf(InputFilter.LengthFilter(14))

        setupButtons()
        setupKeyboardHiding()
    }

    // Initializes click listeners for UI interaction.
    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSaveIncome.setOnClickListener {
            attemptSaveIncome()
        }
    }

    // Validates the input fields before saving a new income action.
    private fun attemptSaveIncome() {
        val title = binding.inputIncomeTitle.text.toString()
        val amount = binding.inputIncomeAmount.text.toString().toDoubleOrNull() ?: 0.0

        // Ensure title is present and amount is valid
        if (title.isNotEmpty() && amount > 0) {
            performSaveIncome(title, amount)
        }
    }

    // Executes the database save for both the Action and initial Transaction.
    private fun performSaveIncome(title: String, amount: Double) {
        // Cloud sync for income using the manager
        com.mor.allocash1.data.cloud.FireStoreManager.saveFinancialAction(
            title = title,
            amount = amount,
            category = "Income",
            isExpense = false, // This is an income action
            onSuccess = {
                // Return to previous screen only after cloud confirmation
                parentFragmentManager.popBackStack()
            },
            onFailure = { error ->
                // Display clear error if cloud sync fails
                android.widget.Toast.makeText(context, "Cloud sync failed: $error", android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    // Configures UI behavior to hide the keyboard on background clicks.
    private fun setupKeyboardHiding() {
        binding.root.setOnClickListener { hideKeyboard() }
    }

    // Helper function to dismiss the soft keyboard.
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        (activity as? BaseActivity)?.hideSystemUI()
    }

    override fun onStart() {
        super.onStart()
        // Hide global header for full-screen aesthetic
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        // Restore header visibility on exit
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}