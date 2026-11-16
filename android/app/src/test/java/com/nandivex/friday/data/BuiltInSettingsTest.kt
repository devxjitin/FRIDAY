package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuiltInSettingsTest {
    @Test fun `prompts default to the built-in text`() {
        val settings = AssistantSettings()
        assertEquals(DEFAULT_SYSTEM_PROMPT.trim(), settings.systemPrompt)
    }
}
