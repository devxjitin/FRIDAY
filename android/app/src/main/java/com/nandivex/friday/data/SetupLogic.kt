package com.nandivex.friday.data

import android.content.Context

/**
 * The version of the agreement text the user accepts in setup. Raise it whenever the agreement, the Terms or the Privacy
 * Policy change in a way that matters; everyone then sees the agreement screen once more, and only that screen.
 */
const val TERMS_VERSION = "2026-11-16.2"

/** The steps of first-run setup, in order. The phone control and the final "all set" screens come after them. */
enum class SetupStep { WELCOME, AGREEMENT, ABOUT_YOU, API_KEY, VOICE, PERMISSIONS }

/** What the phone already holds, which decides where setup starts. */
data class SetupFacts(
    val agreedTermsVersion: String?,
    val ownerName: String,
    val hasApiKey: Boolean,
    /** True once the user has been through the "all set" screen. */
    val finished: Boolean
)

object SetupLogic {
    /** A name FRIDAY can use: not blank once trimmed, and within [MAX_ASSISTANT_NAME_LENGTH]. */
    fun isNameValid(input: String): Boolean = input.trim().length in 1..MAX_ASSISTANT_NAME_LENGTH

    fun agreementCurrent(facts: SetupFacts): Boolean = facts.agreedTermsVersion == TERMS_VERSION

    /**
     * The step to show first. A fresh install starts at the welcome screen. A phone that already has some of the
     * answers (for example a tester build) jumps to the first one that is missing, and an accepted agreement is only
     * asked again when [TERMS_VERSION] has changed. After the required answers, the optional voice and permission steps
     * are shown until the user has finished once. Null means setup has nothing left to ask.
     */
    fun firstStep(facts: SetupFacts): SetupStep? {
        val fresh = facts.agreedTermsVersion == null && !isNameValid(facts.ownerName) && !facts.hasApiKey
        return when {
            fresh -> SetupStep.WELCOME
            !agreementCurrent(facts) -> SetupStep.AGREEMENT
            !isNameValid(facts.ownerName) -> SetupStep.ABOUT_YOU
            !facts.hasApiKey -> SetupStep.API_KEY
            !facts.finished -> SetupStep.VOICE
            else -> null
        }
    }

    /** The step after [step] when the user taps Next, or null after the last one. */
    fun next(step: SetupStep): SetupStep? = SetupStep.entries.getOrNull(step.ordinal + 1)
}

/** The agreement the user accepted and whether they have seen the "all set" screen. Small and read at start-up, so plain preferences. */
class SetupStore(context: Context) {
    private val prefs = context.getSharedPreferences("setup", Context.MODE_PRIVATE)

    val agreedTermsVersion: String? get() = prefs.getString(KEY_TERMS_VERSION, null)

    /** Epoch milliseconds when the agreement was accepted, 0 if never. */
    val agreedAtMillis: Long get() = prefs.getLong(KEY_TERMS_AT, 0)

    val finished: Boolean get() = prefs.getBoolean(KEY_FINISHED, false)

    fun acceptAgreement(version: String, nowMillis: Long) {
        prefs.edit().putString(KEY_TERMS_VERSION, version).putLong(KEY_TERMS_AT, nowMillis).apply()
    }

    fun markFinished() {
        prefs.edit().putBoolean(KEY_FINISHED, true).apply()
    }

    private companion object {
        const val KEY_TERMS_VERSION = "terms_version"
        const val KEY_TERMS_AT = "terms_accepted_at"
        const val KEY_FINISHED = "finished"
    }
}
