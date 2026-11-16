package com.nandivex.friday.network

import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** What Google said about a Gemini API key. */
sealed interface KeyCheckResult {
    /** Google accepted the key. This does not promise the key's plan allows live voice; that shows on first use. */
    data object Valid : KeyCheckResult
    data object Invalid : KeyCheckResult

    /** The key is real but this request is not allowed (the API is off for its project, or a region limit). */
    data object NotAllowed : KeyCheckResult
    data object QuotaExceeded : KeyCheckResult
    data object NoConnection : KeyCheckResult
    data class Unexpected(val code: Int) : KeyCheckResult
}

object GeminiKeyCheck {
    private const val URL = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1"

    /**
     * Asks Google for the first page of models. That request needs a valid key but uses no tokens. The key goes in the
     * `x-goog-api-key` header, never in the address. Blocking: call it off the main thread.
     */
    fun check(key: String): KeyCheckResult {
        val request = Request.Builder().url(URL).header("x-goog-api-key", key.trim()).get().build()
        val client = HttpClients.shared.newBuilder().callTimeout(12, TimeUnit.SECONDS).build()
        return try {
            client.newCall(request).execute().use { interpret(it.code, runCatching { it.body?.string() }.getOrNull()) }
        } catch (e: IOException) {
            KeyCheckResult.NoConnection
        }
    }

    /** Turns Google's answer into a result. Separate from [check] so it can be tested without a network. */
    fun interpret(code: Int, body: String?): KeyCheckResult {
        val keyInvalid = body?.contains("API_KEY_INVALID", ignoreCase = true) == true
        return when {
            code in 200..299 -> KeyCheckResult.Valid
            code == 429 -> KeyCheckResult.QuotaExceeded
            code == 400 || code == 401 || keyInvalid -> KeyCheckResult.Invalid
            code == 403 -> KeyCheckResult.NotAllowed
            else -> KeyCheckResult.Unexpected(code)
        }
    }

    /** A plain sentence for the setup screen. */
    fun message(result: KeyCheckResult): String = when (result) {
        KeyCheckResult.Valid -> "Key accepted by Google."
        KeyCheckResult.Invalid -> "Google did not accept this key. Check that you copied all of it."
        KeyCheckResult.NotAllowed -> "This key is real, but Google does not allow it to be used here. Check that the Gemini API is enabled for its project."
        KeyCheckResult.QuotaExceeded -> "This key has reached its limit right now. Try again later, or use another key."
        KeyCheckResult.NoConnection -> "Could not reach Google. Check your internet connection and try again."
        is KeyCheckResult.Unexpected -> "Google answered with an unexpected error (${result.code}). Try again in a moment."
    }
}
