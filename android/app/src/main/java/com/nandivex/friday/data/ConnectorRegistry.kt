package com.nandivex.friday.data

import android.Manifest
import android.os.Build

/**
 * The groups the Connectors page is split into, in the order they appear. Each has a one-line summary shown under its
 * heading; [advanced] groups carry a warning and are never switched on by default.
 */
enum class ConnectorCategory(val title: String, val summary: String, val advanced: Boolean = false) {
    CALLS_MESSAGES("Calls & messages", "Call people, find contacts and message them. Off until you add them."),
    CONNECTIVITY("Connectivity", "Wi-Fi, Bluetooth and mobile network."),
    DEVICE_CONTROLS("Device controls", "Torch, sound, screen and media keys."),
    CLOCK("Clock", "Alarms, timers and reminders that ring on this phone."),
    APPS("Apps", "Open apps, the camera and settings, and use the clipboard."),
    INFO("Info", "Phone status, weather and the web."),
    PERSONAL("Personal", "Reads private data from your phone, such as calendar, location, files and notifications. What you ask about is sent to Gemini."),
    PHONE_CONTROL("Phone control", "Lets FRIDAY operate other apps through Android accessibility. It can tap, type and read the screen, so turn it on only if you understand that.", advanced = true),
}

/** Extra Android access (a special settings screen, not a normal permission) a connector needs before its tools work. */
enum class SpecialAccess { NONE, NOTIFICATION_ACCESS, ACCESSIBILITY, USAGE_ACCESS, ALL_FILES }

enum class ConnectorKind {
    /** Tools that run on this phone. */
    PHONE,
    /** An online service that needs no sign-in: connecting just switches its tools on. */
    ONLINE
}

/** The long-form facts shown on a connector's own page. */
class ConnectorInfo(val about: String, val needs: String, val privacy: String, val runsOn: String = "Your phone")

/** Everything the app knows about one connector, in one place. */
class ConnectorSpec(
    val id: String,
    val name: String,
    val description: String,
    val subtitle: String,
    val category: ConnectorCategory,
    val kind: ConnectorKind,
    /** Android runtime permissions that must be granted. */
    val permissions: List<String>,
    val access: SpecialAccess,
    /** Switched on for a fresh install. */
    val defaultOn: Boolean,
    val tools: List<DeviceTool>
)

private fun stepsPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
}

private fun bluetoothPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
}

/** Before Android 11 the ordinary storage permission already reaches every file; from 11 the special All files screen is used. */
private fun allFilesPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= 30) emptyList() else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

/**
 * The single list of connectors. The Connectors page, the tool list sent to the model, the fresh-install defaults and
 * the permission requests all read from here, so adding or changing a connector means editing this file only.
 * A tool belongs to exactly one connector.
 */
