package com.mor.allocash1.data.repositories

import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.data.classes.Transaction
import java.util.Calendar

object TransactionRepository {

    //Fetches transactions filtered by month and year. Ready for future server integration.
    fun getTransactionsByMonth(month: Int, year: Int): List<Transaction> {
        // Currently fetching from local Database
        val allTransactions = ActionDatabase.getAllTransactions()

        return allTransactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }.sortedByDescending { it.timestamp }
    }

    //Calculates totals for the selected period.
    fun getMonthlyTotals(transactions: List<Transaction>): Pair<Double, Double> {
        var income = 0.0
        var expense = 0.0

        transactions.forEach {
            if (it.isExpense) expense += it.amount else income += it.amount
        }

        return Pair(income, expense)
    }
}