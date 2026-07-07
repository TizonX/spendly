package com.example.expensetracker.budget

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val tagName: String,
    val amount: Double
)
