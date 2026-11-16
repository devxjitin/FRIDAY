package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupLogicTest {
    private fun facts(
        agreed: String? = TERMS_VERSION, name: String = "Jitin", key: Boolean = true, finished: Boolean = true
    ) = SetupFacts(agreed, name, key, finished)

    @Test fun `a fresh install starts at the welcome screen`() {
        assertEquals(SetupStep.WELCOME, SetupLogic.firstStep(SetupFacts(null, "", false, false)))
    }

    @Test fun `everything in place means setup has nothing to ask`() {
        assertNull(SetupLogic.firstStep(facts()))
    }

    @Test fun `a changed agreement asks only for the agreement again`() {
        assertEquals(SetupStep.AGREEMENT, SetupLogic.firstStep(facts(agreed = "2020-01-01")))
    }

    @Test fun `a phone with a key but no agreement or name goes to the agreement`() {
        assertEquals(SetupStep.AGREEMENT, SetupLogic.firstStep(SetupFacts(null, "", true, false)))
    }

    @Test fun `the first missing required answer decides the step`() {
        assertEquals(SetupStep.ABOUT_YOU, SetupLogic.firstStep(facts(name = "  ", finished = false)))
        assertEquals(SetupStep.API_KEY, SetupLogic.firstStep(facts(key = false, finished = false)))
    }

    @Test fun `the optional steps show until the user has finished once`() {
        assertEquals(SetupStep.VOICE, SetupLogic.firstStep(facts(finished = false)))
    }

    @Test fun `no path reaches home without agreement, name and key`() {
        val agreed = listOf(null, "old", TERMS_VERSION)
        val names = listOf("", "   ", "Jitin")
        val keys = listOf(false, true)
        for (a in agreed) for (n in names) for (k in keys) {
            val ready = SetupLogic.firstStep(SetupFacts(a, n, k, finished = true)) == null
            assertEquals("agreed=$a name='$n' key=$k", a == TERMS_VERSION && n.isNotBlank() && k, ready)
        }
    }

    @Test fun `names are trimmed and limited`() {
        assertTrue(SetupLogic.isNameValid(" Jitin "))
        assertFalse(SetupLogic.isNameValid("   "))
        assertFalse(SetupLogic.isNameValid(""))
        assertTrue(SetupLogic.isNameValid("x".repeat(MAX_ASSISTANT_NAME_LENGTH)))
        assertFalse(SetupLogic.isNameValid("x".repeat(MAX_ASSISTANT_NAME_LENGTH + 1)))
    }

    @Test fun `steps run in order and end after permissions`() {
        assertEquals(SetupStep.AGREEMENT, SetupLogic.next(SetupStep.WELCOME))
        assertEquals(SetupStep.PERMISSIONS, SetupLogic.next(SetupStep.VOICE))
        assertNull(SetupLogic.next(SetupStep.PERMISSIONS))
    }
}
