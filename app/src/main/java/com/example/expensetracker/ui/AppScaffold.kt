package com.example.expensetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.SmsReceiver
import com.example.expensetracker.TodoItem
import com.example.expensetracker.TodoListScreen
import com.example.expensetracker.budget.BudgetScreen
import com.example.expensetracker.tag.TagManagementScreen
import com.example.expensetracker.ui.backup.BackupScreen
import com.example.expensetracker.ui.charts.ExpensePieChartScreen
import com.example.expensetracker.ui.components.FooterBar
import com.example.expensetracker.ui.components.HeaderBar
import com.example.expensetracker.ui.components.ManualEntryBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AppScaffold(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val db      = AppDatabase.getDatabase(context)
    val scope   = rememberCoroutineScope()

    val monthlyTotal by db.todoDao()
        .getCurrentMonthTotal()
        .collectAsState(initial = 0.0)

    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HeaderBar(monthlyTotal = monthlyTotal ?: 0.0) },
    ) { padding ->
        // imePadding() on the Column makes the entire bottom section (ManualEntryBar +
        // FooterBar) rise above the keyboard together — FooterBar stays visible.
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    Screen.HOME         -> TodoListScreen(onManageTags = { currentScreen = Screen.TAGS })
                    Screen.TAGS         -> TagManagementScreen(onBack = { currentScreen = Screen.HOME })
                    Screen.EXPENSE_CHART -> ExpensePieChartScreen()
                    Screen.DOWNLOAD     -> BackupScreen()
                    Screen.BUDGET       -> BudgetScreen()
                }
            }

            if (currentScreen == Screen.HOME) {
                ManualEntryBar { amount, tag ->
                    scope.launch(Dispatchers.IO) {
                        db.todoDao().insert(
                            TodoItem(
                                payee     = tag,
                                amount    = amount,
                                tag       = tag,
                                source    = "MANUAL",
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                            )
                        )
                        SmsReceiver.triggerRefresh()
                    }
                }
            }

            FooterBar(
                currentScreen  = currentScreen,
                onScreenChange = { currentScreen = it },
            )
        }
    }
}
