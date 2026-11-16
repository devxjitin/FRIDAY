package com.nandivex.friday.data

/** One heading on the "What F.R.I.D.A.Y. can do" page, with the tools that belong under it. */
class AbilityGroup(val title: String, val summary: String, val tools: List<DeviceTool>)

/**
 * The built-in abilities, grouped for the sidebar page. FRIDAY has no connectors and nothing to switch on or off: every
 * tool below is always available. A tool belongs to exactly one group (a unit test keeps it that way).
 */
object Abilities {
    val groups: List<AbilityGroup> = listOf(
        AbilityGroup(
            "See and use the screen", "Reads what is on the screen and taps, types and scrolls like a finger.",
            listOf(
                DeviceTool.READ_SCREEN, DeviceTool.LOOK_AT_SCREEN, DeviceTool.CURRENT_APP, DeviceTool.WAIT_FOR_TEXT,
                DeviceTool.CLICK_TEXT, DeviceTool.CLICK_ITEM, DeviceTool.TAP_POSITION, DeviceTool.LONG_PRESS,
                DeviceTool.TYPE_TEXT, DeviceTool.PRESS_ENTER, DeviceTool.SCROLL, DeviceTool.SWIPE
            )
        ),
        AbilityGroup(
            "Phone navigation", "The buttons and gestures you use to move around the phone.",
            listOf(
                DeviceTool.PRESS_BACK, DeviceTool.HOME_SCREEN, DeviceTool.RECENT_APPS, DeviceTool.PREVIOUS_APP,
                DeviceTool.NOTIFICATION_SHADE, DeviceTool.QUICK_SETTINGS, DeviceTool.LOCK_SCREEN, DeviceTool.SCREENSHOT,
                DeviceTool.POWER_MENU, DeviceTool.SPLIT_SCREEN
            )
        ),
        AbilityGroup(
            "Connectivity", "Wi-Fi, Bluetooth, mobile data, airplane mode, hotspot and NFC.",
            listOf(
                DeviceTool.WIFI_INFO, DeviceTool.WIFI_TOGGLE, DeviceTool.BLUETOOTH, DeviceTool.BLUETOOTH_TOGGLE,
                DeviceTool.LIST_PAIRED_DEVICES, DeviceTool.NETWORK, DeviceTool.OPEN_PANEL, DeviceTool.MOBILE_DATA_PANEL,
                DeviceTool.AIRPLANE_MODE, DeviceTool.HOTSPOT_SETTINGS, DeviceTool.NFC_SETTINGS
            )
        ),
        AbilityGroup(
            "Sound and media", "Volume, ringer, vibration, Do Not Disturb and music or video keys.",
            listOf(
                DeviceTool.VOLUME, DeviceTool.GET_VOLUME, DeviceTool.GET_RINGER, DeviceTool.SET_RINGER,
                DeviceTool.VIBRATE, DeviceTool.SET_DND, DeviceTool.MEDIA_CONTROL
            )
        ),
        AbilityGroup(
            "Display and torch", "Brightness, screen timeout, rotation, dark mode, battery saver and the flashlight.",
            listOf(
                DeviceTool.BRIGHTNESS, DeviceTool.GET_BRIGHTNESS, DeviceTool.SCREEN_TIMEOUT, DeviceTool.AUTO_ROTATE,
                DeviceTool.DARK_MODE, DeviceTool.BATTERY_SAVER, DeviceTool.FLASHLIGHT, DeviceTool.FLASHLIGHT_STROBE
            )
        ),
        AbilityGroup(
            "Apps and links", "Open apps, settings pages, the camera and web links.",
            listOf(
                DeviceTool.OPEN_APP, DeviceTool.LIST_APPS, DeviceTool.OPEN_APP_SETTINGS, DeviceTool.OPEN_SETTINGS,
                DeviceTool.OPEN_CAMERA, DeviceTool.OPEN_URL
            )
        ),
        AbilityGroup(
            "Clipboard and status", "Copy and paste text, and tell the time and battery.",
            listOf(DeviceTool.GET_CLIPBOARD, DeviceTool.SET_CLIPBOARD, DeviceTool.TIME, DeviceTool.BATTERY)
        )
    )
}
