package com.medcounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val StatusOut = Color(0xFFB71C1C)
val StatusOutBg = Color(0xFFFFEBEE)
val StatusCritical = Color(0xFFA05C00)
val StatusCriticalBg = Color(0xFFFFF3E0)
val StatusReorder = Color(0xFFC8883A)
val StatusReorderBg = Color(0xFFFFEDD5)
val StatusOk = Color(0xFF3D6B4F)
val StatusOkBg = Color(0xFFEDF2EE)

val ProfileColorHexes = listOf(
    "#3D6B4F",
    "#00695C",
    "#6A1B9A",
    "#C8883A",
    "#AD1457",
    "#2E7D32",
    "#C62828",
    "#F57F17",
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2DECA),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFFC8883A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF4F6F4),
    onBackground = Color(0xFF191C1A),
    surface = Color.White,
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404942),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF707972),
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun MedCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF3D6B4F)): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLong(16)
        if (cleaned.length == 6) Color(0xFF000000 or value) else Color(value)
    } catch (_: NumberFormatException) {
        fallback
    }
}
