package com.nandivex.friday.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantAccentFill
import com.nandivex.friday.ui.theme.AssistantOnButtonFill
import com.nandivex.friday.ui.theme.AssistantSurfaceElevated
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import com.nandivex.friday.viewmodel.UpdateUiState

/** The update prompt: offer, download progress, install and errors. Shows nothing for None and Checking. */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onStart: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state is UpdateUiState.None || state is UpdateUiState.Checking) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = AssistantSurfaceElevated) {
            Column(Modifier.padding(24.dp)) {
                when (state) {
                    is UpdateUiState.Available -> {
                        Heading("Update available", "Version ${state.versionName}" + sizeText(state.sizeBytes))
                        if (state.notes.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                Body(state.notes)
                            }
                        }
                        Actions(primary = "Download", onPrimary = onStart, secondary = "Later", onSecondary = onDismiss)
                    }
                    is UpdateUiState.Downloading -> {
                        Heading("Downloading", "Version ${state.versionName}")
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress }, color = AssistantAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Body("${(state.progress * 100).toInt()}%")
                        Actions(secondary = "Cancel", onSecondary = onDismiss)
                    }
                    is UpdateUiState.Ready -> {
                        Heading("Ready to install", "Version ${state.versionName}")
                        Spacer(Modifier.height(12.dp))
                        Body(
                            if (state.needsInstallPermission) {
                                "Android needs your OK to install apps from F.R.I.D.A.Y. Allow it on the screen that opened, then come back and tap Install."
                            } else {
                                "Android will ask you to confirm. Your memories and settings stay on the phone."
                            }
                        )
                        Actions(primary = "Install", onPrimary = onInstall, secondary = "Later", onSecondary = onDismiss)
                    }
                    is UpdateUiState.UpToDate -> {
                        Heading("You are up to date", "")
                        Actions(primary = "OK", onPrimary = onDismiss)
                    }
                    is UpdateUiState.Failed -> {
                        Heading("Update", "")
                        Spacer(Modifier.height(12.dp))
                        Body(state.message)
                        Actions(primary = "OK", onPrimary = onDismiss)
                    }
                    else -> Unit
                }
            }
        }
    }
}

private fun sizeText(bytes: Long): String = if (bytes > 0) " · ${formatBytes(bytes)}" else ""

@Composable
private fun Heading(title: String, subtitle: String) {
    Text(title, color = AssistantTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    if (subtitle.isNotBlank()) Text(subtitle, color = AssistantAccent, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun Actions(primary: String? = null, onPrimary: () -> Unit = {}, secondary: String? = null, onSecondary: () -> Unit = {}) {
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (secondary != null) TextButton(onClick = onSecondary) { Text(secondary, color = AssistantTextMuted) }
        if (primary != null) {
            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = AssistantAccentFill, contentColor = AssistantOnButtonFill)
            ) { Text(primary) }
        }
    }
}
