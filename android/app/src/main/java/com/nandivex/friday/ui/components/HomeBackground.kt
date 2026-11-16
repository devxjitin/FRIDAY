package com.nandivex.friday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nandivex.friday.ui.theme.AssistantBackground

/** A plain vertical gradient behind the home page: deep violet fading to a darker purple in dark mode, pale violet fading to a soft lilac in light mode. */
@Composable
fun HomeBackground(dark: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val spec = tween<Color>(700)
    val top by animateColorAsState(AssistantBackground, spec, label = "gradient-top")
    val middle by animateColorAsState(if (dark) Color(0xFF140F24) else Color(0xFFF3EFFC), spec, label = "gradient-middle")
    val bottom by animateColorAsState(if (dark) Color(0xFF1C1433) else Color(0xFFE6DEFA), spec, label = "gradient-bottom")
    Box(modifier.background(Brush.verticalGradient(listOf(top, middle, bottom))))
}
