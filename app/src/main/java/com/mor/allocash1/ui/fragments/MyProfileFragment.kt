package com.mor.allocash1.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.mor.allocash1.App
import com.mor.allocash1.R
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.activities.BaseActivity

// Fragment for managing and editing user profile details.
class MyProfileFragment : Fragment(R.layout.fragment_my_profile) {

    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView
    private lateinit var rowUsername: View
    private lateinit var rowEmail: View
    private lateinit var rowMobile: View
    private lateinit var rowCountry: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset system padding for immersive UI
        requireActivity().findViewById<View>(R.id.main_fragment_container)?.setPadding(0, 0, 0, 0)



        initViews(view)
        setupNavigation(view)
        setupEditableFields()
        updateHeaderUI()
    }

    // Maps local variables to XML IDs.
    private fun initViews(view: View) {
        headerName = view.findViewById(R.id.lbl_user_name_header)
        headerEmail = view.findViewById(R.id.lbl_user_email_header)
        rowUsername = view.findViewById(R.id.row_username)
        rowEmail = view.findViewById(R.id.row_email)
        rowMobile = view.findViewById(R.id.row_mobile)
        rowCountry = view.findViewById(R.id.row_country)
    }

    // Configures the back button listener.
    private fun setupNavigation(view: View) {
        view.findViewById<ImageView>(R.id.btn_back_my_profile).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // Initializes logic for each profile row with cloud sync and error handling.
    private fun setupEditableFields() {
        // 1. User Name - Editable with 16-char limit and cloud revert
        setupField(rowUsername, "User Name", UserData.name) { row ->
            showEditTextDialog("Edit User Name", UserData.name, InputType.TYPE_CLASS_TEXT, 16) { newName ->
                val oldName = UserData.name // Save original value for revert

                // Optimistic UI Update
                UserData.name = newName
                updateFieldValue(row, newName)
                updateHeaderUI()

                com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(mapOf("name" to newName),
                    onSuccess = { (activity as? BaseActivity)?.updateNavHeaderName(newName) },
                    onFailure = {
                        // Revert to old value on failure
                        UserData.name = oldName
                        updateFieldValue(row, oldName)
                        updateHeaderUI()
                        android.widget.Toast.makeText(context, "Update failed. Name restored.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 2. Email - Permanent and uneditable as requested
        rowEmail.findViewById<TextView>(R.id.lbl_field_title).text = "Email"
        rowEmail.findViewById<TextView>(R.id.lbl_field_value).text = UserData.email
        rowEmail.findViewById<ImageView>(R.id.btn_edit_field).visibility = View.GONE

        // 3. Mobile Number - Editable with cloud revert
        setupField(rowMobile, "Mobile Number", UserData.phone) { row ->
            val initial = if (UserData.phone == "-") "" else UserData.phone
            showEditTextDialog("Edit Mobile Number", initial, InputType.TYPE_CLASS_PHONE) { newPhone ->
                val oldPhone = UserData.phone // Save original value for revert
                val finalVal = newPhone.ifBlank { "-" }

                UserData.phone = finalVal
                updateFieldValue(row, finalVal)

                com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(mapOf("phone" to finalVal),
                    onSuccess = { /* Sync confirmed */ },
                    onFailure = {
                        // Revert to old value on failure
                        UserData.phone = oldPhone
                        updateFieldValue(row, oldPhone)
                        android.widget.Toast.makeText(context, "Update failed. Phone restored.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 4. Country - Selection with cloud revert
        setupField(rowCountry, "Country", UserData.country) {
            showCountrySelectionDialog()
        }
    }

    // Helper to set labels and edit listeners for profile rows.
    private fun setupField(rowView: View, title: String, value: String, onEdit: (View) -> Unit) {
        rowView.findViewById<TextView>(R.id.lbl_field_title).text = title
        rowView.findViewById<TextView>(R.id.lbl_field_value).text = value
        rowView.findViewById<ImageView>(R.id.btn_edit_field).setOnClickListener { onEdit(rowView) }
    }

    // Updates a specific row value in the UI.
    private fun updateFieldValue(rowView: View, newValue: String) {
        rowView.findViewById<TextView>(R.id.lbl_field_value).text = newValue
    }

    // Syncs top header display with current user data.
    private fun updateHeaderUI() {
        headerName.text = UserData.name
        headerEmail.text = UserData.email
    }

    // Displays the input dialog and ensures system UI is hidden after saving.
    private fun showEditTextDialog(title: String, initial: String, inputType: Int, maxLength: Int? = null, onSave: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme).setView(dialogView).create()

        // Critical: Force hide system bars whenever dialog is closed
        dialog.setOnDismissListener { (activity as? BaseActivity)?.hideSystemUI() }

        val inputField = dialogView.findViewById<EditText>(R.id.input_rename_title)
        dialogView.findViewById<TextView>(R.id.lbl_dialog_title).text = title
        inputField.setText(initial)
        inputField.inputType = inputType
        inputField.setSelection(inputField.text.length)

        if (maxLength != null) inputField.filters = arrayOf(InputFilter.LengthFilter(maxLength))

        dialogView.findViewById<Button>(R.id.btn_confirm_rename).setOnClickListener {
            val result = inputField.text.toString().trim()
            if (result.isNotEmpty() || title.contains("Mobile")) {
                onSave(result)
                dialog.dismiss()
                (activity as? BaseActivity)?.hideSystemUI() // Reinforce hiding after confirm
            }
        }

        dialog.show()
        applyDialogStyles(dialog)
    }

    // Applies custom width and transparent background with margins to the dialog.
    private fun applyDialogStyles(dialog: AlertDialog) {
        dialog.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            val inset = InsetDrawable(ColorDrawable(Color.TRANSPARENT), 50, 0, 50, 0)
            window.setBackgroundDrawable(inset)
            App.Companion.applyGlobalUiSettings(window)
        }
    }

    // Displays the country list and syncs choice with Firestore.
    private fun showCountrySelectionDialog() {
        val countries = UserData.getAllCountries()
        val adapter = createCountryAdapter(countries)
        val oldCountry = UserData.country // Save original value for revert

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("Select Country")
            .setAdapter(adapter) { _, which ->
                val selectedCountry = countries[which]

                // Optimistic UI Update
                UserData.country = selectedCountry
                updateFieldValue(rowCountry, selectedCountry)

                // Cloud synchronization
                com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(mapOf("country" to selectedCountry),
                    onSuccess = { (activity as? BaseActivity)?.hideSystemUI() },
                    onFailure = {
                        // Revert to old value on failure
                        UserData.country = oldCountry
                        updateFieldValue(rowCountry, oldCountry)
                        android.widget.Toast.makeText(context, "Update failed. Country restored.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnDismissListener { (activity as? BaseActivity)?.hideSystemUI() }
        dialog.show()
        configureCountryWindow(dialog)
    }
    // Creates a custom adapter for the country selection list.
    private fun createCountryAdapter(list: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), R.layout.item_country, R.id.lbl_country_name, list) {
            override fun getView(pos: Int, conv: View?, parent: ViewGroup): View {
                val v = super.getView(pos, conv, parent)

                // Set country name color to text_primary for Dark Mode support
                val countryName = v.findViewById<TextView>(R.id.lbl_country_name)
                countryName.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary))

                // Hide the flag emoji view as requested
                v.findViewById<TextView>(R.id.lbl_country_flag).visibility = View.GONE

                return v
            }
        }
    }

    // Sets rounded corners and dimensions for the country picker.
    private fun configureCountryWindow(dialog: AlertDialog) {
        dialog.window?.let { window ->
            window.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), (resources.displayMetrics.heightPixels * 0.7).toInt())
            window.setBackgroundDrawable(GradientDrawable().apply {
                // Updated to 48f to maintain UI consistency with other app dialogs
                cornerRadius = 48f
                // Use card_white instead of Color.WHITE to support Dark Mode background
                setColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.card_white))
            })
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as? BaseActivity)?.setHeaderVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        (activity as? BaseActivity)?.setHeaderVisibility(true)
    }
}