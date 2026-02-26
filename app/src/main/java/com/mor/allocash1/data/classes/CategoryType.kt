package com.mor.allocash1.data.classes

import com.mor.allocash1.R

enum class CategoryType(val displayName: String, val iconRes: Int) {
    FOOD("Food & Dining", R.drawable.ic_food),
    SHOPPING("Shopping", R.drawable.ic_shopping),
    TRANSPORTATION("Transportation", R.drawable.ic_car),
    HEALTH("Health", R.drawable.ic_health),
    ENTERTAINMENT("Entertainment", R.drawable.ic_popcorn),
    BILLS("Bills & Utilities", R.drawable.ic_bill),
    EDUCATION("Education", R.drawable.ic_education),
    GROCERIES("Groceries", R.drawable.ic_cart),
    GIFTS("Gifts", R.drawable.ic_gift),
    TRAVEL("Travel", R.drawable.ic_plane),
    TECH("Tech & Gadgets", R.drawable.ic_smartphone),
    HOME("Home Maintenance", R.drawable.ic_home),
    INCOME("Income", R.drawable.btn_income),
    OTHER("Other", R.drawable.ic_other) // Fallback category
}