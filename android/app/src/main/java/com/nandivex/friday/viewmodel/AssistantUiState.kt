package com.nandivex.friday.viewmodel

import com.nandivex.friday.data.AssistantSettings
import com.nandivex.friday.data.MemoryItem
import com.nandivex.friday.data.StoredTurn

/** The page on screen. Exactly one is shown at a time; HOME is the voice screen. */
enum class AppPage { HOME, PROFILE, MEMORY, HISTORY, CONNECTORS, SETTINGS, USAGE, STORAGE, DEVELOPER, HELP, ABOUT }


enum class AppNotificationType { SUCCESS, ERROR, WARNING, INFO }

enum class VoiceState {
    IDLE,
    CONNECTING,
    LISTENING,
    /** The user has stopped talking (mic's gone quiet after saying something) and the assistant
     *  hasn't started replying yet -- distinct from LISTENING so the UI doesn't look
     *  stuck/unresponsive during that gap. */
    PROCESSING,
    SPEAKING,
    /** The Live session was closed by the server (session/time limit) while the user was still
     *  talking to the assistant -- a fresh session is being opened automatically to continue. */
    RESETTING,
    ERROR
}

data class AssistantUiState(
    val page: AppPage = AppPage.HOME,
    val voiceState: VoiceState = VoiceState.IDLE,
    /** User pressed Mute during a session: mic audio isn't sent, the session stays open. */
    val isMicMuted: Boolean = false,
    /** Tokens the live session has used so far; 0 when no session is running. Drawn as a ring around the mic. */
    val liveContextTokens: Int = 0,
    /** Changes since the version the user last saw; the "What's new" card shows while this is not empty. */
    val whatsNew: List<com.nandivex.friday.data.ChangelogEntry> = emptyList(),
    /** Per-day usage counters, oldest first. */
    val usageDays: List<com.nandivex.friday.data.UsageDay> = emptyList(),
    /** Bytes of temporary files (previews) the app can clear. */
    val cacheBytes: Long = 0L,
    /** Facts the assistant remembers about the user, newest last. */
    val memories: List<MemoryItem> = emptyList(),
    /** Notes the assistant removed itself, newest last; the Memory page offers to restore them. */
    val removedMemories: List<MemoryItem> = emptyList(),
    /** Saved past exchanges, newest first, as many as the Memory page has loaded. */
    val pastConversations: List<StoredTurn> = emptyList(),
    /** How many exchanges are saved in all, for the Memory and Storage pages. */
    val pastConversationCount: Int = 0,
    /** Rough space the saved exchanges take; see ConversationHistoryStore.approxBytes. */
    val pastConversationBytes: Long = 0L,
    val errorMessage: String? = null,
    /** Name of the tool the assistant is running right now (or just ran), shown on the home screen. */
    val activeTool: String? = null,
    val hasApiKey: Boolean = false,
    val currentApiKey: String = "",
    val settings: AssistantSettings = AssistantSettings(),
    /** A short-lived, typed notification shown at the top of the app. */
    val toastMessage: String? = null,
    val toastType: AppNotificationType = AppNotificationType.SUCCESS,
    val toastId: Int = 0,
    /** Increments only after Developer settings are safely persisted, requesting an app restart. */
    val restartRequestId: Int = 0,
    /** True while a Developer Save is in flight; blocks duplicate submissions. */
    val isSavingDeveloperSettings: Boolean = false,
    /** Window brightness override (1-100) set by the brightness tool; null follows the system. */
    val appBrightnessPercent: Int? = null,
    /** Bumps each time a tool needs Android permissions; the activity shows the dialog for [requestedPermissions]. */
    val permissionRequestId: Int = 0,
    val requestedPermissions: List<String> = emptyList(),
)
