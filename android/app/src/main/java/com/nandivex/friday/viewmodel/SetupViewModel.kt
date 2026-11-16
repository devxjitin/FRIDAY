package com.nandivex.friday.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nandivex.friday.data.ApiKeyStore
import com.nandivex.friday.data.AppSettingsStore
import com.nandivex.friday.data.AssistantOptions
import com.nandivex.friday.data.SetupFacts
import com.nandivex.friday.data.SetupLogic
import com.nandivex.friday.data.SetupStep
import com.nandivex.friday.data.SetupStore
import com.nandivex.friday.data.TERMS_VERSION
import com.nandivex.friday.network.GeminiKeyCheck
import com.nandivex.friday.network.KeyCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the setup screens show. [step] is null when setup has nothing left to ask. */
data class SetupUiState(
    val step: SetupStep?,
    /** True once the user has been through the final "all set" screen. */
    val finished: Boolean,
    val ownerName: String,
    val languageCode: String,
    val voiceName: String,
    /** True when a key is already stored (a tester build, or after the check below). */
    val hasApiKey: Boolean,
    val checkingKey: Boolean = false,
    /** The last key-check outcome in words, shown under the key field. */
    val keyMessage: String? = null,
    val keyMessageIsError: Boolean = false,
    /** Raised each time a new key is stored, so the main screen reloads it. */
    val keyVersion: Int = 0,
    val error: String? = null
)

/**
 * First-run setup. It holds the user's answers as they go and saves each one when they tap Next, so closing the app
 * half-way loses nothing that was already confirmed. Nothing here talks to the voice session.
 */
class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val setupStore = SetupStore(application)
    private val settingsStore = AppSettingsStore(application)
    private val apiKeyStore = ApiKeyStore(application)

    private val _state = MutableStateFlow<SetupUiState?>(null)

    /** Null while the phone's saved answers are being read (the Keystore can be slow). */
    val state: StateFlow<SetupUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val settings = runCatching { settingsStore.settings.first() }.getOrNull()
                val hasKey = !apiKeyStore.getApiKey().isNullOrBlank()
                Triple(settings, hasKey, setupStore.agreedTermsVersion)
            }
            val (settings, hasKey, agreed) = loaded
            val facts = SetupFacts(agreed, settings?.ownerName.orEmpty(), hasKey, setupStore.finished)
            _state.value = SetupUiState(
                step = SetupLogic.firstStep(facts),
                finished = facts.finished,
                ownerName = facts.ownerName,
                languageCode = settings?.languageCode ?: AssistantOptions.defaultLanguageCode,
                voiceName = settings?.voiceName ?: AssistantOptions.defaultVoice,
                hasApiKey = hasKey
            )
        }
    }

    private fun update(change: (SetupUiState) -> SetupUiState) = _state.update { it?.let(change) }

    fun onNameChanged(name: String) = update { it.copy(ownerName = name, error = null) }
    fun onLanguageChanged(code: String) = update { it.copy(languageCode = code) }
    fun onVoiceChanged(name: String) = update { it.copy(voiceName = name) }
    fun onKeyEdited() = update { it.copy(keyMessage = null, keyMessageIsError = false) }

    /** The welcome screen's button. */
    fun onWelcomeNext() = goNext(SetupStep.WELCOME)

    /** Records that the user accepted this version of the agreement, then moves on. */
    fun onAgreementAccepted() {
        setupStore.acceptAgreement(TERMS_VERSION, System.currentTimeMillis())
        goNext(SetupStep.AGREEMENT)
    }

    /** Saves the name and language, then moves on. A blank name is refused. */
    fun onAboutYouNext() {
        val current = _state.value ?: return
        if (!SetupLogic.isNameValid(current.ownerName)) {
            update { it.copy(error = "Please enter a name, up to 24 characters.") }
            return
        }
        viewModelScope.launch {
            val saved = runCatching {
                settingsStore.setOwnerName(current.ownerName)
                settingsStore.setLanguageCode(current.languageCode)
            }
            if (saved.isFailure) {
                Log.w(TAG, "Could not save the name", saved.exceptionOrNull())
                update { it.copy(error = "Could not save that. Please try again.") }
            } else {
                goNext(SetupStep.ABOUT_YOU)
            }
        }
    }

    /** Checks the key with Google, and stores it only if Google accepts it. */
    fun onCheckAndSaveKey(input: String) {
        val key = input.trim()
        if (key.isEmpty() || _state.value?.checkingKey == true) return
        update { it.copy(checkingKey = true, keyMessage = null, keyMessageIsError = false) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { GeminiKeyCheck.check(key) }
            if (result != KeyCheckResult.Valid) {
                update { it.copy(checkingKey = false, keyMessage = GeminiKeyCheck.message(result), keyMessageIsError = true) }
                return@launch
            }
            val stored = withContext(Dispatchers.IO) { apiKeyStore.setApiKey(key) }
            if (!stored) {
                update {
                    it.copy(
                        checkingKey = false, keyMessageIsError = true,
                        keyMessage = "The key is valid, but this phone could not store it securely. Please try again."
                    )
                }
                return@launch
            }
            update {
                it.copy(
                    checkingKey = false, hasApiKey = true, keyVersion = it.keyVersion + 1,
                    keyMessage = GeminiKeyCheck.message(result), keyMessageIsError = false
                )
            }
        }
    }

    /** Moves on from the key step once a key is stored. */
    fun onKeyNext() {
        if (_state.value?.hasApiKey != true) return
        goNext(SetupStep.API_KEY)
    }

    /** Saves the voice, then moves on. */
    fun onVoiceNext() {
        val voice = _state.value?.voiceName ?: return
        viewModelScope.launch {
            runCatching { settingsStore.setVoice(voice) }.onFailure { Log.w(TAG, "Could not save the voice", it) }
            goNext(SetupStep.VOICE)
        }
    }

    /** The permissions step is the last of the set-up steps. */
    fun onPermissionsNext() = goNext(SetupStep.PERMISSIONS)

    /** The final "all set" screen was seen. */
    fun onFinished() {
        setupStore.markFinished()
        update { it.copy(finished = true) }
    }

    private fun goNext(from: SetupStep) = update { it.copy(step = SetupLogic.next(from), error = null) }

    private companion object {
        const val TAG = "SetupViewModel"
    }
}
