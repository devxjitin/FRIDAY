package com.nandivex.friday.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Every colour the screens use, as one light set and one dark set. The Assistant* names below are read at use time from
 * the palette that is active, so a page never needs to know which theme it is in. Text pairs were checked for contrast
 * (4.5:1 or better for text on every surface it sits on), and [accent] is for text and outlines while [accentFill] is
 * for backgrounds that carry white text, because one teal cannot be both on a dark page.
 */
@Immutable
class AssistantPalette(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val fieldBackground: Color,
    val selectedBg: Color,
    val primary: Color,
    val accent: Color,
    val accentFill: Color,
    val accentAlt: Color,
    val amber: Color,
    val error: Color,
    val errorFill: Color,
    val success: Color,
    val info: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Fill and text colour of the main button: teal on both pages. */
    val buttonFill: Color,
    val onButtonFill: Color
)

// Light: a cool, pale page with teal accents. Text colours were checked at 4.5:1 or better on every surface (see PaletteContrastTest).
val LightPalette = AssistantPalette(
    dark = false,
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFEBEFF5),
    surfaceElevated = Color(0xFFE0E6EF),
    outline = Color(0xFFD3DBE7),
    fieldBackground = Color(0xFFFFFFFF),
    selectedBg = Color(0xFFDDE9F2),
    primary = Color(0xFF0A6573),
    accent = Color(0xFF0A6573),
    accentFill = Color(0xFF0E7C8C),
    accentAlt = Color(0xFF9A4507),
    amber = Color(0xFF9A4507),
    error = Color(0xFFB42318),
    errorFill = Color(0xFFB42318),
    success = Color(0xFF1B6E37),
    info = Color(0xFF2C5D9B),
    textPrimary = Color(0xFF0F1A2B),
    textSecondary = Color(0xFF3F4E64),
    textMuted = Color(0xFF52627A),
    buttonFill = Color(0xFF0E7C8C),
    onButtonFill = Color(0xFFFFFFFF)
)

// Dark: a deep navy page with cyan accents and an amber highlight.
val DarkPalette = AssistantPalette(
    dark = true,
    background = Color(0xFF0A0F1A),
    surface = Color(0xFF111827),
    surfaceElevated = Color(0xFF172033),
    outline = Color(0xFF22304A),
    fieldBackground = Color(0xFF0D1422),
    selectedBg = Color(0xFF14233A),
    primary = Color(0xFF3DD9EB),
    accent = Color(0xFF3DD9EB),
    accentFill = Color(0xFF0E7C8C),
    accentAlt = Color(0xFFF5A524),
    amber = Color(0xFFF5A524),
    error = Color(0xFFF87171),
    errorFill = Color(0xFFB42318),
    success = Color(0xFF4ADE80),
    info = Color(0xFF7FB2FF),
    textPrimary = Color(0xFFE6EEF8),
    textSecondary = Color(0xFFA6B4C8),
    textMuted = Color(0xFF8B9BB3),
    buttonFill = Color(0xFF0E7C8C),
    onButtonFill = Color(0xFFFFFFFF)
)

/** The palette in use. A snapshot state, so every screen that read a colour redraws when the theme changes. */
object ActivePalette {
    var current by mutableStateOf(LightPalette)
}

val AssistantBackground: Color get() = ActivePalette.current.background
val AssistantSurface: Color get() = ActivePalette.current.surface
val AssistantSurfaceElevated: Color get() = ActivePalette.current.surfaceElevated
// Hairline borders and filled-field backgrounds that give cards and inputs their soft edge.
val AssistantOutline: Color get() = ActivePalette.current.outline
val AssistantFieldBackground: Color get() = ActivePalette.current.fieldBackground
// Fill and accent text for the selected chip, card or menu row.
val AssistantSelectedBg: Color get() = ActivePalette.current.selectedBg

val AssistantPrimaryLight: Color get() = ActivePalette.current.primary
// Accent for text, icons and outlines. Use AssistantAccentFill for a background that carries white text.
val AssistantAccent: Color get() = ActivePalette.current.accent
val AssistantAccentFill: Color get() = ActivePalette.current.accentFill
val AssistantAccentAlt: Color get() = ActivePalette.current.accentAlt
val AssistantAmber: Color get() = ActivePalette.current.amber
val AssistantError: Color get() = ActivePalette.current.error
val AssistantErrorFill: Color get() = ActivePalette.current.errorFill
val AssistantSuccess: Color get() = ActivePalette.current.success
val AssistantInfo: Color get() = ActivePalette.current.info

val AssistantTextPrimary: Color get() = ActivePalette.current.textPrimary
val AssistantTextSecondary: Color get() = ActivePalette.current.textSecondary
val AssistantTextMuted: Color get() = ActivePalette.current.textMuted

/** The main button: teal with white text on both pages. */
val AssistantButtonFill: Color get() = ActivePalette.current.buttonFill
val AssistantOnButtonFill: Color get() = ActivePalette.current.onButtonFill
