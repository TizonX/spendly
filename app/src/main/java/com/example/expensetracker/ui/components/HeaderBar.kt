package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(monthlyTotal: Double) {
    TopAppBar(
        navigationIcon = {
            Column(modifier = Modifier.padding(start = 20.dp, top = 2.dp)) {
                Text(
                    text       = "Spendly",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 22.sp,
                    color      = AccentViolet,
                )
                Text(
                    text          = "track every rupee",
                    fontSize      = 10.sp,
                    color         = TextSecondary,
                    letterSpacing = 0.4.sp,
                )
            }
        },
        title = {},
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentVioletDim)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text          = "THIS MONTH",
                        fontSize      = 9.sp,
                        color         = TextSecondary,
                        letterSpacing = 1.sp,
                        fontWeight    = FontWeight.Medium,
                    )
                    Text(
                        text       = "₹${"%.0f".format(monthlyTotal)}",
                        fontSize   = 20.sp,
                        color      = AccentCoral,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgSurface,
        ),
    )
}
