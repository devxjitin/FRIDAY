package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgeRulesTest {
    private val now = 2026

    @Test fun parsesOnlyFourDigitYears() {
        assertEquals(1990, AgeRules.parseYear(" 1990 "))
        assertNull(AgeRules.parseYear(""))
        assertNull(AgeRules.parseYear("90"))
        assertNull(AgeRules.parseYear("19900"))
        assertNull(AgeRules.parseYear("19a0"))
        assertNull(AgeRules.parseYear("-199"))
    }

    @Test fun missingFutureAndAncientYearsAreInvalid() {
        assertEquals(AgeCheck.Invalid, AgeRules.check(null, now))
        assertEquals(AgeCheck.Invalid, AgeRules.check(now + 1, now))
        assertEquals(AgeCheck.Invalid, AgeRules.check(1899, now))
    }

    @Test fun nineteenYearsOrMoreSinceBirthYearPasses() {
        assertTrue(AgeRules.check(now - 19, now) is AgeCheck.Adult)
        assertTrue(AgeRules.check(now - 40, now) is AgeCheck.Adult)
    }

    @Test fun aYearThatMayStillBeSeventeenIsRefused() {
        assertEquals(AgeCheck.Under18, AgeRules.check(now - 18, now))
        assertEquals(AgeCheck.Under18, AgeRules.check(now - 17, now))
        assertEquals(AgeCheck.Under18, AgeRules.check(now, now))
    }

    @Test fun bandsFollowTheBoundaries() {
        assertEquals(AgeBand.AGE_18_24, AgeRules.bandFor(19))
        assertEquals(AgeBand.AGE_18_24, AgeRules.bandFor(24))
        assertEquals(AgeBand.AGE_25_39, AgeRules.bandFor(25))
        assertEquals(AgeBand.AGE_25_39, AgeRules.bandFor(39))
        assertEquals(AgeBand.AGE_40_59, AgeRules.bandFor(40))
        assertEquals(AgeBand.AGE_40_59, AgeRules.bandFor(59))
        assertEquals(AgeBand.AGE_60_PLUS, AgeRules.bandFor(60))
        assertEquals(AgeBand.AGE_60_PLUS, AgeRules.bandFor(100))
    }

    @Test fun adultResultCarriesTheBand() {
        assertEquals(AgeCheck.Adult(AgeBand.AGE_60_PLUS), AgeRules.check(now - 70, now))
    }

    @Test fun messagesExistOnlyForFailures() {
        assertNull(AgeRules.message(AgeCheck.Adult(AgeBand.AGE_25_39)))
        assertTrue(AgeRules.message(AgeCheck.Under18)!!.contains("18"))
        assertTrue(AgeRules.message(AgeCheck.Invalid)!!.contains("four digits"))
    }
}
