package com.spendlist.app.ui.theme

import androidx.compose.ui.graphics.Color

// MD3 baseline colors (fallback when dynamic color is unavailable)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Category colors
object CategoryColors {
    val AI = Color(0xFFFF6B35)
    val Infrastructure = Color(0xFF4ECDC4)
    val Entertainment = Color(0xFFFF6B6B)
    val Tools = Color(0xFF45B7D1)
    val Cloud = Color(0xFF96CEB4)
    val Domain = Color(0xFFFFEAA7)
    val Storage = Color(0xFFDDA0DD)
    val Other = Color(0xFF95A5A6)
}

// Status colors
object StatusColors {
    val Active = Color(0xFF4CAF50)
    val Cancelled = Color(0xFFFF9800)
    val Expired = Color(0xFFF44336)
}
