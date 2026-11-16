package com.nandivex.friday.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Every text colour must read on every surface it can sit on, in both themes (WCAG AA, 4.5:1). */
class PaletteContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun luminance(c: Color): Double = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { x -> luminance(b).let { y -> if (x >= y) x to y else y to x } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun surfaces(p: AssistantPalette) = mapOf(
        "background" to p.background, "surface" to p.surface, "surfaceElevated" to p.surfaceElevated,
        "fieldBackground" to p.fieldBackground, "selectedBg" to p.selectedBg
    )

    private fun texts(p: AssistantPalette) = mapOf(
        "textPrimary" to p.textPrimary, "textSecondary" to p.textSecondary, "textMuted" to p.textMuted,
        "accent" to p.accent, "warning" to p.warning, "error" to p.error, "success" to p.success, "info" to p.info
    )

    private fun check(name: String, p: AssistantPalette) {
        for ((tn, t) in texts(p)) for ((sn, s) in surfaces(p)) {
            val ratio = contrast(t, s)
            assertTrue("$name: $tn on $sn is only ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
        val fills = mapOf("accentFill" to p.accentFill, "errorFill" to p.errorFill)
        for ((fn, f) in fills) {
            val ratio = contrast(Color.White, f)
            assertTrue("$name: white text on $fn is only ${"%.2f".format(ratio)}:1", ratio >= 4.5)
        }
        assertTrue("$name: button text on button", contrast(p.onButtonFill, p.buttonFill) >= 4.5)
    }

    @Test fun lightPaletteIsReadable() = check("light", LightPalette)

    @Test fun darkPaletteIsReadable() = check("dark", DarkPalette)

    @Test fun noRedBrandColourIsLeft() {
        listOf(LightPalette, DarkPalette).forEach { p ->
            listOf(p.primary, p.accent, p.accentFill, p.buttonFill).forEach { c ->
                assertTrue("brand colour must not be red", c.red <= c.blue || c.red <= c.green)
            }
        }
    }
}
