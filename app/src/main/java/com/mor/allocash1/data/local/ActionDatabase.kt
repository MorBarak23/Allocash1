package com.mor.allocash1.data.local

import com.mor.allocash1.data.classes.Action
import com.mor.allocash1.data.classes.CategoryType
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.data.classes.WeeklyDetail
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoField

// In-memory data management for transactions and monthly budgets (Actions).
object ActionDatabase {
    // Lists are now pure caches, populated only via cloud listeners
    private val transactions = mutableListOf<Transaction>()
    private val expenseActions = mutableListOf<Action>()
    private val incomeActions = mutableListOf<Action>()

    // --- Read Methods ---

    fun getExpenseActions(): List<Action> = expenseActions
    fun getIncomeActions(): List<Action> = incomeActions
    fun getAllTransactions(): List<Transaction> = transactions

    // Returns actions from the last 72 hours
    fun getRecentTransactions(): List<Transaction> {
        val seventyTwoHoursAgo = System.currentTimeMillis() - (72 * 60 * 60 * 1000)
        return transactions.filter { it.timestamp >= seventyTwoHoursAgo }
    }

    // --- Sync Methods (Called by FireStoreManager) ---

    // Updates local action lists based on fresh cloud snapshots
    fun updateActions(newList: List<Action>) {
        expenseActions.clear()
        incomeActions.clear()
        newList.forEach {
            if (it.category == CategoryType.INCOME) incomeActions.add(it) else expenseActions.add(it)
            // Trigger weekly breakdown recalculation whenever actions are updated
            updateWeeklyBreakdown(it)
        }
    }

    // Updates local transaction history from the cloud
    fun updateTransactions(newList: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newList)
        // Refresh weekly breakdown for all existing actions when new transactions arrive
        expenseActions.forEach { updateWeeklyBreakdown(it) }
    }

    // --- Calculation Helpers ---

    fun getTotalIncome(): Double = incomeActions.sumOf { it.currentAmount }
    fun getTotalExpense(): Double = expenseActions.sumOf { it.currentAmount }

    // Monthly balance is now calculated reactively in the UI

    // --- Weekly Breakdown Logic (Remains as core application logic) ---

    fun updateWeeklyBreakdown(action: Action) {
        val now = java.time.LocalDate.now()
        val daysInMonth = java.time.YearMonth.from(now).lengthOfMonth()
        val firstDayOfMonth = now.withDayOfMonth(1)

        action.weeklyDetails.clear()
        calculateWeeklySlices(action, firstDayOfMonth, now, daysInMonth)
    }

    private fun calculateWeeklySlices(action: Action, start: java.time.LocalDate, now: java.time.LocalDate, totalDays: Int) {
        var currentStart = start
        var weekIndex = 1

        while (currentStart.month == now.month) {
            val endOfWeek = calculateEndOfWeek(currentStart, now, totalDays)
            val daysInWeek = (endOfWeek.dayOfMonth - currentStart.dayOfMonth) + 1
            val weeklyBudget = (action.limit * daysInWeek) / totalDays
            val spentInWeek = calculateSpentInPeriod(action, currentStart, endOfWeek)

            action.weeklyDetails.add(WeeklyDetail(weekIndex, spentInWeek, weeklyBudget))

            if (endOfWeek.dayOfMonth == totalDays) break
            currentStart = endOfWeek.plusDays(1)
            weekIndex++
        }
    }

    private fun calculateEndOfWeek(start: java.time.LocalDate, now: java.time.LocalDate, totalDays: Int): java.time.LocalDate {
        val daysUntilEnd = 7 - start.get(java.time.temporal.ChronoField.DAY_OF_WEEK)
        var end = start.plusDays(daysUntilEnd.toLong())
        return if (end.month != now.month) now.withDayOfMonth(totalDays) else end
    }

    private fun calculateSpentInPeriod(action: Action, start: java.time.LocalDate, end: java.time.LocalDate): Double {
        return transactions.filter {
            // Correctly convert timestamp to LocalDate for accurate comparison
            val tDate = java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            it.category == action.category &&
                    it.isExpense &&
                    !tDate.isBefore(start) &&
                    !tDate.isAfter(end)
        }.sumOf { it.amount }
    }
}