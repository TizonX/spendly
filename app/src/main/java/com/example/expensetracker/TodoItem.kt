package com.example.expensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Who / where money went (UPI app, merchant, manual tag, etc.)
    val payee: String,

    // Store as String for now (we can change to Double later)
    val amount: String,

    // Manual / SMS / Notification
    val source: String = "SMS",

    // Tag selected in manual entry (Food, Travel, etc.)
    val tag: String = "General",

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Flags
    val isEdited: Boolean = false
)

