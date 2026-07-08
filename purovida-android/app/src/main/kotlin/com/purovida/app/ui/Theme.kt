package com.purovida.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dark luxury color tokens for Puro Vida. The app is always dark-mode —
 * one palette, no toggling (ported from the original design system).
 */
object PuroColors {
    val text = Color(0xFFF2E2BC)
    val background = Color(0xFF0D0A06)
    val surface = Color(0xFF141009)
    val surfaceElevated = Color(0xFF1C1208)
    val card = Color(0xFF1C1208)
    val cardElevated = Color(0xFF241A0A)
    val primary = Color(0xFFC9A84C)
    val primaryForeground = Color(0xFF0D0A06)
    val primaryLight = Color(0xFFE8C96A)
    val primaryDark = Color(0xFF9E7E35)
    val secondary = Color(0xFF2A1A0A)
    val accent = Color(0xFFA05C2A)
    val accentLight = Color(0xFFC47A3F)
    val muted = Color(0xFF1E1408)
    val mutedForeground = Color(0xFF7A6244)
    val destructive = Color(0xFF8C2D20)
    val success = Color(0xFF4A7A38)
    val border = Color(0xFF2E1E0C)
    val borderLight = Color(0xFF3D2A14)
    val input = Color(0xFF1E1408)
    val tabBar = Color(0xFF100C07)
    val gold = Color(0xFFC9A84C)
    val copper = Color(0xFFB06A2B)
    val cedar = Color(0xFF8B5E3C)
    val leather = Color(0xFF6B4226)
    val cream = Color(0xFFF2E2BC)
}

/** Strength badge colors (ported verbatim). */
val strengthColors = mapOf(
    "mild" to Color(0xFF4A8C3F),
    "mild-medium" to Color(0xFF7A8C3F),
    "medium" to Color(0xFFC9A84C),
    "medium-full" to Color(0xFFC97A3F),
    "full" to Color(0xFFC94C3F),
)

private val puroColorScheme = darkColorScheme(
    primary = PuroColors.primary,
    onPrimary = PuroColors.primaryForeground,
    secondary = PuroColors.accent,
    onSecondary = PuroColors.text,
    background = PuroColors.background,
    onBackground = PuroColors.text,
    surface = PuroColors.card,
    onSurface = PuroColors.text,
    surfaceVariant = PuroColors.surfaceElevated,
    onSurfaceVariant = PuroColors.mutedForeground,
    error = PuroColors.destructive,
    outline = PuroColors.border,
    surfaceContainer = PuroColors.card,
    surfaceContainerHigh = PuroColors.cardElevated,
    surfaceContainerHighest = PuroColors.cardElevated,
    surfaceContainerLow = PuroColors.surface,
    surfaceContainerLowest = PuroColors.background,
)

@Composable
fun PuroVidaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = puroColorScheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
        ),
        content = content,
    )
}
