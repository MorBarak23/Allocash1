package com.mor.allocash1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.data.classes.ProfileOption
import com.mor.allocash1.R

// Adapter for displaying various profile management options.
class ProfileOptionAdapter(
    private val options: List<ProfileOption>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ProfileOptionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.img_option_icon)
        val title: TextView = view.findViewById(R.id.lbl_option_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]

        // Bind data and setup listeners
        bindOptionData(holder, option)
        setupItemClickListener(holder, option)
    }

    // Maps the option's text and icon to the view holder views.
    private fun bindOptionData(holder: ViewHolder, option: ProfileOption) {
        holder.title.text = option.title
        holder.icon.setImageResource(option.iconRes)
    }

    // Sets up the click listener to return the selected option title to the fragment.
    private fun setupItemClickListener(holder: ViewHolder, option: ProfileOption) {
        holder.itemView.setOnClickListener {
            onItemClick(option.title)
        }
    }

    override fun getItemCount() = options.size
}