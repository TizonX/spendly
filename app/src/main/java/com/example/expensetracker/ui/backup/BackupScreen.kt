package com.example.expensetracker.ui.backup

import android.content.Context
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.*

@Composable
fun BackupScreen() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { importBackupFromUri(context, it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Text(
            text          = "DATA",
            color         = TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text       = "Manage your backup",
            color      = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize   = 22.sp,
            modifier   = Modifier.padding(bottom = 32.dp),
        )

        BackupCard(
            icon        = Icons.Filled.Download,
            iconTint    = AccentMint,
            iconBg      = AccentMintDim,
            title       = "Export",
            subtitle    = "Save your data to Downloads",
            onClick     = { exportBackup(context) },
        )

        Spacer(Modifier.height(16.dp))

        BackupCard(
            icon        = Icons.Filled.Upload,
            iconTint    = AccentViolet,
            iconBg      = AccentVioletDim,
            title       = "Import",
            subtitle    = "Restore from a backup file",
            onClick     = { launcher.launch(arrayOf("application/json")) },
        )
    }
}

@Composable
private fun BackupCard(
    icon:     ImageVector,
    iconTint: Color,
    iconBg:   Color,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = title,
                tint               = iconTint,
                modifier           = Modifier.size(26.dp),
            )
        }
        Column {
            Text(title,    color = TextPrimary,   fontWeight = FontWeight.Bold,    fontSize = 18.sp)
            Text(subtitle, color = TextSecondary, fontWeight = FontWeight.Normal,  fontSize = 13.sp)
        }
    }
}

private fun exportBackup(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val db     = AppDatabase.getDatabase(context)
            val todos  = db.todoDao().getAllList()
            val tags   = db.tagDao().getAllTagsList()
            val backup = BackupData(todos, tags)
            val json   = Gson().toJson(backup)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver     = context.contentResolver
                val collection   = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "todo_backup.json")
                    put(MediaStore.MediaColumns.MIME_TYPE,    "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(collection, contentValues)
                uri?.let { resolver.openOutputStream(it, "w")?.use { stream -> stream.write(json.toByteArray()) } }
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                java.io.File(downloads, "todo_backup.json").writeText(json)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup saved to Downloads", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun importBackupFromUri(context: Context, uri: android.net.Uri) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val jsonData = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (jsonData.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Invalid or empty file", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            val backup = Gson().fromJson(jsonData, BackupData::class.java)
            val db     = AppDatabase.getDatabase(context)
            db.tagDao().insertAll(backup.tags)
            backup.todos.forEach { db.todoDao().insert(it) }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
