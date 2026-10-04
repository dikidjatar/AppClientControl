package com.xeg911.appclient.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val Teal10 = Color(0xFF00201F)
val Teal20 = Color(0xFF003736)
val Teal30 = Color(0xFF00504E)
val Teal40 = Color(0xFF006A67)
val Teal80 = Color(0xFF4DDAD5)
val Teal90 = Color(0xFF70F7F1)

// Neutral
val Slate10 = Color(0xFF191C1C)
val Slate20 = Color(0xFF2E3131)
val Slate30 = Color(0xFF3F4949)
val Slate50 = Color(0xFF6F7979)
val Slate80 = Color(0xFFBEC9C8)
val Slate90 = Color(0xFFDAE5E4)
val Slate95 = Color(0xFFE8F3F2)
val Slate99 = Color(0xFFF7FAFA)

// Accent
val Amber40 = Color(0xFF7A5900)
val Amber80 = Color(0xFFF5BD3D)
val Amber90 = Color(0xFFFFDF9E)
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)
val Green40 = Color(0xFF2E6B2F)
val Green80 = Color(0xFF9BD597)

/** Semantic colors that are not part of the Material scheme. */
data class AppStatusColors(
    val success: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color,
)

val LightStatusColors = AppStatusColors(
    success = Green40,
    warning = Amber40,
    warningContainer = Amber90,
    onWarningContainer = Color(0xFF261A00),
)

val DarkStatusColors = AppStatusColors(
    success = Green80,
    warning = Amber80,
    warningContainer = Color(0xFF5C4300),
    onWarningContainer = Amber90,
)
