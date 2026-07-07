package com.example.expensetracker.ui.backup

import com.example.expensetracker.TodoItem
import com.example.expensetracker.tag.TagEntity

data class BackupData(
    val todos: List<TodoItem>,
    val tags: List<TagEntity>
)