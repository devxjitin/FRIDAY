package com.nandivex.friday.data

import com.nandivex.friday.network.liveFunctionDeclarations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectorsTest {
    @Test fun everyDeviceToolBelongsToExactlyOneConnector() {
        val owners = PhoneConnectors.all.flatMap { c -> c.tools.map { it to c.id } }.groupBy({ it.first }, { it.second })
        DeviceTool.entries.forEach { assertEquals("$it", 1, owners[it]?.size ?: 0) }
    }

    @Test fun functionNamesAreUnique() {
        val names = DeviceTool.entries.map { it.functionName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun screenAndNavigationAreOffOnAFreshInstall() {
        val on = effectiveDeviceTools(null)
        PhoneConnectors.all.filter { it.id == "screen" || it.id == "navigation" }.forEach { connector ->
            connector.tools.forEach { assertTrue("${it.functionName} should be off", it !in on) }
        }
    }

    /** Every extra tool makes the Live setup bigger and gives the model one more wrong choice. */
    @Test fun allConnectorsTogetherStayUnderTheToolCap() {
        val settings = AssistantSettings(enabledDeviceTools = PhoneConnectors.all.flatMap { it.tools }.toSet())
        val count = liveFunctionDeclarations(settings).length()
        assertTrue("$count tools declared", count <= TOOL_CAP)
    }

    @Test fun connectorIdsAreUniqueAndEveryOneHasAPageText() {
        val ids = ConnectorRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("$it has no info text", ConnectorRegistry.info(it) != null) }
    }

    @Test fun aFreshInstallStartsWithTheEverydayConnectors() {
        assertEquals(
            setOf(
                "wifi", "bluetooth", "network", "torch", "sound", "display", "media", "alarms", "reminders",
                "apps", "camera", "clipboard", "device_info", "weather", "web"
            ),
            ConnectorRegistry.defaultIds.toSet()
        )
        assertEquals(ConnectorRegistry.defaultIds, ConnectorRegistry.essentials.map { it.id })
    }

    @Test fun nothingPersonalIsOnByDefault() {
        val off = setOf(ConnectorCategory.CALLS_MESSAGES, ConnectorCategory.PERSONAL, ConnectorCategory.PHONE_CONTROL)
        ConnectorRegistry.all.filter { it.defaultOn }.forEach {
            assertTrue("${it.id} is on by default", it.category !in off)
        }
        assertTrue("app_usage reads private data", !ConnectorRegistry.get("app_usage").defaultOn)
    }

    @Test fun categoriesAreTheEightGroupsInOrderAndEachHasConnectors() {
        assertEquals(
            listOf("Calls & messages", "Connectivity", "Device controls", "Clock", "Apps", "Info", "Personal", "Phone control"),
            ConnectorCategory.entries.map { it.title }
        )
        ConnectorCategory.entries.forEach { c ->
            assertTrue("${c.title} is empty", ConnectorRegistry.all.any { it.category == c })
            assertTrue("${c.title} has no summary line", c.summary.isNotBlank())
        }
    }

    @Test fun everyConnectorHasToolsAndAShortLineForEachTool() {
        ConnectorRegistry.all.forEach { c ->
            assertTrue("${c.id} has no tools", c.tools.isNotEmpty())
            c.tools.forEach { assertTrue("${it.functionName} has no hint", it.hint.isNotBlank()) }
        }
    }

    @Test fun phoneSettingsFromBeforeTheSplitKeepEveryCommandWorking() {
        // Someone who had Device controls and Device info on keeps all of those tools after they were split into smaller connectors.
        val saved = setOf("set_volume=1", "set_flashlight=1", "set_brightness=1", "media_control=1", "get_wifi_info=1", "get_bluetooth_status=1", "get_battery_status=1")
        val on = effectiveDeviceTools(saved)
        listOf("sound", "torch", "display", "media", "wifi", "bluetooth", "device_info").forEach { id ->
            ConnectorRegistry.get(id).tools.forEach { assertTrue("${it.functionName} should stay on", it in on) }
        }
        assertTrue(DeviceTool.WEB_SEARCH !in on && DeviceTool.MAKE_CALL !in on)
    }

    @Test fun aToolIsDeclaredToTheModelOnlyWhenItsConnectorIsOn() {
        val oneConnector = AssistantSettings(enabledDeviceTools = ConnectorRegistry.get("torch").tools.toSet(), memoryEnabled = false)
        val names = com.nandivex.friday.network.liveFunctionDeclarations(oneConnector).let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("name") } }
        assertEquals(ConnectorRegistry.get("torch").tools.map { it.functionName }, names)
        assertTrue(DeviceTool.WIFI_TOGGLE.functionName !in names)
    }

    @Test fun riskyConnectorsLiveInTheirOwnCategories() {
        fun category(id: String) = ConnectorRegistry.get(id).category
        assertEquals(ConnectorCategory.PHONE_CONTROL, category("screen"))
        assertEquals(ConnectorCategory.PHONE_CONTROL, category("navigation"))
        assertTrue(ConnectorCategory.PHONE_CONTROL.advanced)
    }

    private companion object {
        const val TOOL_CAP = 110
    }
}
