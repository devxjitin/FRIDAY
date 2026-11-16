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

/** Tools that never need a screen, so they can run from an alarm or a phone event. */
val AUTOMATABLE_TOOL_NAMES = listOf(
    "set_volume", "set_ringer_mode", "vibrate_device", "set_flashlight", "media_control", "set_do_not_disturb",
    "set_clipboard_text", "show_notification", "set_screen_timeout", "set_auto_rotate",
    "schedule_reminder", "lock_screen", "take_screenshot", "get_battery_status",
    "get_network_status", "get_current_time", "get_storage_and_memory"
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
    DEVICE_INFO(
        "get_device_info", "Device info", "Tell the phone model, Android version, screen, language and time zone.",
        "Returns the device model, Android version, screen size and orientation, language and time zone."
    ),
    STORAGE_MEMORY(
        "get_storage_and_memory", "Storage and memory", "Tell free storage and free RAM.",
        "Returns free and total internal storage and free and total RAM."
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
    SHOW_NOTIFICATION(
        "show_notification", "Show notification", "Post a notification on the phone.",
        "Shows a notification with a title and message, for example as a reminder.",
        listOf(
            ToolParam("title", ToolParamType.STRING, "Notification title."),
            ToolParam("message", ToolParamType.STRING, "Notification text.")
        )
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

    SET_ALARM(
        "set_alarm", "Set alarm", "Set an alarm that rings with sound.",
        "Sets an alarm for a time of day in 24-hour format, for waking up or ringing at a clock time. For \"in 10 minutes\" use set_timer; " +
            "for \"remind me to ...\" use schedule_reminder. It is the assistant's own alarm: it rings on the alarm volume until stopped or snoozed, " +
            "and you can list, change and cancel it. Add repeat daily or weekly for an alarm that keeps ringing. " +
            "Setting the same time again replaces the old alarm. Act immediately, then say it is set.",
        listOf(
            ToolParam("hour", ToolParamType.INTEGER, "Hour 0-23."),
            ToolParam("minute", ToolParamType.INTEGER, "Minute 0-59."),
            ToolParam("label", ToolParamType.STRING, "Optional alarm name.", required = false),
            ToolParam("repeat", ToolParamType.STRING, "daily or weekly. Leave out for a single alarm.", required = false)
        )
    ),
    LIST_ALARMS(
        "list_alarms", "List alarms", "See the alarms that are set.",
        "Lists every alarm still waiting to ring, with its time and name. An alarm that already rang or was cancelled is not listed."
    ),
    CANCEL_ALARM(
        "cancel_alarm", "Cancel alarm", "Cancel an alarm.",
        "Cancels an alarm for real, without opening any app: give hour and minute for a time, a label for a named alarm, " +
            "or neither for the next alarm. While an alarm is ringing, calling it with nothing stops the ringing. " +
            "Use this whenever the user asks to cancel, delete, stop or turn off an alarm; never open the Clock app. " +
            "The result says which alarm was removed, or lists the alarms that exist when none matched; tell the user exactly that.",
        listOf(
            ToolParam("hour", ToolParamType.INTEGER, "Hour 0-23 of the alarm to cancel.", required = false),
            ToolParam("minute", ToolParamType.INTEGER, "Minute 0-59 of the alarm to cancel. Default 0.", required = false),
            ToolParam("label", ToolParamType.STRING, "Name of the alarm to cancel, when no time is given.", required = false),
            ToolParam("id", ToolParamType.INTEGER, "Alarm number, if known.", required = false)
        )
    ),
    MODIFY_ALARM(
        "modify_alarm", "Change an alarm", "Move an alarm to a new time.",
        "Moves the alarm at hour:minute to new_hour:new_minute, keeping its name and repeat unless a new label is given. " +
            "Call list_alarms first if the time is not known.",
        listOf(
            ToolParam("hour", ToolParamType.INTEGER, "Current hour 0-23 of the alarm."),
            ToolParam("minute", ToolParamType.INTEGER, "Current minute 0-59 of the alarm."),
            ToolParam("new_hour", ToolParamType.INTEGER, "New hour 0-23."),
            ToolParam("new_minute", ToolParamType.INTEGER, "New minute 0-59."),
            ToolParam("label", ToolParamType.STRING, "Optional new alarm name. The old name is kept if left out.", required = false)
        )
    ),
    SET_TIMER(
        "set_timer", "Set timer", "Start a countdown timer that rings with sound.",
        "Starts a countdown timer for the given number of seconds (up to 24 hours), for \"in 10 minutes\" or \"after half an hour\" with nothing to remember. " +
            "It rings on the alarm volume until stopped. " +
            "Several timers can run at once; give each a label when the user names it.",
        listOf(
            ToolParam("seconds", ToolParamType.INTEGER, "Timer length in seconds."),
            ToolParam("label", ToolParamType.STRING, "Optional timer name.", required = false)
        )
    ),
    LIST_TIMERS(
        "list_timers", "List timers", "See the running timers.",
        "Lists every running timer with the time left. Use it for questions like how much time is left."
    ),
    CANCEL_TIMER(
        "cancel_timer", "Cancel timer", "Stop a running timer.",
        "Cancels a running timer for real. Give its label or number; with neither, a ringing timer is stopped, or else the one ringing soonest is cancelled. " +
            "Use this whenever the user asks to stop, cancel or delete a timer.",
        listOf(
            ToolParam("label", ToolParamType.STRING, "Name of the timer to cancel.", required = false),
            ToolParam("id", ToolParamType.INTEGER, "Timer number, if known.", required = false)
        )
    ),

    SCHEDULE_REMINDER(
        "schedule_reminder", "Reminder", "Remind you at a time or after a while, with a sound.",
        "Sets a reminder: a message about something to do, which pops up with a sound and vibration. Use it when the user says what to be reminded of; " +
            "use set_alarm or set_timer when they only want ringing. Give minutes from now (up to a year), or at_time as HH:MM in 24-hour time, " +
            "with an optional date (YYYY-MM-DD) for another day. Without a date, at_time means the next time the clock shows it. " +
            "Add repeat daily or weekly for a reminder that keeps coming. Call get_current_time first if the date or time is unclear.",
        listOf(
            ToolParam("message", ToolParamType.STRING, "What to remind about."),
            ToolParam("minutes", ToolParamType.INTEGER, "Minutes from now, 1-525600. Leave out when using at_time.", required = false),
            ToolParam("at_time", ToolParamType.STRING, "Clock time HH:MM, 24-hour.", required = false),
            ToolParam("date", ToolParamType.STRING, "Day for at_time as YYYY-MM-DD. Leave out for the next occurrence.", required = false),
            ToolParam("repeat", ToolParamType.STRING, "daily or weekly. Leave out for a single reminder.", required = false)
        )
    ),

    LIST_REMINDERS(
        "list_reminders", "List reminders", "See the reminders that are still waiting.",
        "Lists the reminders that have not gone off yet, each with its number and time. Alarms and timers are not in it; use list_alarms or list_timers."
    ),

    CANCEL_REMINDER(
        "cancel_reminder", "Cancel a reminder", "Cancel a waiting reminder.",
        "Cancels one waiting reminder by its number. Call list_reminders first to find the number.",
        listOf(ToolParam("id", ToolParamType.INTEGER, "Reminder number from list_reminders."))
    ),

    MODIFY_REMINDER(
        "modify_reminder", "Change a reminder", "Change the time or text of a waiting reminder.",
        "Changes a waiting reminder by its number: a new time (minutes from now, or at_time with an optional date), a new message, or both. Call list_reminders first to find the number.",
        listOf(
            ToolParam("id", ToolParamType.INTEGER, "Reminder number from list_reminders."),
            ToolParam("minutes", ToolParamType.INTEGER, "New delay in minutes from now, 1-525600.", required = false),
            ToolParam("at_time", ToolParamType.STRING, "New clock time HH:MM, 24-hour.", required = false),
            ToolParam("date", ToolParamType.STRING, "Day for at_time as YYYY-MM-DD.", required = false),
            ToolParam("message", ToolParamType.STRING, "New text of the reminder.", required = false)
        )
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





    STEPS(
        "get_step_count", "Step counter", "Tell the steps counted by the phone.",
        "Returns the step counter value (steps since the phone last restarted)."
    ),
    LIGHT(
        "get_light_level", "Light sensor", "Tell how bright the room is.",
        "Reads the ambient light sensor and describes how bright the surroundings are."
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
