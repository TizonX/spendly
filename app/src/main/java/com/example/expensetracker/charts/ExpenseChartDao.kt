package com.example.expensetracker.data.charts

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseChartDao {

    @Query("""
        SELECT tag as tagName, SUM(amount) as totalAmount
        FROM todo_items
        GROUP BY tag
    """)
    fun getExpenseByTag(): Flow<List<ExpenseChartModel>>

    @Query("""
SELECT tag as tagName,
       SUM(CAST(amount AS REAL)) as totalAmount
FROM todo_items
WHERE createdAt >= (strftime('%s','now','-7 day') * 1000)
GROUP BY tag
""")
    fun getWeeklyExpense(): Flow<List<ExpenseChartModel>>

    @Query("""
SELECT tag as tagName,
       SUM(CAST(amount AS REAL)) as totalAmount
FROM todo_items
WHERE strftime('%Y-%m', createdAt / 1000, 'unixepoch')
      = strftime('%Y-%m','now')
GROUP BY tag
""")
    fun getMonthlyExpense(): Flow<List<ExpenseChartModel>>

    @Query("""
SELECT tag as tagName,
       SUM(CAST(amount AS REAL)) as totalAmount
FROM todo_items
WHERE strftime('%Y', createdAt / 1000, 'unixepoch')
      = strftime('%Y','now')
GROUP BY tag
""")
    fun getYearlyExpense(): Flow<List<ExpenseChartModel>>
}