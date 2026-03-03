package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.ui.interfaces.OnActionUpdateListener
import com.mor.allocash1.R
import com.mor.allocash1.ui.adapters.TransactionAdapter
import com.mor.allocash1.databinding.FragmentRecentActionsBinding
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for viewing the list of all recent financial transactions.
class RecentActionsFragment : Fragment(R.layout.fragment_recent_actions), OnActionUpdateListener {

    private lateinit var binding: FragmentRecentActionsBinding
    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecentActionsBinding.bind(view)

        setupRecyclerView()
        setupClickListeners()
        onActionUpdated() // Initial sync
    }

    // Initializes the recycler view with transaction data.
    private fun setupRecyclerView() {
        adapter = TransactionAdapter(mutableListOf(), this)
        binding.rvRecentActions.adapter = adapter

        loadRecentActions()
    }

    // Pulls real-time or one-time data from the cloud.
    private fun loadRecentActions() {
        com.mor.allocash1.data.cloud.FireStoreManager.getTransactions(
            onSuccess = { transactionMaps ->
                val transactions = transactionMaps.map { map ->
                    com.mor.allocash1.data.classes.Transaction(
                        title = map["title"] as? String ?: "",
                        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                        category = com.mor.allocash1.data.classes.CategoryType.values().find {
                            it.displayName == map["category"]
                        } ?: com.mor.allocash1.data.classes.CategoryType.OTHER,
                        isExpense = map["isExpense"] as? Boolean ?: true,
                        timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis()
                    )
                }
                // Sync with DB to allow 7-day filtering logic
                ActionDatabase.updateTransactions(transactions)

                // Retrieve and display ONLY the filtered 7-day list
                val filteredRecent = ActionDatabase.getRecentTransactions()
                adapter.updateList(filteredRecent)

                // Manage visibility based on the filtered list
                binding.rvRecentActions.visibility = if (filteredRecent.isEmpty()) View.GONE else View.VISIBLE
                binding.root.findViewById<View>(R.id.layout_empty_recent)?.visibility = if (filteredRecent.isEmpty()) View.VISIBLE else View.GONE
            },
            onFailure = { error ->
                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Configures UI interaction listeners.
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // Refresh the transaction list when data changes.
    override fun onActionUpdated() {
        loadRecentActions()
    }

    override fun onStart() {
        super.onStart()
        // Hide global header for immersive view
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        // Restore header when leaving the screen
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}