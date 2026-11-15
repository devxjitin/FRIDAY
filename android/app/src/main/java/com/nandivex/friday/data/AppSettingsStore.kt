package com.nandivex.friday.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nandivex.friday.util.UserInputException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "assistant_settings")

/**
 * Connectors that are on before the user has changed anything. Navigation and screen control let the assistant read and
 * tap other apps, so they stay off until the user adds them in Connectors.
 */
val DEFAULT_CONNECTOR_IDS: List<String> get() = ConnectorRegistry.defaultIds

const val MAX_ASSISTANT_NAME_LENGTH = 24

/**
 * A phone connector is installed when any of its tools was switched on ("name=1" in the saved choices), and an installed
 * connector uses all of its tools. Older versions let single tools be switched on or off, so a connector can have been left
 * with only one tool on; that counts as installed and gets the rest of its tools, instead of staying half-working.
 */
fun effectiveDeviceTools(overrides: Set<String>?): Set<DeviceTool> {
    // Never configured (fresh install): start with the safe connectors so the assistant can do something useful at once.
    // Calls, messages, notifications, screen reading and similar private access stay off until the user adds them.
    if (overrides == null) return DEFAULT_CONNECTOR_IDS.flatMap { PhoneConnectors.tools(it) }.toSet()
    val on = overrides.orEmpty().mapNotNull {
        val parts = it.split("=", limit = 2)
        if (parts.size == 2 && parts[1] == "1") parts[0] else null
    }.toSet()
    return PhoneConnectors.all.filter { connector -> connector.tools.any { it.functionName in on } }
        .flatMap { it.tools }.toSet()
}

data class AssistantSettings(
    /** Gemini Live model id; one of [AssistantOptions.liveModels]. */
    val liveModel: String = AssistantOptions.defaultLiveModel,
    val voiceName: String = AssistantOptions.defaultVoice,
    val micSensitivity: MicSensitivity = MicSensitivity.MEDIUM,
    /** BCP-47 code for one of the kiosk's supported languages. */
    val languageCode: String = AssistantOptions.defaultLanguageCode,
    /** On by default. When on, the mic stays live while the assistant is speaking (relying on
     *  VOICE_COMMUNICATION's echo cancellation) so the user can interrupt it mid-reply; when
     *  off, the mic mutes during playback as before. */
    val bargeInEnabled: Boolean = true,
    /** Off by default. When on, a phone notification that arrives during a voice session is read out by the assistant. */
    val announceNotificationsEnabled: Boolean = false,
    /** When on, the assistant saves lasting facts about the user and recalls them in later sessions. */
    val memoryEnabled: Boolean = true,
    /** Short (default) or Detailed replies, picked in Settings. */
    val replyStyle: ReplyStyle = ReplyStyle.SHORT,
    /** The persona text for [replyStyle]; the fixed policy is added on top when a session starts. */
    val systemPrompt: String = fridayPersona(replyStyle),
    /** Name shown on the home screen and given to the assistant in its prompt. It is fixed. */
    val assistantName: String = DEFAULT_ASSISTANT_NAME,
    /** Who the assistant serves; empty until the user sets it, then the prompt says "Boss". */
    val ownerName: String = "",
    /** Device tools the voice model is allowed to call; each is switched on/off in Settings. */
    val enabledDeviceTools: Set<DeviceTool> = effectiveDeviceTools(null),
)

/** Persists the user's Live-session customization choices (voice, language, prompt, etc). */
class AppSettingsStore(private val context: Context) {

    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")
    private val liveModelKey = stringPreferencesKey("live_model")
    private val voiceKey = stringPreferencesKey("voice_name")
    private val micSensitivityKey = stringPreferencesKey("mic_sensitivity")
    private val languageCodeKey = stringPreferencesKey("language_code")
    private val bargeInKey = booleanPreferencesKey("barge_in_enabled")
    private val announceNotificationsKey = booleanPreferencesKey("announce_notifications")
    private val memoryEnabledKey = booleanPreferencesKey("memory_enabled")
    private val systemPromptKey = stringPreferencesKey("system_prompt")
    private val replyStyleKey = stringPreferencesKey("reply_style")
    /** Set while the stored agent prompt is the one drafted from the knowledge documents (not typed by the user). */
    private val systemPromptAutoKey = booleanPreferencesKey("system_prompt_auto")
    private val assistantNameKey = stringPreferencesKey("assistant_name")
    private val ownerNameKey = stringPreferencesKey("owner_name")
    private val greetingKey = stringPreferencesKey("greeting")  // legacy; only cleared
    private val documentFormatPromptKey = stringPreferencesKey("document_format_prompt")
    private val retrievalChunkLimitKey = stringPreferencesKey("retrieval_chunk_limit")
    private val toolOverridesKey = stringSetPreferencesKey("device_tool_overrides")

