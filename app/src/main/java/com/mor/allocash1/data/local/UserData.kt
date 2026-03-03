package com.mor.allocash1.data.local

import java.util.Locale

// Model for family members shared within an account.
data class FamilyMember(
    val name: String,
    val email: String
)

// Singleton object managing current user profile and global UI preferences.
object UserData {
    // User profile information - Default values used until cloud sync completes
    var name: String = "User"
    var email: String = ""
    var phone: String = "-"
    var country: String = "Israel"

    // Localization and currency settings
    var currencySymbol: String = "₪"
    var currencyCode: String = "ILS"

    // Internal storage for family members fetched from cloud
    var familyId: String? = null
    private val familyMembers = mutableListOf<FamilyMember>()

    // Additional
    var monthlySavingsGoal: Double = 500.0

    var isBalanceVisible: Boolean = true

    // Generates a sorted list of all unique country names from system locales.
    fun getAllCountries(): List<String> {
        val locales = Locale.getAvailableLocales()
        val countries = mutableListOf<String>()

        for (locale in locales) {
            val countryName = locale.displayCountry
            if (countryName.isNotEmpty() && !countries.contains(countryName)) {
                countries.add(countryName)
            }
        }
        countries.sort()
        return countries
    }

    // Returns the list of family members associated with this account.
    fun getFamilyMembers(): List<FamilyMember> = familyMembers

    // Updates the local family list after a cloud fetch
    fun updateFamilyMembers(newList: List<FamilyMember>) {
        familyMembers.clear()
        familyMembers.addAll(newList)
    }

    // Formats a double value into a currency string based on current user settings.
    fun formatCurrency(amount: Double): String {
        return "$currencySymbol${String.format("%.2f", amount)}"
    }

    // Syncs currency preferences to the Firestore profile document.
    fun syncCurrencyWithServer() {
        val updates = mapOf(
            "currencySymbol" to currencySymbol,
            "currencyCode" to currencyCode
        )
        com.mor.allocash1.data.cloud.FireStoreManager.updateUserProfile(updates, {}, {})
    }
}










