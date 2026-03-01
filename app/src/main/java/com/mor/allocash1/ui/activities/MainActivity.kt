package com.mor.allocash1.ui.activities

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mor.allocash1.ui.fragments.HomeFragment
import com.mor.allocash1.R
import com.mor.allocash1.data.cloud.FireStoreManager
import com.mor.allocash1.data.repositories.SettingsRepository
import com.mor.allocash1.databinding.ActivityMainBinding
import com.mor.allocash1.utils.BiometricHelper

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding and set the content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupDrawerNavigation()
        checkInvites()

        if (savedInstanceState == null) {
            initUI()
        }
    }

    // MainActivity.kt

    // Updated: Dialog now adapts to Dark Mode by using R.color.card_white
    private fun checkInvites() {
        FireStoreManager.listenForInvites { inviteData ->

            if (isFinishing || isDestroyed) return@listenForInvites

            val from = inviteData["fromName"] as String
            val context = this

            val dialog = AlertDialog.Builder(context, R.style.CustomDialogTheme)
                .setTitle("Family Invitation")
                .setMessage("$from invited you to join their family account. Accept?")
                .setPositiveButton("Accept") { _, _ ->
                    FireStoreManager.acceptFamilyInvite(inviteData) {
                        FireStoreManager.fetchAndSyncUserProfile {
                            initUI()
                        }
                    }
                }
                .setNegativeButton("Ignore", null)
                .create()

            dialog.show()

            // Fix: Use card_white instead of hardcoded WHITE to support Dark Mode
            dialog.window?.let { window ->
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 48f
                    setColor(androidx.core.content.ContextCompat.getColor(context, R.color.card_white))
                }
                window.setBackgroundDrawable(bg)
            }

            // Button Styling
            val brandColor = androidx.core.content.ContextCompat.getColor(context, R.color.brand_teal)
            val secondaryColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(brandColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(secondaryColor)
        }
    }

    // Configures top padding to avoid overlap with system status bars.
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    // Initializes the navigation drawer and its trigger button.
    private fun setupDrawerNavigation() {
        setupDrawer(R.layout.activity_main)

        // Setup the menu button to open the side drawer
        val btnOpenDrawer = findViewById<ImageView>(R.id.btn_open_drawer)
        btnOpenDrawer.setOnClickListener {
            openDrawer() // Helper from BaseActivity
        }
    }

    // Sets the initial fragment to be displayed in the container.
    private fun initUI() {
        FireStoreManager.fetchAndSyncUserProfile {
            val fragment = HomeFragment()
            replaceFragment(fragment, R.id.main_fragment_container)
        }
    }
}