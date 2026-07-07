package com.example.expensetracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoListScreen(onManageTags: () -> Unit = {}) {

    val context = LocalContext.current
    val db      = AppDatabase.getDatabase(context)

    var todoList by remember { mutableStateOf<List<TodoItem>>(emptyList()) }
    var lastTime by remember { mutableStateOf(Long.MAX_VALUE) }
    var lastId   by remember { mutableStateOf(Int.MAX_VALUE) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore   by remember { mutableStateOf(true) }

    val pageSize     = 20
    val coroutineScope = rememberCoroutineScope()
    val listState    = rememberLazyListState()

    var deleteTarget by remember { mutableStateOf<TodoItem?>(null) }
    var editTarget   by remember { mutableStateOf<TodoItem?>(null) }

    val tags by db.todoDao().getAllTags().collectAsState(initial = emptyList())

    val groupedTodos = remember(todoList) {
        todoList
            .sortedByDescending { it.createdAt }
            .groupBy {
                val cal1 = Calendar.getInstance()
                val cal2 = Calendar.getInstance()
                cal2.timeInMillis = it.createdAt
                when {
                    isSameDay(cal1, cal2) -> "Today"
                    isYesterday(cal2)     -> "Yesterday"
                    else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.createdAt))
                }
            }
    }

    fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val newData = db.todoDao().getBefore(lastTime, lastId, pageSize)
            if (newData.isEmpty()) {
                hasMore = false
            } else {
                todoList = (todoList + newData).distinctBy { it.id }.sortedByDescending { it.createdAt }
                val lastItem = newData.last()
                lastTime = lastItem.createdAt
                lastId   = lastItem.id
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadMore() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            val lastVisible = visibleItems.lastOrNull()?.index ?: return@collect
            if (!isLoading && hasMore && lastVisible >= todoList.size - 5) loadMore()
        }
    }

    LaunchedEffect(Unit) {
        SmsReceiver.newTodoEvent.collectLatest {
            val refreshed = withContext(Dispatchers.IO) {
                db.todoDao().getBefore(Long.MAX_VALUE, Int.MAX_VALUE, pageSize)
            }
            todoList = refreshed
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text       = "Recent",
                color      = TextSecondary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier   = Modifier.padding(bottom = 12.dp),
            )

            if (todoList.isEmpty() && !isLoading) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = TextMuted,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No payments yet", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        Text("Add one below", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    state                = listState,
                    reverseLayout        = true,
                    contentPadding       = PaddingValues(bottom = 8.dp),
                    verticalArrangement  = Arrangement.spacedBy(10.dp),
                ) {
                    if (isLoading) {
                        item(key = "loader") {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color       = AccentViolet,
                                    modifier    = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    groupedTodos.forEach { (date, todos) ->
                        items(todos, key = { it.id }) { todo ->
                            val dateText = remember(todo.createdAt) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(todo.createdAt))
                            }
                            PaymentItem(
                                todo     = todo,
                                dateText = dateText,
                                onClick  = { editTarget = it },
                                onDelete = { deleteTarget = it },
                            )
                        }

                        item {
                            val totalSpent = todos.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(BgCardAlt)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text          = date.uppercase(),
                                        color         = TextSecondary,
                                        fontWeight    = FontWeight.SemiBold,
                                        fontSize      = 10.sp,
                                        letterSpacing = 0.8.sp,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(TextMuted),
                                    )
                                    Text(
                                        text       = "₹${"%.0f".format(totalSpent)}",
                                        color      = AccentAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    // Delete dialog
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title            = { Text("Delete payment?", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val item = deleteTarget ?: return@launch
                        withContext(Dispatchers.IO) { db.todoDao().deleteById(item.id) }
                        todoList    = todoList.filter { it.id != item.id }
                        deleteTarget = null
                    }
                }) { Text("Delete", color = AccentCoral) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // Edit dialog
    if (editTarget != null) {
        var payee    by remember(editTarget) { mutableStateOf(editTarget!!.payee) }
        var amount   by remember(editTarget) { mutableStateOf(editTarget!!.amount) }
        var tag      by remember(editTarget) { mutableStateOf(editTarget!!.tag ?: "") }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title            = { Text("Edit payment", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value         = payee ?: "",
                        onValueChange = { payee = it },
                        label         = { Text("Payee") },
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentViolet,
                            unfocusedBorderColor = BgCardAlt,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = AccentViolet,
                            focusedLabelColor    = AccentViolet,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = amount,
                        onValueChange = { amount = it },
                        label         = { Text("Amount") },
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = AccentViolet,
                            unfocusedBorderColor = BgCardAlt,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = AccentViolet,
                            focusedLabelColor    = AccentViolet,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded        = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value         = tag,
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Tag") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier      = Modifier.menuAnchor(),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AccentViolet,
                                unfocusedBorderColor = BgCardAlt,
                                focusedTextColor     = TextPrimary,
                                unfocusedTextColor   = TextPrimary,
                                cursorColor          = AccentViolet,
                                focusedLabelColor    = AccentViolet,
                            ),
                        )
                        ExposedDropdownMenu(
                            expanded        = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            tags.forEach {
                                DropdownMenuItem(
                                    text    = { Text(it) },
                                    onClick = { tag = it; expanded = false },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val updatedItem = editTarget!!
                        db.todoDao().updateTodo(
                            id        = updatedItem.id,
                            payee     = payee ?: "",
                            amount    = amount,
                            tag       = tag,
                            updatedAt = System.currentTimeMillis(),
                        )
                        todoList  = todoList.map {
                            if (it.id == updatedItem.id) it.copy(payee = payee, amount = amount, tag = tag, updatedAt = System.currentTimeMillis()) else it
                        }
                        editTarget = null
                    }
                }) { Text("Save", color = AccentViolet, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentItem(
    todo:     TodoItem,
    dateText: String,
    onClick:  (TodoItem) -> Unit,
    onDelete: (TodoItem) -> Unit,
) {
    val avatarColors = listOf(
        Color(0xFF7B61FF), Color(0xFF00E5A0), Color(0xFFFFB74D),
        Color(0xFF40C4FF), Color(0xFFFF8A65), Color(0xFF4DD0E1),
        Color(0xFFAED581), Color(0xFFF06292),
    )
    val avatarColor = remember(todo.tag) {
        avatarColors[((todo.tag?.hashCode() ?: todo.payee?.hashCode() ?: 0) and 0x7FFFFFFF) % avatarColors.size]
    }
    val initial = (todo.payee?.firstOrNull() ?: todo.tag?.firstOrNull() ?: 'U').uppercaseChar()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap      = { onClick(todo) },
                    onLongPress = { onDelete(todo) },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = initial.toString(),
                    color      = avatarColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 18.sp,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        !todo.payee.isNullOrBlank() -> todo.payee
                        !todo.tag.isNullOrBlank()   -> todo.tag!!
                        else                        -> "UPI"
                    },
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    maxLines   = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!todo.tag.isNullOrBlank()) {
                        Text(todo.tag!!, color = avatarColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(TextMuted))
                    }
                    Text(dateText, color = TextSecondary, fontSize = 11.sp)
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "₹${todo.amount}",
                    color      = AccentCoral,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 17.sp,
                )
                if (todo.source == "MANUAL") {
                    Text(
                        text          = "manual",
                        color         = TextMuted,
                        fontSize      = 9.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

fun isYesterday(cal: Calendar): Boolean {
    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}
