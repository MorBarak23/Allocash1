package com.mor.allocash1.data.classes

data class Transaction(
    val title: String,
    val amount: Double,
    val category: CategoryType,
    val timestamp: Long = System.currentTimeMillis(),
    val isExpense: Boolean = true
)