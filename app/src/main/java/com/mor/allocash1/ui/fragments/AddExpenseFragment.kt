package com.mor.allocash1.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.mor.allocash1.data.classes.Action
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.data.classes.CategoryType
import com.mor.allocash1.R
import com.mor.allocash1.databinding.FragmentAddExpenseBinding
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for creating a new monthly expense goal/budget.
class AddExpenseFragment : Fragment(R.layout.fragment_add_expense) {

    private lateinit var binding: FragmentAddExpenseBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddExpenseBinding.bind(view)

        // Set 14-character limit to the title input
        binding.inputTitle.filters = arrayOf(InputFilter.LengthFilter(14))

        setupCategoryDropdown()
        setupButtons()
    }

    // Configures the category selection dropdown and keyboard behavior.
    private fun setupCategoryDropdown() {
        val categories = CategoryType.values().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        binding.inputCategory.setAdapter(adapter)

        // Hide keyboard when interacting with the dropdown
        binding.inputCategory.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) hideKeyboard() }
        binding.inputCategory.setOnClickListener { hideKeyboard() }
    }

    // Hides the software keyboard manually.
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        (activity as? BaseActivity)?.hideSystemUI()
    }

    // Initializes click listeners for UI buttons.
    private fun setupButtons() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { attemptSave() }
    }

    // Validates inputs before proceeding with the save operation.
    private fun attemptSave() {
        val title = binding.inputTitle.text.toString()
        val categoryName = binding.inputCategory.text.toString()
        val budgetLimit = binding.inputBudget.text.toString().toDoubleOrNull() ?: 0.0

        // Validation logic for creating a new action
        if (title.isNotEmpty() && categoryName.isNotEmpty() && budgetLimit > 0) {
            performSave(title, categoryName, budgetLimit)
        }
    }

    // Creates the Action object and saves it to the local database.
    private fun performSave(title: String, categoryName: String, budget: Double) {
        com.mor.allocash1.data.cloud.FireStoreManager.saveFinancialAction(
            title = title,
            amount = budget,
            category = categoryName,
            isExpense = true,
            onSuccess = {
                parentFragmentManager.popBackStack()
            },
            onFailure = { error ->
                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
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