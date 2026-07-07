package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ManualEntryBar(onSubmit: (amount: String, tag: String) -> Unit) {
    var amount          by remember { mutableStateOf("") }
    var selectedTag     by remember { mutableStateOf("") }
    var tagError        by remember { mutableStateOf(false) }
    var userSelectedTag by remember { mutableStateOf(false) }

    val context            = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val db                 = AppDatabase.getDatabase(context)
    val tags by db.tagDao().getAllTags().collectAsState(initial = emptyList())

    val scrollState = rememberScrollState()
    val chipOffsets = remember { mutableStateMapOf<String, Int>() }

    // Scroll to the selected chip whenever selection changes
    LaunchedEffect(selectedTag) {
        val x = chipOffsets[selectedTag] ?: return@LaunchedEffect
        scrollState.animateScrollTo(x)
    }

    // Auto-suggest tag from history when amount changes (debounced, skipped if user already picked a tag)
    LaunchedEffect(amount) {
        if (userSelectedTag) return@LaunchedEffect
        val parsed = amount.toDoubleOrNull() ?: return@LaunchedEffect
        delay(400)
        if (userSelectedTag) return@LaunchedEffect
        val tolerance = maxOf(parsed * 0.20, 5.0)
        val suggested = db.todoDao().getMostUsedTagNearAmount(parsed - tolerance, parsed + tolerance)
        if (suggested != null && tags.any { it.name == suggested }) {
            selectedTag = suggested
        }
    }

    val submitEntry = {
        val validTag = tags.any { it.name == selectedTag }
        when {
            !validTag         -> tagError = true
            amount.isBlank()  -> { /* do nothing — send button is visually disabled */ }
            else -> {
                onSubmit(amount, selectedTag)
                amount          = ""
                selectedTag     = ""
                userSelectedTag = false
                tagError        = false
                keyboardController?.hide()
            }
        }
    }

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = BgSurface,
        shadowElevation = 24.dp,
        shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // Tag chips
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(bottom = if (tagError) 6.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    val isSelected = selectedTag == tag.name
                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            selectedTag     = tag.name
                            userSelectedTag = true
                            tagError        = false
                        },
                        label = {
                            Text(
                                text       = tag.name,
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor         = BgCardAlt,
                            selectedContainerColor = AccentViolet,
                            labelColor             = TextSecondary,
                            selectedLabelColor     = White,
                        ),
                        modifier = Modifier.onGloballyPositioned { coords ->
                            chipOffsets[tag.name] = coords.positionInParent().x.toInt()
                        },
                    )
                }
            }

            // Tag error
            if (tagError) {
                Text(
                    text       = "Select a tag to continue",
                    color      = AccentCoral,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }

            // Input + send
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { amount = it },
                    placeholder   = { Text("Enter amount", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon   = {
                        Text(
                            text       = "₹",
                            color      = AccentViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            modifier   = Modifier.padding(start = 4.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction    = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitEntry() },
                    ),
                    modifier   = Modifier.weight(1f),
                    singleLine = true,
                    shape      = RoundedCornerShape(16.dp),
                    colors     = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentViolet,
                        unfocusedBorderColor = BgCardAlt,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = AccentViolet,
                    ),
                )

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (amount.isNotBlank()) AccentViolet else BgCardAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { submitEntry() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            tint               = if (amount.isNotBlank()) White else TextMuted,
                        )
                    }
                }
            }
        }
    }
}
