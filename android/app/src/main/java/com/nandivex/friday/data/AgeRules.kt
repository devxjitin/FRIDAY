package com.nandivex.friday.data

/** The age range used to set the tone and pace of replies. It never changes which actions F.R.I.D.A.Y. will do. */
enum class AgeBand(val label: String) {
    AGE_18_24("18 to 24"),
    AGE_25_39("25 to 39"),
    AGE_40_59("40 to 59"),
    AGE_60_PLUS("60 and over")
}

/** The result of checking a year of birth typed in setup. */
sealed interface AgeCheck {
    /** Not a usable year (empty, not a number, in the future, or before [AgeRules.EARLIEST_YEAR]). */
    data object Invalid : AgeCheck

    /** The person is, or may still be, younger than 18. */
    data object Under18 : AgeCheck

    /** Certainly 18 or older. [band] is the tone band. */
    data class Adult(val band: AgeBand) : AgeCheck
}

/**
 * The 18+ rule. Google's Gemini API terms require users to be 18 or older, so setup stops under-18 users here.
 *
 * Only the year of birth is asked, so the exact age is unknown by up to a year. Someone born in year Y is certainly
 * 18 once the year is Y + 19; during Y + 18 they might still be 17. The rule takes the safe side and asks for Y + 19,
 * which means a person who turns 18 this year waits until January. `firestore.rules` applies the same rule on the
 * server, so the two must stay the same.
 */
object AgeRules {
    const val EARLIEST_YEAR = 1900
    private const val ADULT_AFTER_YEARS = 19

    /** Parses what the user typed. Blank or non-digit input is not a year. */
    fun parseYear(input: String): Int? = input.trim().takeIf { it.length == 4 && it.all(Char::isDigit) }?.toIntOrNull()

    fun check(yearOfBirth: Int?, currentYear: Int): AgeCheck {
        if (yearOfBirth == null || yearOfBirth < EARLIEST_YEAR || yearOfBirth > currentYear) return AgeCheck.Invalid
        val years = currentYear - yearOfBirth
        if (years < ADULT_AFTER_YEARS) return AgeCheck.Under18
        return AgeCheck.Adult(bandFor(years))
    }

    /** The band for a person who is about [years] old (the real age is [years] or [years] minus one). */
    fun bandFor(years: Int): AgeBand = when {
        years < 25 -> AgeBand.AGE_18_24
        years < 40 -> AgeBand.AGE_25_39
        years < 60 -> AgeBand.AGE_40_59
        else -> AgeBand.AGE_60_PLUS
    }

    /** What to tell the user when the check does not pass. */
    fun message(check: AgeCheck): String? = when (check) {
        AgeCheck.Invalid -> "Enter your year of birth with four digits, for example 1990."
        AgeCheck.Under18 -> "F.R.I.D.A.Y. is for people aged 18 or older, because Google's Gemini terms require it. " +
            "If you turn 18 this year, you can set it up from January."
        is AgeCheck.Adult -> null
    }
}
