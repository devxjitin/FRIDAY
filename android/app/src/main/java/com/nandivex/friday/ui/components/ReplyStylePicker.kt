package com.nandivex.friday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.data.ReplyStyle
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantOutline
import com.nandivex.friday.ui.theme.AssistantSurface
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import com.nandivex.friday.ui.theme.CardShape

/** Short or Detailed replies: one card each, the chosen one has an accent border. */
@Composable
fun ReplyStylePicker(selected: ReplyStyle, onSelect: (ReplyStyle) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReplyStyle.entries.forEach { style -> ReplyStyleCard(style, style == selected) { onSelect(style) } }
    }
}

@Composable
private fun ReplyStyleCard(style: ReplyStyle, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) AssistantAccent else AssistantOutline, label = "replyStyleBorder")
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(AssistantSurface, CardShape)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, border), CardShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = AssistantAccent, unselectedColor = AssistantTextMuted),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(style.label, color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(style.hint, color = AssistantTextSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
