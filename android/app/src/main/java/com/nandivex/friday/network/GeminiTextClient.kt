package com.nandivex.friday.network

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Plain (non-streaming) Gemini text generation, used to describe a screenshot for look_at_screen. Plain text output
 * has no cross-model compatibility issue, so [PRIMARY_MODEL] and [FALLBACK_MODEL] can be tried in plain sequence.
 */
class GeminiTextClient(
    private val apiKey: String,
    /** Called with the input and output tokens Gemini reports for each successful request. */
    private val onUsage: ((inputTokens: Int, outputTokens: Int) -> Unit)? = null
) {

    companion object {
        const val PRIMARY_MODEL = "gemini-3.5-flash-lite"
        const val FALLBACK_MODEL = "gemini-3.8-flash"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    private val client = HttpClients.shared.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Describes a picture (for example a screenshot) following [prompt]. */
    suspend fun describeImage(jpeg: ByteArray, prompt: String): String {
        val contents = JSONArray().put(
            JSONObject().put(
                "parts",
                JSONArray()
                    .put(JSONObject().put("text", prompt))
                    .put(
                        JSONObject().put(
                            "inline_data",
                            JSONObject().put("mime_type", "image/jpeg").put("data", Base64.getEncoder().encodeToString(jpeg))
                        )
                    )
            )
        )
        return try {
            generateWithContents(contents, PRIMARY_MODEL, maxOutputTokens = 4_096)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (primaryError: Exception) {
            try {
                generateWithContents(contents, FALLBACK_MODEL, maxOutputTokens = 4_096)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                throw primaryError
            }
        }
    }

    private suspend fun generateWithContents(
        contents: JSONArray,
        model: String,
        maxOutputTokens: Int? = null,
        rejectTruncatedOutput: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val body = JSONObject()
            .put("contents", contents)
            .apply { if (maxOutputTokens != null) put("generationConfig", JSONObject().put("maxOutputTokens", maxOutputTokens)) }
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        // Key goes in a header, not the URL, so it can't leak through logged URLs.
        val request = Request.Builder().url(url).header("x-goog-api-key", apiKey).post(body).build()

        val call = client.newCall(request)
        // A timeout or cancel must also drop the open request, not wait for the blocking read to end.
        val watcher = watchCancellation(call)
        try { call.execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpFailure(
                    response.code, "Text generation request failed (${response.code}): ${raw.take(200)}"
                )
            }
            val parsed = JSONObject(raw)
            parsed.optJSONObject("usageMetadata")?.let {
                onUsage?.invoke(it.optInt("promptTokenCount"), it.optInt("candidatesTokenCount") + it.optInt("thoughtsTokenCount"))
            }
            val candidate = parsed.getJSONArray("candidates").getJSONObject(0)
            if (rejectTruncatedOutput && candidate.optString("finishReason") == "MAX_TOKENS") {
                throw java.io.IOException("Document formatting exceeded the 65,536-output-token limit.")
            }
            candidate
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } } catch (e: java.io.IOException) {
            // A cancelled call fails with an IOException; report it as the cancellation it is.
            currentCoroutineContext().ensureActive()
            throw e
        } finally {
            watcher.cancel()
        }
    }
}
