package com.nandivex.friday.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.nandivex.friday.AssistantAccessibilityService

/** How far an Android special access (the accessibility service) is from working. */
enum class AccessState { GRANTED, ENABLED_NOT_CONNECTED, OFF }

/** One place that answers "is this access really on", shared by the phone control gate and the tool runner. */
object AccessChecks {
    fun accessibility(context: Context): AccessState {
        if (AssistantAccessibilityService.instance != null) return AccessState.GRANTED
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val mine = ComponentName(context, AssistantAccessibilityService::class.java)
        val listed = enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.let { name -> name == mine } == true
        }
        return if (listed) AccessState.ENABLED_NOT_CONNECTED else AccessState.OFF
    }

    /** True once the user has switched on FRIDAY's phone control (it may still be connecting). */
    fun phoneControlOn(context: Context): Boolean = accessibility(context) != AccessState.OFF

    /**
     * The runtime permissions the built-in tools use, asked once after phone control is on: the Wi-Fi name needs
     * location, Bluetooth needs nearby devices from Android 12, and the step counter needs physical activity.
     */
    fun toolPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    fun missingToolPermissions(context: Context): List<String> = toolPermissions().filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}
