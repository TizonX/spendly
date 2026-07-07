package com.example.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary            = AccentViolet,
    onPrimary          = White,
    primaryContainer   = AccentVioletDim,
    onPrimaryContainer = TextPrimary,
    secondary          = AccentMint,
    onSecondary        = BgDeep,
    secondaryContainer = AccentMintDim,
    tertiary           = AccentCoral,
    onTertiary         = White,
    background         = BgDeep,
    onBackground       = TextPrimary,
    surface            = BgSurface,
    onSurface          = TextPrimary,
    surfaceVariant     = BgCard,
    onSurfaceVariant   = TextSecondary,
    outline            = BgCardAlt,
    error              = AccentCoral,
    onError            = White,
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content,
    )
}
