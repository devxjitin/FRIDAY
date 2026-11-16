package com.nandivex.friday.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** The API-key storage a Save needs. Writes report success so failures are never silent. */
interface ApiKeyStorage {
    fun getApiKey(): String?
    fun setApiKey(key: String): Boolean
    fun clearApiKey(): Boolean
}

/** The Save did not complete. [rollbackComplete] is false if restoring the old key also failed. */
class DeveloperSaveFailedException(
    val rollbackComplete: Boolean,
    cause: Throwable
) : Exception(cause)

/**
 * Applies a Save across the key store and the settings store. The key is written only when it changed (null = leave as
 * is). If any step fails before settings are committed, the key is restored to its previous value, so a failed Save never
 * leaves a mixed configuration behind. This is best-effort across two stores, not an atomic transaction.
 */
suspend fun saveDeveloperConfiguration(
    storage: ApiKeyStorage,
    newApiKey: String?,
    writeSettings: suspend () -> Unit
) {
    val previousApiKey = storage.getApiKey()
    var apiKeyTouched = false
    try {
        if (newApiKey != null) {
            apiKeyTouched = true
            check(storage.setApiKey(newApiKey)) { "Couldn't store the API key" }
        }
        writeSettings()
    } catch (e: Throwable) {
        val rollbackComplete = withContext(NonCancellable) {
            if (!apiKeyTouched) true
            else runCatching {
                if (previousApiKey == null) storage.clearApiKey() else storage.setApiKey(previousApiKey)
            }.getOrDefault(false)
        }
        if (e is CancellationException) throw e
        throw DeveloperSaveFailedException(rollbackComplete, e)
    }
}
