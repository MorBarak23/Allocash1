package com.mor.allocash1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.App
import com.mor.allocash1.ui.interfaces.OnActionUpdateListener
import com.mor.allocash1.R
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter responsible for displaying the list of recent transactions.
class TransactionAdapter(private var list: List<Transaction>, private val updateListener: OnActionUpdateListener) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.img_action_icon)
        val lblTitle: TextView = view.findViewById(R.id.lbl_action_title)
        val lblCategory: TextView = view.findViewById(R.id.lbl_action_category)
        val lblAmount: TextView = view.findViewById(R.id.lbl_action_amount)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = list[position]

        // Populate the UI with transaction details
        bindTransactionData(holder, item)

        // Handle the edit button click to show options
        holder.btnEdit.setOnClickListener {
            showEditDialog(holder.itemView.context, item, position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_actions, parent, false)
        return TransactionViewHolder(view)
    }

    // Binds the transaction's text, icons, and formatted date to the views.
    private fun bindTransactionData(holder: TransactionViewHolder, item: Transaction) {
        holder.lblTitle.text = item.title
        holder.imgIcon.setImageResource(item.category.iconRes)

        // Format and display the timestamp
        val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.timestamp))
        holder.lblCategory.text = "${item.category.displayName} • $dateStr"

        // Set amount text and color based on expense status
        if (item.isExpense) {
            holder.lblAmount.text = "-${UserData.formatCurrency(item.amount)}"
            holder.lblAmount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        } else {
            holder.lblAmount.text = "+${UserData.formatCurrency(item.amount)}"
            holder.lblAmount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        }
    }

    // Shows the bottom sheet dialog for editing or deleting a transaction.
    private fun showEditDialog(context: android.content.Context, item: Transaction, position: Int) {
        val bottomSheet = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_transaction_menu, null)
        bottomSheet.setContentView(view)

        bottomSheet.setOnDismissListener { (context as? BaseActivity)?.hideSystemUI() }

        // Handle the delete option within the menu
        val btnDelete = view.findViewById<LinearLayout>(R.id.menu_delete_transaction)
        btnDelete.setOnClickListener {
            bottomSheet.dismiss()

            // Remove the specific transaction from Firestore
            com.mor.allocash1.data.cloud.FireStoreManager.deleteTransaction(item) {
                // Trigger UI refresh in the fragment via the listener
                updateListener.onActionUpdated()

                // Note: notifyItemRemoved is not enough; the list must refresh from cloud
            }
        }

        bottomSheet.show()
        App.Companion.applyGlobalUiSettings(bottomSheet.window!!)
    }

    override fun getItemCount(): Int = list.size

    // Refreshes the adapter with a new list of transactions.
    fun updateList(newList: List<Transaction>) {
        this.list = newList
        notifyDataSetChanged()
    }
}