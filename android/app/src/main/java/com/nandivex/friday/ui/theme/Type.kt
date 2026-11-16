package com.nandivex.friday.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The phone's own system font everywhere: nothing is bundled, and it follows the user's font and text size settings. */
private val SystemFont = FontFamily.Default

/** Built per palette: the text colours are read when this runs, so a switch between light and dark must build it again. */
fun assistantTypography() = Typography(
    displayLarge = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = AssistantTextPrimary),
    headlineMedium = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = AssistantTextPrimary),
    // AlertDialog titles (Settings, confirmation dialogs) use this style by default.
    headlineSmall = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = AssistantTextPrimary),
    titleLarge = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = AssistantTextPrimary),
    titleMedium = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = AssistantTextPrimary),
    titleSmall = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AssistantTextPrimary),
    bodyLarge = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = AssistantTextPrimary),
    bodyMedium = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = AssistantTextSecondary),
    bodySmall = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, color = AssistantTextSecondary),
    labelLarge = TextStyle(fontFamily = SystemFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AssistantTextSecondary)
)
