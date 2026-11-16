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
 * for backgrounds that carry white text, because one purple cannot be both on a dark page.
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
    val warning: Color,
    val error: Color,
    val errorFill: Color,
    val success: Color,
    val info: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Fill and text colour of the main button: purple with white text on the light page, light purple with dark text on the dark page. */
    val buttonFill: Color,
    val onButtonFill: Color
)

// Light: a pale violet page with purple accents. Text colours were checked at 4.5:1 or better on every surface (see PaletteContrastTest).
val LightPalette = AssistantPalette(
    dark = false,
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF3EFFC),
    outline = Color(0xFFDDD6EE),
    fieldBackground = Color(0xFFFFFFFF),
    selectedBg = Color(0xFFEDE7FB),
    primary = Color(0xFF5B2EC9),
    accent = Color(0xFF5B2EC9),
    accentFill = Color(0xFF5B2EC9),
    accentAlt = Color(0xFF5B2EC9),
    warning = Color(0xFF9A4507),
    error = Color(0xFFB3261E),
    errorFill = Color(0xFFB3261E),
    success = Color(0xFF1B6E3C),
    info = Color(0xFF2C5D9B),
    textPrimary = Color(0xFF1B1530),
    textSecondary = Color(0xFF5A5470),
    textMuted = Color(0xFF6B6584),
    buttonFill = Color(0xFF5B2EC9),
    onButtonFill = Color(0xFFFFFFFF)
)

// Dark: a deep violet page with a light purple accent. The main button is light purple with dark text.
val DarkPalette = AssistantPalette(
    dark = true,
    background = Color(0xFF0F0B1A),
    surface = Color(0xFF19132B),
    surfaceElevated = Color(0xFF221A38),
    outline = Color(0xFF2E2548),
    fieldBackground = Color(0xFF140F24),
    selectedBg = Color(0xFF2A2046),
    primary = Color(0xFFB79CFF),
    accent = Color(0xFFB79CFF),
    accentFill = Color(0xFF5B2EC9),
    accentAlt = Color(0xFFB79CFF),
    warning = Color(0xFFFFB454),
    error = Color(0xFFFF8A80),
    errorFill = Color(0xFFB3261E),
    success = Color(0xFF6FDB9A),
    info = Color(0xFF8FB8FF),
    textPrimary = Color(0xFFF1EDFB),
    textSecondary = Color(0xFFB9B1D1),
    textMuted = Color(0xFFA097BC),
    buttonFill = Color(0xFFB79CFF),
    onButtonFill = Color(0xFF0F0B1A)
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
val AssistantWarning: Color get() = ActivePalette.current.warning
val AssistantError: Color get() = ActivePalette.current.error
val AssistantErrorFill: Color get() = ActivePalette.current.errorFill
val AssistantSuccess: Color get() = ActivePalette.current.success
val AssistantInfo: Color get() = ActivePalette.current.info

val AssistantTextPrimary: Color get() = ActivePalette.current.textPrimary
val AssistantTextSecondary: Color get() = ActivePalette.current.textSecondary
val AssistantTextMuted: Color get() = ActivePalette.current.textMuted

/** The main button fill and the text colour that sits on it. */
val AssistantButtonFill: Color get() = ActivePalette.current.buttonFill
val AssistantOnButtonFill: Color get() = ActivePalette.current.onButtonFill
