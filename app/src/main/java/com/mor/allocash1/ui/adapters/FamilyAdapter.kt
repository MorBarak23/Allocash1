package com.mor.allocash1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.data.local.FamilyMember
import com.mor.allocash1.R
import com.mor.allocash1.data.local.UserData

// Adapter modified to include the current user as the first row in the family list.
class FamilyAdapter(private val members: List<FamilyMember>) :
    RecyclerView.Adapter<FamilyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.lbl_member_name)
        val email: TextView = view.findViewById(R.id.lbl_member_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_account, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            // Row 0: Display the current logged-in user's data
            holder.name.text = "${UserData.name} (Me)"
            holder.email.text = UserData.email

        } else {
            // Other rows: Display family members from the list (offset by 1)
            val member = members[position - 1]
            holder.name.text = member.name
            holder.email.text = member.email

            // Reset color for other members to default
            holder.name.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
        }
    }

    // Increase count by 1 to accommodate the current user row.
    override fun getItemCount() = members.size + 1
}