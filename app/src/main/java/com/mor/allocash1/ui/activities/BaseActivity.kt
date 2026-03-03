package com.mor.allocash1.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.mor.allocash1.ui.fragments.HistoryFragment
import com.mor.allocash1.ui.fragments.HomeFragment
import com.mor.allocash1.R
import com.mor.allocash1.ui.fragments.SettingsFragment
import com.mor.allocash1.data.local.UserData
import com.mor.allocash1.ui.fragments.UserProfileFragment


//Base activity providing shared navigation and UI management logic.
open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var drawerLayout: DrawerLayout

    // Callback to handle back button behavior when drawer is open.
    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                closeDrawer()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val settings = com.mor.allocash1.data.repositories.SettingsRepository(this)
        val mode = if (settings.isDarkModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        setupSystemUiListener()

        // Sync data from cloud and update UI
        com.mor.allocash1.data.cloud.FireStoreManager.fetchAndSyncUserProfile {
            com.mor.allocash1.data.cloud.FireStoreManager.fetchFamilyMembers { members ->
                UserData.updateFamilyMembers(members)
                updateNavHeaderName(UserData.name)
            }
        }
    }

    //Hides system navigation bars for a cleaner full-screen experience.
    fun hideSystemUI() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    //Monitors keyboard visibility to ensure system UI stays hidden when needed.
    private fun setupSystemUiListener() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (!isKeyboardVisible) {
                hideSystemUI() // Unified logic to avoid duplication
            }
            ViewCompat.onApplyWindowInsets(view, insets)
        }
    }

    //Main configuration for the navigation drawer and content container.
    protected fun setupDrawer(contentLayoutId: Int) {
        setContentView(R.layout.activity_base)

        // Inflate specific activity content into the shared frame.
        val container = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(contentLayoutId, container, true)

        initDrawerLogic()
        updateNavHeaderName(UserData.name)
    }

    //Initializes drawer components and listeners.
    private fun initDrawerLogic() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) { backPressedCallback.isEnabled = true }
            override fun onDrawerClosed(drawerView: View) { backPressedCallback.isEnabled = false }
        })
    }

    //Handles side menu item selections and navigates to fragments.
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle specific non-fragment items first
        if (item.itemId == R.id.nav_logout) {
            showLogoutConfirmationDialog()
            return true
        }

        val targetFragment: Fragment = when (item.itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_settings -> SettingsFragment()
            R.id.nav_history -> HistoryFragment()
            R.id.nav_user -> UserProfileFragment()
            else -> {
                closeDrawer()
                return false
            }
        }

        // Only navigate if we are not already viewing the target fragment.
        if (isAlreadyOnFragment(targetFragment)) {
            closeDrawer()
            return true
        }

        performDrawerNavigation(targetFragment)
        return false
    }

    // Shows a styled confirmation dialog before logging out.
    fun showLogoutConfirmationDialog() {
        closeDrawer() // Ensure drawer is closed if called from menu

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> executeLogout() }
            .setNegativeButton("No", null)
            .create()

        dialog.show()
        styleLogoutDialog(dialog)
    }

    // Forces button colors to black to override system defaults.
    private fun styleLogoutDialog(dialog: AlertDialog) {
        val context = this@BaseActivity

        // Fix: Resolve the resource ID into an actual color integer
        val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor)

        dialog.window?.let { window ->
            val shape = android.graphics.drawable.GradientDrawable().apply {
                // Increased to 48f to match the consistency of other dialogs
                cornerRadius = 48f
                // Correctly fetch the themed background color
                setColor(androidx.core.content.ContextCompat.getColor(context, R.color.card_white))
            }
            window.setBackgroundDrawable(shape)

            // Ensure consistent width across devices
            val metrics = resources.displayMetrics
            window.setLayout((metrics.widthPixels * 0.85).toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    // Performs the actual Firebase logout and redirects to Auth screen.
    private fun executeLogout() {
        com.mor.allocash1.data.cloud.FireStoreManager.logout()

        val intent = android.content.Intent(this, AuthActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    //Checks if the requested fragment is already the current one.
    private fun isAlreadyOnFragment(target: Fragment): Boolean {
        val current = supportFragmentManager.findFragmentById(R.id.main_fragment_container)
        return current != null && current::class.java == target::class.java
    }

    //Closes the drawer and executes the fragment swap after the animation finishes.
    private fun performDrawerNavigation(fragment: Fragment) {
        closeDrawer()
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                replaceFragment(fragment, R.id.main_fragment_container)
                drawerLayout.removeDrawerListener(this)
            }
        })
    }

    //Swaps the current fragment with a smooth fade animation.
    protected fun replaceFragment(fragment: Fragment, containerId: Int) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    //Utility methods for manual drawer control.
    fun openDrawer() { if (::drawerLayout.isInitialized) drawerLayout.openDrawer(GravityCompat.START) }

    fun closeDrawer() { if (::drawerLayout.isInitialized) drawerLayout.closeDrawer(GravityCompat.START) }

    //Updates visibility of common header components.
    fun setHeaderVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btn_open_drawer).visibility = visibility
        findViewById<View>(R.id.lbl_app_name).visibility = visibility
        findViewById<View>(R.id.ic_user).visibility = visibility
    }

    //Updates the username in the navigation header.
    fun updateNavHeaderName(newName: String) {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.lbl_user_name).text = newName
    }
}