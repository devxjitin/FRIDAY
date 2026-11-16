package com.nandivex.friday.data

import com.nandivex.friday.network.liveFunctionDeclarations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** FRIDAY has no connectors: every tool is built in and on. These tests keep that tool set small and safe. */
class BuiltInToolsTest {
    @Test fun functionNamesAreUnique() {
        val names = DeviceTool.entries.map { it.functionName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun everyToolIsOnInTheApp() {
        assertEquals(DeviceTool.entries.toSet(), AssistantSettings().enabledDeviceTools)
    }

    @Test fun everyToolHasAHintAndADescription() {
        DeviceTool.entries.forEach {
            assertTrue("${it.functionName} has no hint", it.hint.isNotBlank())
            assertTrue("${it.functionName} has no description", it.description.isNotBlank())
        }
    }

    /** Every extra tool makes the Live setup bigger and gives the model one more wrong choice. */
    @Test fun allToolsTogetherStayUnderTheToolCap() {
        val count = liveFunctionDeclarations(AssistantSettings()).length()
        assertTrue("$count tools declared", count <= TOOL_CAP)
    }

    @Test fun noToolReachesOtherPeopleOrPrivateData() {
        val gone = setOf(
            "make_call", "answer_call", "end_call", "reject_call", "set_speakerphone", "mute_call", "dial_number",
            "send_sms", "open_sms_draft", "whatsapp_message", "whatsapp_call", "search_contact", "create_contact",
            "get_contact_birthday", "get_upcoming_events", "create_calendar_event", "cancel_calendar_event",
            "check_calendar_conflicts", "get_current_location", "get_directions", "read_notifications",
            "reply_to_notification", "dismiss_notification", "get_now_playing", "search_files", "open_file",
            "list_folder", "share_file", "share_text", "get_most_used_apps", "get_app_usage_time",
            "web_search", "fetch_webpage", "get_weather"
        )
        val names = DeviceTool.entries.map { it.functionName }.toSet()
        assertTrue("still declared: ${names intersect gone}", (names intersect gone).isEmpty())
    }

    @Test fun phoneControlToolsStayAvailable() {
        val names = DeviceTool.entries.map { it.functionName }.toSet()
        listOf(
            "read_screen", "click_on_text", "tap_screen_position", "type_text", "swipe_screen", "scroll_screen",
            "press_back", "go_to_home_screen", "open_app", "set_flashlight", "set_volume", "set_brightness",
            "wifi_toggle", "bluetooth_toggle", "set_alarm", "set_timer", "schedule_reminder"
        ).forEach { assertTrue("$it is missing", it in names) }
    }

    @Test fun aSessionDeclaresExactlyTheToolsThatAreOn() {
        val torchOnly = AssistantSettings(enabledDeviceTools = setOf(DeviceTool.FLASHLIGHT, DeviceTool.FLASHLIGHT_STROBE), memoryEnabled = false)
        val names = liveFunctionDeclarations(torchOnly).let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("name") } }
        assertEquals(listOf("set_flashlight", "flashlight_strobe"), names)
    }

    private companion object {
        const val TOOL_CAP = 80
    }
}
