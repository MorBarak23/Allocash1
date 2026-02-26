package com.mor.allocash1.data.classes

// Simple model for weekly breakdown
data class WeeklyDetail(
    val weekNumber: Int,
    val spent: Double,
    val total: Double
)

// Main Category Action model
data class Action(
    var title: String,
    var limit: Double,       // Monthly Budget (Goal)
    var currentAmount: Double = 0.0, // Cumulative spent so far
    val category: CategoryType,
    val isExpense: Boolean = true,
    val weeklyDetails: MutableList<WeeklyDetail> = mutableListOf()
)