    val settings: Flow<AssistantSettings> = context.settingsDataStore.data.map { prefs ->
        AssistantSettings(
            liveModel = prefs[liveModelKey]
                ?.takeIf(AssistantOptions::isApprovedLiveModel)
                ?: AssistantOptions.defaultLiveModel,
            voiceName = prefs[voiceKey]
                ?.takeIf(AssistantOptions::isApprovedVoice)
                ?: AssistantOptions.defaultVoice,
            micSensitivity = MicSensitivity.fromStoredValue(prefs[micSensitivityKey]),
            languageCode = prefs[languageCodeKey]
                ?.takeIf(AssistantOptions::isApprovedLanguage)
                ?: AssistantOptions.defaultLanguageCode,
            bargeInEnabled = prefs[bargeInKey] ?: true,
            announceNotificationsEnabled = prefs[announceNotificationsKey] ?: false,
            memoryEnabled = prefs[memoryEnabledKey] ?: true,
            replyStyle = ReplyStyle.fromStoredValue(prefs[replyStyleKey]),
            systemPrompt = fridayPersona(ReplyStyle.fromStoredValue(prefs[replyStyleKey])),
            assistantName = DEFAULT_ASSISTANT_NAME,
            ownerName = prefs[ownerNameKey]?.trim().orEmpty(),
            enabledDeviceTools = effectiveDeviceTools(prefs[toolOverridesKey])
        )
    }

    /** The newest app version whose "What's new" card the user has already seen; null before the first one. */
    suspend fun lastSeenVersion(): String? = context.settingsDataStore.data.first()[lastSeenVersionKey]

    suspend fun setLastSeenVersion(version: String) {
        context.settingsDataStore.edit { it[lastSeenVersionKey] = version }
    }

    suspend fun setLiveModel(id: String) {
        if (!AssistantOptions.isApprovedLiveModel(id)) throw UserInputException("Choose a supported model.")
        context.settingsDataStore.edit { it[liveModelKey] = id }
    }

    suspend fun setVoice(voiceName: String) {
        if (!AssistantOptions.isApprovedVoice(voiceName)) throw UserInputException("Choose a supported voice.")
        context.settingsDataStore.edit { it[voiceKey] = voiceName }
    }

    suspend fun setMicSensitivity(sensitivity: MicSensitivity) {
        context.settingsDataStore.edit { it[micSensitivityKey] = sensitivity.name }
    }

    suspend fun setLanguageCode(code: String) {
        if (!AssistantOptions.isApprovedLanguage(code)) throw UserInputException("Choose a supported language.")
        context.settingsDataStore.edit { it[languageCodeKey] = code }
    }

    suspend fun setReplyStyle(style: ReplyStyle) {
        context.settingsDataStore.edit { it[replyStyleKey] = style.name }
    }

    suspend fun setMemoryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[memoryEnabledKey] = enabled }
    }

    suspend fun setOwnerName(name: String) {
        val clean = name.trim().take(MAX_ASSISTANT_NAME_LENGTH)
        context.settingsDataStore.edit { if (clean.isBlank()) it.remove(ownerNameKey) else it[ownerNameKey] = clean }
    }

    suspend fun setAnnounceNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[announceNotificationsKey] = enabled }
    }

    suspend fun setBargeInEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[bargeInKey] = enabled }
    }

    suspend fun setDeviceToolEnabled(tool: DeviceTool, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val others = (prefs[toolOverridesKey] ?: emptySet()).filterNot { it.startsWith(tool.functionName + "=") }
            prefs[toolOverridesKey] = (others + "${tool.functionName}=${if (enabled) 1 else 0}").toSet()
        }
    }

    /** Applies the Developer form together so its fields never save as unrelated partial edits. */
    suspend fun setDeveloperSettings() {
        context.settingsDataStore.edit {
            // The name is fixed and greetings are picked at random; drop anything an earlier version saved.
            it.remove(assistantNameKey)
            it.remove(greetingKey)
            // The prompts are built in and no longer editable; drop any text an earlier version saved.
            it.remove(systemPromptKey)
            it.remove(systemPromptAutoKey)
            it.remove(documentFormatPromptKey)
            it.remove(retrievalChunkLimitKey)
        }
    }
}
