package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mor.allocash1.ui.adapters.ActionAdapter
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.ui.interfaces.OnActionUpdateListener
import com.mor.allocash1.R
import com.mor.allocash1.data.cloud.FireStoreManager
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.databinding.FragmentHomeBinding

// Main dashboard of Allocash, handling financial summaries and action lists.
class HomeFragment : Fragment(R.layout.fragment_home), OnActionUpdateListener {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var incomeAdapter: ActionAdapter
    private lateinit var expenseAdapter: ActionAdapter
    private lateinit var lblBalance: TextView
    private lateinit var icUser: ImageView
    private lateinit var lblExpense: TextView
    private lateinit var lblIncome: TextView
    private lateinit var lblMonthlySaving: TextView
    private lateinit var icDot: View
    private lateinit var lblTrackStatus: TextView
    private lateinit var icShowBalance: ImageView
    private lateinit var lblBalanceTrend: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        // Initialize UI components and data listeners
        initViews(view)
        injectSavingsGoal(view)
        setupRecyclerViews()
        setupClickListeners()

        updateSummaryCards()
    }

    // Triggered by adapters when a transaction is added/modified.
    override fun onActionUpdated() {
        updateSummaryCards()
    }

    // Fetches and displays current financial totals from the database.
    private fun updateSummaryCards() {
        val safeContext = context ?: return

        val income = ActionDatabase.getTotalIncome()
        val expense = ActionDatabase.getTotalExpense()
        val balance = income - expense

        if (UserData.isBalanceVisible) {
            lblBalance.text = UserData.formatCurrency(balance)
            icShowBalance.setImageResource(R.drawable.ic_eye)
        } else {
            val formatted = UserData.formatCurrency(balance)
            lblBalance.text = "•".repeat(formatted.length)
            icShowBalance.setImageResource(R.drawable.ic_covered)
        }

        lblIncome.text = UserData.formatCurrency(income)
        lblExpense.text = UserData.formatCurrency(expense)

        val savingsRate = if (income > 0) (balance / income) * 100 else 0.0
        val sign = if (savingsRate >= 0) "+" else ""
        lblBalanceTrend.text = "$sign${String.format("%.1f", savingsRate)}% saved this month"

        val trendColor = if (savingsRate >= 0) R.color.card_white else R.color.expense_red
        lblBalanceTrend.setTextColor(androidx.core.content.ContextCompat.getColor(safeContext, trendColor))

        if (::lblMonthlySaving.isInitialized) {
            val isGoalMet = balance >= UserData.monthlySavingsGoal
            icDot.setBackgroundResource(if (isGoalMet) R.drawable.ic_circle_green else R.drawable.ic_circle_red)
            lblTrackStatus.text = if (isGoalMet) "On Track" else "Try Harder"

            val statusColor = if (isGoalMet) R.color.brand_teal else R.color.expense_red
            lblTrackStatus.setTextColor(androidx.core.content.ContextCompat.getColor(safeContext, statusColor))
        }
    }

    // Binds the status dot, text, and edit button
    private fun injectSavingsGoal(view: View) {
        val container = view.findViewById<FrameLayout>(R.id.container_savings_goal)
        val savingsView = LayoutInflater.from(requireContext()).inflate(R.layout.item_savings_goal, container, false)

        lblMonthlySaving = savingsView.findViewById(R.id.lbl_Monthly_saving)
        icDot = savingsView.findViewById(R.id.ic_dot)
        lblTrackStatus = savingsView.findViewById(R.id.lbl_track_status)
        val btnEditSavings = savingsView.findViewById<ImageView>(R.id.btn_edit_savings)

        // Open dialog to update savings goal
        btnEditSavings.setOnClickListener { showEditSavingsGoalDialog() }

        lblMonthlySaving.text = UserData.formatCurrency(UserData.monthlySavingsGoal)
        container.addView(savingsView)
    }

    //Displays a dialog to modify the monthly savings goal amount
    private fun showEditSavingsGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme).setView(dialogView).create()

        // Mapping view components from the existing transaction dialog
        val inputAmount = dialogView.findViewById<android.widget.EditText>(R.id.input_transaction_amount)
        val inputTitle = dialogView.findViewById<android.widget.EditText>(R.id.input_transaction_title)
        val lblDialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.lbl_dialog_title)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btn_confirm_add)

        // Update texts to match the savings goal context
        lblDialogTitle.text = "Enter Savings Goal"
        btnConfirm.text = "Change Now"

        // Customize layout for savings goal entry
        inputTitle.visibility = View.GONE
        inputAmount.hint = "Amount"
        inputAmount.setText(UserData.monthlySavingsGoal.toString())

        btnConfirm.setOnClickListener {
            val newGoal = inputAmount.text.toString().toDoubleOrNull() ?: 500.0
            // Persist the new goal to Firestore
            FireStoreManager.updateMonthlySavingsGoal(newGoal) {
                UserData.monthlySavingsGoal = newGoal
                refreshCurrencyDisplay()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Maps local variables
    private fun initViews(view: View) {
        lblBalance = view.findViewById(R.id.lbl_balance_amount)
        lblExpense = view.findViewById(R.id.lbl_total_expense)
        lblIncome = view.findViewById(R.id.lbl_total_income)
        icUser = requireActivity().findViewById(R.id.ic_user)
        icShowBalance = view.findViewById(R.id.ic_show_balance)
        lblBalanceTrend = view.findViewById(R.id.lbl_balance_trend)

        // Display personalized username
        val userName = view.findViewById<TextView>(R.id.lbl_user_name)
        userName.text = UserData.name
    }

    // Sets up independent adapters and layouts for income and expense lists.
    private fun setupRecyclerViews() {
        // Initialize Income List
        incomeAdapter = ActionAdapter(ActionDatabase.getIncomeActions(), true, this)
        binding.rvIncomes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }

        // Initialize Expense List
        expenseAdapter = ActionAdapter(ActionDatabase.getExpenseActions(), false, this)
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    // Configures navigation click listeners for all interactive dashboard elements.
    private fun setupClickListeners() {
        binding.btnAddExpense.setOnClickListener { navigateTo(AddExpenseFragment()) }
        binding.btnAddIncome.setOnClickListener { navigateTo(AddIncomeFragment()) }
        binding.btnRecentActions.setOnClickListener { navigateTo(RecentActionsFragment()) }
        icUser.setOnClickListener { navigateTo(UserProfileFragment()) }

        // Toggle balance visibility and update the UI
        icShowBalance.setOnClickListener {
            UserData.isBalanceVisible = !UserData.isBalanceVisible
            updateSummaryCards()
        }
    }

    // Centralized helper function to handle fragment transactions with fade animations.
    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            // Added fade animations for consistent navigation experience
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Forces a visual refresh of all currency-based text fields in the view.
    private fun refreshCurrencyDisplay() {
        updateSummaryCards()
        if (::lblMonthlySaving.isInitialized) {
            lblMonthlySaving.text = UserData.formatCurrency(UserData.monthlySavingsGoal)
        }
        incomeAdapter.notifyDataSetChanged()
        expenseAdapter.notifyDataSetChanged()
    }

    // Listens to all actions and handles monthly resets automatically
    private fun fetchDataFromFirebase() {
        // 1. Profile Listener
        FireStoreManager.listenToUserProfile {
            if (isAdded && view != null) { // Essential safety check
                view?.findViewById<TextView>(R.id.lbl_user_name)?.text = UserData.name
                refreshCurrencyDisplay()
            }
        }

        // 2. Actions Listener
        FireStoreManager.listenToAllActions { actions ->
            if (isAdded && view != null) { // Essential safety check
                ActionDatabase.updateActions(actions)

                val incomes = actions.filter { it.category == com.mor.allocash1.data.classes.CategoryType.INCOME }
                val expenses = actions.filter { it.category != com.mor.allocash1.data.classes.CategoryType.INCOME }

                binding.rvIncomes.visibility = if (incomes.isEmpty()) View.GONE else View.VISIBLE
                view?.findViewById<View>(R.id.layout_empty_income)?.visibility = if (incomes.isEmpty()) View.VISIBLE else View.GONE
                incomeAdapter.updateList(incomes)

                binding.rvExpenses.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
                view?.findViewById<View>(R.id.layout_empty_expense)?.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
                expenseAdapter.updateList(expenses)

                updateSummaryCards()
                refreshCurrencyDisplay()
            }
        }

        // 3. Transactions Listener
        FireStoreManager.listenToTransactions { transactions ->
            if (isAdded && view != null) { // Essential safety check
                ActionDatabase.updateTransactions(transactions)
                expenseAdapter.notifyDataSetChanged()
                updateSummaryCards()
            }
        }
    }

    // Ensures the UI remains synchronized when returning from other screens.
    override fun onResume() {
        super.onResume()

        fetchDataFromFirebase()
        view?.findViewById<TextView>(R.id.lbl_user_name)?.text = UserData.name
    }

    override fun onPause() {
        super.onPause()
        FireStoreManager.stopAllListeners() // Prevents background crashes and leaks
    }
}