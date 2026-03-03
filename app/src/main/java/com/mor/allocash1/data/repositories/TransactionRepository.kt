package com.mor.allocash1.data.repositories

import com.mor.allocash1.data.local.ActionDatabase
import com.mor.allocash1.data.classes.Transaction
import java.util.Calendar

object TransactionRepository {

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