object ConnectorRegistry {
    val all: List<ConnectorSpec> = listOf(
        ConnectorSpec(
            id = "calls", name = "Phone calls", description = "Call, answer, end and decline calls.",
            subtitle = "On your phone", category = ConnectorCategory.CALLS_MESSAGES, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS, Manifest.permission.ANSWER_PHONE_CALLS), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.MAKE_CALL, DeviceTool.ANSWER_CALL, DeviceTool.END_CALL, DeviceTool.REJECT_CALL, DeviceTool.SPEAKERPHONE, DeviceTool.MUTE_CALL, DeviceTool.DIAL_NUMBER)
        ),
        ConnectorSpec(
            id = "contacts", name = "Contacts", description = "Find contacts, birthdays and save new contacts.",
            subtitle = "On your phone", category = ConnectorCategory.CALLS_MESSAGES, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.READ_CONTACTS), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.SEARCH_CONTACT, DeviceTool.CONTACT_BIRTHDAY, DeviceTool.CREATE_CONTACT)
        ),
        ConnectorSpec(
            id = "sms", name = "Text messages", description = "Send a text or open a draft.",
            subtitle = "On your phone", category = ConnectorCategory.CALLS_MESSAGES, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.SEND_SMS, DeviceTool.OPEN_SMS_DRAFT)
        ),
        ConnectorSpec(
            id = "whatsapp", name = "WhatsApp", description = "Open a chat with your message typed in, or to call.",
            subtitle = "On your phone", category = ConnectorCategory.CALLS_MESSAGES, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.READ_CONTACTS), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.WHATSAPP_MESSAGE, DeviceTool.WHATSAPP_CALL)
        ),
        ConnectorSpec(
            id = "wifi", name = "Wi-Fi", description = "Which Wi-Fi you are on, and turn it on or off.",
            subtitle = "On your phone", category = ConnectorCategory.CONNECTIVITY, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.WIFI_INFO, DeviceTool.WIFI_TOGGLE)
        ),
        ConnectorSpec(
            id = "bluetooth", name = "Bluetooth", description = "Bluetooth on or off, and what is paired.",
            subtitle = "On your phone", category = ConnectorCategory.CONNECTIVITY, kind = ConnectorKind.PHONE,
            permissions = bluetoothPermissions(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.BLUETOOTH, DeviceTool.BLUETOOTH_TOGGLE, DeviceTool.LIST_PAIRED_DEVICES)
        ),
        ConnectorSpec(
            id = "network", name = "Mobile & network", description = "Online or not, mobile data, airplane mode, hotspot and NFC.",
            subtitle = "On your phone", category = ConnectorCategory.CONNECTIVITY, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.NETWORK, DeviceTool.OPEN_PANEL, DeviceTool.MOBILE_DATA_PANEL, DeviceTool.AIRPLANE_MODE, DeviceTool.HOTSPOT_SETTINGS, DeviceTool.NFC_SETTINGS)
        ),
        ConnectorSpec(
            id = "torch", name = "Torch", description = "Flashlight on and off, strobe and SOS.",
            subtitle = "On your phone", category = ConnectorCategory.DEVICE_CONTROLS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.FLASHLIGHT, DeviceTool.FLASHLIGHT_STROBE)
        ),
        ConnectorSpec(
            id = "sound", name = "Sound", description = "Volume, ringer mode, vibration and Do Not Disturb.",
            subtitle = "On your phone", category = ConnectorCategory.DEVICE_CONTROLS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.VOLUME, DeviceTool.GET_VOLUME, DeviceTool.GET_RINGER, DeviceTool.SET_RINGER, DeviceTool.VIBRATE, DeviceTool.SET_DND)
        ),
        ConnectorSpec(
            id = "display", name = "Display", description = "Brightness, screen timeout, auto-rotate, dark mode and battery saver.",
            subtitle = "On your phone", category = ConnectorCategory.DEVICE_CONTROLS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.BRIGHTNESS, DeviceTool.GET_BRIGHTNESS, DeviceTool.SCREEN_TIMEOUT, DeviceTool.AUTO_ROTATE, DeviceTool.DARK_MODE, DeviceTool.BATTERY_SAVER)
        ),
        ConnectorSpec(
            id = "media", name = "Media", description = "Play, pause and skip in any music or video app.",
            subtitle = "On your phone", category = ConnectorCategory.DEVICE_CONTROLS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.MEDIA_CONTROL)
        ),
        ConnectorSpec(
            id = "alarms", name = "Alarms & timers", description = "Set alarms and timers.",
            subtitle = "On your phone", category = ConnectorCategory.CLOCK, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.SET_ALARM, DeviceTool.LIST_ALARMS, DeviceTool.CANCEL_ALARM, DeviceTool.MODIFY_ALARM, DeviceTool.SET_TIMER, DeviceTool.LIST_TIMERS, DeviceTool.CANCEL_TIMER)
        ),
        ConnectorSpec(
            id = "reminders", name = "Reminders", description = "Get reminded at a time, with a sound.",
            subtitle = "On your phone", category = ConnectorCategory.CLOCK, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.SCHEDULE_REMINDER, DeviceTool.LIST_REMINDERS, DeviceTool.CANCEL_REMINDER, DeviceTool.MODIFY_REMINDER)
        ),
        ConnectorSpec(
            id = "apps", name = "Apps", description = "Open apps and settings, links and share text.",
            subtitle = "On your phone", category = ConnectorCategory.APPS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.OPEN_APP, DeviceTool.LIST_APPS, DeviceTool.OPEN_APP_SETTINGS, DeviceTool.OPEN_SETTINGS, DeviceTool.OPEN_URL, DeviceTool.SHARE_TEXT)
        ),
        ConnectorSpec(
            id = "camera", name = "Camera", description = "Open the camera for a photo or video.",
            subtitle = "On your phone", category = ConnectorCategory.APPS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.OPEN_CAMERA)
        ),
        ConnectorSpec(
            id = "clipboard", name = "Clipboard & notes", description = "Read and set the clipboard, and show a note.",
            subtitle = "On your phone", category = ConnectorCategory.APPS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.GET_CLIPBOARD, DeviceTool.SET_CLIPBOARD, DeviceTool.SHOW_NOTIFICATION)
        ),
        ConnectorSpec(
            id = "app_usage", name = "App usage", description = "See which apps you use most.",
            subtitle = "On your phone", category = ConnectorCategory.APPS, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.USAGE_ACCESS, defaultOn = false,
            tools = listOf(DeviceTool.MOST_USED_APPS, DeviceTool.APP_USAGE)
        ),
        ConnectorSpec(
            id = "device_info", name = "Device status", description = "Time, battery, storage, steps and light.",
            subtitle = "On your phone", category = ConnectorCategory.INFO, kind = ConnectorKind.PHONE,
            permissions = stepsPermissions(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.TIME, DeviceTool.BATTERY, DeviceTool.DEVICE_INFO, DeviceTool.STORAGE_MEMORY, DeviceTool.STEPS, DeviceTool.LIGHT)
        ),
        ConnectorSpec(
            id = "weather", name = "Weather", description = "Weather now and for the next few days.",
            subtitle = "Free, no sign-in", category = ConnectorCategory.INFO, kind = ConnectorKind.ONLINE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.GET_WEATHER)
        ),
        ConnectorSpec(
            id = "web", name = "Web", description = "Search the web and read any page or link.",
            subtitle = "No sign-in", category = ConnectorCategory.INFO, kind = ConnectorKind.ONLINE,
            permissions = emptyList(), access = SpecialAccess.NONE, defaultOn = true,
            tools = listOf(DeviceTool.WEB_SEARCH, DeviceTool.FETCH_WEBPAGE)
        ),
        ConnectorSpec(
            id = "calendar", name = "Calendar", description = "Check your schedule and add events.",
            subtitle = "On your phone", category = ConnectorCategory.PERSONAL, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.UPCOMING_EVENTS, DeviceTool.CALENDAR_CONFLICTS, DeviceTool.CREATE_EVENT, DeviceTool.CANCEL_EVENT)
        ),
        ConnectorSpec(
            id = "location", name = "Location", description = "Know where you are and get directions.",
            subtitle = "On your phone", category = ConnectorCategory.PERSONAL, kind = ConnectorKind.PHONE,
            permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), access = SpecialAccess.NONE, defaultOn = false,
            tools = listOf(DeviceTool.LOCATION, DeviceTool.DIRECTIONS)
        ),
        ConnectorSpec(
            id = "notifications", name = "Notifications", description = "Read and reply to notifications.",
            subtitle = "On your phone", category = ConnectorCategory.PERSONAL, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.NOTIFICATION_ACCESS, defaultOn = false,
            tools = listOf(DeviceTool.READ_NOTIFICATIONS, DeviceTool.REPLY_NOTIFICATION, DeviceTool.DISMISS_NOTIFICATION, DeviceTool.NOW_PLAYING)
        ),
        ConnectorSpec(
            id = "files", name = "Files", description = "Browse folders, find any file and open it.",
            subtitle = "On your phone", category = ConnectorCategory.PERSONAL, kind = ConnectorKind.PHONE,
            permissions = allFilesPermissions(), access = SpecialAccess.ALL_FILES, defaultOn = false,
            tools = listOf(DeviceTool.SEARCH_FILES, DeviceTool.OPEN_FILE, DeviceTool.LIST_FOLDER, DeviceTool.SHARE_FILE)
        ),
        ConnectorSpec(
            id = "navigation", name = "Phone navigation", description = "Go back, go home, scroll and take screenshots.",
            subtitle = "On your phone", category = ConnectorCategory.PHONE_CONTROL, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.ACCESSIBILITY, defaultOn = false,
            tools = listOf(DeviceTool.PRESS_BACK, DeviceTool.RECENT_APPS, DeviceTool.PREVIOUS_APP, DeviceTool.NOTIFICATION_SHADE, DeviceTool.QUICK_SETTINGS, DeviceTool.SCROLL, DeviceTool.LOCK_SCREEN, DeviceTool.SCREENSHOT, DeviceTool.HOME_SCREEN, DeviceTool.POWER_MENU, DeviceTool.SPLIT_SCREEN)
        ),
        ConnectorSpec(
            id = "screen", name = "Screen control", description = "Read the screen, tap and type.",
            subtitle = "On your phone", category = ConnectorCategory.PHONE_CONTROL, kind = ConnectorKind.PHONE,
            permissions = emptyList(), access = SpecialAccess.ACCESSIBILITY, defaultOn = false,
            tools = listOf(DeviceTool.READ_SCREEN, DeviceTool.LOOK_AT_SCREEN, DeviceTool.CURRENT_APP, DeviceTool.CLICK_TEXT, DeviceTool.CLICK_ITEM, DeviceTool.TAP_POSITION, DeviceTool.TYPE_TEXT, DeviceTool.PRESS_ENTER, DeviceTool.WAIT_FOR_TEXT, DeviceTool.SWIPE, DeviceTool.LONG_PRESS)
        )
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ConnectorSpec = byId.getValue(id)

    fun info(id: String): ConnectorInfo? = infos[id]

    /** Ids switched on for a fresh install. These are the essentials the "Connect all essentials" button adds. */
    val defaultIds: List<String> get() = all.filter { it.defaultOn }.map { it.id }

    /** The connectors the "Connect all essentials" button adds: the ones that are on for a fresh install. */
    val essentials: List<ConnectorSpec> get() = all.filter { it.defaultOn }

    private val infos: Map<String, ConnectorInfo> = mapOf(
        "wifi" to ConnectorInfo(
            "Tells you which Wi-Fi network you are on, how strong it is and how fast the link is, and turns Wi-Fi on or off. Android 10 and newer do not let apps flip it, so the internet panel opens and F.R.I.D.A.Y. taps the switch when Screen control is on; otherwise you tap it.",
            "Location access. Android needs it to show a Wi-Fi name.",
            "Shared only when you ask."
        ),
        "bluetooth" to ConnectorInfo(
            "Tells you whether Bluetooth is on and which devices are paired, and turns it on (Android asks you to allow) or off.",
            "Nearby devices access on Android 12 and newer.",
            "Shared only when you ask."
        ),
        "network" to ConnectorInfo(
            "Tells you whether you are online, and opens the mobile data, airplane mode, hotspot, NFC and volume panels. Android does not let apps flip these, so F.R.I.D.A.Y. taps the switch when Screen control is on; otherwise you tap it.",
            "Nothing to allow.",
            "Nothing leaves your phone."
        ),
        "torch" to ConnectorInfo(
            "Switches the flashlight on and off, blinks it fast (strobe) or in SOS for up to 30 seconds.",
            "Nothing to allow.",
            "Nothing leaves your phone."
        ),
        "sound" to ConnectorInfo(
            "Changes the volume of the assistant, media, ring, alarm or notifications, sets silent, vibrate or normal, vibrates the phone and turns Do Not Disturb on or off.",
            "Android asks the first time for Do Not Disturb access.",
            "Nothing leaves your phone."
        ),
        "display" to ConnectorInfo(
            "Makes the screen brighter or dimmer, sets how long it stays on, turns auto-rotate on or off, and opens the dark theme and battery saver switches. F.R.I.D.A.Y. taps them when Screen control is on; otherwise you tap them.",
            "Android asks the first time to change system settings.",
            "Nothing leaves your phone."
        ),
        "media" to ConnectorInfo(
            "Plays, pauses, skips and goes back in any music or video app.",
            "Nothing to allow.",
            "Nothing leaves your phone."
        ),
        "camera" to ConnectorInfo(
            "Opens the camera app ready for a photo or a video.",
            "Nothing to allow.",
            "Nothing leaves your phone."
        ),
        "clipboard" to ConnectorInfo(
            "Reads what you copied, copies text for you and shows a short note on the screen.",
            "Nothing to allow.",
            "Clipboard text is shared only when you ask."
        ),
        "web" to ConnectorInfo(
            "Searches the web, answers from the results and reads a page or link you name.",
            "Nothing to allow.",
            "Your search words and the pages you ask about go from your phone straight to the website, as in a browser. Nothing goes through our servers."
        ),
        "apps" to ConnectorInfo(
            "Opens apps and settings pages, opens links and shares text.",
            "Nothing to allow.",
            "Nothing leaves your phone."
        ),
        "device_info" to ConnectorInfo(
            "Tells you the time, battery, storage and memory, steps, light and the phone model. It only reads.",
            "Physical activity access for steps.",
            "Shared only when you ask."
        ),
        "alarms" to ConnectorInfo(
            "F.R.I.D.A.Y.'s own alarms and timers. They ring on the alarm volume until you stop or snooze them, and you can list, change and cancel them by voice. They do not use the clock app.",
            "Keep notifications on for F.R.I.D.A.Y. On Xiaomi, Oppo, Vivo and Realme phones also allow Autostart for F.R.I.D.A.Y., or the phone may stop it from ringing.",
            "Nothing leaves your phone."
        ),
        "reminders" to ConnectorInfo(
            "F.R.I.D.A.Y.'s own reminders: a message at a time or after a while, once or every day or week, with a sound and vibration.",
            "Keep notifications on for F.R.I.D.A.Y. On Xiaomi, Oppo, Vivo and Realme phones also allow Autostart for F.R.I.D.A.Y.",
            "Nothing leaves your phone."
        ),
        "calendar" to ConnectorInfo(
            "Checks your schedule, finds free time and adds or cancels events.",
            "Calendar access.",
            "Event names and times are shared when you ask. Changes happen in your real calendar."
        ),
        "contacts" to ConnectorInfo(
            "Finds a contact's number and birthday, and opens a form to save a new contact.",
            "Contacts access.",
            "Shared only when you ask."
        ),
        "calls" to ConnectorInfo(
            "Calls the contact you name straight away, even when it is spelled differently on your phone, and answers, ends or declines a call. Speaker and mute work on most phones.",
            "Phone, Contacts and Answer phone calls access.",
            "The call is placed on your phone and the name is matched on your phone. Nothing is sent except what you say to Gemini."
        ),
        "sms" to ConnectorInfo(
            "Sends a text to a contact or number, or opens the messages app with the text ready. No more than 10 texts an hour. If the text is based on a notification, web page or screen, F.R.I.D.A.Y. asks you first and waits for your spoken yes.",
            "Send SMS and Contacts access. If SMS is not allowed, the text opens in your messages app instead.",
            "The text goes through your phone's own SMS. Nothing is stored by us."
        ),
        "whatsapp" to ConnectorInfo(
            "Opens a WhatsApp chat with your message typed in, or the chat to start a call. WhatsApp does not allow sending without a tap, so you tap Send, or F.R.I.D.A.Y. taps it when Screen control is on.",
            "Contacts access and WhatsApp installed.",
            "The message goes through WhatsApp only when you send it."
        ),
        "location" to ConnectorInfo(
            "Tells you where you are and starts directions in your maps app.",
            "Location access.",
            "Your location is shared when you ask where you are."
        ),
        "notifications" to ConnectorInfo(
            "Reads your notifications so the assistant can tell you what you missed.",
            "Notification access.",
            "Notifications can be private. They are shared whenever you ask."
        ),
        "app_usage" to ConnectorInfo(
            "Shows which apps you use most and for how long.",
            "Usage access.",
            "App names and times are shared when you ask."
        ),
        "weather" to ConnectorInfo(
            "Tells you the weather now and for the next few days, for where you are or any city.",
            "Location access, only for where you are.",
            "A city name or your location goes to a free weather service.",
            "Your phone"
        ),
        "files" to ConnectorInfo(
            "Looks inside any folder on the phone, finds photos, videos, music, documents and any other file by name, and opens them in your own apps.",
            "All files access. A settings screen opens when you add it.",
            "Names and folders are read on your phone. File contents are not sent anywhere unless you ask F.R.I.D.A.Y. to read a file aloud."
        ),
        "navigation" to ConnectorInfo(
            "Goes back and home, opens recents, scrolls, locks the screen and takes screenshots.",
            "Accessibility access. A settings screen opens when you add it.",
            "It does not read the screen."
        ),
        "screen" to ConnectorInfo(
            "Reads the screen and taps and types for you, so the assistant can use other apps.",
            "Accessibility access. A settings screen opens when you add it.",
            "Screen text is shared with Gemini and can be private. Use with care."
        )
    )
}
