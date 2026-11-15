package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityToolsTest {
    @Test fun sosIsThreeShortThreeLongThreeShort() {
        val on = FlashPatterns.sos().filter { it.on }.map { it.ms }
        assertEquals(listOf(200L, 200L, 200L, 600L, 600L, 600L, 200L, 200L, 200L), on)
    }

    @Test fun sosEndsWithAPauseAndNeverStartsOff() {
        val steps = FlashPatterns.sos()
        assertTrue(steps.first().on)
        assertTrue(!steps.last().on && steps.last().ms >= 600L)
    }

    @Test fun strobeBlinksFast() {
        val steps = FlashPatterns.strobe()
        assertEquals(listOf(true, false), steps.map { it.on })
        assertTrue(steps.all { it.ms <= 100L })
    }

    @Test fun modesAreMatchedAndUnknownOnesRefused() {
        assertEquals(FlashPatterns.sos(), FlashPatterns.forMode("sos"))
        assertEquals(FlashPatterns.strobe(), FlashPatterns.forMode("strobe"))
        assertNull(FlashPatterns.forMode("disco"))
        assertNull(FlashPatterns.forMode(null))
    }

    @Test fun secondsStayBetweenTwoAndThirtyAndDefaultToTen() {
        assertEquals(10, FlashPatterns.seconds(null))
        assertEquals(2, FlashPatterns.seconds(0))
        assertEquals(30, FlashPatterns.seconds(500))
        assertEquals(15, FlashPatterns.seconds(15))
    }

    @Test fun everySwitchToolTakesOnOrOff() {
        listOf(
            DeviceTool.WIFI_TOGGLE, DeviceTool.BLUETOOTH_TOGGLE, DeviceTool.MOBILE_DATA_PANEL, DeviceTool.AIRPLANE_MODE,
            DeviceTool.HOTSPOT_SETTINGS, DeviceTool.NFC_SETTINGS, DeviceTool.DARK_MODE, DeviceTool.BATTERY_SAVER
        ).forEach { tool ->
            val state = tool.params.single { it.name == "state" }
            assertEquals("$tool", listOf("on", "off"), state.choices)
            assertTrue("$tool state is required", state.required)
        }
    }

    @Test fun theNewToolsLiveInTheirConnectors() {
        fun tools(id: String) = ConnectorRegistry.get(id).tools.map { it.functionName }.toSet()
        assertTrue(tools("wifi").containsAll(setOf("get_wifi_info", "wifi_toggle")))
        assertTrue(tools("bluetooth").containsAll(setOf("get_bluetooth_status", "bluetooth_toggle", "list_paired_devices")))
        assertTrue(tools("network").containsAll(setOf("mobile_data_panel", "airplane_mode", "hotspot_settings", "nfc_settings", "open_connectivity_panel")))
        assertTrue(tools("torch").containsAll(setOf("set_flashlight", "flashlight_strobe")))
        assertTrue(tools("display").containsAll(setOf("dark_mode", "battery_saver")))
    }

    @Test fun aStrobeNeedsAModeAndTheTimeIsOptional() {
        val params = DeviceTool.FLASHLIGHT_STROBE.params
        assertEquals(listOf("strobe", "sos"), params.first { it.name == "mode" }.choices)
        assertTrue(!params.first { it.name == "seconds" }.required)
    }

    @Test fun savedSettingsKeepTheNewToolsWithTheirConnector() {
        // A phone that already had Wi-Fi and Torch on gets the new tools of those connectors too.
        val on = effectiveDeviceTools(setOf("get_wifi_info=1", "set_flashlight=1"))
        assertTrue(DeviceTool.WIFI_TOGGLE in on && DeviceTool.FLASHLIGHT_STROBE in on)
        assertTrue(DeviceTool.BLUETOOTH_TOGGLE !in on)
    }
}
