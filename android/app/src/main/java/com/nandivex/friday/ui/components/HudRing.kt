package com.nandivex.friday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nandivex.friday.ui.theme.ActivePalette
import com.nandivex.friday.viewmodel.VoiceState
import kotlin.math.cos
import kotlin.math.sin

/**
 * The F.R.I.D.A.Y. ring: three broken arcs that turn slowly, with an amber dot riding one gap. It is dim while idle and
 * brightens, thickens and turns faster with the voice level while a session is running. [amplitude] is read while
 * drawing, so only this canvas redraws as the level changes.
 */
@Composable
fun HudRing(voiceState: VoiceState, amplitude: () -> Float, modifier: Modifier = Modifier) {
    val live = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING
    val palette = ActivePalette.current
    val target = when (voiceState) {
        VoiceState.ERROR -> palette.amber
        else -> palette.accent
    }
    val color by animateColorAsState(target, tween(700), label = "hudRingColor")
    val amber = palette.amber

    // One slow turn every 40 s; the transition stops when the screen leaves composition.
    val transition = rememberInfiniteTransition(label = "hudRing")
    val turn by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "hudRingTurn"
    )
    val level = remember { floatArrayOf(0f) }

    Canvas(modifier) {
        val raw = if (live) amplitude().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f } else 0f
        level[0] += (raw - level[0]) * 0.2f
        val l = level[0]
        val alpha = (if (voiceState == VoiceState.IDLE) 0.3f else 0.5f) + l * 0.45f
        drawRing(color.copy(alpha = alpha.coerceIn(0f, 1f)), amber, turn * (1f + l * 2f), l)
    }
}

private fun DrawScope.drawRing(color: androidx.compose.ui.graphics.Color, dot: androidx.compose.ui.graphics.Color, turn: Float, level: Float) {
    val d = minOf(size.width, size.height)
    val center = Offset(size.width / 2f, size.height / 2f)
    val stroke = d * (0.03f + level * 0.012f)
    val radius = d * 0.41f
    val gap = 26f
    val arcSize = Size(radius * 2f, radius * 2f)
    val topLeft = Offset(center.x - radius, center.y - radius)
    // Faint inner reticle
    drawCircle(color.copy(alpha = color.alpha * 0.45f), radius = d * 0.31f, center = center, style = Stroke(width = d * 0.006f))
    for (i in 0 until 3) {
        drawArc(
            color = color, startAngle = turn + i * 120f + gap / 2f, sweepAngle = 120f - gap, useCenter = false,
            topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
    val a = Math.toRadians(turn.toDouble())
    drawCircle(dot, radius = d * 0.035f, center = Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat()))
}
