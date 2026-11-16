package com.nandivex.friday.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import com.nandivex.friday.ui.theme.AssistantAccent
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.ui.theme.AlertDialog
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantOutline
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/** One kind of data kept on the phone. [bytes] is null when a size makes no sense for it. */
private class StorageItem(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val detail: String,
    val bytes: Long?,
    val clearTitle: String? = null,
    val clearText: String = "",
    val onClear: (() -> Unit)? = null
)

@Composable
fun StoragePage(
    memoryCount: Int,
    cacheBytes: Long,
    hasUsage: Boolean,
    onClearCache: () -> Unit,
    onClearMemory: () -> Unit,
    onResetUsage: () -> Unit,
    modifier: Modifier = Modifier,
    pastConversationCount: Int = 0,
    pastConversationBytes: Long = 0L,
    onClearPastConversations: () -> Unit = {},
    memoryBytes: Long = 0L
) {
    var pending by remember { mutableStateOf<StorageItem?>(null) }
    pending?.let { item ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(item.clearTitle.orEmpty()) },
            text = { Text(item.clearText) },
            confirmButton = { TextButton(onClick = { pending = null; item.onClear?.invoke() }) { Text("Clear", color = AssistantError) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }

    val items = listOf(
        StorageItem(
            Icons.Outlined.Psychology, "Memories",
            "$memoryCount ${if (memoryCount == 1) "fact" else "facts"}", "Facts F.R.I.D.A.Y. remembered about you.", memoryBytes,
            "Clear memories?", "F.R.I.D.A.Y. will forget everything it remembered about you. This cannot be undone.", if (memoryCount > 0) onClearMemory else null
        ),
        StorageItem(
            Icons.Outlined.History, "Conversations",
            if (pastConversationCount == 0) "None" else "$pastConversationCount saved",
            "What you and F.R.I.D.A.Y. said. Shown on the History page.", pastConversationBytes,
            "Clear conversations?", "Your History page will be empty. This cannot be undone.",
            if (pastConversationCount > 0) onClearPastConversations else null
        ),
        StorageItem(
            Icons.Outlined.Cached, "Temporary files",
            if (cacheBytes > 0) "In use" else "Empty", "Files the app can rebuild by itself.", cacheBytes,
            "Clear temporary files?", "They will be deleted. The app makes new ones when it needs them.", if (cacheBytes > 0) onClearCache else null
        ),
        StorageItem(
            Icons.Outlined.Insights, "Usage numbers",
            if (hasUsage) "Recorded" else "Empty", "The counts on the Usage page.", null,
            "Reset usage numbers?", "The Usage page will start again from zero. This cannot be undone.", if (hasUsage) onResetUsage else null
        )
    )
    val total = items.sumOf { it.bytes ?: 0L }

    PageColumn(modifier) {
        // One number: how much room FRIDAY takes on this phone. No bars, since nothing here has a limit to fill.
        PageCard {
            Text("Used by F.R.I.D.A.Y.", color = AssistantTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(formatBytes(total), color = AssistantTextPrimary, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            val parts = items.filter { (it.bytes ?: 0L) > 0L }
            if (total > 0 && parts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val shades = listOf(AssistantAccent, AssistantAccent.copy(alpha = 0.65f), AssistantAccent.copy(alpha = 0.4f), AssistantTextMuted)
                Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    parts.forEachIndexed { i, item ->
                        Box(Modifier.weight((item.bytes ?: 1L).toFloat().coerceAtLeast(1f)).fillMaxHeight().background(shades[i % shades.size]))
                    }
                }
                Spacer(Modifier.height(10.dp))
                parts.forEachIndexed { i, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(shades[i % shades.size]))
                        Spacer(Modifier.size(10.dp))
                        Text(item.title, color = AssistantTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(formatBytes(item.bytes ?: 0L), color = AssistantTextSecondary, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("All of this is stored only on this phone.", color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        PageLabel("What is stored")
        PageCard {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(AssistantOutline))
                }
                StorageRow(item, onClear = { pending = item })
            }
        }
        Text(
            "Your Gemini key is not listed here. Remove it in Settings.",
            color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )
    }
}

@Composable
private fun StorageRow(item: StorageItem, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NeutralIcon(item.icon)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = AssistantTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            val size = item.bytes?.takeIf { it > 0 }?.let { " · ${formatBytes(it)}" }.orEmpty()
            Text(item.value + size, color = AssistantTextSecondary, fontSize = 14.sp)
            Text(item.detail, color = AssistantTextMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (item.clearTitle != null) {
            val enabled = item.onClear != null
            TextButton(onClick = onClear, enabled = enabled) {
                Text("Clear", color = if (enabled) AssistantError else AssistantTextMuted)
            }
        }
    }
}
