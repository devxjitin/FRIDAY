package com.nandivex.friday.viewmodel

import android.util.Log
import com.nandivex.friday.data.AppSettingsStore
import com.nandivex.friday.data.ActionGate
import com.nandivex.friday.data.DeviceTool
import com.nandivex.friday.data.DeviceToolRunner
import com.nandivex.friday.data.MEMORY_FORGET_TOOL
import com.nandivex.friday.data.MEMORY_SAVE_TOOL
import com.nandivex.friday.data.MemoryStore
import com.nandivex.friday.data.UntrustedContent
import com.nandivex.friday.network.GeminiLiveClient
import com.nandivex.friday.util.LatencyTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Runs the tools the voice model calls, by name, and returns the text handed back to it. */
internal class ToolRunner(
    private val state: StateFlow<AssistantUiState>,
    private val memoryStore: MemoryStore,
    private val settingsStore: AppSettingsStore,
    private val deviceToolRunner: DeviceToolRunner,
    private val onMemorySaved: (String) -> Unit = {},
    /** Stops calls, texts and shares that follow outside text until the user has said yes aloud. */
    private val gate: ActionGate = ActionGate(),
    /** What the user has said so far in this turn (transcribed). */
    private val userSaid: () -> String = { "" }
) {
    companion object {
        /** What the home screen shows while a tool runs, e.g. "Set alarm". */
        fun displayName(functionName: String): String = when {
            functionName == MEMORY_SAVE_TOOL -> "Saving to memory"
            functionName == MEMORY_FORGET_TOOL -> "Forgetting a memory"
            else -> DeviceTool.fromFunctionName(functionName)?.label ?: functionName.replace('_', ' ')
        }
    }

    suspend fun run(call: GeminiLiveClient.FunctionCall, trace: LatencyTool?): String {
        val deviceTool = DeviceTool.fromFunctionName(call.name)
        return if (deviceTool != null) {
            runDeviceTool(deviceTool, call.args)
        } else if (call.name == MEMORY_SAVE_TOOL || call.name == MEMORY_FORGET_TOOL) {
            runToolSafely(call.name) { runMemoryTool(call.name, call.args) }
        } else {
            "status=unknown_tool\nThere is no tool named ${call.name}. Do not call it again; tell the user you cannot do that with the tools you have."
        }
    }

    /** save_memory / forget_memory, called by the voice model. Memory is kept on this device only. */
    private suspend fun runMemoryTool(name: String, args: JSONObject): String {
        if (!state.value.settings.memoryEnabled) {
            return "status=disabled\nMemory is turned off by the user. Do not save or forget anything."
        }
        if (name != MEMORY_SAVE_TOOL) {
            val forgotten = memoryStore.forget(args.optString("about"))
            return when {
                forgotten.removed.isNotEmpty() -> "status=ok\nForgot: ${forgotten.removed.joinToString("; ") { it.text }}."
                forgotten.ambiguous.isNotEmpty() -> "status=ambiguous\nNothing was forgotten because several notes match: " +
                    forgotten.ambiguous.take(5).joinToString("; ") { it.text } +
                    ". Ask the user which one, then call forget_memory again with words from that note only."
                else -> "status=ok\nNothing saved matched that."
            }
        }
        val result = memoryStore.save(args.optString("fact"), args.optString("replaces").takeIf { it.isNotBlank() })
        if (result.added) onMemorySaved(args.optString("fact"))
        return buildString {
            append("status=ok\n")
            append(if (result.added) "Saved to memory." else "That is already remembered or empty.")
            if (result.replaced.isNotEmpty()) append(" Replaced: ").append(result.replaced.joinToString("; ")).append(".")
            if (result.ambiguous.isNotEmpty()) {
                append("\nNothing was deleted because the replaces hint matched several notes: ").append(result.ambiguous.joinToString("; ")).append(". ")
                append("Call save_memory again with replaces set to the exact old note, or ask the user which one is outdated.")
            }
            if (result.related.isNotEmpty()) {
                append("\nRelated older notes still saved: ").append(result.related.joinToString("; ")).append(". ")
                append("If any of them is now outdated or contradicted, call save_memory again with the same fact and replaces set to that note.")
            }
        }
    }

    /** Turns an unexpected exception in a tool into a failed result, so the model still gets an answer and the app never crashes. */
    private suspend fun runToolSafely(name: String, block: suspend () -> String): String = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("AssistantError", "Tool $name failed", e)
        "status=failed\nThat action could not be completed."
    }

    /** Runs a device tool and returns the text handed back to the voice model. */
    private suspend fun runDeviceTool(tool: DeviceTool, args: JSONObject): String {
        if (tool !in state.value.settings.enabledDeviceTools) {
            return "status=disabled\nThe user has turned this tool off. Tell them it is switched off in Settings."
        }
        gateQuestion(tool, args)?.let { return it }
        val result = UntrustedContent.track(tool, deviceToolRunner.run(tool, args))
        if (tool in UntrustedContent.TOOLS && result.startsWith("status=ok")) gate.noteOutsideText()
        return result
    }

    /** Null when the action may run; otherwise the result telling the model to ask the user first. */
    private fun gateQuestion(tool: DeviceTool, args: JSONObject): String? {
        if (tool !in ActionGate.SENSITIVE_TOOLS) return null
        val parts = listOf("name", "number", "contact", "app", "message", "text", "path", "file")
            .mapNotNull { k -> args.optString(k).trim().takeIf { it.isNotEmpty() }?.let { "$k=$it" } }
        val who = args.optString("name").ifBlank { args.optString("contact").ifBlank { args.optString("number") } }.trim()
        val what = args.optString("message").ifBlank { args.optString("text") }.trim()
        val question = when (tool) {
            DeviceTool.MAKE_CALL, DeviceTool.WHATSAPP_CALL -> "Call ${who.ifBlank { "this number" }}?"
            DeviceTool.SHARE_TEXT, DeviceTool.SHARE_FILE -> "Share this?"
            else -> "Send \"$what\" to ${who.ifBlank { "them" }}?"
        }
        return when (val decision = gate.check(tool, parts.joinToString("|"), question, userSaid())) {
            ActionGate.Decision.Allow -> null
            is ActionGate.Decision.Ask ->
                "status=confirm_needed\nText from another person or app was read in this turn, so nothing was done. " +
                    "Ask the user aloud: \"${decision.question}\" Then wait for their spoken yes. Do not call this tool again until the user has answered."
        }
    }
}
