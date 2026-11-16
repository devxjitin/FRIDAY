package com.nandivex.friday.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Custom rounded-stroke nav icons (24x24 viewport). Colour comes from the `Icon` tint. */
object AssistantIcons {

    /** Soft rounded house with an orb "door", echoing the voice-orb identity. */
    val Home: ImageVector by lazy {
        icon("AssistantHome") {
            path(stroke = ink, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.5f, 10.5f)
                lineTo(12f, 4f)
                lineTo(19.5f, 10.5f)
                verticalLineTo(18.5f)
                quadTo(19.5f, 20.5f, 17.5f, 20.5f)
                horizontalLineTo(6.5f)
                quadTo(4.5f, 20.5f, 4.5f, 18.5f)
                close()
                circle(12f, 14.5f, 2.6f)
            }
        }
    }

    /** Open book with a small spark: knowledge that "lights up" the assistant. */
    val Knowledge: ImageVector by lazy {
        icon("AssistantKnowledge") {
            path(stroke = ink, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 8.5f)
                curveTo(10.5f, 7f, 7.5f, 6.5f, 4f, 7f)
                verticalLineTo(19f)
                curveTo(7.5f, 18.5f, 10.5f, 19f, 12f, 20.5f)
                curveTo(13.5f, 19f, 16.5f, 18.5f, 20f, 19f)
                verticalLineTo(7f)
                curveTo(16.5f, 6.5f, 13.5f, 7f, 12f, 8.5f)
                verticalLineTo(20.5f)
                moveTo(19f, 1.5f)
                verticalLineTo(4.5f)
                moveTo(17.5f, 3f)
                horizontalLineTo(20.5f)
            }
        }
    }

    /** Ring with a sound-wave inside: past conversations. */

    /** Three sliders instead of a gear. */
    val Settings: ImageVector by lazy {
        icon("AssistantSettings") {
            path(stroke = ink, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 6f); horizontalLineTo(12.8f)
                moveTo(17.2f, 6f); horizontalLineTo(20f)
                circle(15f, 6f, 2.2f)
                moveTo(4f, 12f); horizontalLineTo(5.8f)
                moveTo(10.2f, 12f); horizontalLineTo(20f)
                circle(8f, 12f, 2.2f)
                moveTo(4f, 18f); horizontalLineTo(11.8f)
                moveTo(16.2f, 18f); horizontalLineTo(20f)
                circle(14f, 18f, 2.2f)
            }
        }
    }

    private const val STROKE = 1.8f
    private val ink = SolidColor(Color.Black)

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
        close()
    }

    /** Menu icons: round-capped 2 dp outlines drawn from SVG path data on a 24 dp grid. */
    private fun outline(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toNodes(),
                    stroke = ink, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    val MenuHome: ImageVector by lazy { outline("MenuHome", "M4 11l8-7 8 7v9H4z") }
    val MenuHistory: ImageVector by lazy { outline("MenuHistory", "M3 12a9 9 0 1 0 3-6.7M3 4v5h5M12 8v5l3 2") }
    val MenuAbilities: ImageVector by lazy { outline("MenuAbilities", "M9 11l2 2 4-4M12 3l8 3v6c0 4-3 7-8 9-5-2-8-5-8-9V6z") }
    val MenuMemory: ImageVector by lazy { outline("MenuMemory", "M12 3a6 6 0 0 0-4 10.5V17h8v-3.5A6 6 0 0 0 12 3zM9 21h6") }
    val MenuStorage: ImageVector by lazy { outline("MenuStorage", "M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3") }
    val MenuUsage: ImageVector by lazy { outline("MenuUsage", "M5 20V10M12 20V4M19 20v-7") }
    val MenuProfile: ImageVector by lazy { outline("MenuProfile", "M8 8a4 4 0 1 0 8 0 4 4 0 1 0-8 0zM4 21c1-4 4-6 8-6s7 2 8 6") }
    val MenuSettings: ImageVector by lazy { outline("MenuSettings", "M9 12a3 3 0 1 0 6 0 3 3 0 1 0-6 0zM12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2") }
    val MenuHelp: ImageVector by lazy { outline("MenuHelp", "M3 12a9 9 0 1 0 18 0 9 9 0 1 0-18 0zM9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7M12 17v.1") }
    val MenuAbout: ImageVector by lazy { outline("MenuAbout", "M3 12a9 9 0 1 0 18 0 9 9 0 1 0-18 0zM12 11v6M12 7.5v.1") }
}
