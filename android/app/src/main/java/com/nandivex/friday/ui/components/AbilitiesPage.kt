package com.nandivex.friday.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.data.Abilities
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/**
 * Lists everything F.R.I.D.A.Y. can do on the phone. It only informs: every ability is built in and always on, so there is
 * nothing to switch on or off here.
 */
@Composable
fun AbilitiesPage(modifier: Modifier = Modifier) {
    PageColumn(modifier) {
        Text(
            "Everything here is built in and always on. Just say what you want done.",
            color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Abilities.groups.forEach { group ->
            PageLabel(group.title)
            PageCard {
                Text(group.summary, color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                group.tools.forEach { tool ->
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth()) {
                        Text(tool.label, color = AssistantTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(tool.hint, color = AssistantTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
