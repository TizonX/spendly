package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.Screen
import com.example.expensetracker.ui.theme.*

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun FooterBar(currentScreen: Screen, onScreenChange: (Screen) -> Unit) {
    val items = listOf(
        NavItem(Screen.HOME,          Icons.Filled.Home,                 "Home"),
        NavItem(Screen.TAGS,          Icons.Filled.LocalOffer,           "Tags"),
        NavItem(Screen.EXPENSE_CHART, Icons.Filled.PieChart,             "Chart"),
        NavItem(Screen.DOWNLOAD,      Icons.Filled.Download,             "Backup"),
        NavItem(Screen.BUDGET,        Icons.Filled.AccountBalanceWallet, "Budget"),
    )

    NavigationBar(
        containerColor = BgSurface,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationBarItem(
                selected = selected,
                onClick  = { onScreenChange(item.screen) },
                icon = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) AccentViolet.copy(alpha = 0.18f) else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector        = item.icon,
                            contentDescription = item.label,
                            tint               = if (selected) AccentViolet else TextMuted,
                        )
                    }
                },
                label  = null,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
