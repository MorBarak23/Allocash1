package com.mor.allocash1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.R
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.data.local.UserData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter for displaying a detailed history of transactions.
class HistoryAdapter(private var transactions: List<Transaction>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lblDayName: TextView = view.findViewById(R.id.lbl_day_name)
        val lblDayNumber: TextView = view.findViewById(R.id.lbl_day_number)
        val imgIcon: ImageView = view.findViewById(R.id.img_category_icon)
        val lblTitle: TextView = view.findViewById(R.id.lbl_transaction_title)
        val lblCategory: TextView = view.findViewById(R.id.lbl_transaction_category)
        val lblAmount: TextView = view.findViewById(R.id.lbl_transaction_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_action_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = transactions[position]

        // Bind different logical sections of the transaction item
        bindDateDetails(holder, item)
        bindTransactionContent(holder, item)
        bindAmountVisuals(holder, item)
    }

    // Formats and binds the day name and day number to the view.
    private fun bindDateDetails(holder: HistoryViewHolder, item: Transaction) {
        val date = Date(item.timestamp)
        holder.lblDayName.text = SimpleDateFormat("EEE", Locale.US).format(date).uppercase()
        holder.lblDayNumber.text = SimpleDateFormat("dd", Locale.US).format(date)
    }

    // Maps the title, category name, and icon to the view holder.
    private fun bindTransactionContent(holder: HistoryViewHolder, item: Transaction) {
        holder.lblTitle.text = item.title
        holder.lblCategory.text = item.category.displayName
        holder.imgIcon.setImageResource(item.category.iconRes)
    }

    // Styles the amount text and color based on whether it is an expense or income.
    private fun bindAmountVisuals(holder: HistoryViewHolder, item: Transaction) {
        val context = holder.itemView.context

        // Dynamic amount formatting and color coding
        if (item.isExpense) {
            holder.lblAmount.text = "-${UserData.formatCurrency(item.amount)}"
            holder.lblAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
        } else {
            holder.lblAmount.text = "+${UserData.formatCurrency(item.amount)}"
            holder.lblAmount.setTextColor(ContextCompat.getColor(context, R.color.brand_teal))
        }
    }

    override fun getItemCount() = transactions.size

    // Updates the transaction list and notifies the adapter of the data change.
    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }
}