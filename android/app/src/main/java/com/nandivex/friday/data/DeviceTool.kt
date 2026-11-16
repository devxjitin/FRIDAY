package com.nandivex.friday.data

enum class ToolParamType { STRING, INTEGER, NUMBER }

/** One argument of a device tool, as declared to the voice model. */
data class ToolParam(
    val name: String,
    val type: ToolParamType,
    val description: String,
    val required: Boolean = true,
    val choices: List<String> = emptyList()
)

private fun volumeStreamParam() = ToolParam(
    "stream", ToolParamType.STRING,
    "Which volume: assistant is the assistant's own voice (the call volume), media is music and videos, " +
        "ring is the ringtone. Default assistant.",
    required = false, choices = listOf("assistant", "media", "ring", "alarm", "notification")
)

private fun levelParams() = listOf(
    ToolParam("action", ToolParamType.STRING, "How to change the level.", choices = listOf("increase", "decrease", "set")),
    ToolParam("percent", ToolParamType.INTEGER, "Target level 1-100. Only used with action set.", required = false)
)

/** Small on-device actions the voice model may call. Each one has its own on/off switch in Settings. */
enum class DeviceTool(
    val functionName: String,
    val label: String,
    val hint: String,
    val description: String,
    val params: List<ToolParam> = emptyList()
) {
    BRIGHTNESS(
        "set_brightness", "Set brightness", "Make the screen brighter or dimmer.",
        "Changes the screen brightness when the user asks for it to be brighter, dimmer, or a specific level.",
        levelParams()
    ),
    GET_BRIGHTNESS(
        "get_brightness", "Read brightness", "Tell the current screen brightness.",
        "Returns the real current screen brightness. Always call this when asked about brightness; the user may have changed it by hand."
    ),
    VOLUME(
        "set_volume", "Set volume", "Raise or lower the assistant or media volume.",
        "Changes the volume when the user asks for it to be louder, quieter, or a specific level. " +
            "Phones have separate volumes: the assistant's own voice (default), media, ring, alarm and notification.",
        levelParams() + volumeStreamParam()
    ),
    GET_VOLUME(
        "get_volume", "Read volume", "Tell the current volumes (assistant, media, ring, alarm).",
        "Returns the real current volume. Always call this when asked about volume; the user may have changed it with the buttons. " +
            "Without a stream it reports all of them.",
        listOf(volumeStreamParam())
    ),
    GET_RINGER(
        "get_ringer_mode", "Read ringer mode", "Tell if the phone is on normal, vibrate or silent.",
        "Returns the ringer mode: normal, vibrate or silent."
    ),
    SET_RINGER(
        "set_ringer_mode", "Set ringer mode", "Switch the phone to normal, vibrate or silent.",
        "Sets the ringer mode. Silent only works if Do Not Disturb access was granted to this app.",
        listOf(ToolParam("mode", ToolParamType.STRING, "Desired ringer mode.", choices = listOf("normal", "vibrate", "silent")))
    ),
    VIBRATE(
        "vibrate_device", "Vibrate", "Make the phone vibrate.",
        "Vibrates the device once, for example to help the user find it.",
        listOf(ToolParam("milliseconds", ToolParamType.INTEGER, "Length 50-2000. Default 500.", required = false))
    ),
    PRESS_BACK(
        "press_back", "Back button", "Press the Back button.",
        "Presses the Back button, like the user swiping back. Needs the accessibility service to be enabled."
    ),
    RECENT_APPS(
        "open_recent_apps", "Recent apps", "Open the recent apps screen.",
        "Opens the recent apps screen where the user can switch apps. Needs the accessibility service."
    ),
    PREVIOUS_APP(
        "switch_to_previous_app", "Previous app", "Switch back to the app used before this one.",
        "Switches to the previously used app. To open a specific app use open_app instead. Needs the accessibility service."
    ),
    NOTIFICATION_SHADE(
        "open_notification_shade", "Notification shade", "Pull down the notification panel.",
        "Opens the notification panel. Needs the accessibility service."
    ),
    QUICK_SETTINGS(
        "open_quick_settings", "Quick settings", "Open the quick settings panel (Wi-Fi, Bluetooth tiles).",
        "Opens the quick settings panel with the Wi-Fi, Bluetooth and other tiles. Needs the accessibility service."
    ),
    SCROLL(
        "scroll_screen", "Scroll", "Scroll or swipe the screen up, down, left or right.",
        "Scrolls the current screen in a direction, like a finger swipe that stops where it lifts (no coasting). Down means see content further down the page. " +
            "Choose the amount to fit the request: small for a line or two or fine adjusting, medium for a bit more (default), large for most of a screen, " +
            "page for a full screen. When the user asks to scroll a lot or to the end, use page repeatedly; when they say a little or slightly, use small. " +
            "Needs the accessibility service.",
        listOf(
            ToolParam("direction", ToolParamType.STRING, "Scroll direction.", choices = listOf("up", "down", "left", "right")),
            ToolParam("amount", ToolParamType.STRING, "How far to scroll. Default medium.", required = false, choices = listOf("small", "medium", "large", "page")),
            ToolParam("times", ToolParamType.INTEGER, "Repeat the scroll 1-10 times in one go. Default 1.", required = false)
        )
    ),
    LOCK_SCREEN(
        "lock_screen", "Lock screen", "Lock the phone.",
        "Locks the phone screen. Needs the accessibility service and Android 9 or newer."
    ),
    SCREENSHOT(
        "take_screenshot", "Screenshot", "Take a screenshot (saved in the gallery).",
        "Takes a screenshot of whatever is on screen. Needs the accessibility service and Android 9 or newer."
    ),
    HOME_SCREEN(
        "go_to_home_screen", "Go to home screen", "Leave the current app and show the home screen.",
        "Shows the phone's home screen, for example when the user says go home."
    ),
    MEDIA_CONTROL(
        "media_control", "Media control", "Play, pause, or skip music in other apps.",
        "Sends a media key to whatever app is playing music or video.",
        listOf(ToolParam("command", ToolParamType.STRING, "Media command.",
            choices = listOf("play_pause", "next", "previous", "stop")))
    ),

    TIME(
        "get_current_time", "Date and time", "Tell the current date and time.",
        "Returns the current local date and time. Call it whenever the user asks for the time or date."
    ),
    BATTERY(
        "get_battery_status", "Battery", "Tell the battery level and charging state.",
        "Returns the battery percentage, charging state, temperature and whether battery saver is on."
    ),
    NETWORK(
        "get_network_status", "Network status", "Tell whether Wi-Fi or mobile data is connected.",
        "Returns whether the device is online and whether it is on Wi-Fi, mobile data or another network."
    ),
    FLASHLIGHT(
        "set_flashlight", "Flashlight", "Turn the flashlight on or off.",
        "Turns the device flashlight on or off.",
        listOf(ToolParam("state", ToolParamType.STRING, "Desired flashlight state.", choices = listOf("on", "off")))
    ),
    GET_CLIPBOARD(
        "get_clipboard_text", "Read clipboard", "Read the text the user copied.",
        "Returns the text currently on the clipboard. Only works while the app is on screen."
    ),
    SET_CLIPBOARD(
        "set_clipboard_text", "Write clipboard", "Copy text to the clipboard.",
        "Copies the given text to the clipboard so the user can paste it elsewhere.",
        listOf(ToolParam("text", ToolParamType.STRING, "Text to copy."))
    ),
    SET_DND(
        "set_do_not_disturb", "Do Not Disturb", "Turn Do Not Disturb on or off.",
        "Turns Do Not Disturb on or off, or reports its state. Needs Do Not Disturb access; if missing, the access screen opens.",
        listOf(ToolParam("state", ToolParamType.STRING, "Desired state.", choices = listOf("on", "off", "status")))
    ),
    BLUETOOTH(
        "get_bluetooth_status", "Bluetooth status", "Tell if Bluetooth is on and which devices are paired.",
        "Returns whether Bluetooth is on and the names of paired devices."
    ),
    WIFI_INFO(
        "get_wifi_info", "Wi-Fi details", "Tell the Wi-Fi network name and signal.",
        "Returns the connected Wi-Fi network name, signal strength and link speed. Needs location permission."
    ),
    SCREEN_TIMEOUT(
        "set_screen_timeout", "Screen timeout", "Change how long the screen stays on.",
        "Sets how long the screen stays on before sleeping. Needs the Modify system settings permission; if missing, its screen opens.",
        listOf(ToolParam("seconds", ToolParamType.INTEGER, "Seconds, 15-1800."))
    ),
    AUTO_ROTATE(
        "set_auto_rotate", "Auto-rotate", "Turn screen auto-rotate on or off.",
        "Turns automatic screen rotation on or off. Needs the Modify system settings permission; if missing, its screen opens.",
        listOf(ToolParam("state", ToolParamType.STRING, "Desired state.", choices = listOf("on", "off")))
    ),
    OPEN_SETTINGS(
        "open_settings", "Open settings", "Open a system settings screen.",
        "Opens a system settings screen such as Wi-Fi or Bluetooth. Apps cannot switch these on or off directly.",
        listOf(ToolParam("screen", ToolParamType.STRING, "Which settings screen.",
            choices = listOf("wifi", "bluetooth", "display", "sound", "location", "battery", "main")))
    ),

    OPEN_APP(
        "open_app", "Open apps", "Launch an installed app by name.",
        "Opens an installed app by its name, for example Camera, Calculator or YouTube.",
        listOf(ToolParam("name", ToolParamType.STRING, "The app name as the user said it."))
    ),

    OPEN_CAMERA(
        "open_camera", "Open camera", "Open the camera for a photo or video.",
        "Opens the camera app ready to take a photo or record a video.",
        listOf(ToolParam("mode", ToolParamType.STRING, "Photo or video. Default photo.", required = false,
            choices = listOf("photo", "video")))
    ),



    OPEN_APP_SETTINGS(
        "open_app_settings", "App settings page", "Open an app's info page (permissions, storage, uninstall).",
        "Opens the system info page of an installed app, where the user can manage its permissions or uninstall it.",
        listOf(ToolParam("name", ToolParamType.STRING, "App name."))
    ),






    WIFI_TOGGLE(
        "wifi_toggle", "Wi-Fi on or off", "Turn Wi-Fi on or off (opens the panel).",
        "Turns Wi-Fi on or off. Android 10 and newer do not let apps flip it directly, so the internet panel opens; the result then says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    BLUETOOTH_TOGGLE(
        "bluetooth_toggle", "Bluetooth on or off", "Turn Bluetooth on or off (Android asks to allow).",
        "Turns Bluetooth on (Android shows an Allow question) or off (the Bluetooth settings open). The result says whether to tap the control yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    LIST_PAIRED_DEVICES(
        "list_paired_devices", "Paired Bluetooth devices", "List the Bluetooth devices paired with the phone.",
        "Lists the paired Bluetooth devices by name and kind, such as headphones, car or watch. Works even when Bluetooth is off."
    ),
    MOBILE_DATA_PANEL(
        "mobile_data_panel", "Mobile data panel", "Open the mobile data and internet panel.",
        "Opens the internet panel where mobile data can be switched. Android does not let apps switch mobile data directly, so the result says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    AIRPLANE_MODE(
        "airplane_mode", "Airplane mode", "Turn airplane mode on or off (opens its settings).",
        "Opens the airplane mode settings so it can be turned on or off. Warn the user that turning it on cuts calls and data, and turning it on while a voice session needs the internet will end this conversation.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    HOTSPOT_SETTINGS(
        "hotspot_settings", "Hotspot settings", "Open the hotspot and tethering settings.",
        "Opens the hotspot and tethering settings so the hotspot can be switched on or off. The result says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    NFC_SETTINGS(
        "nfc_settings", "NFC settings", "Open the NFC panel.",
        "Opens the NFC settings or panel so NFC can be switched on or off. The result says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    FLASHLIGHT_STROBE(
        "flashlight_strobe", "Flashlight strobe or SOS", "Blink the flashlight fast, or in SOS.",
        "Blinks the flashlight for a few seconds: strobe is a fast blink, sos is three short, three long, three short. It stops by itself, or when the flashlight is switched on or off.",
        listOf(
            ToolParam("mode", ToolParamType.STRING, "Blink pattern.", choices = listOf("strobe", "sos")),
            ToolParam("seconds", ToolParamType.INTEGER, "How long, 2-30. Default 10.", required = false)
        )
    ),
    DARK_MODE(
        "dark_mode", "Dark mode", "Turn the phone's dark theme on or off (opens display settings).",
        "Opens the display settings where the dark theme switch is. The result says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),
    BATTERY_SAVER(
        "battery_saver", "Battery saver", "Turn battery saver on or off (opens its settings).",
        "Opens the battery saver settings so it can be switched on or off. The result says whether to tap the switch yourself or ask the user.",
        listOf(ToolParam("state", ToolParamType.STRING, "Wanted state.", choices = listOf("on", "off")))
    ),






    READ_SCREEN(
        "read_screen", "Read the screen", "See the text and buttons on screen (sends them to Gemini).",
        "Lists the visible text, buttons and fields of the app on screen, numbered, with tap positions. " +
            "Call it before clicking or typing so you know what is there. Needs the accessibility service."
    ),
    LOOK_AT_SCREEN(
        "look_at_screen", "Look at the screen", "See the screen as a picture when an app gives no readable text (sends a screenshot to Gemini).",
        "Takes a screenshot and describes it, with tap positions. Use it when read_screen finds nothing readable (a game, a canvas or a web view). " +
            "Needs the accessibility service and Android 11 or newer. Apps that protect their window cannot be seen, and then this cannot help.",
        listOf(ToolParam("question", ToolParamType.STRING, "What to look for, if anything specific.", required = false))
    ),
    CURRENT_APP(
        "get_current_app", "Current app", "Tell which app is on screen.",
        "Returns the name of the app currently on screen. Needs the accessibility service."
    ),
    CLICK_TEXT(
        "click_on_text", "Click by text", "Tap a button or link by its label.",
        "Taps the element on screen whose text or description contains the given text. Read the screen first if unsure. " +
            "Needs the accessibility service.",
        listOf(
            ToolParam("text", ToolParamType.STRING, "Text or label of the element."),
            ToolParam("occurrence", ToolParamType.INTEGER, "Which match to tap if several, starting at 1. Default 1.", required = false)
        )
    ),
    CLICK_ITEM(
        "click_screen_item", "Click by number", "Tap an item by its number from the last screen reading.",
        "Taps the item with this number from the latest read_screen result. Needs the accessibility service.",
        listOf(ToolParam("number", ToolParamType.INTEGER, "Item number from read_screen."))
    ),
    TAP_POSITION(
        "tap_screen_position", "Tap a position", "Tap at exact screen coordinates.",
        "Taps at x,y pixel coordinates, for example the @x,y shown by read_screen. Needs the accessibility service.",
        listOf(
            ToolParam("x", ToolParamType.INTEGER, "Horizontal pixel position."),
            ToolParam("y", ToolParamType.INTEGER, "Vertical pixel position.")
        )
    ),
    TYPE_TEXT(
        "type_text", "Type text", "Type into the text field on screen.",
        "Types text into the focused text field, or the first one on screen, password fields included (for the user's own logins). " +
            "Needs the accessibility service.",
        listOf(
            ToolParam("text", ToolParamType.STRING, "Text to type."),
            ToolParam("append", ToolParamType.STRING, "yes to add to existing text; default replaces it.", required = false,
                choices = listOf("yes", "no"))
        )
    ),
    PRESS_ENTER(
        "press_enter", "Press Enter", "Submit the text field (Search/Go/Send).",
        "Presses Enter on the focused text field, for example to run a search after typing. Needs the accessibility service and Android 11."
    ),
    WAIT_FOR_TEXT(
        "wait_for_screen_text", "Wait for text", "Wait until some text appears on screen.",
        "Waits until the given text shows on screen, up to a time limit. Use it after opening an app or tapping something that loads.",
        listOf(
            ToolParam("text", ToolParamType.STRING, "Text to wait for."),
            ToolParam("seconds", ToolParamType.INTEGER, "Longest wait, 1-30. Default 10.", required = false)
        )
    ),
    OPEN_PANEL(
        "open_connectivity_panel", "Wi-Fi, NFC and volume panels", "Show the quick panel for Wi-Fi, mobile data, NFC or volume.",
        "Opens a small system panel for Wi-Fi, mobile data, NFC or volume. Afterwards flip the switch yourself with read_screen and click_on_text, then check the screen. " +
            "Only ask the user to tap it when the accessibility service is off.",
        listOf(ToolParam("panel", ToolParamType.STRING, "Which panel.", choices = listOf("internet", "wifi", "nfc", "volume")))
    ),
    SWIPE(
        "swipe_screen", "Swipe or drag", "Swipe or drag between two points on the screen.",
        "Moves a finger from one pixel position to another, for a swipe, a drag, a slider or a pull-down. Needs the accessibility service.",
        listOf(
            ToolParam("from_x", ToolParamType.INTEGER, "Start horizontal pixel."),
            ToolParam("from_y", ToolParamType.INTEGER, "Start vertical pixel."),
            ToolParam("to_x", ToolParamType.INTEGER, "End horizontal pixel."),
            ToolParam("to_y", ToolParamType.INTEGER, "End vertical pixel."),
            ToolParam("milliseconds", ToolParamType.INTEGER, "How long the move takes, 100-3000. Default 400.", required = false)
        )
    ),
    LONG_PRESS(
        "long_press_position", "Long press", "Press and hold at screen coordinates.",
        "Presses and holds at x,y pixel coordinates, for menus, selecting text or moving icons. Needs the accessibility service.",
        listOf(
            ToolParam("x", ToolParamType.INTEGER, "Horizontal pixel position."),
            ToolParam("y", ToolParamType.INTEGER, "Vertical pixel position.")
        )
    ),
    POWER_MENU(
        "open_power_menu", "Power menu", "Open the power off and restart menu.",
        "Opens the system power menu. The user must choose power off or restart themselves. Needs the accessibility service."
    ),
    SPLIT_SCREEN(
        "toggle_split_screen", "Split screen", "Turn split screen on or off.",
        "Switches split-screen mode on or off for the app in front. Needs the accessibility service."
    ),
    LIST_APPS(
        "list_installed_apps", "List installed apps", "List the apps that can be opened.",
        "Returns the names of the apps installed on the phone that have a launcher icon, so you can pick the right name for open_app.",
        listOf(ToolParam("filter", ToolParamType.STRING, "Only names containing this text.", required = false))
    ),
    OPEN_URL(
        "open_url", "Open a web link", "Open a web address in the browser.",
        "Opens an http or https link in the user's browser or the app that handles it. Only open links the user asked for.",
        listOf(ToolParam("url", ToolParamType.STRING, "Full web address starting with http or https."))
    );

    companion object {
        fun fromFunctionName(name: String): DeviceTool? = entries.firstOrNull { it.functionName == name }

        /** Function names of tools that take seconds (network, screenshot upload). They are declared non-blocking, so the
         *  model keeps talking while they run, and their results wait until the user stops speaking. */
        val NON_BLOCKING_NAMES: Set<String> = setOf(LOOK_AT_SCREEN).map { it.functionName }.toSet()
    }
}

/** Pure level math for the brightness and volume tools, kept free of Android types for unit tests. */
object DeviceToolLevels {
    const val STEP_PERCENT = 20

    /** Returns the new percent, or null when [action] is unknown or "set" has no [percent]. */
    fun next(current: Int, action: String?, percent: Int?, min: Int): Int? = when (action) {
        "increase" -> current + STEP_PERCENT
        "decrease" -> current - STEP_PERCENT
        "set" -> percent
        else -> null
    }?.coerceIn(min, 100)
}
