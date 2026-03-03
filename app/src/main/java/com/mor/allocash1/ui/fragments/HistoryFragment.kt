package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.ui.adapters.HistoryAdapter
import com.mor.allocash1.R
import com.mor.allocash1.data.repositories.TransactionRepository
import com.mor.allocash1.data.local.UserData
import java.util.Calendar

// Fragment for viewing and filtering transaction history by month and year.
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var lblSelectedDate: TextView
    private lateinit var lblTotalIncome: TextView
    private lateinit var lblTotalExpense: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvHistory: RecyclerView

    private val months = arrayOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    // Default selection initialized to current system date
    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSpinners(view)
        loadData() // Initial load for current period
    }

    // Connects class variables to the XML layout components.
    private fun initViews(view: View) {
        lblSelectedDate = view.findViewById(R.id.lbl_selected_date)
        lblTotalIncome = view.findViewById(R.id.lbl_total_income)
        lblTotalExpense = view.findViewById(R.id.lbl_total_expense)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        rvHistory = view.findViewById(R.id.rv_history)
    }

    // Configures the RecyclerView with the history adapter.
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList())
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter
    }

    // Orchestrates the setup of both month and year selection menus.
    private fun setupSpinners(view: View) {
        setupMonthSpinner(view)
        setupYearSpinner(view)
    }

    // Configures the month dropdown with names and selection logic.
    private fun setupMonthSpinner(view: View) {
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, months)
        val spinnerMonth = view.findViewById<AutoCompleteTextView>(R.id.spinner_month)

        spinnerMonth.setAdapter(monthAdapter)
        spinnerMonth.setText(months[selectedMonth], false)
        spinnerMonth.setOnItemClickListener { _, _, position, _ ->
            selectedMonth = position
            loadData()
        }
    }

    // Configures the year dropdown with a 4-year historical range.
    private fun setupYearSpinner(view: View) {
        val years = (selectedYear downTo selectedYear - 4).map { it.toString() }.toTypedArray()
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years)
        val spinnerYear = view.findViewById<AutoCompleteTextView>(R.id.spinner_year)

        spinnerYear.setAdapter(yearAdapter)
        spinnerYear.setText(selectedYear.toString(), false)
        spinnerYear.setOnItemClickListener { _, _, position, _ ->
            selectedYear = years[position].toInt()
            loadData()
        }
    }

    // Main data loading function that updates UI and handles empty states.
    private fun loadData() {
        lblSelectedDate.text = "${months[selectedMonth]} $selectedYear"

        // Call FireStoreManager to get actions filtered by month and year
        com.mor.allocash1.data.cloud.FireStoreManager.getActionsByPeriod(selectedMonth, selectedYear,
            onSuccess = { actions ->
                // Process the list and update summary
                updateFinancialSummary(actions)
                handleDataVisibility(actions)
            },
            onFailure = { error ->
                android.widget.Toast.makeText(context, "Failed to load history: $error", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Calculates and displays the income and expense totals for the month.
    private fun updateFinancialSummary(filteredList: List<com.mor.allocash1.data.classes.Transaction>) {
        val (income, expense) = TransactionRepository.getMonthlyTotals(filteredList)

        // Formatting with UserData settings
        lblTotalIncome.text = "+${UserData.formatCurrency(income)}"
        lblTotalExpense.text = "-${UserData.formatCurrency(expense)}"
    }

    // Toggles between the transaction list and the empty state view.
    private fun handleDataVisibility(filteredList: List<com.mor.allocash1.data.classes.Transaction>) {
        if (filteredList.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            historyAdapter.updateList(filteredList)
        }
    }
}