package com.nandivex.friday.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Phone actions that only need a system service or an intent: system panels, the app list and links. Each returns the
 * "status=..." text the voice model reads. Permission and access checks stay with the caller ([DeviceToolRunner]).
 */
class PhoneExtras(private val app: Context, private val start: (Intent) -> Unit) {

    fun openPanel(panel: String?): String {
        val action = if (Build.VERSION.SDK_INT >= 29) when (panel) {
            "wifi" -> Settings.Panel.ACTION_WIFI
            "nfc" -> Settings.Panel.ACTION_NFC
            "volume" -> Settings.Panel.ACTION_VOLUME
            else -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        } else when (panel) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "nfc" -> Settings.ACTION_NFC_SETTINGS
            "volume" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_WIRELESS_SETTINGS
        }
        start(Intent(action))
        return "status=ok\nThe panel is open. Now tap the switch yourself: read_screen, then click_on_text on it (or tap_screen_position), then read_screen again to confirm. Do not ask the user to do it unless accessibility is off."
    }

    fun listApps(filter: String?): String {
        val pm = app.packageManager
        val names = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() }
            .filter { filter.isNullOrBlank() || it.contains(filter, ignoreCase = true) }
            .distinct().sortedBy { it.lowercase() }
        return if (names.isEmpty()) "status=empty\nNo matching apps."
        else "status=ok\n${names.size} apps: " + names.take(120).joinToString(", ")
    }

    fun openUrl(url: String): String {
        val uri = Uri.parse(url.trim())
        if (uri.scheme != "http" && uri.scheme != "https") return "status=invalid\nOnly http and https links can be opened."
        start(Intent(Intent.ACTION_VIEW, uri))
        return "status=ok\nOpened the link."
    }
}
