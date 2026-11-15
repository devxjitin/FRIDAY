package com.nandivex.friday.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaToneTest {
    @Test fun voicePersonaIsLoyalToOwnerAndSupportPerPersonTone() {
        listOf(DEFAULT_SYSTEM_PROMPT).forEach { prompt ->
            assertTrue(prompt.contains("{Owner} is the only person you answer to"))
            assertTrue(prompt.contains("Tone rule: be polite with"))
            assertTrue(prompt.contains("save_memory"))
            assertTrue(prompt.contains("You never harm"))
        }
    }

    @Test fun ownerNameFillsPlaceholdersAndFallsBackToBoss() {
        val named = KnowledgeBaseProvider.buildSystemInstruction(DEFAULT_SYSTEM_PROMPT, "Hindi", "FRIDAY", ownerName = "Asha")
        assertTrue(named.contains("Asha is the only person you answer to"))
        assertFalse(named.contains("{owner}") || named.contains("{Owner}"))
        val unnamed = KnowledgeBaseProvider.buildSystemInstruction(DEFAULT_SYSTEM_PROMPT, "Hindi", "FRIDAY")
        assertTrue(unnamed.contains("Boss is the only person you answer to"))
    }

    @Test fun policyKeepsToolNamesOutOfSpeech() {
        assertTrue(KNOWLEDGE_GROUNDING_POLICY.contains("never speak an internal name"))
        assertFalse(KNOWLEDGE_GROUNDING_POLICY.contains("tool is switched off"))
    }
}
