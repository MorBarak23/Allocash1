package com.mor.allocash1.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mor.allocash1.data.classes.Action
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.App
import com.mor.allocash1.ui.interfaces.OnActionUpdateListener
import com.mor.allocash1.R
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity
import com.mor.allocash1.ui.fragments.RecentActionsFragment

// Adapter managing the list of saving goals, expenses, or income categories.
class ActionAdapter(
    private var actionList: List<Action>,
    private val isIncomeAdapter: Boolean = false,
    private val updateListener: OnActionUpdateListener
) : RecyclerView.Adapter<ActionAdapter.ActionViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    class ActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lblTitle: TextView = view.findViewById(R.id.lbl_title)
        val lblSummary: TextView = view.findViewById(R.id.lbl_summary)
        val pbMonthly: ProgressBar = view.findViewById(R.id.pb_monthly)
        val lblPercent: TextView = view.findViewById(R.id.lbl_percent)
        val icCategory: ImageView = view.findViewById(R.id.ic_category)
        val btnPlus: ImageView = view.findViewById(R.id.btn_plus)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit)
        val layoutWeekly: LinearLayout = view.findViewById(R.id.layout_weekly_details)
        val icRightArrow: ImageView = view.findViewById(R.id.ic_right_arrow)
        val layoutWeeklyContainer: LinearLayout = view.findViewById(R.id.layout_weekly_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_action, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val action = actionList[position]
        bindViewContent(holder, action, position)
        setupClickListeners(holder, action, position)
    }

    // Binds visuals and determines if the item should show expanded details.
    private fun bindViewContent(holder: ActionViewHolder, action: Action, position: Int) {
        val isExpanded = expandedPositions.contains(position)
        holder.icCategory.setImageResource(if (isIncomeAdapter) R.drawable.btn_income else action.category.iconRes)
        holder.lblTitle.text = action.title

        if (isIncomeAdapter) bindIncomeUI(holder, action) else bindExpenseUI(holder, action)

        holder.layoutWeeklyContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.icRightArrow.rotation = if (isExpanded) 90f else 0f
        if (isExpanded && !isIncomeAdapter) populateWeeklyDetails(holder, action)
    }

    // Sets totals for income actions.
    private fun bindIncomeUI(holder: ActionViewHolder, action: Action) {
        holder.lblSummary.text = "${UserData.formatCurrency(action.currentAmount)} total received"
        holder.lblPercent.visibility = View.GONE
        holder.pbMonthly.progress = 100
        holder.layoutWeekly.visibility = View.GONE
    }

    // Sets progress and colors for expense actions.
    private fun bindExpenseUI(holder: ActionViewHolder, action: Action) {
        holder.lblSummary.text = "${UserData.formatCurrency(action.currentAmount)} spent of ${UserData.formatCurrency(action.limit)}"
        holder.lblPercent.visibility = View.VISIBLE
        holder.layoutWeekly.visibility = View.VISIBLE

        val progress = if (action.limit > 0) ((action.currentAmount / action.limit) * 100).toInt() else 0
        holder.pbMonthly.progress = progress
        holder.lblPercent.text = "$progress%"

        val color = if (progress >= 100) R.color.expense_red else R.color.brand_teal
        holder.pbMonthly.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, color))
    }

    // Initializes interaction listeners for the action item.
    private fun setupClickListeners(holder: ActionViewHolder, action: Action, position: Int) {
        holder.btnPlus.setOnClickListener { showAddTransactionDialog(action, position, holder.itemView.context, isIncomeAdapter) }
        holder.btnEdit.setOnClickListener { showEditMenu(action, position, holder.itemView.context) }
        holder.layoutWeekly.setOnClickListener { toggleExpansion(position, action) }
    }

    // Displays the rename dialog.
    private fun showRenameDialog(action: Action, position: Int, context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme).setView(dialogView).create()

        dialog.setOnDismissListener { (context as? BaseActivity)?.hideSystemUI() }
        dialog.show()

        val inputTitle = dialogView.findViewById<EditText>(R.id.input_rename_title)
        inputTitle.filters = arrayOf(InputFilter.LengthFilter(14))
        inputTitle.setText(action.title)
        inputTitle.selectAll()

        dialogView.findViewById<Button>(R.id.btn_confirm_rename).setOnClickListener {
            val newName = inputTitle.text.toString()
            if (newName.isNotEmpty()) {
                // Call FireStoreManager to update the action name in the cloud
                com.mor.allocash1.data.cloud.FireStoreManager.updateActionName(action.title, newName) {
                    action.title = newName
                    notifyItemChanged(position)
                    updateListener.onActionUpdated()
                    dialog.dismiss()
                }
            }
        }
        App.Companion.applyGlobalUiSettings(dialog.window!!)
    }

    // Adds a specific transaction to the action and updates cloud data
    private fun showAddTransactionDialog(action: Action, position: Int, context: Context, isIncome: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme).setView(dialogView).create()

        dialog.setOnDismissListener { (context as? BaseActivity)?.hideSystemUI() }
        dialog.show()

        val inputTitle = dialogView.findViewById<EditText>(R.id.input_transaction_title)
        inputTitle.filters = arrayOf(InputFilter.LengthFilter(14))
        val inputAmount = dialogView.findViewById<EditText>(R.id.input_transaction_amount)

        dialogView.findViewById<Button>(R.id.btn_confirm_add).setOnClickListener {
            val title = inputTitle.text.toString()
            val amount = inputAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (title.isNotEmpty() && amount > 0) {
                // Updated: Now passes action.category.displayName to save it in the cloud
                com.mor.allocash1.data.cloud.FireStoreManager.addTransactionAndUpdateAction(
                    actionTitle = action.title,
                    transactionTitle = title,
                    amount = amount,
                    category = action.category.displayName, // Pass the category string
                    isExpense = !isIncome
                ) {
                    dialog.dismiss()
                    (context as? BaseActivity)?.hideSystemUI()
                }
            }
        }
    }

    // Handles the toggling of the weekly breakdown view.
    private fun toggleExpansion(position: Int, action: Action) {
        if (expandedPositions.contains(position)) expandedPositions.remove(position)
        else {
            ActionDatabase.updateWeeklyBreakdown(action)
            expandedPositions.add(position)
        }
        notifyItemChanged(position)
    }

    // Handles budget modification options: renaming, deleting, or navigating to recent transactions for cancellation.
    private fun showEditMenu(action: Action, position: Int, context: Context) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_menu, null)
        bottomSheetDialog.setContentView(view)

        bottomSheetDialog.setOnDismissListener { (context as? BaseActivity)?.hideSystemUI() }

        // Logic for renaming the budget category
        view.findViewById<LinearLayout>(R.id.menu_rename).setOnClickListener {
            bottomSheetDialog.dismiss()
            showRenameDialog(action, position, context)
        }

        // Logic for redefine the budget
        val menuEditAmount = view.findViewById<LinearLayout>(R.id.menu_edit_amount)
        if (isIncomeAdapter) {
            menuEditAmount.visibility = View.GONE
        } else {
            menuEditAmount.visibility = View.VISIBLE
            menuEditAmount.setOnClickListener {
                bottomSheetDialog.dismiss()
                showEditAmountDialog(action, position, context)
            }
        }

        // Navigates specifically to RecentActionsFragment so the user can select a transaction to cancel.
        view.findViewById<LinearLayout>(R.id.menu_cancel_transaction).setOnClickListener {
            bottomSheetDialog.dismiss()

            // Use FragmentManager to replace the current view with the Recent Actions screen
            val fragment = RecentActionsFragment()
            (context as? FragmentActivity)?.supportFragmentManager?.beginTransaction()
                ?.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                ?.replace(R.id.main_fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }

        // Logic for deleting the entire category and its associated data
        view.findViewById<LinearLayout>(R.id.menu_delete).setOnClickListener {
            bottomSheetDialog.dismiss()
            // Remove the entire action and its history from the cloud
            com.mor.allocash1.data.cloud.FireStoreManager.deleteAction(action.title) {
                // HomeFragment listener will automatically remove the item from list
            }
        }

        bottomSheetDialog.show()
        App.Companion.applyGlobalUiSettings(bottomSheetDialog.window!!)
    }

    // Displays a dialog to modify the monthly budget limit
    private fun showEditAmountDialog(action: Action, position: Int, context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme).setView(dialogView).create()

        val inputAmount = dialogView.findViewById<EditText>(R.id.input_transaction_amount)
        val inputTitle = dialogView.findViewById<EditText>(R.id.input_transaction_title)
        val lblDialogTitle = dialogView.findViewById<TextView>(R.id.lbl_dialog_title)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_add)

        lblDialogTitle.text = "Edit Monthly Budget"
        btnConfirm.text = "Update"
        inputTitle.visibility = View.GONE
        inputAmount.setText(action.limit.toString())

        btnConfirm.setOnClickListener {
            val newLimit = inputAmount.text.toString().toDoubleOrNull() ?: action.limit
            com.mor.allocash1.data.cloud.FireStoreManager.updateActionLimit(action.title, newLimit) {
                action.limit = newLimit
                // Recalculate weekly slices because the budget-per-day has changed
                ActionDatabase.updateWeeklyBreakdown(action)
                notifyItemChanged(position)
                updateListener.onActionUpdated()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Populates the weekly breakdown details dynamically.
    private fun populateWeeklyDetails(holder: ActionViewHolder, action: Action) {
        holder.layoutWeeklyContainer.removeAllViews()
        action.weeklyDetails.forEach { detail ->
            val row = LayoutInflater.from(holder.itemView.context).inflate(R.layout.item_weekly, holder.layoutWeeklyContainer, false)
            row.findViewById<TextView>(R.id.lbl_week_name).text = "Week ${detail.weekNumber}"
            row.findViewById<TextView>(R.id.lbl_week_status).text = "${UserData.formatCurrency(detail.spent)} / ${UserData.formatCurrency(detail.total)}"

            val pbWeekly = row.findViewById<ProgressBar>(R.id.pb_weekly)
            pbWeekly.progress = if (detail.total > 0) ((detail.spent / detail.total) * 100).toInt() else 0
            pbWeekly.progressDrawable = ContextCompat.getDrawable(holder.itemView.context, if (detail.spent > detail.total) R.drawable.pg_red else R.drawable.bg_progress_gradient_thick)
            holder.layoutWeeklyContainer.addView(row)
        }
    }

    fun updateList(newList: List<Action>) {
        this.actionList = newList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = actionList.size
}