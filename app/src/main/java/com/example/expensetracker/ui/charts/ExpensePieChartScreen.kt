package com.example.expensetracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.ui.theme.*

@Composable
fun ExpensePieChartScreen() {

    val context = LocalContext.current
    val db      = AppDatabase.getDatabase(context)

    var filter by remember { mutableStateOf(ChartFilter.MONTHLY) }

    val data by when (filter) {
        ChartFilter.WEEKLY  -> db.expenseChartDao().getWeeklyExpense().collectAsState(initial = emptyList())
        ChartFilter.MONTHLY -> db.expenseChartDao().getMonthlyExpense().collectAsState(initial = emptyList())
        ChartFilter.YEARLY  -> db.expenseChartDao().getYearlyExpense().collectAsState(initial = emptyList())
    }

    val total = data.sumOf { it.totalAmount }

    val colors = listOf(
        Color(0xFF7B61FF), Color(0xFF00E5A0), Color(0xFFFF6B6B), Color(0xFFFFB74D),
        Color(0xFF40C4FF), Color(0xFFFF8A65), Color(0xFF4DD0E1), Color(0xFFAED581),
        Color(0xFFF06292), Color(0xFF80CBC4), Color(0xFFFFCC02), Color(0xFF9FA8DA),
        Color(0xFFEF9A9A), Color(0xFFA5D6A7), Color(0xFFFFAB40), Color(0xFF80DEEA),
        Color(0xFFCE93D8), Color(0xFF90CAF9), Color(0xFFF48FB1), Color(0xFFB0BEC5),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {

        // Screen title
        Text(
            text          = "BREAKDOWN",
            color         = TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 16.dp),
        )

        // Filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(BgCard)
                .padding(4.dp),
        ) {
            listOf(
                "Weekly"  to ChartFilter.WEEKLY,
                "Monthly" to ChartFilter.MONTHLY,
                "Yearly"  to ChartFilter.YEARLY,
            ).forEach { (label, chartFilter) ->
                val isSelected = filter == chartFilter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) AccentViolet else Color.Transparent)
                        .clickable { filter = chartFilter }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = label,
                        color      = if (isSelected) White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 13.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Donut chart
        if (data.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No data", color = TextSecondary, fontSize = 16.sp)
                    Text("for this period", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    var startAngle = -90f
                    data.forEachIndexed { index, item ->
                        val sweep = if (total == 0.0) 0f else ((item.totalAmount / total) * 360f).toFloat()
                        drawArc(
                            color      = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter  = true,
                        )
                        startAngle += sweep
                    }
                    // Center hole
                    drawCircle(color = BgCard, radius = size.minDimension * 0.32f)
                }
                // Center label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text          = "TOTAL",
                        color         = TextSecondary,
                        fontSize      = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Medium,
                    )
                    Text(
                        text       = "₹${"%.0f".format(total)}",
                        color      = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Legend
        Text(
            text          = "CATEGORIES",
            color         = TextSecondary,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            modifier      = Modifier.padding(bottom = 10.dp),
        )

        data.forEachIndexed { index, item ->
            val percent = if (total == 0.0) 0 else ((item.totalAmount / total) * 100).toInt()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size]),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = item.tagName,
                        color      = TextPrimary,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = "₹${"%.0f".format(item.totalAmount)}",
                        color      = AccentCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                    )
                    Text(
                        text   = "$percent%",
                        color  = TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
