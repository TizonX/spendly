package com.example.expensetracker.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db      = AppDatabase.getDatabase(context)
    val tags by db.tagDao().getAllTags().collectAsState(initial = emptyList())
    val scope   = rememberCoroutineScope()

    var newTag     by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<TagEntity?>(null) }
    var editText   by remember { mutableStateOf("") }

    val tagColors = listOf(
        AccentViolet, AccentMint, AccentAmber, AccentBlue,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(16.dp),
    ) {
        Text(
            text          = "MANAGE TAGS",
            color         = TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 16.dp),
        )

        // Add tag row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value         = newTag,
                onValueChange = { newTag = it },
                placeholder   = { Text("New tag name", color = TextMuted) },
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                shape         = RoundedCornerShape(14.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentViolet,
                    unfocusedBorderColor = BgCardAlt,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AccentViolet,
                ),
            )
            Button(
                onClick = {
                    val value = newTag.trim()
                    if (value.isNotBlank()) {
                        scope.launch { db.tagDao().insert(TagEntity(name = value)) }
                        newTag = ""
                    }
                },
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentViolet,
                    contentColor   = White,
                ),
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tags, key = { it.id }) { tag ->
                val color = tagColors[tag.id % tagColors.size]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text       = tag.name,
                        modifier   = Modifier.weight(1f),
                        color      = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                    )
                    IconButton(
                        onClick  = { editTarget = tag; editText = tag.name },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint               = AccentViolet,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick  = { scope.launch { db.tagDao().deleteById(tag.id) } },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint               = AccentCoral,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title            = { Text("Rename Tag", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value         = editText,
                    onValueChange = { editText = it },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentViolet,
                        unfocusedBorderColor = BgCardAlt,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = AccentViolet,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = editTarget
                    if (target != null) scope.launch { db.tagDao().update(target.id, editText.trim()) }
                    editTarget = null
                }) { Text("Save", color = AccentViolet, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}
