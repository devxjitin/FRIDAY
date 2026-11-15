package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaTest {
    @Test fun `FRIDAY is the default persona and Short the default style`() {
        assertEquals(ReplyStyle.SHORT, AssistantSettings().replyStyle)
        assertEquals(fridayPersona(ReplyStyle.SHORT), AssistantSettings().systemPrompt)
        assertEquals("FRIDAY", DEFAULT_ASSISTANT_NAME)
    }

    @Test fun `an unknown stored reply style falls back to Short`() {
        assertEquals(ReplyStyle.SHORT, ReplyStyle.fromStoredValue(null))
        assertEquals(ReplyStyle.SHORT, ReplyStyle.fromStoredValue("evil"))
        assertEquals(ReplyStyle.DETAILED, ReplyStyle.fromStoredValue("DETAILED"))
    }

    @Test fun `the two styles differ and leave no placeholder behind`() {
        val short = fridayPersona(ReplyStyle.SHORT)
        val detailed = fridayPersona(ReplyStyle.DETAILED)
        assertTrue(short != detailed)
        listOf(short, detailed).forEach { assertFalse(it.contains("{style}")) }
    }

    @Test fun `the persona names FRIDAY and Nandivex and no one else`() {
        val text = fridayPersona(ReplyStyle.SHORT)
        assertTrue(text.contains("I'm FRIDAY, your phone assistant, developed by Nandivex Technologies."))
        listOf("Ultron", "Marvel", "Stark", "Iron Man", "Chandra", "Sonia", "Jitin").forEach {
            assertFalse("$it must not appear", text.contains(it, ignoreCase = true))
        }
    }

    @Test fun `the owner is called Boss until a name is set`() {
        assertTrue(fridayPersona(ReplyStyle.SHORT).withOwner("").contains("Address Boss as \"Boss\""))
        assertTrue(fridayPersona(ReplyStyle.SHORT).withOwner("Asha").contains("Address Asha as \"Asha\""))
    }

    @Test fun `the persona keeps the other-people privacy and tone rules`() {
        val text = fridayPersona(ReplyStyle.SHORT)
        assertTrue(text.contains("nothing of {owner}'s private details"))
        assertTrue(text.contains("Tone rule"))
        assertTrue(text.contains("never harm"))
    }

    @Test fun `FRIDAY defaults to a female voice`() {
        assertEquals("Female", AssistantOptions.voiceGender(AssistantOptions.defaultVoice))
    }
}
