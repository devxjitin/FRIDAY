package com.nandivex.friday.data

import android.Manifest
import android.app.ActivityManager
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Application
import android.app.NotificationManager
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Path
import android.net.wifi.WifiManager
import android.content.Intent
import android.content.IntentFilter
import android.bluetooth.BluetoothManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.BatteryManager
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nandivex.friday.AssistantAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.nandivex.friday.network.HttpClients
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/**
 * Executes [DeviceTool] calls and returns the text handed back to the voice model. Every result
 * starts with a `status=` line. Anything that opens another screen only prepares the action; the
 * user still confirms calls and messages themselves.
 */
class DeviceToolRunner(
    private val app: Application,
    private val audioManager: AudioManager,
    /** Brightness override set by this app (1-100), or null when following the system. */
    private val getBrightnessOverride: () -> Int?,
    private val setBrightnessOverride: (Int) -> Unit,
    /** Asks the on-screen activity to show Android's permission dialog for these permissions. */
    private val requestPermissions: (List<String>) -> Unit,
    /** Describes a screenshot (JPEG bytes) following the prompt; used by look_at_screen. */
    private val describeScreen: suspend (ByteArray, String) -> String = { _, _ -> "" }
) {
    private val http by lazy {
        HttpClients.shared.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun run(tool: DeviceTool, args: JSONObject): String = try {
        // Content providers, usage stats and local files can be slow, so they never run on the main thread.
        if (tool in BLOCKING_TOOLS) withContext(Dispatchers.IO) { execute(tool, args) } else execute(tool, args)
    } catch (e: ActivityNotFoundException) {
        "status=unavailable\nNo app on this device can do that."
    } catch (e: SecurityException) {
        "status=unavailable\nAndroid did not allow that action."
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        "status=failed\nCould not complete that action."
    }

    private suspend fun execute(tool: DeviceTool, args: JSONObject): String {
        fun text(name: String) = args.optString(name).trim().takeIf { it.isNotEmpty() }
        fun number(name: String) = if (args.has(name)) args.optInt(name, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE } else null
        return when (tool) {
            DeviceTool.BRIGHTNESS -> {
                val target = DeviceToolLevels.next(currentBrightness(), text("action"), number("percent"), min = 5)
                    ?: return INVALID_LEVEL
                setBrightness(target)
                "status=ok\nBrightness is now $target%."
            }
            DeviceTool.GET_BRIGHTNESS -> "status=ok\nBrightness is ${currentBrightness()}%."
            DeviceTool.VOLUME -> {
                val name = text("stream") ?: "assistant"
                val stream = streamFor(name)
                val max = maxVolume(stream)
                // The assistant and alarm keep a floor so they never silence themselves.
                val floor = if (name == "assistant" || name == "alarm") 10 else 0
                val target = DeviceToolLevels.next(currentVolume(stream), text("action"), number("percent"), min = floor)
                    ?: return INVALID_LEVEL
                audioManager.setStreamVolume(stream, (target * max / 100).coerceIn(if (floor > 0) 1 else 0, max), 0)
                "status=ok\n${streamLabel(name)} volume is now ${currentVolume(stream)}%."
            }
            DeviceTool.GET_VOLUME -> {
                val name = text("stream")
                if (name != null) "status=ok\n${streamLabel(name)} volume is ${currentVolume(streamFor(name))}%."
                else "status=ok\n" + VOLUME_NAMES.joinToString(" ") { "${streamLabel(it)} volume is ${currentVolume(streamFor(it))}%." }
            }
            DeviceTool.TIME -> "status=ok\n" + ZonedDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, hh:mm a (zzz), 'time zone' VV, 'ISO date' yyyy-MM-dd, HH:mm 'in 24-hour'", Locale.ENGLISH))
            DeviceTool.BATTERY -> battery()
            DeviceTool.DEVICE_INFO -> deviceInfo()
            DeviceTool.STORAGE_MEMORY -> storageAndMemory()
            DeviceTool.NETWORK -> network()
            DeviceTool.GET_RINGER -> "status=ok\nRinger mode is ${ringerName(audioManager.ringerMode)}."
            DeviceTool.SET_RINGER -> setRinger(text("mode"))
            DeviceTool.VIBRATE -> {
                val ms = (number("milliseconds") ?: 500).coerceIn(50, 2000)
                app.getSystemService(Vibrator::class.java)
                    ?.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                    ?: return "status=unavailable\nThis device cannot vibrate."
                "status=ok\nVibrated for $ms ms."
            }
            DeviceTool.GET_CLIPBOARD -> {
                val clip = app.getSystemService(ClipboardManager::class.java).primaryClip
                val value = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString()?.take(500)
                if (value.isNullOrBlank()) "status=empty\nThe clipboard has no readable text right now." else "status=ok\n$value"
            }
            DeviceTool.SET_CLIPBOARD -> {
                val value = text("text") ?: return MISSING
                app.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("assistant", value))
                "status=ok\nCopied to the clipboard."
            }
            DeviceTool.SHOW_NOTIFICATION -> showNotification(text("title") ?: return MISSING, text("message") ?: return MISSING, args.optBoolean("urgent"), text("kind"), text("label").orEmpty())
            DeviceTool.FLASHLIGHT -> flashlight(text("state"))
            DeviceTool.MEDIA_CONTROL -> mediaControl(text("command"))
            DeviceTool.OPEN_APP -> openApp(text("name") ?: return MISSING)
            DeviceTool.WIFI_TOGGLE -> wifiToggle(wanted(args))
            DeviceTool.BLUETOOTH_TOGGLE -> bluetoothToggle(wanted(args))
            DeviceTool.LIST_PAIRED_DEVICES -> listPairedDevices()
            DeviceTool.MOBILE_DATA_PANEL -> openInternetPanel("The internet panel", "Mobile data", wanted(args))
            DeviceTool.AIRPLANE_MODE -> {
                start(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
                panelHint("The airplane mode settings", "Airplane mode", wanted(args))
            }
            DeviceTool.HOTSPOT_SETTINGS -> hotspotSettings(wanted(args))
            DeviceTool.NFC_SETTINGS -> nfcSettings(wanted(args))
            DeviceTool.FLASHLIGHT_STROBE -> flashlightStrobe(text("mode"), number("seconds"))
            DeviceTool.DARK_MODE -> {
                start(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                panelHint("The display settings", "Dark theme", wanted(args))
            }
            DeviceTool.BATTERY_SAVER -> {
                start(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                panelHint("The battery saver settings", "Battery Saver", wanted(args))
            }
            DeviceTool.SET_DND -> doNotDisturb(text("state"))
            DeviceTool.BLUETOOTH -> bluetooth()
            DeviceTool.OPEN_CAMERA -> {
                start(Intent(if (text("mode") == "video") MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE))
                "status=ok\nCamera opened."
            }
            DeviceTool.SCHEDULE_REMINDER -> {
                val message = text("message") ?: return MISSING
                val due = runCatching { AutomationEngine.reminderTime(number("minutes"), text("at_time"), text("date")) }
                    .getOrElse { return "status=invalid\n${it.message}" }
                val repeat = AutomationEngine.repeatMinutes(text("repeat"))
                notificationsMissing()?.let { return it }
                // Saved like any scheduled action, so the reminder survives a restart of the phone.
                val scheduled = AutomationEngine.scheduleAt(
                    app, DeviceTool.SHOW_NOTIFICATION,
                    JSONObject().put("title", "Reminder").put("message", message).put("urgent", true).toString(),
                    due, repeat
                )
                val every = when (repeat) { 1_440 -> ", and again every day"; 10_080 -> ", and again every week"; else -> "" }
                if (scheduled.startsWith("status=ok")) "status=ok\nReminder set for ${AutomationEngine.format(due)}$every."
                else scheduled
            }
            DeviceTool.LIST_REMINDERS -> AutomationEngine.listScheduled(app)
            DeviceTool.CANCEL_REMINDER -> AutomationEngine.cancelReminder(app, number("id"))
            DeviceTool.MODIFY_REMINDER -> AutomationEngine.modifyReminder(app, number("id"), number("minutes"), text("message"), text("at_time"), text("date"))
            DeviceTool.STEPS -> steps()
            DeviceTool.LIGHT -> lightLevel()
            DeviceTool.WIFI_INFO -> wifiInfo()
            DeviceTool.READ_SCREEN -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                // A screen can be empty for a moment while an app opens or changes page, so look a few times before concluding.
                var body = service.readScreen()
                var attempt = 0
                while (body.isNullOrBlank() && attempt < 3) {
                    delay(500)
                    body = service.readScreen()
                    attempt++
                }
                val appName = service.currentPackage()?.let { appLabel(it) }
                if (body.isNullOrBlank()) {
                    "status=hidden\n${appName ?: "This app"} shows nothing the assistant can read; some apps hide their screen on purpose. " +
                        "Do not stop: call look_at_screen (it reads the screen as a picture), " +
                        "or try press_back, scroll, wait_for_text, click_on_text with a likely label, or tap_position if the layout is known, " +
                        "or reach the same goal another way (the app's search, a different screen, another app or tool). " +
                        "If nothing works, tell the user which app it is and what you could not see, and offer what you can still do."
                } else "status=ok\nApp: ${appName ?: "unknown"}\n$body"
            }
            DeviceTool.LOOK_AT_SCREEN -> lookAtScreen(text("question"))
            DeviceTool.CURRENT_APP -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                val pkg = service.currentPackage() ?: return "status=unavailable\nCannot tell which app is on screen."
                "status=ok\n${appLabel(pkg)} ($pkg) is on screen."
            }
            DeviceTool.CLICK_TEXT -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                service.clickByText(text("text") ?: return MISSING, (number("occurrence") ?: 1).coerceAtLeast(1))
            }
            DeviceTool.CLICK_ITEM -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                service.clickItem(number("number") ?: return MISSING)
            }
            DeviceTool.OPEN_PANEL -> extras.openPanel(text("panel"))
            DeviceTool.LIST_APPS -> extras.listApps(text("filter"))
            DeviceTool.OPEN_URL -> extras.openUrl(text("url") ?: return MISSING)
            DeviceTool.POWER_MENU -> globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG, "Power menu")
            DeviceTool.SPLIT_SCREEN -> globalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN, "Split screen")
            DeviceTool.SWIPE -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                val x1 = number("from_x") ?: return MISSING
                val y1 = number("from_y") ?: return MISSING
                val x2 = number("to_x") ?: return MISSING
                val y2 = number("to_y") ?: return MISSING
                val metrics = app.resources.displayMetrics
                if (listOf(x1, x2).any { it !in 0..metrics.widthPixels } || listOf(y1, y2).any { it !in 0..metrics.heightPixels }) {
                    return "status=invalid\nThe screen is ${metrics.widthPixels}x${metrics.heightPixels} pixels."
                }
                val ms = (number("milliseconds") ?: 400).coerceIn(100, 3_000).toLong()
                if (service.gesture(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), ms)) "status=ok\nSwiped from $x1,$y1 to $x2,$y2."
                else "status=failed\nThe swipe did not go through."
            }
            DeviceTool.LONG_PRESS -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                val x = number("x") ?: return MISSING
                val y = number("y") ?: return MISSING
                val metrics = app.resources.displayMetrics
                if (x !in 0..metrics.widthPixels || y !in 0..metrics.heightPixels) {
                    return "status=invalid\nThe screen is ${metrics.widthPixels}x${metrics.heightPixels} pixels."
                }
                if (service.longPress(x.toFloat(), y.toFloat())) "status=ok\nLong pressed $x,$y." else "status=failed\nThe long press did not go through."
            }
            DeviceTool.TAP_POSITION -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                val x = number("x") ?: return MISSING
                val y = number("y") ?: return MISSING
                val metrics = app.resources.displayMetrics
                if (x !in 0..metrics.widthPixels || y !in 0..metrics.heightPixels) {
                    return "status=invalid\nThe screen is ${metrics.widthPixels}x${metrics.heightPixels} pixels."
                }
                if (service.tap(x.toFloat(), y.toFloat())) "status=ok\nTapped $x,$y." else "status=failed\nThe tap did not go through."
            }
            DeviceTool.TYPE_TEXT -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                service.typeText(text("text") ?: return MISSING, text("append") == "yes")
            }
            DeviceTool.PRESS_ENTER -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                service.pressEnter()
            }
            DeviceTool.WAIT_FOR_TEXT -> {
                val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
                val wanted = text("text") ?: return MISSING
                val seconds = (number("seconds") ?: 10).coerceIn(1, 30)
                val deadline = System.currentTimeMillis() + seconds * 1000L
                var found = service.screenContains(wanted)
                while (!found && System.currentTimeMillis() < deadline) {
                    delay(500)
                    found = service.screenContains(wanted)
                }
                if (found) "status=ok\n\"$wanted\" is on screen." else "status=timeout\n\"$wanted\" did not appear within $seconds seconds."
            }
            DeviceTool.PRESS_BACK -> globalAction(AccessibilityService.GLOBAL_ACTION_BACK, "Back")
            DeviceTool.RECENT_APPS -> globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "Recent apps")
            DeviceTool.NOTIFICATION_SHADE -> globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notification panel")
            DeviceTool.QUICK_SETTINGS -> globalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Quick settings")
            DeviceTool.LOCK_SCREEN ->
                if (Build.VERSION.SDK_INT < 28) "status=unavailable\nThis needs Android 9 or newer."
                else globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, "Lock screen")
            DeviceTool.SCREENSHOT ->
                if (Build.VERSION.SDK_INT < 28) "status=unavailable\nThis needs Android 9 or newer."
                else globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, "Screenshot")
            DeviceTool.PREVIOUS_APP -> {
                val first = globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "Switch app")
                if (!first.startsWith("status=ok")) return first
                delay(350)
                globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "Switch to the previous app")
            }
            DeviceTool.SCROLL -> scrollScreen(text("direction"), text("amount"), number("times"))
            DeviceTool.HOME_SCREEN -> {
                start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                "status=ok\nShowing the home screen."
            }
            DeviceTool.OPEN_APP_SETTINGS -> {
                val (packageName, label) = findLauncherApp(text("name") ?: return MISSING)
                    ?: return "status=not_found\nNo installed app matches that name."
                start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                "status=ok\nOpened the info page for $label."
            }
            DeviceTool.SCREEN_TIMEOUT -> {
                val seconds = number("seconds")?.takeIf { it in 15..1800 } ?: return "status=invalid\nSeconds must be 15-1800."
                needWriteSettings()?.let { return it }
                Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000)
                "status=ok\nScreen timeout is now $seconds seconds."
            }
            DeviceTool.AUTO_ROTATE -> {
                val on = when (text("state")) { "on" -> 1; "off" -> 0; else -> return "status=invalid\nState must be on or off." }
                needWriteSettings()?.let { return it }
                Settings.System.putInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION, on)
                "status=ok\nAuto-rotate is ${if (on == 1) "on" else "off"}."
            }
            DeviceTool.SET_ALARM -> {
                val hour = number("hour") ?: return MISSING
                val minute = number("minute") ?: return MISSING
                notificationsMissing()?.let { return it }
                OwnClock.setAlarm(app, hour, minute, text("label") ?: "", text("repeat"))
            }
            DeviceTool.LIST_ALARMS -> OwnClock.list(app, OwnClock.KIND_ALARM)
            DeviceTool.CANCEL_ALARM -> OwnClock.cancel(app, OwnClock.KIND_ALARM, number("id"), number("hour"), number("minute"), text("label"))
            DeviceTool.MODIFY_ALARM -> OwnClock.modifyAlarm(
                app, number("hour") ?: return MISSING, number("minute") ?: return MISSING,
                number("new_hour") ?: return MISSING, number("new_minute") ?: return MISSING, text("label")
            )
            DeviceTool.SET_TIMER -> {
                val seconds = number("seconds") ?: return MISSING
                notificationsMissing()?.let { return it }
                OwnClock.setTimer(app, seconds, text("label") ?: "")
            }
            DeviceTool.LIST_TIMERS -> OwnClock.list(app, OwnClock.KIND_TIMER)
            DeviceTool.CANCEL_TIMER -> OwnClock.cancel(app, OwnClock.KIND_TIMER, number("id"), null, null, text("label"))
            DeviceTool.OPEN_SETTINGS -> {
                val action = when (text("screen")) {
                    "wifi" -> Settings.ACTION_WIFI_SETTINGS
                    "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                    "display" -> Settings.ACTION_DISPLAY_SETTINGS
                    "sound" -> Settings.ACTION_SOUND_SETTINGS
                    "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
                start(Intent(action))
                "status=ok\nSettings opened. Now switch it yourself: read_screen, then click_on_text on the switch, then read_screen again to confirm. Do not ask the user to do it unless accessibility is off."
            }
        }
    }

    /** Returns a tool result asking the user to allow [permissions], or null when all are granted. */
    private fun missingPermission(vararg permissions: String): String? {
        val absent = permissions.filter {
            ContextCompat.checkSelfPermission(app, it) != PackageManager.PERMISSION_GRANTED
        }
        if (absent.isEmpty()) return null
        // Asked before and still missing: Android shows no second dialog after "don't allow", so send them to the app's settings page.
        if (absent.all { it in askedPermissions }) {
            start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName)))
            return "status=permission_needed\nThe permission was denied earlier, so Android will not ask again. FRIDAY's App info page " +
                "was opened; the user must open Permissions there and allow it, then ask again."
        }
        askedPermissions.addAll(absent)
        requestPermissions(absent)
        return "status=permission_needed\nAndroid is asking the user for permission on the phone screen. " +
            "Tell them to allow it, then try again."
    }

    private val askedPermissions = mutableSetOf<String>()

    private val extras by lazy { PhoneExtras(app) { intent -> start(intent) } }

    /** Returns a tool result asking for the Modify system settings permission, or null when granted. */
    private fun needWriteSettings(): String? {
        if (Settings.System.canWrite(app)) return null
        start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + app.packageName)))
        return "status=permission_needed\nModify system settings is not allowed for this app. The permission screen was " +
            "opened; the user must allow it there, then ask again."
    }

    /** Package name and label of the launcher app whose name best matches [name]. */
    private fun findLauncherApp(name: String): Pair<String, String>? {
        val pm = app.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        return apps.firstOrNull { it.second.equals(name, ignoreCase = true) }
            ?: apps.firstOrNull { it.second.contains(name, ignoreCase = true) }
    }

    /** Reads the screen as a picture, using the accessibility service's own screenshot (no extra permission). */
    private suspend fun lookAtScreen(question: String?): String {
        val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
        val blocked = "status=blocked\nThis app protects its window, so Android does not let any assistant see it. " +
            "Do not try to get around that. Tell him which app it is, then use the buttons and positions he describes, " +
            "or ask him to read out what he sees."
        val bitmap = when (val shot = service.takeShot()) {
            is AssistantAccessibilityService.Shot.Ok -> shot.bitmap
            AssistantAccessibilityService.Shot.Protected -> return blocked
            AssistantAccessibilityService.Shot.Unsupported -> return "status=unavailable\nThis needs Android 11 or newer."
            AssistantAccessibilityService.Shot.Failed -> return "status=failed\nNo picture of the screen could be taken right now. " +
                "If this keeps happening, the accessibility service may need to be turned off and on again in Android settings."
        }
        if (ScreenImage.looksBlank(bitmap)) return blocked
        val appName = AssistantAccessibilityService.instance?.currentPackage()?.let { appLabel(it) }
        val prompt = "This is a screenshot of an Android phone" + (appName?.let { " showing the app $it" } ?: "") + ". " +
            (question?.takeIf { it.isNotBlank() }?.let { "Focus on: $it. " } ?: "") +
            "Reply with JSON only, no code fence: {\"summary\": \"two or three sentences about what the screen shows\", " +
            "\"items\": [{\"label\": \"visible text or icon meaning\", \"x\": 0, \"y\": 0}]}. " +
            "List up to 40 buttons, links, fields and other things worth tapping, top to bottom. x and y are the centre of the item " +
            "as numbers from 0 to 1000 across the picture (0,0 top left; 1000,1000 bottom right). Never include passwords or card numbers."
        val raw = try {
            describeScreen(ScreenImage.toJpeg(bitmap), prompt)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return "status=failed\nThe picture could not be read (${e.javaClass.simpleName}). Try again."
        }
        if (raw.isBlank()) return "status=failed\nThe picture could not be read."
        val json = runCatching {
            JSONObject(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
        }.getOrNull() ?: return "status=ok\nApp: ${appName ?: "unknown"}\n${raw.trim().take(3_000)}"
        val width = bitmap.width
        val height = bitmap.height
        val items = json.optJSONArray("items")
        val lines = buildString {
            if (items != null) for (i in 0 until minOf(items.length(), 40)) {
                val item = items.optJSONObject(i) ?: continue
                val label = item.optString("label").takeIf { it.isNotBlank() } ?: continue
                val x = (item.optDouble("x", -1.0) / 1000.0 * width).toInt()
                val y = (item.optDouble("y", -1.0) / 1000.0 * height).toInt()
                if (x < 0 || y < 0) continue
                append("[${i + 1}] \"${label.take(80)}\" @$x,$y\n")
            }
        }
        return "status=ok\nApp: ${appName ?: "unknown"}\n${json.optString("summary")}\n$lines" +
            "(Positions are screen pixels for tap_screen_position; they are estimates from a picture, so check the result after tapping.)"
    }

    private fun accessibilityDenied(): String {
        if (AccessChecks.accessibility(app) == AccessState.ENABLED_NOT_CONNECTED) {
            // Switched on but Android has not (re)started the service, common right after an update or an OEM kill.
            return "status=permission_needed\nThe FRIDAY accessibility service is switched on but not running yet. " +
                "Ask the user to switch it off and on again in Accessibility settings, then try again."
        }
        start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        val restricted = if (Build.VERSION.SDK_INT >= 33) {
            " If the switch is greyed out, open FRIDAY's App info, tap the three dots and choose Allow restricted settings first."
        } else ""
        return "status=permission_needed\nThe accessibility service is not enabled. The Accessibility settings were opened; " +
            "the user must turn on FRIDAY there, then ask again.$restricted"
    }

    private fun globalAction(action: Int, label: String): String {
        val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
        return if (service.performGlobalAction(action)) "status=ok\n$label done."
        else "status=failed\nAndroid did not allow that action right now."
    }

    private suspend fun scrollScreen(direction: String?, amount: String?, times: Int?): String {
        val service = AssistantAccessibilityService.instance ?: return accessibilityDenied()
        // Share of the screen the finger travels; a full "page" leaves a margin so the last lines stay in view.
        val share = when (amount) {
            "small" -> 0.15f
            null, "", "medium" -> 0.30f
            "large" -> 0.50f
            "page" -> 0.70f
            else -> return "status=invalid\nAmount must be small, medium, large or page."
        }
        val repeats = (times ?: 1).coerceIn(1, 10)
        val metrics = app.resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()
        val vertical = direction == "up" || direction == "down"
        if (!vertical && direction != "left" && direction != "right") return "status=invalid\nDirection must be up, down, left or right."
        val length = share * if (vertical) h else w
        val centre = if (vertical) h / 2 else w / 2
        // A finger swipe moves content the opposite way: scrolling down swipes up.
        val start = if (direction == "down" || direction == "right") centre + length / 2 else centre - length / 2
        val end = if (direction == "down" || direction == "right") centre - length / 2 else centre + length / 2
        var failed = false
        for (i in 0 until repeats) {
            if (!swipeOnce(service, vertical, w, h, start, end)) { failed = true; break }
            if (i < repeats - 1) delay(250)
        }
        return if (!failed) "status=ok\nScrolled $direction ${amount ?: "medium"}${if (repeats > 1) " $repeats times" else ""}."
        else "status=failed\nThe scroll gesture did not go through."
    }

    /**
     * One slow drag that ends with a short hold, so the finger lifts at zero speed and the list stops where it was
     * dragged instead of coasting on. Longer drags take longer, which keeps the speed steady.
     */
    private suspend fun swipeOnce(
        service: AssistantAccessibilityService, vertical: Boolean, w: Float, h: Float, start: Float, end: Float
    ): Boolean {
        val across = if (vertical) w / 2 else h / 2
        fun point(along: Float) = if (vertical) across to along else along to across
        val (x0, y0) = point(start)
        val (x1, y1) = point(end)
        val dragMs = (kotlin.math.abs(end - start) / 1.2f).toLong().coerceIn(250L, 900L)
        val drag = GestureDescription.StrokeDescription(Path().apply { moveTo(x0, y0); lineTo(x1, y1) }, 0, dragMs, true)
        val hold = drag.continueStroke(Path().apply { moveTo(x1, y1); lineTo(x1, y1 + 1f) }, dragMs, 120, false)
        val dragGesture = GestureDescription.Builder().addStroke(drag).build()
        val holdGesture = GestureDescription.Builder().addStroke(hold).build()
        val done = withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine { continuation ->
                // The hold must be sent from inside the drag's completion callback, or Android drops the continued stroke.
                val finish = object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(true) }
                    override fun onCancelled(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(false) }
                }
                val first = object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (!service.dispatchGesture(holdGesture, finish, null) && continuation.isActive) continuation.resume(false)
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(false) }
                }
                if (!service.dispatchGesture(dragGesture, first, null) && continuation.isActive) continuation.resume(false)
            }
        }
        return done == true
    }

    private fun wifiInfo(): String {
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for both together: on Android 12+ a request for only the precise one shows no dialog.
            requestPermissions(listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return "status=permission_needed\nAndroid is asking the user for location permission on the phone screen, which it needs to read the Wi-Fi name. " +
                "Tell them to allow it, then try again."
        }
        @Suppress("DEPRECATION")
        val info = app.applicationContext.getSystemService(WifiManager::class.java).connectionInfo
        val ssid = info?.ssid?.trim('"')
        if (info == null || ssid.isNullOrBlank() || ssid == "<unknown ssid>") {
            return "status=unavailable\nNot connected to Wi-Fi, or Android is hiding the name because location is switched off."
        }
        return "status=ok\nConnected to $ssid, signal ${info.rssi} dBm, speed ${info.linkSpeed} Mbps."
    }

    private fun appLabel(packageName: String): String {
        val pm = app.packageManager
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)
    }

    private fun doNotDisturb(state: String?): String {
        val manager = app.getSystemService(NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) {
            start(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return "status=permission_needed\nDo Not Disturb access is not granted. The access screen was opened; " +
                "the user must allow this app there, then ask again."
        }
        return when (state) {
            "on" -> { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY); "status=ok\nDo Not Disturb is on." }
            "off" -> { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL); "status=ok\nDo Not Disturb is off." }
            "status" -> "status=ok\nDo Not Disturb is " +
                if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) "off." else "on."
            else -> "status=invalid\nState must be on, off or status."
        }
    }

    private fun bluetooth(): String {
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return "status=unavailable\nThis device has no Bluetooth."
        // Even reading whether Bluetooth is on needs this permission on Android 12+, so it is checked first.
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return missingPermission(Manifest.permission.BLUETOOTH_CONNECT).orEmpty()
        }
        if (!adapter.isEnabled) return "status=ok\nBluetooth is off."
        val names = adapter.bondedDevices.map { it.name ?: "unknown device" }
        return "status=ok\nBluetooth is on. Paired devices: ${if (names.isEmpty()) "none" else names.joinToString()}."
    }

    private suspend fun readSensor(type: Int): Float? {
        val manager = app.getSystemService(SensorManager::class.java)
        val sensor = manager.getDefaultSensor(type) ?: return null
        return withTimeoutOrNull(3_000L) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (continuation.isActive) continuation.resume(event.values[0])
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
    }

    private suspend fun steps(): String {
        if (Build.VERSION.SDK_INT >= 29) missingPermission(Manifest.permission.ACTIVITY_RECOGNITION)?.let { return it }
        val value = readSensor(Sensor.TYPE_STEP_COUNTER)
            ?: return "status=unavailable\nThis phone has no step counter or it gave no reading."
        return "status=ok\n${value.toLong()} steps counted since the phone last restarted."
    }

    private suspend fun lightLevel(): String {
        val lux = readSensor(Sensor.TYPE_LIGHT) ?: return "status=unavailable\nThis phone has no light sensor or it gave no reading."
        val word = when {
            lux < 10 -> "very dark"
            lux < 50 -> "dim"
            lux < 400 -> "normal indoor light"
            lux < 5_000 -> "bright"
            else -> "very bright, like daylight"
        }
        return "status=ok\nAmbient light is ${lux.toInt()} lux ($word)."
    }

    /** Sets screen brightness for the whole phone when Modify system settings is allowed, otherwise only inside FRIDAY. */
    private fun setBrightness(percent: Int) {
        if (Settings.System.canWrite(app)) {
            val ok = runCatching {
                Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (percent * 255 / 100).coerceIn(1, 255))
            }.isSuccess
            if (ok) { setBrightnessOverride(-1); return }
        }
        setBrightnessOverride(percent)
    }

    private fun start(intent: Intent) {
        // The accessibility service is allowed to start screens from the background; a plain app context is not, and Android drops that silently.
        val context: android.content.Context = AssistantAccessibilityService.instance ?: app
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun streamFor(name: String) = when (name) {
        "media" -> AudioManager.STREAM_MUSIC
        "ring" -> AudioManager.STREAM_RING
        "alarm" -> AudioManager.STREAM_ALARM
        "notification" -> AudioManager.STREAM_NOTIFICATION
        else -> VOLUME_STREAM
    }

    private fun streamLabel(name: String) = name.replaceFirstChar { it.uppercase() }

    private fun maxVolume(stream: Int) = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)

    private fun currentVolume(stream: Int) = audioManager.getStreamVolume(stream) * 100 / maxVolume(stream)

    private fun currentBrightness(): Int = getBrightnessOverride() ?: runCatching {
        Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS) * 100 / 255
    }.getOrDefault(50).coerceIn(1, 100)

    private fun battery(): String {
        val intent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return "status=unavailable\nBattery information is not available."
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val saver = app.getSystemService(PowerManager::class.java).isPowerSaveMode
        return "status=ok\nBattery is ${level * 100 / scale}% and ${if (charging) "charging" else "not charging"}. " +
            "Temperature $tempC C. Battery saver is ${if (saver) "on" else "off"}."
    }

    private fun deviceInfo(): String {
        val metrics = app.resources.displayMetrics
        val landscape = app.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val screenOn = app.getSystemService(PowerManager::class.java).isInteractive
        return "status=ok\nDevice ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}). " +
            "Screen ${metrics.widthPixels}x${metrics.heightPixels}, ${if (landscape) "landscape" else "portrait"}, " +
            "${if (screenOn) "on" else "off"}. Language ${Locale.getDefault().displayName}. Time zone ${TimeZone.getDefault().id}."
    }

    private fun storageAndMemory(): String {
        fun gb(bytes: Long) = "%.1f GB".format(Locale.ENGLISH, bytes / 1_073_741_824.0)
        val stat = StatFs(Environment.getDataDirectory().path)
        val memory = ActivityManager.MemoryInfo().also { app.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
        return "status=ok\nStorage: ${gb(stat.availableBytes)} free of ${gb(stat.totalBytes)}. " +
            "RAM: ${gb(memory.availMem)} free of ${gb(memory.totalMem)}."
    }

    private fun network(): String {
        val manager = app.getSystemService(ConnectivityManager::class.java)
        val caps = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
            ?: return "status=ok\nThe device is offline."
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "another network"
        }
        val internet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return "status=ok\nConnected via $type. Internet access is ${if (internet) "working" else "not confirmed"}."
    }

    private fun ringerName(mode: Int) = when (mode) {
        AudioManager.RINGER_MODE_SILENT -> "silent"
        AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
        else -> "normal"
    }

    private fun setRinger(mode: String?): String {
        val target = when (mode) {
            "normal" -> AudioManager.RINGER_MODE_NORMAL
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "silent" -> AudioManager.RINGER_MODE_SILENT
            else -> return "status=invalid\nMode must be normal, vibrate or silent."
        }
        return try {
            audioManager.ringerMode = target
            "status=ok\nRinger mode is now ${ringerName(audioManager.ringerMode)}."
        } catch (e: SecurityException) {
            "status=unavailable\nChanging this needs Do Not Disturb access, which has not been granted to the app."
        }
    }

    private fun showNotification(title: String, message: String, urgent: Boolean, kind: String? = null, label: String = ""): String =
        if (AlertNotifications.show(app, title, message, urgent, kind, label)) "status=ok\nNotification shown."
        else "status=unavailable\nNotifications are turned off for this app."

    /**
     * Alarms, timers and reminders need FRIDAY's notifications: that is where they show and where Stop is. Returns a tool
     * result asking for them (Android 13+ shows its own dialog), or null when they are on.
     */
    private fun notificationsMissing(): String? {
        if (NotificationManagerCompat.from(app).areNotificationsEnabled()) return null
        if (Build.VERSION.SDK_INT >= 33) missingPermission(Manifest.permission.POST_NOTIFICATIONS)?.let { return it }
        start(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName))
        return "status=permission_needed\nNotifications are turned off for FRIDAY, so nothing was set: it could not ring or show. " +
            "FRIDAY's notification settings were opened; the user must turn them on, then ask again."
    }

    private fun flashlight(state: String?): String {
        if (state != "on" && state != "off") return "status=invalid\nState must be on or off."
        val manager = app.getSystemService(CameraManager::class.java)
        val id = manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "status=unavailable\nThis device has no flashlight."
        strobeGeneration.incrementAndGet()
        manager.setTorchMode(id, state == "on")
        return "status=ok\nFlashlight is $state."
    }

    private val strobeGeneration = java.util.concurrent.atomic.AtomicInteger()
    private val strobeScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

    /** Blinks the torch in the background and returns at once; it stops by itself, or when the flashlight tool is used. */
    private fun flashlightStrobe(mode: String?, requestedSeconds: Int?): String {
        val steps = FlashPatterns.forMode(mode) ?: return "status=invalid\nMode must be strobe or sos."
        val seconds = FlashPatterns.seconds(requestedSeconds)
        val manager = app.getSystemService(CameraManager::class.java)
        val id = manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "status=unavailable\nThis device has no flashlight."
        val mine = strobeGeneration.incrementAndGet()
        strobeScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(seconds * 1_000L) {
                    while (strobeGeneration.get() == mine) {
                        for (step in steps) {
                            if (strobeGeneration.get() != mine) return@withTimeoutOrNull
                            manager.setTorchMode(id, step.on)
                            kotlinx.coroutines.delay(step.ms)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                // Only the blink that is still current switches the torch off; a newer on/off request owns it now.
                if (strobeGeneration.get() == mine) runCatching { manager.setTorchMode(id, false) }
            }
        }
        return "status=ok\n${if (mode == "sos") "SOS" else "Strobe"} blink started for $seconds seconds. It stops by itself, or say flashlight off."
    }

    /** After a system panel or settings page opened: tap the switch with Screen control when it is on, otherwise ask the user. */
    private fun panelHint(what: String, switchName: String, wanted: String?): String {
        val goal = wanted?.let { " to $it" }.orEmpty()
        return if (AssistantAccessibilityService.instance != null)
            "status=ok\n$what opened. Screen control is on: call read_screen, click the \"$switchName\" switch$goal, then read the screen again to confirm and tell the user the result."
        else "status=ok\n$what opened. Screen control is off, so nothing was switched: tell the user to tap \"$switchName\"$goal themselves, or to turn on Screen control so you can do it."
    }

    private fun wanted(args: JSONObject) = if (args.optString("state").trim() == "off") "off" else "on"

    @Suppress("DEPRECATION")
    private fun wifiToggle(state: String): String {
        val wifi = app.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
            ?: return "status=unavailable\nThis device has no Wi-Fi."
        val on = state == "on"
        if (wifi.isWifiEnabled == on) return "status=ok\nWi-Fi is already $state."
        if (Build.VERSION.SDK_INT < 29) {
            wifi.isWifiEnabled = on
            return "status=ok\nWi-Fi turned $state."
        }
        start(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        return panelHint("The internet panel", "Wi-Fi", state)
    }

    private fun bluetoothAdapterOrError(): Pair<android.bluetooth.BluetoothAdapter?, String?> {
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return null to "status=unavailable\nThis device has no Bluetooth."
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return null to missingPermission(Manifest.permission.BLUETOOTH_CONNECT).orEmpty()
        return adapter to null
    }

    private fun bluetoothToggle(state: String): String {
        val (adapter, error) = bluetoothAdapterOrError()
        if (adapter == null) return error.orEmpty()
        val on = state == "on"
        if (adapter.isEnabled == on) return "status=ok\nBluetooth is already $state."
        return if (on) {
            start(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            panelHint("Android's Bluetooth question", "Allow", null)
        } else {
            start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            panelHint("The Bluetooth settings", "Bluetooth", "off")
        }
    }

    @android.annotation.SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is checked in bluetoothAdapterOrError
    private fun listPairedDevices(): String {
        val (adapter, error) = bluetoothAdapterOrError()
        if (adapter == null) return error.orEmpty()
        val devices = adapter.bondedDevices.orEmpty()
        if (devices.isEmpty()) return "status=empty\nNo Bluetooth devices are paired."
        val lines = devices.take(30).map { d ->
            val kind = when (d.bluetoothClass?.majorDeviceClass) {
                android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> "audio"
                android.bluetooth.BluetoothClass.Device.Major.PHONE -> "phone"
                android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> "computer"
                android.bluetooth.BluetoothClass.Device.Major.WEARABLE -> "wearable"
                android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL -> "keyboard or mouse"
                else -> "other"
            }
            "${d.name ?: "unknown device"} ($kind)"
        }
        return "status=ok\nBluetooth is ${if (adapter.isEnabled) "on" else "off"}. Paired: ${lines.joinToString("; ")}."
    }

    private fun openInternetPanel(what: String, switchName: String, wanted: String): String {
        start(if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_WIRELESS_SETTINGS))
        return panelHint(what, switchName, wanted)
    }

    private fun hotspotSettings(wanted: String): String {
        val direct = Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings")
        try {
            start(direct)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            start(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
        return panelHint("The hotspot settings", "Wi-Fi hotspot", wanted)
    }

    private fun nfcSettings(wanted: String): String {
        start(if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_NFC) else Intent(Settings.ACTION_NFC_SETTINGS))
        return panelHint("The NFC panel", "NFC", wanted)
    }

    private fun mediaControl(command: String?): String {
        val key = when (command) {
            "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return "status=invalid\nUnknown media command."
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        return "status=ok\nSent $command."
    }

    /** Matches the launcher label exactly first, then by substring. Never returns the app list. */
    private fun openApp(name: String): String {
        val pm = app.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0).map { it to it.loadLabel(pm).toString() }
        val match = apps.firstOrNull { it.second.equals(name, ignoreCase = true) }
            ?: apps.firstOrNull { it.second.contains(name, ignoreCase = true) }
            ?: return "status=not_found\nNo installed app matches \"$name\"."
        val launch = pm.getLaunchIntentForPackage(match.first.activityInfo.packageName)
            ?: return "status=unavailable\nThat app cannot be opened."
        start(launch)
        return "status=ok\nOpened ${match.second}."
    }

    private companion object {
        /** The assistant's voice plays in communication mode, so it follows the call volume. */
        const val VOLUME_STREAM = AudioManager.STREAM_VOICE_CALL
        val VOLUME_NAMES = listOf("assistant", "media", "ring", "alarm", "notification")
        const val INVALID_LEVEL = "status=invalid\nUse action increase, decrease, or set with a percent."
        /** Tools that query storage or the installed apps. */
        val BLOCKING_TOOLS = setOf(
            DeviceTool.STORAGE_MEMORY, DeviceTool.OPEN_APP, DeviceTool.OPEN_APP_SETTINGS, DeviceTool.LIST_APPS
        )
        const val MISSING ="status=invalid\nA required value was missing."
    }
}
