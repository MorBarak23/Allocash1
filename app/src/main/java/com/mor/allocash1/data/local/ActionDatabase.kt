package com.mor.allocash1.data.local

import com.mor.allocash1.data.classes.Action
import com.mor.allocash1.data.classes.CategoryType
import com.mor.allocash1.data.classes.Transaction
import com.mor.allocash1.data.classes.WeeklyDetail
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoField
import java.time.Instant
import java.time.ZoneId

// In-memory data management for transactions and monthly budgets (Actions).
object ActionDatabase {
    private val transactions = mutableListOf<Transaction>()
    private val expenseActions = mutableListOf<Action>()
    private val incomeActions = mutableListOf<Action>()

    // --- Read Methods ---

    fun getExpenseActions(): List<Action> = expenseActions
    fun getIncomeActions(): List<Action> = incomeActions
    fun getAllTransactions(): List<Transaction> = transactions

    // Returns actions from the last 7 days (168 hours)
    fun getRecentTransactions(): List<Transaction> {
        // 7 days * 24 hours * 60 min * 60 sec * 1000 ms
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return transactions.filter { it.timestamp >= sevenDaysAgo }
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
        // Updated: Refresh breakdown for BOTH income and expense actions to ensure full sync
        expenseActions.forEach { updateWeeklyBreakdown(it) }
        incomeActions.forEach { updateWeeklyBreakdown(it) }
    }

    // --- Calculation Helpers ---

    fun getTotalIncome(): Double = incomeActions.sumOf { it.currentAmount }
    fun getTotalExpense(): Double = expenseActions.sumOf { it.currentAmount }

    // --- Weekly Breakdown Logic (core application logic) ---

    // Recalculates and initializes weekly budget details for a specific action.
    fun updateWeeklyBreakdown(action: Action) {
        val now = LocalDate.now()
        val daysInMonth = YearMonth.from(now).lengthOfMonth()
        val firstDayOfMonth = now.withDayOfMonth(1)

        action.weeklyDetails.clear()
        calculateWeeklySlices(action, firstDayOfMonth, now, daysInMonth)
    }

    // Segments the month into weekly slices to calculate budgets and actual spending.
    private fun calculateWeeklySlices(action: Action, start: LocalDate, now: LocalDate, totalDays: Int) {
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

    // Determines the end date of a week while respecting monthly boundaries.
    private fun calculateEndOfWeek(start: LocalDate, now: LocalDate, totalDays: Int): LocalDate {
        // In ISO-8601: Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6, Sun=7
        val dayOfWeek = start.get(ChronoField.DAY_OF_WEEK)
        val daysUntilSaturday = (6 - dayOfWeek + 7) % 7

        var end = start.plusDays(daysUntilSaturday.toLong())

        // Ensure the slice doesn't cross the month boundary
        return if (end.month != now.month) now.withDayOfMonth(totalDays) else end
    }

    // Sums transaction totals for a specific category within a defined date range.
    private fun calculateSpentInPeriod(action: Action, start: LocalDate, end: LocalDate): Double {
        return transactions.filter {
            val tDate = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // Updated: Matches the action category and its type (Income vs Expense)
            it.category == action.category &&
                    it.isExpense == (action.category != CategoryType.INCOME) &&
                    !tDate.isBefore(start) &&
                    !tDate.isAfter(end)
        }.sumOf { it.amount }
    }
}









