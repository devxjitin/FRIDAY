package com.nandivex.friday.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.nandivex.friday.BuildConfig
import com.nandivex.friday.data.InstallStep
import com.nandivex.friday.data.UpdateException
import com.nandivex.friday.data.UpdateInstaller
import com.nandivex.friday.data.UpdateManifest
import com.nandivex.friday.data.UpdateRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** What the update dialog and the About page show. Only the website release has updates; other builds stay at [None]. */
sealed interface UpdateUiState {
    data object None : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val versionName: String, val notes: String, val sizeBytes: Long) : UpdateUiState
    data class Downloading(val versionName: String, val progress: Float) : UpdateUiState
    data class Ready(val versionName: String, val needsInstallPermission: Boolean = false) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

/**
 * Checks the website for a newer release, downloads it on request and opens Android's installer. It runs only in the
 * `release` build ([BuildConfig.SELF_UPDATE]); testers get updates from Firebase App Distribution instead.
 */
internal class UpdateController(
    private val app: Application,
    private val scope: CoroutineScope,
    private val ui: MutableStateFlow<AssistantUiState>,
    private val installer: UpdateInstaller = UpdateInstaller(app)
) {
    private val prefs = app.getSharedPreferences("updates", Application.MODE_PRIVATE)
    private var job: Job? = null
    private var offered: UpdateManifest? = null
    private var downloaded: File? = null

    private val deviceKey: String
        get() = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    private fun set(state: UpdateUiState) = ui.update { it.copy(update = state) }

    /** Quiet check when the app comes to the front, at most once every 12 hours. */
    fun checkInBackground() {
        if (!BuildConfig.SELF_UPDATE) return
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_EVERY_MS) return
        check(manual = false)
    }

    /** The About page's "Check for updates": always asks, and always answers. */
    fun checkNow() {
        if (!BuildConfig.SELF_UPDATE) return
        check(manual = true)
    }

    private fun check(manual: Boolean) {
        if (job?.isActive == true) return
        if (manual) set(UpdateUiState.Checking)
        job = scope.launch {
            try {
                val manifest = installer.fetchManifest(BuildConfig.UPDATE_URLS.split(','))
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                val current = BuildConfig.VERSION_CODE.toLong()
                when {
                    manifest == null -> set(if (manual) UpdateUiState.Failed("Could not reach the update site. Try again later.") else UpdateUiState.None)
                    UpdateRules.shouldOffer(manifest, current, android.os.Build.VERSION.SDK_INT, deviceKey) -> {
                        offered = manifest
                        set(UpdateUiState.Available(manifest.versionName, manifest.notes, manifest.sizeBytes))
                    }
                    else -> set(if (manual) UpdateUiState.UpToDate else UpdateUiState.None)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                set(if (manual) UpdateUiState.Failed("Could not check for updates.") else UpdateUiState.None)
            }
        }
    }

    fun startDownload() {
        val manifest = offered ?: return
        if (job?.isActive == true) return
        set(UpdateUiState.Downloading(manifest.versionName, 0f))
        job = scope.launch {
            try {
                val file = installer.download(manifest) { p -> set(UpdateUiState.Downloading(manifest.versionName, p)) }
                installer.problemWith(file)?.let { problem ->
                    file.delete()
                    throw UpdateException(problem)
                }
                downloaded = file
                set(UpdateUiState.Ready(manifest.versionName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: UpdateException) {
                set(UpdateUiState.Failed(e.message ?: "The update failed."))
            } catch (e: Exception) {
                Log.w(TAG, "Update download failed", e)
                set(UpdateUiState.Failed("The download failed. Check your connection and try again."))
            }
        }
    }

    fun install() {
        val file = downloaded?.takeIf { it.exists() } ?: return set(UpdateUiState.None)
        val name = offered?.versionName.orEmpty()
        try {
            when (installer.install(file)) {
                InstallStep.STARTED -> Unit
                InstallStep.NEEDS_PERMISSION -> set(UpdateUiState.Ready(name, needsInstallPermission = true))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the installer", e)
            set(UpdateUiState.Failed("Android could not open the installer."))
        }
    }

    /** Closes the dialog. A download in progress is cancelled. */
    fun dismiss() {
        if (ui.value.update is UpdateUiState.Downloading) job?.cancel()
        set(UpdateUiState.None)
    }

    private companion object {
        const val TAG = "UpdateController"
        const val KEY_LAST_CHECK = "last_check"
        const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L
    }
}
