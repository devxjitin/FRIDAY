package com.nandivex.friday.data

import org.json.JSONObject
import java.net.URI

/**
 * What `latest.json` on the website says about the newest release. [sha256] is lower-case hex of the APK. [rollout] is the
 * share of phones (0 to 100) that may be offered it yet, so a bad release can be held back before it reaches everyone.
 */
class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String,
    val minAndroid: Int,
    val rollout: Int,
    val sizeBytes: Long
)

/** The pure rules of the in-app updater, free of Android types so they are unit tested. */
object UpdateRules {
    /** The only hosts an update may be downloaded from: our site and its Firebase backup address. */
    val ALLOWED_HOSTS: Set<String> = setOf("friday.nandivex.in", "friday-nandivex.web.app", "friday-nandivex.firebaseapp.com")

    /** No FRIDAY release comes close to this, so a larger download is refused. */
    const val MAX_APK_BYTES: Long = 150L * 1024 * 1024

    private val SHA256 = Regex("^[0-9a-fA-F]{64}$")

    /** True for an https address on one of [ALLOWED_HOSTS], with no user info or odd port. */
    fun isAllowedUrl(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return false
        return uri.host?.lowercase() in ALLOWED_HOSTS
    }

    /** Reads `latest.json`. Returns null when a needed field is missing or unsafe, so a broken file is never offered. */
    fun parse(json: String): UpdateManifest? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val code = obj.optLong("versionCode", -1L)
        val name = obj.optString("versionName").trim()
        val url = obj.optString("apkUrl").trim()
        val sha = obj.optString("sha256").trim()
        if (code <= 0 || name.isEmpty() || !isAllowedUrl(url) || !SHA256.matches(sha)) return null
        val size = obj.optLong("sizeBytes", 0L)
        if (size < 0 || size > MAX_APK_BYTES) return null
        return UpdateManifest(
            versionCode = code,
            versionName = name,
            apkUrl = url,
            sha256 = sha.lowercase(),
            notes = obj.optString("notes").trim().take(2000),
            minAndroid = obj.optInt("minAndroid", 26),
            rollout = obj.optInt("rollout", 100).coerceIn(0, 100),
            sizeBytes = size
        )
    }

    /** A stable number from 0 to 99 for this phone, so the same phone is always inside or outside a given rollout. */
    fun bucket(deviceKey: String): Int {
        var h = 17L
        for (c in deviceKey) h = (h * 31 + c.code) % 1_000_003L
        return (h % 100).toInt()
    }

    fun inRollout(deviceKey: String, percent: Int): Boolean = bucket(deviceKey) < percent

    /** Whether this phone should be offered [manifest]: newer, runs on this Android version, and inside the rollout. */
    fun shouldOffer(manifest: UpdateManifest, currentVersionCode: Long, sdkInt: Int, deviceKey: String): Boolean =
        manifest.versionCode > currentVersionCode && sdkInt >= manifest.minAndroid && inRollout(deviceKey, manifest.rollout)

    /** Lower-case hex of [bytes], for comparing a download's SHA-256 with the manifest. */
    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
