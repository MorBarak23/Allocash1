package com.mor.allocash1.ui.fragments

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.mor.allocash1.utils.BiometricHelper
import com.mor.allocash1.R
import com.mor.allocash1.data.repositories.SettingsRepository
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for managing application settings including appearance and security.
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var biometricHelper: BiometricHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRepository = SettingsRepository(requireContext())
        biometricHelper = BiometricHelper(requireActivity())

        updateUserHeader(view)
        setupNavigationRows(view)
        setupDarkModeLogic(view)
        setupBiometricLogic(view)
        setupHeaderUserIcon()
    }

    // Displays the current user name from UserData in the settings header.
    private fun updateUserHeader(view: View) {
        view.findViewById<TextView>(R.id.lbl_settings_user_name).text = UserData.name
    }

    // Configures the clickable setting rows for profile, accounts, and currency.
    private fun setupNavigationRows(view: View) {
        setupRow(view.findViewById(R.id.row_edit_profile), "Edit Profile", R.drawable.ic_user3) {
            navigateTo(MyProfileFragment())
        }
        setupRow(view.findViewById(R.id.row_manage_accounts), "Manage Accounts", R.drawable.ic_list) {
            navigateTo(AccountsFragment())
        }
        setupRow(view.findViewById(R.id.row_currency), "Primary Currency", R.drawable.ic_coin) {
            showCurrencyDialog()
        }
    }

    // Helper function to bind data and click listeners to a setting row view.
    private fun setupRow(rowView: View, title: String, iconRes: Int, onClick: () -> Unit) {
        rowView.findViewById<TextView>(R.id.lbl_setting_title).text = title
        rowView.findViewById<ImageView>(R.id.img_setting_icon).setImageResource(iconRes)
        rowView.setOnClickListener { onClick() }
    }

    // Handles dark mode switch state and system theme application.
    private fun setupDarkModeLogic(view: View) {
        val switchDarkMode = view.findViewById<MaterialSwitch>(R.id.switch_dark_mode)

        switchDarkMode.isChecked = settingsRepository.isDarkModeEnabled()

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setDarkModeEnabled(isChecked)

            com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(
                mapOf("isDarkMode" to isChecked), {}, {}
            )

            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    // Configures biometric authentication switch and persistent cloud sync.
    private fun setupBiometricLogic(view: View) {
        val switchBiometric = view.findViewById<MaterialSwitch>(R.id.switch_biometric)
        switchBiometric.isChecked = settingsRepository.isSmartLoginEnabled()

        switchBiometric.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                handleEnablingBiometrics(buttonView)
            } else {
                // Update locally and sync preference to Cloud
                settingsRepository.setSmartLoginEnabled(false)
                com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(
                    mapOf("isBiometricEnabled" to false), {}, {}
                )
            }
        }
    }

    // Verifies identity and syncs the 'Enabled' state to the cloud.
    private fun handleEnablingBiometrics(buttonView: View) {
        if (biometricHelper.canAuthenticate()) {
            biometricHelper.showBiometricPrompt(
                title = "Enable Smart Login",
                subtitle = "Confirm your identity to secure your account",
                onSuccess = {
                    settingsRepository.setSmartLoginEnabled(true)
                    // Sync the preference to Firestore
                    com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(
                        mapOf("isBiometricEnabled" to true),
                        onSuccess = { Toast.makeText(context, "Smart Login enabled & synced", Toast.LENGTH_SHORT).show() },
                        onFailure = {}
                    )
                },
                onError = { _, _ ->
                    (buttonView as? MaterialSwitch)?.isChecked = false
                    settingsRepository.setSmartLoginEnabled(false)
                }
            )
        } else {
            Toast.makeText(context, "Biometrics not supported", Toast.LENGTH_SHORT).show()
            (buttonView as? MaterialSwitch)?.isChecked = false
        }
    }

    // Configures the user icon click listener in the global header.
    private fun setupHeaderUserIcon() {
        val imgHeaderUser = activity?.findViewById<ImageView>(R.id.ic_user)
        imgHeaderUser?.setOnClickListener {
            navigateTo(UserProfileFragment())
        }
    }

    // Displays the currency picker and ensures system UI is hidden after dismissal.
    private fun showCurrencyDialog() {
        val currencies = arrayOf("₪ ILS", "$ USD", "€ EUR", "£ GBP")
        val context = requireContext()
        val dialogBgColor = ContextCompat.getColor(context, R.color.card_white)
        val brandColor = ContextCompat.getColor(context, R.color.brand_teal)

        val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                updateUserCurrency(currencies[which])
                (activity as? BaseActivity)?.hideSystemUI() // Hide Galaxy bars after selection
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Hide system bars when dialog is closed for any reason
        dialog.setOnDismissListener { (activity as? BaseActivity)?.hideSystemUI() }
        dialog.show()
        applyCurrencyDialogStyles(dialog, dialogBgColor, brandColor)
    }

    // Updates UserData and syncs the new currency selection to Firestore.
    private fun updateUserCurrency(selected: String) {
        val parts = selected.split(" ")
        UserData.currencySymbol = parts[0]
        UserData.currencyCode = parts[1]

        // Use the central sync method from UserData
        UserData.syncCurrencyWithServer()

        Toast.makeText(context, "Currency updated to $selected", Toast.LENGTH_SHORT).show()
    }

    // Styles the currency dialog window with rounded corners and branded colors.
    private fun applyCurrencyDialogStyles(dialog: AlertDialog, bgColor: Int, btnColor: Int) {
        dialog.window?.let { window ->
            window.setBackgroundDrawable(GradientDrawable().apply {
                cornerRadius = 32f
                setColor(bgColor)
            })
            window.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(btnColor)
    }

    // Navigates to a target fragment with a consistent fade animation.
    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
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

    override fun onStart() {
        super.onStart()
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}