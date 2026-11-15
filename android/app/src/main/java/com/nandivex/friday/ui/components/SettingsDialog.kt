package com.nandivex.friday.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.nandivex.friday.ui.theme.AppButtonShape
import com.nandivex.friday.ui.theme.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.data.AssistantOptions
import com.nandivex.friday.data.AssistantSettings
import com.nandivex.friday.data.DeviceTool
import com.nandivex.friday.data.MicSensitivity
import com.nandivex.friday.viewmodel.AppPage
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantAccentFill
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantSurfaceElevated
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextSecondary

// Label to page-branch index (branch 2, Knowledge, has its own bottom-bar tab instead).


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(
    currentApiKey: String,
    settings: AssistantSettings,
    onClearApiKey: () -> Unit,
    onLiveModelSelected: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onMicSensitivitySelected: (MicSensitivity) -> Unit,
    onBargeInToggled: (Boolean) -> Unit,
    onAnnounceNotificationsToggled: (Boolean) -> Unit,
    onOwnerNameChanged: (String) -> Unit = {},
    onReplyStyleSelected: (com.nandivex.friday.data.ReplyStyle) -> Unit = {},
    onSaveDeveloperChanges: (String) -> Unit,
    page: AppPage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** Reports whether the Developer draft has unsaved edits, so navigation outside this page can guard it. */
    onDirtyChanged: (Boolean) -> Unit = {},
    /** An exit requested from outside this page (e.g. bottom nav); runs only after Save/Discard. */
    pendingNavigation: (() -> Unit)? = null,
    onPendingNavigationHandled: () -> Unit = {},
    /** True while a Developer Save is running; Save is disabled so it can't be submitted twice. */
    isSavingDeveloper: Boolean = false,
) {
    var keyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var confirmingRemove by remember { mutableStateOf(false) }
    var confirmingUnsavedChanges by remember { mutableStateOf(false) }
    val developerChangesPending = keyInput != currentApiKey

    val requestDismiss = {
        if (developerChangesPending) {
            confirmingUnsavedChanges = true
        } else {
            onDismiss()
        }
    }
    BackHandler(onBack = requestDismiss)
    LaunchedEffect(developerChangesPending) { onDirtyChanged(developerChangesPending) }
    DisposableEffect(Unit) { onDispose { onDirtyChanged(false) } }
    // Leave via the outside destination if one was requested, otherwise just close the page.
    val leaveAfterConfirm = {
        confirmingUnsavedChanges = false
        val destination = pendingNavigation
        onPendingNavigationHandled()
        if (destination != null) destination() else onDismiss()
    }
    val activeBranch = when (page) {
        AppPage.SETTINGS, AppPage.DEVELOPER -> 0
        else -> 3
    }
    val developerContent: @Composable () -> Unit = {
        Column {
                    PageLabel("Gemini API key")
                    PageCard {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("AIza...") },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (keyVisible) "Hide key" else "Show key"
                                )
                            }
                        },
                        colors = assistantTextFieldColors()
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (currentApiKey.isNotBlank()) {
                            TextButton(onClick = { confirmingRemove = true }) {
                                Text("Remove", color = AssistantError)
                            }
                        }
                    }
                    }
                }
    }
    // Scoped to this page only: slightly smaller text than the app-wide typography.
    val base = androidx.compose.material3.MaterialTheme.typography
    val compactTypography = base.copy(
        titleLarge = base.titleLarge.copy(fontSize = 20.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = base.labelLarge.copy(fontSize = 13.sp)
    )
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.MaterialTheme.colorScheme,
        typography = compactTypography
    ) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        key(activeBranch) {
        val contentScrollState = rememberScrollState()
        Column(modifier = Modifier.weight(1f).verticalScroll(contentScrollState)) {
            when (activeBranch) {
                0 -> Column {
                    GeneralSettingsContent(
                        settings, onLiveModelSelected, onVoiceSelected, onLanguageSelected,
                        onMicSensitivitySelected, onBargeInToggled, onAnnounceNotificationsToggled, onOwnerNameChanged, onReplyStyleSelected
                    )
                    // The old Developer page now lives at the end of Settings.
                    Spacer(Modifier.height(24.dp))
                    developerContent()
                }
            }
        }
        if (activeBranch == 0 && developerChangesPending) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = {
                    onSaveDeveloperChanges(keyInput)
                },
                enabled = !isSavingDeveloper,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.nandivex.friday.ui.theme.AssistantButtonFill,
                    contentColor = com.nandivex.friday.ui.theme.AssistantOnButtonFill
                ),
                shape = AppButtonShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) { Text("Save changes") }
        }
        }
    }
    }

    val cancelUnsavedPrompt = {
        confirmingUnsavedChanges = false
        onPendingNavigationHandled()
    }
    if (confirmingUnsavedChanges || pendingNavigation != null) {
        AlertDialog(
            onDismissRequest = cancelUnsavedPrompt,
            shape = com.nandivex.friday.ui.theme.SheetShape,
            containerColor = AssistantSurfaceElevated,
            tonalElevation = 6.dp,
            title = { Text("Save changes?") },
            text = { Text("You have unsaved settings. Save them before leaving?") },
            confirmButton = {
                TextButton(enabled = !isSavingDeveloper, onClick = {
                    onSaveDeveloperChanges(keyInput)
                    // Stay on the page: success restarts the app, failure keeps the draft for a retry.
                    cancelUnsavedPrompt()
                }) {
                    Text("Save changes", color = AssistantAccent)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = leaveAfterConfirm) {
                        Text("Discard", color = AssistantError)
                    }
                    TextButton(onClick = cancelUnsavedPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            shape = com.nandivex.friday.ui.theme.SheetShape,
            containerColor = AssistantSurfaceElevated,
            tonalElevation = 6.dp,
            title = { Text("Remove API key?") },
            text = {
                Text("The assistant stops working until you add a key again. The active conversation, if any, isn't affected.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearApiKey()
                    confirmingRemove = false
                }) {
                    Text("Remove", color = AssistantError)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemove = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** Shared field styling so every input across Settings has the same accent-tinted border,
 *  matching the home screen's look instead of the flat default outline. */
@Composable
internal fun assistantTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = com.nandivex.friday.ui.theme.AssistantFieldBackground,
    unfocusedContainerColor = com.nandivex.friday.ui.theme.AssistantFieldBackground,
    focusedTextColor = com.nandivex.friday.ui.theme.AssistantTextPrimary,
    unfocusedTextColor = AssistantTextSecondary,
    focusedIndicatorColor = AssistantAccent,
    unfocusedIndicatorColor = com.nandivex.friday.ui.theme.AssistantOutline,
    cursorColor = AssistantAccent,
    focusedLabelColor = AssistantAccent,
    unfocusedLabelColor = AssistantAccent,
    focusedPlaceholderColor = AssistantTextMuted,
    unfocusedPlaceholderColor = AssistantTextMuted,
    focusedTrailingIconColor = AssistantTextSecondary,
    unfocusedTrailingIconColor = AssistantTextMuted
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsContent(
    settings: AssistantSettings,
    onLiveModelSelected: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onMicSensitivitySelected: (MicSensitivity) -> Unit,
    onBargeInToggled: (Boolean) -> Unit,
    onAnnounceNotificationsToggled: (Boolean) -> Unit,
    onOwnerNameChanged: (String) -> Unit,
    onReplyStyleSelected: (com.nandivex.friday.data.ReplyStyle) -> Unit
) {
    val micHints = mapOf(
        MicSensitivity.LOW to "Cuts background noise",
        MicSensitivity.MEDIUM to "Best for normal rooms",
        MicSensitivity.HIGH to "Picks up quiet voices"
    )
    Column {
        SettingsHeading("Reply style")
        ReplyStylePicker(selected = settings.replyStyle, onSelect = onReplyStyleSelected)
        SettingsHeading("Model")
        SettingsDropdown(
            value = AssistantOptions.liveModels.firstOrNull { it.id == settings.liveModel }?.label ?: settings.liveModel,
            options = AssistantOptions.liveModels.map { it.id to it.label },
            selectedId = settings.liveModel,
            onSelect = onLiveModelSelected
        )
        SettingsHeading("Voice")
        SettingsDropdown(
            value = AssistantOptions.voices.firstOrNull { it.name == settings.voiceName }
                ?.let { "${it.name}, ${it.gender}, ${it.style}" } ?: settings.voiceName,
            options = AssistantOptions.voices.map { it.name to "${it.name}, ${it.gender}, ${it.style}" },
            selectedId = settings.voiceName,
            onSelect = onVoiceSelected
        )
        SettingsHeading("Language")
        SettingsDropdown(
            value = AssistantOptions.languageLabel(settings.languageCode),
            options = AssistantOptions.languages.map { it.code to it.label },
            selectedId = settings.languageCode,
            onSelect = onLanguageSelected
        )
        SettingsHeading("Your name")
        var ownerInput by remember(settings.ownerName) { mutableStateOf(settings.ownerName) }
        OutlinedTextField(
            value = ownerInput,
            onValueChange = { ownerInput = it.take(com.nandivex.friday.data.MAX_ASSISTANT_NAME_LENGTH) },
            singleLine = true,
            placeholder = { Text("How should the assistant address you?", color = AssistantTextMuted) },
            colors = assistantTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                if (ownerInput.trim() != settings.ownerName) onOwnerNameChanged(ownerInput)
            }),
            modifier = Modifier.fillMaxWidth()
        )
        SettingsHeading("Microphone sensitivity")
        ChoiceDropdown(
            options = MicSensitivity.entries.map { it.name to "${it.label}: ${micHints.getValue(it)}" },
            selectedId = settings.micSensitivity.name,
            onSelect = { name -> onMicSensitivitySelected(MicSensitivity.fromStoredValue(name)) }
        )
        Spacer(Modifier.height(22.dp))
        ToggleCard(
            title = "Barge-in",
            hint = "Talk to interrupt. Turn off if it hears itself.",
            checked = settings.bargeInEnabled, onChange = onBargeInToggled
        )
        Spacer(Modifier.height(12.dp))
        ToggleCard(
            title = "Read out notifications",
            hint = "Tells you about new notifications while you talk. The text is shared with Gemini.",
            checked = settings.announceNotificationsEnabled, onChange = onAnnounceNotificationsToggled
        )
        DefaultAssistantSection()
    }
}

/** A labelled read-only field that opens a dropdown of [options] (id to label). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsDropdown(
    value: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    hint: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = assistantTextFieldColors()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp).background(AssistantSurfaceElevated)
        ) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = if (id == selectedId) AssistantAccent else AssistantTextSecondary) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    }
                )
            }
        }
    }
    if (hint != null) {
        Text(text = hint, color = AssistantTextMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}
