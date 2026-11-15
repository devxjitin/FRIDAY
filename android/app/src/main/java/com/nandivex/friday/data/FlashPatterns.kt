package com.nandivex.friday.data

/** One step of a blink pattern: the torch is [on] for [ms] milliseconds. */
data class FlashStep(val on: Boolean, val ms: Long)

/** The blink patterns for flashlight_strobe, kept free of Android so they can be tested. */
object FlashPatterns {
    private const val DOT = 200L
    private const val DASH = 600L
    private const val GAP = 200L
    private const val LETTER_GAP = 600L
    private const val WORD_GAP = 1_400L

    /** One round of SOS: three dots, three dashes, three dots, then a pause. */
    fun sos(): List<FlashStep> = buildList {
        fun letter(length: Long) {
            repeat(3) { add(FlashStep(true, length)); add(FlashStep(false, GAP)) }
            add(FlashStep(false, LETTER_GAP - GAP))
        }
        letter(DOT); letter(DASH); letter(DOT)
        add(FlashStep(false, WORD_GAP - LETTER_GAP))
    }

    /** One fast blink. */
    fun strobe(): List<FlashStep> = listOf(FlashStep(true, 80L), FlashStep(false, 80L))

    fun forMode(mode: String?): List<FlashStep>? = when (mode) {
        "sos" -> sos()
        "strobe" -> strobe()
        else -> null
    }

    /** How long a pattern runs: 2 to 30 seconds, [default] when none was given. */
    fun seconds(requested: Int?, default: Int = 10): Int = (requested ?: default).coerceIn(2, 30)
}
