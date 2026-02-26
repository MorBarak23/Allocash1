package com.mor.allocash1.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.ui.adapters.FamilyAdapter
import com.mor.allocash1.R
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment responsible for managing family account members and settings.
class AccountsFragment : Fragment(R.layout.fragment_accounts) {

    private lateinit var familyAdapter: FamilyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvMembers = view.findViewById<RecyclerView>(R.id.rv_family_members)
        rvMembers.layoutManager = LinearLayoutManager(context)

        familyAdapter = FamilyAdapter(UserData.getFamilyMembers())
        rvMembers.adapter = FamilyAdapter(UserData.getFamilyMembers())

        setupNavigation(view)
        loadFamilyData()
    }

    // Configures back navigation and navigation to add user screen.
    private fun setupNavigation(view: View) {
        view.findViewById<ImageView>(R.id.btn_back_accounts).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.btn_add_user_account).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_fragment_container, AddUserFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btn_remove_my_account).setOnClickListener {
            showRemoveConfirmationDialog()
        }
    }

    // Fetches the latest family members from Firestore and updates UI.
    private fun loadFamilyData() {
        com.mor.allocash1.data.cloud.FireStoreManager.fetchFamilyMembers { members ->
            // Update the singleton and notify the adapter
            UserData.updateFamilyMembers(members)
            familyAdapter.notifyDataSetChanged()
        }
    }

    // Displays a confirmation prompt before removing the user from the family account.
    private fun showRemoveConfirmationDialog() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("Remove Account")
            .setMessage("Are you sure you want to leave this family account?")
            .setPositiveButton("Remove") { _, _ ->
                performRemoval()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Background now follows the app's dark mode colors
        dialog.window?.let { window ->
            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 48f
                setColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.card_white))
            }
            window.setBackgroundDrawable(bg)

            val metrics = resources.displayMetrics
            window.setLayout((metrics.widthPixels * 0.85).toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Ensuring buttons are visible and color-coded correctly
        val removeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red)
        val cancelColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(removeColor)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(cancelColor)
    }

    // Logic to handle account removal (To be implemented with Server).
    private fun performRemoval() {
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: return

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(email)
            .update("familyId", null)
            .addOnSuccessListener {
                android.widget.Toast.makeText(context, "You have left the family account", android.widget.Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(context, "Failed to leave: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        // Hide global header for full-screen aesthetic
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        // Restore header when leaving fragment
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}