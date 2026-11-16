package com.nandivex.friday.network

import com.nandivex.friday.data.AssistantSettings
import com.nandivex.friday.data.DeviceTool
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonBlockingToolsTest {
    private fun declaration(name: String): JSONObject {
        val settings = AssistantSettings(enabledDeviceTools = DeviceTool.entries.toSet())
        val all = liveFunctionDeclarations(settings)
        return (0 until all.length()).map { all.getJSONObject(it) }.first { it.getString("name") == name }
    }

    @Test fun slowToolsAreDeclaredNonBlocking() {
        listOf("look_at_screen")
            .forEach { assertEquals(it, "NON_BLOCKING", declaration(it).optString("behavior")) }
    }

    @Test fun quickToolsStayBlocking() {
        listOf("set_volume", "set_flashlight", "set_alarm", "read_screen")
            .forEach { assertFalse(it, declaration(it).has("behavior")) }
    }

    @Test fun everyNonBlockingNameIsARealTool() {
        val known = DeviceTool.entries.map { it.functionName }
        assertTrue(DeviceTool.NON_BLOCKING_NAMES.all { it in known })
    }

    @Test fun defaultToolsStillBuild() {
        val settings = AssistantSettings()
        assertTrue(liveFunctionDeclarations(settings).length() > 0)
    }
}
