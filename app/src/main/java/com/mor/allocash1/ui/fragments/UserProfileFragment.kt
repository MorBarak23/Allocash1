package com.mor.allocash1.ui.fragments

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mor.allocash1.data.classes.ProfileOption
import com.mor.allocash1.ui.adapters.ProfileOptionAdapter
import com.mor.allocash1.R
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment providing a list of profile-related settings and info.
class UserProfileFragment : Fragment(R.layout.fragment_user_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Remove system padding for full-screen aesthetic
        requireActivity().findViewById<View>(R.id.main_fragment_container)?.setPadding(0, 0, 0, 0)

        setupNavigation(view)
        setupOptionsList(view)
    }

    // Configures the back button listener.
    private fun setupNavigation(view: View) {
        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // Initializes the list of profile options and its adapter.
    private fun setupOptionsList(view: View) {
        val profileOptions = listOf(
            ProfileOption(1, "My Profile", R.drawable.ic_user3),
            ProfileOption(2, "Accounts", R.drawable.ic_list),
            ProfileOption(3, "About Us", R.drawable.ic_about),
            ProfileOption(4, "Log Out", R.drawable.ic_logout2)
        )

        val rvOptions = view.findViewById<RecyclerView>(R.id.rv_profile_options)
        rvOptions.layoutManager = LinearLayoutManager(requireContext())
        rvOptions.adapter = ProfileOptionAdapter(profileOptions) { title ->
            handleOptionClick(title)
        }
    }

    // Logic for navigating based on selected profile item.
    private fun handleOptionClick(title: String) {
        when (title) {
            "My Profile" -> navigateWithFade(MyProfileFragment())
            "Accounts" -> navigateWithFade(AccountsFragment()) // Unified with fade animation
            "About Us" -> showAboutDialog()
            "Log Out" -> performLogout()
        }
    }

    // Performs fragment transition with a consistent fade animation.
    private fun navigateWithFade(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.main_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Displays the "About" dialog and ensures system bars stay hidden after.
    private fun showAboutDialog() {
        val context = requireContext()
        val primaryTextColor = ContextCompat.getColor(context, R.color.text_primary)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)
        val dialogBgColor = ContextCompat.getColor(context, R.color.card_white)
        val brandColor = ContextCompat.getColor(context, R.color.brand_teal)

        val titleView = TextView(context).apply {
            text = "About Allocash"
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 20)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val aboutMessage = "Allocash is your ultimate personal and family financial assistant, designed to be completely free and accessible for everyone.\n\n" +
                "Our mission is to provide you with smart, ongoing money management through a seamless and integrated experience. Allocash makes tracking your finances easy, intuitive, and intelligent, allowing you to monitor your entire cash flow in one unified space.\n\n" +
                "Experience a smarter way to manage your wealth and take control of your financial future with simplicity at your fingertips. \n\nDeveloped by Mor Barak"

        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setCustomTitle(titleView)
            .setMessage(aboutMessage)
            .setPositiveButton("Close") { d, _ -> d.dismiss() }
            .create()

        // Hide system navigation bar after dialog is closed
        dialog.setOnDismissListener { (activity as? BaseActivity)?.hideSystemUI() }
        dialog.show()

        applyAboutDialogStyles(dialog, secondaryTextColor, dialogBgColor, brandColor)
    }

    // Configures the visual appearance of the About dialog.
    private fun applyAboutDialogStyles(dialog: AlertDialog, msgColor: Int, bgColor: Int, btnColor: Int) {
        dialog.findViewById<TextView>(android.R.id.message)?.let {
            it.gravity = Gravity.CENTER
            it.setTextColor(msgColor)
        }

        dialog.window?.let { window ->
            window.setBackgroundDrawable(GradientDrawable().apply {
                cornerRadius = 32f
                setColor(bgColor)
            })
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(btnColor)
    }

    private fun performLogout() {
        // Call the central dialog method from BaseActivity
        (activity as? BaseActivity)?.showLogoutConfirmationDialog()
    }

    override fun onStart() {
        super.onStart()
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onResume() {
        super.onResume()
        // Sync user details with latest state
        view?.findViewById<TextView>(R.id.lbl_user_name)?.text = UserData.name
        view?.findViewById<TextView>(R.id.lbl_user_email)?.text = UserData.email
    }

    override fun onStop() {
        super.onStop()
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}