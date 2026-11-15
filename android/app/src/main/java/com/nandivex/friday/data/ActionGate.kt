package com.nandivex.friday.data

/**
 * The local safety rule for actions that reach other people. When text written by someone else (a notification, a
 * screen, a web page, the clipboard) was read earlier in the same turn, an injected line could try to make the
 * assistant message or call someone. So a sensitive tool does not run until the user has said "yes" aloud for exactly
 * that action. The yes is read from the user's own transcribed speech, never from a tool argument the model controls.
 *
 * Flow: the sensitive tool is called while outside text is in the turn, so it is blocked and remembered as pending. The
 * model asks the user. The user says yes in the next turn, and the same call is then allowed once. Anything other than a
 * yes cancels it. No network call is involved.
 */
class ActionGate {
    private var outsideText = false
    private var pending: String? = null
    private var pendingAge = 0

    /** A tool that returns text other people or apps wrote ran in this turn. */
    @Synchronized
    fun noteOutsideText() { outsideText = true }

    /** The assistant finished a turn: the next one starts clean, but a pending question stays open for the user's answer. */
    @Synchronized
    fun turnFinished() {
        outsideText = false
        // The question is open for the user's next turn only; after that it lapses.
        if (pending != null && ++pendingAge >= 2) pending = null
    }

    /** The session ended or restarted. */
    @Synchronized
    fun reset() { outsideText = false; pending = null; pendingAge = 0 }

    sealed interface Decision {
        data object Allow : Decision
        /** Blocked: ask the user [question] aloud and wait for a yes. */
        data class Ask(val question: String) : Decision
    }

    /**
     * Decides whether [tool] may run now. [key] names the exact action (who and what), [question] is what the model
     * should ask, and [userSaid] is what the user has said so far in this turn.
     */
    @Synchronized
    fun check(tool: DeviceTool, key: String, question: String, userSaid: String): Decision {
        if (tool !in SENSITIVE_TOOLS) return Decision.Allow
        val fullKey = tool.functionName + "|" + key.trim().lowercase()
        if (pending == fullKey && isAffirmative(userSaid)) {
            pending = null
            return Decision.Allow
        }
        if (!outsideText && pending != fullKey) return Decision.Allow
        // Either outside text is in this turn, or this action is awaiting a yes that has not been heard yet.
        if (pending != fullKey) pendingAge = 0
        pending = fullKey
        return Decision.Ask(question)
    }

    companion object {
        /** Actions that send something to a person, place a call or share data out of the phone. */
        val SENSITIVE_TOOLS: Set<DeviceTool> = setOf(
            DeviceTool.MAKE_CALL, DeviceTool.SEND_SMS, DeviceTool.WHATSAPP_MESSAGE, DeviceTool.WHATSAPP_CALL,
            DeviceTool.SHARE_TEXT, DeviceTool.SHARE_FILE, DeviceTool.REPLY_NOTIFICATION
        )

        private val YES = setOf(
            "yes", "yeah", "yep", "yup", "ok", "okay", "sure", "confirm", "confirmed", "send", "go", "proceed", "correct", "right",
            "haan", "han", "ha", "haa", "hanji", "haanji", "ji", "bilkul", "theek", "thik", "kardo", "bhejdo", "karo", "bhejo"
        )
        private val NO = setOf(
            "no", "nope", "nahi", "nahin", "na", "mat", "dont", "don't", "cancel", "stop", "wait", "ruko", "ruk", "galat", "wrong"
        )

        /** True for a short reply that clearly agrees, in English or Hinglish. Any no-word wins over a yes-word. */
        fun isAffirmative(text: String): Boolean {
            val words = text.lowercase().split(Regex("[^\\p{L}']+")).filter { it.isNotEmpty() }
            if (words.isEmpty() || words.size > 6) return false
            if (words.any { it in NO }) return false
            return words.any { it in YES }
        }
    }
}

/** At most [max] texts per [windowMs], so a runaway or injected loop cannot spam contacts. */
class SmsRateLimit(private val max: Int = 10, private val windowMs: Long = 3_600_000L) {
    private val sent = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(now: Long): Boolean {
        while (sent.isNotEmpty() && now - sent.first() >= windowMs) sent.removeFirst()
        if (sent.size >= max) return false
        sent.addLast(now)
        return true
    }
}

/** Phone number clean-up for dialling, texting and WhatsApp links. */
object PhoneNumbers {
    /** Digits plus a leading +, *, # kept; null when fewer than 3 digits remain. */
    fun clean(raw: String?): String? =
        raw?.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }?.takeIf { it.count { c -> c.isDigit() } >= 3 }

    /**
     * Digits for a wa.me link: international form without + or 00. A bare 10-digit number gets [defaultCountry]
     * (91, India), because most saved numbers are written without a country code.
     */
    fun forWhatsApp(raw: String?, defaultCountry: String = "91"): String? {
        val cleaned = clean(raw)?.filter { it.isDigit() || it == '+' } ?: return null
        val digits = cleaned.filter { it.isDigit() }
        return when {
            cleaned.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> defaultCountry + digits.drop(1)
            digits.length == 10 -> defaultCountry + digits
            else -> digits
        }.takeIf { it.length in 8..15 }
    }

    fun whatsAppLink(raw: String?, message: String? = null): String? {
        val number = forWhatsApp(raw) ?: return null
        val text = message?.takeIf { it.isNotBlank() }?.let { "?text=" + java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }.orEmpty()
        return "https://wa.me/$number$text"
    }
}
