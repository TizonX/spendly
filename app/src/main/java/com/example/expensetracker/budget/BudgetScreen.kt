package com.example.expensetracker.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.ui.theme.*
import kotlinx.coroutines.launch

private fun <T> SnapshotStateList<T>.swap(from: Int, to: Int) {
    if (from < 0 || to < 0 || from >= size || to >= size) return
    val tmp = this[from]; this[from] = this[to]; this[to] = tmp
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen() {
    val context = LocalContext.current
    val db      = AppDatabase.getDatabase(context)
    val scope   = rememberCoroutineScope()

    val budgets         by db.budgetDao().getAllBudgets().collectAsState(initial = emptyList())
    val monthlyExpenses by db.expenseChartDao().getMonthlyExpense().collectAsState(initial = emptyList())
    val tags            by db.tagDao().getAllTags().collectAsState(initial = emptyList())

    val spentByTag = monthlyExpenses.associate { it.tagName to it.totalAmount }

    // Local ordered list — drag only reorders in-memory
    val localBudgets = remember { mutableStateListOf<BudgetEntity>() }

    // Sync local list when DB changes (add/delete/amount update)
    LaunchedEffect(budgets) {
        val freshMap = budgets.associate { it.tagName to it }
        localBudgets.removeAll { it.tagName !in freshMap }
        val existingNames = localBudgets.map { it.tagName }.toSet()
        budgets.filter { it.tagName !in existingNames }.forEach { localBudgets.add(it) }
        localBudgets.indices.toList().forEach { i ->
            freshMap[localBudgets[i].tagName]?.let { localBudgets[i] = it }
        }
    }

    // Drag state
    var draggingTagName by remember { mutableStateOf<String?>(null) }
    var dragOffsetY     by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    // Estimated height per item (card ~140dp + 12dp spacing)
    val itemHeightPx = with(density) { 152.dp.toPx() }

    var showDialog          by remember { mutableStateOf(false) }
    var editTarget          by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedTag         by remember { mutableStateOf("") }
    var budgetAmountText    by remember { mutableStateOf("") }
    var tagDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { editTarget = null; selectedTag = ""; budgetAmountText = ""; showDialog = true },
                containerColor = AccentViolet,
                contentColor   = White,
                shape          = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Budget")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .background(BgDeep)
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text          = "MONTHLY BUDGETS",
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier      = Modifier.padding(bottom = 4.dp),
                )
            }

            if (localBudgets.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text      = "No budgets set yet.\nTap + to add one.",
                            color     = TextMuted,
                            textAlign = TextAlign.Center,
                            fontSize  = 14.sp,
                        )
                    }
                }
            }

            items(localBudgets, key = { it.tagName }) { budget ->
                val spent        = spentByTag[budget.tagName] ?: 0.0
                val remaining    = budget.amount - spent
                val progress     = (spent / budget.amount).coerceIn(0.0, 1.0).toFloat()
                val isOverBudget = spent > budget.amount
                val isDragging   = draggingTagName == budget.tagName

                Box(
                    modifier = Modifier
                        .animateItemPlacement()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer(
                            translationY = if (isDragging) dragOffsetY else 0f,
                            scaleX       = if (isDragging) 1.03f else 1f,
                            scaleY       = if (isDragging) 1.03f else 1f,
                            alpha        = if (isDragging) 0.93f else 1f,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDragging) BgCardAlt else BgCard)
                            .padding(18.dp),
                    ) {
                        Column {
                            // Header row: dot + name + actions + drag handle
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isOverBudget) AccentCoral else AccentMint),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text       = budget.tagName,
                                        color      = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 16.sp,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick        = {
                                            editTarget       = budget
                                            selectedTag      = budget.tagName
                                            budgetAmountText = budget.amount.toLong().toString()
                                            showDialog       = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                    ) {
                                        Text("Edit", color = AccentViolet, fontSize = 13.sp)
                                    }
                                    IconButton(
                                        onClick  = { scope.launch { db.budgetDao().deleteByTagName(budget.tagName) } },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint               = TextMuted,
                                            modifier           = Modifier.size(18.dp),
                                        )
                                    }
                                    // Drag handle — long-press + drag to reorder
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .pointerInput(budget.tagName) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingTagName = budget.tagName
                                                        dragOffsetY     = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val idx = localBudgets.indexOfFirst { it.tagName == draggingTagName }
                                                        if (idx < 0) return@detectDragGesturesAfterLongPress
                                                        when {
                                                            dragOffsetY > itemHeightPx / 2 && idx < localBudgets.size - 1 -> {
                                                                localBudgets.swap(idx, idx + 1)
                                                                dragOffsetY -= itemHeightPx
                                                            }
                                                            dragOffsetY < -itemHeightPx / 2 && idx > 0 -> {
                                                                localBudgets.swap(idx, idx - 1)
                                                                dragOffsetY += itemHeightPx
                                                            }
                                                        }
                                                    },
                                                    onDragEnd    = { draggingTagName = null; dragOffsetY = 0f },
                                                    onDragCancel = { draggingTagName = null; dragOffsetY = 0f },
                                                )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint               = if (isDragging) AccentViolet else TextMuted,
                                            modifier           = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(BgCardAlt),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isOverBudget) AccentCoral else AccentMint),
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            // Spent / Budget / Left
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Spent",  fontSize = 10.sp, color = TextMuted, letterSpacing = 0.5.sp)
                                    Text("₹%.0f".format(spent),         fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Budget", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.5.sp)
                                    Text("₹%.0f".format(budget.amount), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Left",   fontSize = 10.sp, color = TextMuted, letterSpacing = 0.5.sp)
                                    Text(
                                        if (isOverBudget) "-₹%.0f".format(-remaining) else "₹%.0f".format(remaining),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 15.sp,
                                        color      = if (isOverBudget) AccentCoral else AccentMint,
                                    )
                                }
                            }

                            if (isOverBudget) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentCoralDim)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text       = "Over budget by ₹%.0f".format(-remaining),
                                        color      = AccentCoral,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Add / Edit dialog
    if (showDialog) {
        val budgetedTagNames = budgets.map { it.tagName }.toSet()
        val availableTags    = tags.map { it.name }.filter { tagName ->
            editTarget != null || tagName !in budgetedTagNames
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title            = { Text(if (editTarget != null) "Edit Budget" else "Set Budget", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (editTarget == null) {
                        ExposedDropdownMenuBox(
                            expanded         = tagDropdownExpanded,
                            onExpandedChange = { tagDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value         = selectedTag,
                                onValueChange = {},
                                readOnly      = true,
                                label         = { Text("Select Tag") },
                                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagDropdownExpanded) },
                                modifier      = Modifier.menuAnchor().fillMaxWidth(),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = AccentViolet,
                                    unfocusedBorderColor = BgCardAlt,
                                    focusedTextColor     = TextPrimary,
                                    unfocusedTextColor   = TextPrimary,
                                    focusedLabelColor    = AccentViolet,
                                ),
                            )
                            ExposedDropdownMenu(
                                expanded         = tagDropdownExpanded,
                                onDismissRequest = { tagDropdownExpanded = false },
                            ) {
                                if (availableTags.isEmpty()) {
                                    DropdownMenuItem(
                                        text    = { Text("All tags have budgets", color = TextMuted) },
                                        onClick = { tagDropdownExpanded = false },
                                    )
                                } else {
                                    availableTags.forEach { tag ->
                                        DropdownMenuItem(
                                            text    = { Text(tag, color = TextPrimary) },
                                            onClick = { selectedTag = tag; tagDropdownExpanded = false },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Tag: ${editTarget!!.tagName}", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    OutlinedTextField(
                        value           = budgetAmountText,
                        onValueChange   = { budgetAmountText = it.filter { c -> c.isDigit() } },
                        label           = { Text("Monthly Budget (₹)") },
                        modifier        = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentViolet,
                            unfocusedBorderColor = BgCardAlt,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = AccentViolet,
                            focusedLabelColor    = AccentViolet,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount  = budgetAmountText.toDoubleOrNull()
                    val tagName = if (editTarget != null) editTarget!!.tagName else selectedTag
                    if (amount != null && amount > 0 && tagName.isNotBlank()) {
                        scope.launch { db.budgetDao().upsert(BudgetEntity(tagName = tagName, amount = amount)) }
                        showDialog = false
                    }
                }) { Text("Save", color = AccentViolet, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}
