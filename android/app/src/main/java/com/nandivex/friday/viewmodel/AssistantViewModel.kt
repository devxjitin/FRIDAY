package com.nandivex.friday.viewmodel

import android.app.Application
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nandivex.friday.AppForegroundService
import com.nandivex.friday.BuildConfig
import com.nandivex.friday.data.AccessGate
import com.nandivex.friday.data.ApiKeyStore
import com.nandivex.friday.data.AppSettingsStore
import com.nandivex.friday.data.ConversationHistoryStore
import com.nandivex.friday.data.ConversationStore
import com.nandivex.friday.data.ConversationTurn
import com.nandivex.friday.data.UsageDay
import com.nandivex.friday.data.UsageStore
import com.nandivex.friday.data.DeveloperSaveFailedException
import com.nandivex.friday.data.ReplyStyle
import com.nandivex.friday.data.MemoryStore
import com.nandivex.friday.data.DeviceToolRunner
import com.nandivex.friday.data.saveDeveloperConfiguration
import com.nandivex.friday.data.MicSensitivity
import com.nandivex.friday.network.GeminiTextClient
import com.nandivex.friday.util.UserInputException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        /** Outlives the ViewModel, so usage counted while it is being cleared is still saved. */
        val usageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val MAX_PICKED_IMAGE_BYTES = 25 * 1024 * 1024
        /** Past exchanges shown on the History page at first, and added by each "Show older". */
        private const val PAST_CONVERSATION_PAGE = 50
    }

    private val conversationStore = ConversationStore(application)
    /** Every finished exchange, kept for long-term memory. Opened on first use, off the main thread. */
    private val conversationHistoryStore by lazy {
        ConversationHistoryStore(com.nandivex.friday.data.AppObjectBox.get(application))
    }
    private val apiKeyStore = ApiKeyStore(application)
    private val settingsStore = AppSettingsStore(application)
    private val memoryStore = MemoryStore(application)
    private val usageStore = UsageStore(application)


    private val audioManager = application.getSystemService(Application.AUDIO_SERVICE) as AudioManager


    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()


    /** Mic level while listening, playback level while speaking. Kept out of [uiState] because it changes
     *  ~25 times a second and only the voice ribbon needs it; putting it in the state recomposed the whole screen. */
    private val _voiceLevel = MutableStateFlow(0f)
    val voiceLevel: StateFlow<Float> = _voiceLevel.asStateFlow()

    private val session: VoiceSessionController = VoiceSessionController(
        application = application,
        scope = viewModelScope,
        uiState = _uiState,
        voiceLevel = _voiceLevel,
        audioManager = audioManager,
        settingsStore = settingsStore,
        conversationStore = conversationStore,
        memoryStore = memoryStore,
        historyStoreProvider = { conversationHistoryStore },
        resolveApiKey = { resolveApiKey() },
        hasGeminiAccess = { hasGeminiAccess() },
        execute = { call, trace -> toolRunner.run(call, trace) },
        showToast = { message, type -> showToast(message, type) },
        recordUsage = { recordUsage(it) },
        recordVoiceUsage = { recordVoiceUsage() },
        markVoiceUsageStart = { voiceUsageStartMs = SystemClock.elapsedRealtime() },
        openDeveloperPage = { openPage(AppPage.DEVELOPER) }
    )


    private fun notificationStatus(state: VoiceState, tool: String?, last: ConversationTurn?): Pair<String, String> {
        fun clip(t: String) = t.trim().replace(Regex("""\s+"""), " ").let { if (it.length > 140) it.take(140) + "…" else it }
        val lastUser = last?.userText?.takeIf { it.isNotBlank() }?.let { "You: " + clip(it) }
        val lastReply = last?.assistantText?.takeIf { it.isNotBlank() }?.let { "F.R.I.D.A.Y.: " + clip(it) }
        return when {
            tool != null -> "F.R.I.D.A.Y.: Working" to "Using tool: " + tool.replace('_', ' ')
            state == VoiceState.CONNECTING || state == VoiceState.RESETTING -> "F.R.I.D.A.Y.: Connecting" to "Opening a voice session…"
            state == VoiceState.LISTENING -> "F.R.I.D.A.Y.: Listening" to (lastReply ?: "Waiting for your command.")
            state == VoiceState.PROCESSING -> "F.R.I.D.A.Y.: Thinking" to (lastUser ?: "Working on your request…")
            state == VoiceState.SPEAKING -> "F.R.I.D.A.Y.: Speaking" to (lastReply ?: lastUser ?: "Replying…")
            state == VoiceState.ERROR -> "F.R.I.D.A.Y.: Error" to (_uiState.value.errorMessage ?: "Something went wrong.")
            else -> "F.R.I.D.A.Y.: Idle" to (lastReply ?: lastUser ?: "Standing by.")
        }
    }

    // Kept as fields so onCleared can tell whether the static hooks still point at this instance: after a restart the
    // new view model registers before the old one is cleared, and the old one must not wipe the new one's hooks.
    private val stopFromNotification: () -> Unit = { session.stopSession() }
    private val muteFromNotification: () -> Unit = { onMuteToggled() }

    init {
        loadWhatsNew()
        // The notification mirrors the session: "listening" (with a Stop button) while one is
        // active, "ready" otherwise. A reconnect passes through RESETTING, so it never flickers.
        AppForegroundService.onStopRequested = stopFromNotification
        AppForegroundService.onMuteRequested = muteFromNotification
        viewModelScope.launch {
            usageStore.days.catch { e -> Log.e("AssistantError", "Failed to read usage", e) }
                .collect { days -> _uiState.update { it.copy(usageDays = days) } }
        }
        viewModelScope.launch {
            _uiState.map { it.isMicMuted }.distinctUntilChanged().collect {
                AppForegroundService.setMuted(getApplication(), it)
            }
        }
        viewModelScope.launch {
            _uiState.map { isSessionActive(it.voiceState) }.distinctUntilChanged().collect { active ->
                AppForegroundService.setListening(getApplication(), active)
                if (!active) {
                    session.backgroundSessionAllowed = false
                }
            }
        }
        // The notification also tells what the assistant is doing, stamped with the time.
        viewModelScope.launch {
            combine(
                _uiState.map { it.voiceState to it.activeTool }.distinctUntilChanged(),
                conversationStore.recentTurns.catch { emit(emptyList()) }
            ) { (state, tool), turns -> notificationStatus(state, tool, turns.lastOrNull()) }
                .distinctUntilChanged()
                .collect { (title, text) -> AppForegroundService.setStatus(getApplication(), title, text) }
        }
        // .catch on each -- a DataStore read failure (corrupted file, storage unavailable) would
        // otherwise escape this collector uncaught and crash the app; logging and letting that
        // one piece of state go stale is far better than taking the whole session down with it.
        // A session that ends in an error never reaches stopSession, so its time is counted when the error appears.
        viewModelScope.launch {
            _uiState.map { it.voiceState }.distinctUntilChanged().collect { if (it == VoiceState.ERROR) recordVoiceUsage() }
        }
        viewModelScope.launch {
            settingsStore.settings
                .catch { e -> Log.e("AssistantError", "Failed to read settings", e) }
                .collect { settings ->
                    _uiState.update {
                        it.copy(
                            settings = settings
                        )
                    }
                    session.setMicSensitivity(settings.micSensitivity)
                    session.prewarmIfPossible()
                }
        }
        viewModelScope.launch {
            memoryStore.items
                .catch { e -> Log.e("AssistantError", "Failed to read memory", e) }
                .collect { items -> _uiState.update { it.copy(memories = items) } }
        }
        viewModelScope.launch {
            memoryStore.removedByAssistant
                .catch { e -> Log.e("AssistantError", "Failed to read removed memory", e) }
                .collect { items -> _uiState.update { it.copy(removedMemories = items) } }
        }
        reloadApiKey(replaceWarmSession = false)
    }

    /** First Keystore access is slow and can fail, so it happens off the main thread. Also called after setup stores a key. */
    private fun reloadApiKey(replaceWarmSession: Boolean) {
        viewModelScope.launch {
            val resolvedKey = withContext(Dispatchers.IO) { resolveApiKey() }
            session.keysLoaded = true
            _uiState.update {
                it.copy(
                    hasApiKey = resolvedKey.isNotBlank(),
                    currentApiKey = resolvedKey
                )
            }
            if (replaceWarmSession) session.invalidateWarmSessionAndReprewarm() else session.prewarmIfPossible()
        }
    }

    /** Setup stored a new Gemini key: pick it up without restarting the app. */
    fun onApiKeyStoredBySetup() = reloadApiKey(replaceWarmSession = true)

    /** The Gemini key the user typed in Settings; blank until they add one. There is no built-in access. */
    private fun resolveApiKey(): String = apiKeyStore.getApiKey().orEmpty()

    private fun hasGeminiAccess(): Boolean = resolveApiKey().isNotBlank()

    fun onMicTapped() {
        when (_uiState.value.voiceState) {
            VoiceState.IDLE, VoiceState.ERROR -> session.startSession()
            // stopSession() clears history first and only then re-prewarms; a second prewarm from
            // here could read the not-yet-cleared history into the next warm session.
            else -> session.stopSession()
        }
    }

    /** The phone's assistant gesture opened FRIDAY: start a voice session unless one is already running. */
    fun onAssistInvoked() {
        assistPending = true
        startPendingAssist()
    }

    /** The gesture can arrive before the app is on screen and before the key check has finished (a cold start), so the
     *  request waits here until both are true; [onAppForegrounded] and [onAccessChanged] try again. */
    private var assistPending = false

    private fun startPendingAssist() {
        if (!assistPending || !AccessGate.unlocked || !session.isForeground) return
        assistPending = false
        if (!isSessionActive(_uiState.value.voiceState)) session.startSession()
    }

    /** Mutes/unmutes the user's mic without ending the session. Resets on every session boundary
     *  (see [releaseResources]). */
    fun onMuteToggled() {
        val state = _uiState.value.voiceState
        if (state == VoiceState.IDLE || state == VoiceState.ERROR) return
        val muted = !_uiState.value.isMicMuted
        session.setUserMuted(muted)
        if (muted) _voiceLevel.value = 0f
        _uiState.update { it.copy(isMicMuted = muted) }
    }

    private fun clearAdminDrafts() {
        _uiState.update { it }
    }

    private fun auxiliaryUsage(purpose: String): (Int, Int) -> Unit = { inputTokens, outputTokens ->
        recordUsage(UsageDay("", inputTokens = inputTokens.toLong(), outputTokens = outputTokens.toLong(),
            purposes = mapOf(purpose to inputTokens.toLong() + outputTokens),
            modelInput = mapOf(GeminiTextClient.PRIMARY_MODEL to inputTokens.toLong()),
            modelOutput = mapOf(GeminiTextClient.PRIMARY_MODEL to outputTokens.toLong())))
    }
    /** Text generation for titles, personas and formatting; the tokens it uses count in Usage, under [purpose]. */
    private fun auxTextClient(apiKey: String, purpose: String) =
        GeminiTextClient(apiKey, onUsage = auxiliaryUsage(purpose))

    fun onSettingsTapped() = openPage(AppPage.SETTINGS)

    fun onDeveloperTapped() = openPage(AppPage.DEVELOPER)

    fun onAboutTapped() = openPage(AppPage.ABOUT)

    fun onProfileTapped() = openPage(AppPage.PROFILE)

    fun onUsageTapped() = openPage(AppPage.USAGE)

    fun onStorageTapped() = openPage(AppPage.STORAGE)

    /** Back to the home page -- closes whichever full page is open. */
    fun onHomeTapped() {
        clearAdminDrafts()
        _uiState.update { it.copy(page = AppPage.HOME) }
    }

    /** Opens a full page. A live session keeps running behind it: the pages show a slim live bar with the status,
     *  Mute and Stop, so the mic is never streaming without a visible way to end it. */
    private fun openPage(page: AppPage) {
        if (page == AppPage.STORAGE) refreshCacheSize()
        if (page == AppPage.STORAGE || page == AppPage.HISTORY) refreshPastConversations()
        _uiState.update { it.copy(page = page) }
    }

    fun onMemoryTapped() = openPage(AppPage.MEMORY)

    fun onHistoryTapped() = openPage(AppPage.HISTORY)

    fun onHelpTapped() = openPage(AppPage.HELP)

    fun onAbilitiesTapped() = openPage(AppPage.ABILITIES)

    /**
     * Opens the email app with a feature request or bug report filled in for the developer. [onDone] gets null when
     * the email app opened, else a message for the user. (Sending from inside the app comes with accounts in Phase 4.)
     */
    fun onSendFeatureRequest(form: com.nandivex.friday.ui.components.FeatureRequest, onDone: (String?) -> Unit) {
        val kind = if (form.type == "bug") "Bug report" else "Feature request"
        val body = buildString {
            append(form.name).append("\n\n").append(form.details)
            if (form.examples.isNotBlank()) append("\n\nExamples: ").append(form.examples)
            if (form.email.isNotBlank()) append("\n\nReply to: ").append(form.email)
            append("\n\n---\nApp version: ").append(com.nandivex.friday.BuildConfig.VERSION_NAME)
            if (form.device.isNotBlank()) append("\nPhone: ").append(form.device)
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:")).apply {
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(com.nandivex.friday.ui.components.DEVELOPER_EMAIL))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "$kind: ${form.name.take(60)}")
            putExtra(android.content.Intent.EXTRA_TEXT, body)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val error = try {
            getApplication<Application>().startActivity(intent)
            null
        } catch (e: android.content.ActivityNotFoundException) {
            "No email app found. Write to ${com.nandivex.friday.ui.components.DEVELOPER_EMAIL} instead."
        }
        onDone(error)
    }

    fun onMemoryEnabledToggled(enabled: Boolean) {
        launchPersist(if (enabled) "Memory on" else "Memory off") {
            settingsStore.setMemoryEnabled(enabled)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onAddMemory(text: String) {
        viewModelScope.launch {
            try {
                if (memoryStore.add(text)) session.invalidateWarmSessionAndReprewarm()
                else showToast("Already remembered", AppNotificationType.INFO)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AssistantError", "Failed to save memory", e)
                showToast("Couldn't save that. Please try again.", AppNotificationType.ERROR)
            }
        }
    }

    fun onUpdateMemory(id: String, text: String) {
        launchPersist("Memory updated") { memoryStore.update(id, text); session.invalidateWarmSessionAndReprewarm() }
    }

    fun onDeleteMemory(id: String) {
        launchPersist("Memory deleted") { memoryStore.remove(id); session.invalidateWarmSessionAndReprewarm() }
    }

    /** Brings back a note the assistant had removed. */
    fun onRestoreMemory(id: String) {
        launchPersist("Memory restored") { memoryStore.restore(id); session.invalidateWarmSessionAndReprewarm() }
    }

    fun onClearMemory() {
        launchPersist("Memory cleared") { memoryStore.clear(); session.invalidateWarmSessionAndReprewarm() }
    }

    private var pastConversationLimit = PAST_CONVERSATION_PAGE

    /** Reloads the saved past exchanges (newest [pastConversationLimit]) and their totals for the History and Storage pages. */
    private fun refreshPastConversations() {
        viewModelScope.launch {
            try {
                val (turns, count, bytes) = withContext(Dispatchers.IO) {
                    Triple(
                        conversationHistoryStore.recent(pastConversationLimit).reversed(),
                        conversationHistoryStore.count().toInt(),
                        conversationHistoryStore.approxBytes()
                    )
                }
                _uiState.update { it.copy(pastConversations = turns, pastConversationCount = count, pastConversationBytes = bytes) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AssistantError", "Could not read past conversations", e)
            }
        }
    }

    fun onShowOlderConversations() {
        pastConversationLimit += PAST_CONVERSATION_PAGE
        refreshPastConversations()
    }

    fun onDeletePastSession(ids: List<Long>) =
        changePastConversations("Session deleted") { ids.forEach { conversationHistoryStore.delete(it) } }

    fun onClearPastConversations() = changePastConversations("Past conversations cleared") { conversationHistoryStore.clearAll() }

    private fun changePastConversations(successMessage: String, change: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { change() }
                refreshPastConversations()
                showToast(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AssistantError", "Could not change past conversations", e)
                showToast("Could not delete that. Try again.", AppNotificationType.ERROR)
            }
        }
    }

    /** Asks Android for these runtime permissions (the activity shows the dialog). */
    fun onRequestPermissions(permissions: List<String>) {
        if (permissions.isEmpty()) return
        _uiState.update {
            it.copy(permissionRequestId = it.permissionRequestId + 1, requestedPermissions = permissions)
        }
    }

    /** After an update, shows what changed since the version the user last saw. A fresh install shows nothing. */
    private fun loadWhatsNew() {
        viewModelScope.launch {
            runCatching {
                val app = getApplication<Application>()
                val current = BuildConfig.VERSION_NAME
                val lastSeen = settingsStore.lastSeenVersion()
                val info = app.packageManager.getPackageInfo(app.packageName, 0)
                // Never shown before: only a phone that was updated (not freshly installed) has something to show.
                val updated = info.lastUpdateTime - info.firstInstallTime > 60_000L
                if (lastSeen == null && !updated) {
                    settingsStore.setLastSeenVersion(current)
                    return@runCatching
                }
                val entries = withContext(Dispatchers.IO) {
                    com.nandivex.friday.data.Changelog.parse(app.assets.open("changelog.md").bufferedReader().use { it.readText() })
                }
                val toShow = com.nandivex.friday.data.WhatsNew.entriesToShow(entries, lastSeen, current)
                if (toShow.isEmpty()) settingsStore.setLastSeenVersion(current)
                else _uiState.update { it.copy(whatsNew = toShow) }
            }.onFailure { Log.w("AssistantUpdate", "What's new failed: ${it.javaClass.simpleName}") }
        }
    }

    fun onDismissWhatsNew() {
        _uiState.update { it.copy(whatsNew = emptyList()) }
        viewModelScope.launch { runCatching { settingsStore.setLastSeenVersion(BuildConfig.VERSION_NAME) } }
    }

    private var voiceUsageStartMs = 0L

    private fun recordUsage(delta: UsageDay) {
        // Not viewModelScope: that scope is already cancelled when onCleared stops a running voice session.
        usageScope.launch { runCatching { usageStore.add(delta) } }
    }

    private fun recordVoiceUsage() {
        val started = voiceUsageStartMs
        if (started == 0L) return
        voiceUsageStartMs = 0L
        val seconds = (SystemClock.elapsedRealtime() - started) / 1000
        if (seconds >= 2) recordUsage(UsageDay("", voiceSessions = 1, voiceSeconds = seconds))
    }

    /** Starts the usage counters again from zero. */
    fun onResetUsage() {
        viewModelScope.launch { runCatching { usageStore.clear() } }
    }

    /** Measures the temporary files (previews) the app keeps in its cache. */
    private fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { getApplication<Application>().cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }
            _uiState.update { it.copy(cacheBytes = bytes) }
        }
    }

    /** Deletes the temporary files. */
    fun onClearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().cacheDir.listFiles()?.forEach { entry ->
                    // Leave alone what is in use: downloads and open previews hold a lease on their files.
                    val inUse = com.nandivex.friday.data.CacheLeases.isLeased(entry.name)
                    if (!inUse) runCatching { entry.deleteRecursively() }
                }
            }
            refreshCacheSize()
        }
    }

    fun onDismissSettings() = onHomeTapped()

    /** Shows a short-lived typed notification at the top of the app. */
    private fun showToast(message: String, type: AppNotificationType = AppNotificationType.SUCCESS) {
        _uiState.update { it.copy(toastMessage = message, toastType = type, toastId = it.toastId + 1) }
    }

    /** Runs a DataStore/settings write and only announces [successMessage] once it actually
     *  commits, instead of announcing success up front and hoping. Catches storage exceptions
     *  (e.g. disk full/unavailable) that would otherwise escape this launched coroutine uncaught
     *  and crash the app -- surfaces a short retry toast instead. */
    private fun launchPersist(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                if (successMessage.isNotBlank()) showToast(successMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: UserInputException) {
                showToast(e.message ?: "Couldn't save that. Please try again.", AppNotificationType.ERROR)
            } catch (e: Exception) {
                Log.e("AssistantError", "Failed to save: $successMessage", e)
                showToast("Couldn't save that. Please try again.", AppNotificationType.ERROR)
            }
        }
    }

    /** Voice/language/prompt settings only take effect on the *next* session (only [warmClient]
     *  is invalidated -- an already-active [VoiceSessionController.liveClient] keeps whatever was live when it
     *  connected). Appends a clarifying suffix to the save confirmation whenever a session is
     *  currently active/connecting, so the toast doesn't imply the live conversation changed. */
    private fun settingsAppliedMessage(base: String): String {
        val sessionActive = _uiState.value.voiceState != VoiceState.IDLE &&
            _uiState.value.voiceState != VoiceState.ERROR
        return if (sessionActive) "$base -- applies to your next conversation" else base
    }

    fun onDismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun onClearApiKey() {
        viewModelScope.launch {
            // Keystore access is slow, so it stays off the main thread.
            val hasAccess = withContext(Dispatchers.IO) {
                apiKeyStore.clearApiKey()
                hasGeminiAccess()
            }
            _uiState.update { it.copy(hasApiKey = hasAccess, currentApiKey = "") }
            showToast("API key removed")
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onLiveModelSelected(id: String) {
        launchPersist(settingsAppliedMessage("Model updated")) {
            settingsStore.setLiveModel(id)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onOwnerNameChanged(name: String) {
        launchPersist(settingsAppliedMessage("Name saved")) {
            settingsStore.setOwnerName(name)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onVoiceSelected(voiceName: String) {
        launchPersist(settingsAppliedMessage("Voice updated")) {
            settingsStore.setVoice(voiceName)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onReplyStyleSelected(style: ReplyStyle) {
        launchPersist(settingsAppliedMessage("Replies: ${style.label}")) {
            settingsStore.setReplyStyle(style)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onLanguageSelected(code: String) {
        launchPersist(settingsAppliedMessage("Language updated")) {
            settingsStore.setLanguageCode(code)
            session.invalidateWarmSessionAndReprewarm()
        }
    }

    fun onMicSensitivitySelected(sensitivity: MicSensitivity) {
        launchPersist("Microphone sensitivity set to ${sensitivity.label}") {
            settingsStore.setMicSensitivity(sensitivity)
        }
    }

    /** Purely local mute behavior, not part of the Gemini setup message -- no need to invalidate
     *  the warm session for this one. */
    fun onBargeInToggled(enabled: Boolean) {
        launchPersist(if (enabled) "Barge-in enabled" else "Barge-in disabled") {
            settingsStore.setBargeInEnabled(enabled)
        }
    }

    private val deviceToolRunner = DeviceToolRunner(
        application, audioManager,
        getBrightnessOverride = { _uiState.value.appBrightnessPercent },
        setBrightnessOverride = { percent -> _uiState.update { it.copy(appBrightnessPercent = percent.takeIf { it >= 0 }) } },
        requestPermissions = { permissions -> onRequestPermissions(permissions) },
        describeScreen = { jpeg, prompt ->
            auxTextClient(resolveApiKey(), UsageStore.PURPOSE_SCREEN).describeImage(jpeg, prompt)
        }
    )

    private val toolRunner: ToolRunner = ToolRunner(
        state = _uiState,
        memoryStore = memoryStore,
        settingsStore = settingsStore,
        deviceToolRunner = deviceToolRunner
    )

    fun onSaveDeveloperChanges(apiKey: String) {
        if (_uiState.value.isSavingDeveloperSettings) return
        val current = _uiState.value
        val cleanApiKey = apiKey.trim()
        val apiKeyChange = cleanApiKey.takeIf { it.isNotBlank() && it != current.currentApiKey }
        _uiState.update { it.copy(isSavingDeveloperSettings = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    saveDeveloperConfiguration(apiKeyStore, apiKeyChange) {
                        settingsStore.setDeveloperSettings()
                    }
                }
                // Only now that everything is persisted does the UI/runtime state change.
                _uiState.update {
                    it.copy(
                        isSavingDeveloperSettings = false,
                        hasApiKey = cleanApiKey.isNotBlank() || it.hasApiKey,
                        currentApiKey = cleanApiKey.ifBlank { it.currentApiKey },
                        errorMessage = null,
                        voiceState = if (it.voiceState == VoiceState.ERROR) VoiceState.IDLE else it.voiceState,
                        restartRequestId = it.restartRequestId + 1
                    )
                }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isSavingDeveloperSettings = false) }
                throw e
            } catch (e: Exception) {
                Log.e("AssistantError", "Failed to save Developer settings", e)
                _uiState.update { it.copy(isSavingDeveloperSettings = false) }
                val rollbackFailed = (e as? DeveloperSaveFailedException)?.rollbackComplete == false
                showToast(
                    if (rollbackFailed) {
                        "Couldn't save changes, and the API keys may not have been restored. Please re-enter them."
                    } else {
                        "Couldn't save changes. Nothing was changed."
                    },
                    AppNotificationType.ERROR
                )
            }
        }
    }

    /** Called when the Activity actually stops (not just loses focus, e.g. a permission dialog). */
    fun onAppBackgrounded() {
        clearAdminDrafts()
        assistPending = false
        session.onAppBackgrounded()
    }

    /** The subscription screen opened or closed the app: warm up a session, or end everything that is running. */
    fun onAccessChanged(unlocked: Boolean) {
        if (unlocked) {
            session.prewarmIfPossible()
            startPendingAssist()
            return
        }
        assistPending = false
        session.onAccessLocked()
    }

    /** Called when the Activity comes back to the foreground -- re-arm the warm session so the
     *  next mic tap is fast again. */
    fun onAppForegrounded() {
        session.onAppForegrounded()
        startPendingAssist()
    }

    /** Resume also covers returning from dialogs without an intervening Activity stop. */
    fun onAppResumed() {
        session.prewarmIfPossible()
    }

    override fun onCleared() {
        super.onCleared()
        if (AppForegroundService.onStopRequested === stopFromNotification) AppForegroundService.onStopRequested = null
        if (AppForegroundService.onMuteRequested === muteFromNotification) AppForegroundService.onMuteRequested = null
        session.onCleared()
    }
}
