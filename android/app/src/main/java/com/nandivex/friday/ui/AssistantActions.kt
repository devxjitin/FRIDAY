package com.nandivex.friday.ui

import com.nandivex.friday.ui.components.FeatureRequest
import com.nandivex.friday.ui.theme.ThemeMode
import com.nandivex.friday.data.MicSensitivity

/** Everything the screen can ask the app to do, in one place instead of 70+ separate parameters. */
class AssistantActions(
    val onMicTapped: () -> Unit,
    val onMuteToggled: () -> Unit = {},
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onRequestPermission: () -> Unit,
    val onOpenAppSettings: () -> Unit = {},
    val onSettingsTapped: () -> Unit,
    val onDeveloperTapped: () -> Unit = {},
    val onAboutTapped: () -> Unit = {},
    val onProfileTapped: () -> Unit = {},
    val onHomeTapped: () -> Unit = {},
    val onDismissWhatsNew: () -> Unit = {},
    val onUsageTapped: () -> Unit = {},
    val onStorageTapped: () -> Unit = {},
    val onClearCache: () -> Unit = {},
    val onResetUsage: () -> Unit = {},
    val onMemoryTapped: () -> Unit = {},
    val onMemoryEnabledToggled: (Boolean) -> Unit = {},
    val onAddMemory: (String) -> Unit = {},
    val onUpdateMemory: (String, String) -> Unit = { _, _ -> },
    val onDeleteMemory: (String) -> Unit = {},
    val onClearMemory: () -> Unit = {},
    val onRestoreMemory: (String) -> Unit = {},
    val onHistoryTapped: () -> Unit = {},
    val onHelpTapped: () -> Unit = {},
    val onAbilitiesTapped: () -> Unit = {},
    val onSendFeatureRequest: (FeatureRequest, (String?) -> Unit) -> Unit = { _, _ -> },
    val onDeletePastSession: (List<Long>) -> Unit = {},
    val onClearPastConversations: () -> Unit = {},
    val onShowOlderConversations: () -> Unit = {},
    val onDismissSettings: () -> Unit,
    val onClearApiKey: () -> Unit,
    val onLiveModelSelected: (String) -> Unit,
    val onVoiceSelected: (String) -> Unit,
    val onLanguageSelected: (String) -> Unit,
    val onMicSensitivitySelected: (MicSensitivity) -> Unit,
    val onBargeInToggled: (Boolean) -> Unit,
    val onOwnerNameChanged: (String) -> Unit = {},
    val onReplyStyleSelected: (com.nandivex.friday.data.ReplyStyle) -> Unit = {},
    val onSaveDeveloperChanges: (String) -> Unit,
    val onDismissToast: () -> Unit,
)
