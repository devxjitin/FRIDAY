package com.nandivex.friday.data

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.nandivex.friday.network.HttpClients
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** A problem the user can be told about in plain words. */
class UpdateException(message: String) : Exception(message)

/** What happened when the installer was asked to run. */
enum class InstallStep { STARTED, NEEDS_PERMISSION }

/**
 * Finds, downloads, checks and hands over an update: `latest.json` comes from the Nandivex website and the APK from our GitHub release. Nothing is installed without the user
 * tapping Install and then confirming in Android's own installer. The download must come from an allowed host, match the
 * checksum in `latest.json`, be our own package and be a newer version, before Android is asked to install it.
 */
class UpdateInstaller(
    private val app: Application,
    http: OkHttpClient = HttpClients.shared
) {
    private val client = http.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val dir get() = File(app.cacheDir, "updates")

    /** The first readable `latest.json` among [urls], or null when none could be reached or none was valid. */
    suspend fun fetchManifest(urls: List<String>): UpdateManifest? = withContext(Dispatchers.IO) {
        for (url in urls) {
            if (!UpdateRules.isAllowedUrl(url)) continue
            try {
                val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
                val text = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null
                    else response.body?.byteStream()?.readAtMost(MAX_MANIFEST_BYTES)?.toString(Charsets.UTF_8)
                }
                UpdateRules.parse(text.orEmpty())?.let { return@withContext it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Try the next address.
            }
        }
        null
    }

    /** Downloads [manifest]'s APK into the cache, reporting 0..1 progress, and returns it once the checksum matches. */
    suspend fun download(manifest: UpdateManifest, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "download.part")
        val target = File(dir, "FRIDAY-${manifest.versionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            client.newCall(Request.Builder().url(manifest.apkUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw UpdateException("The download failed (error ${response.code}).")
                if (!UpdateRules.isAllowedDownloadUrl(response.request.url.toString())) {
                    throw UpdateException("The download moved to an address that is not allowed.")
                }
                val body = response.body ?: throw UpdateException("The download was empty.")
                val total = body.contentLength().takeIf { it > 0 } ?: manifest.sizeBytes
                if (total > UpdateRules.MAX_APK_BYTES) throw UpdateException("The download is too large.")
                var read = 0L
                body.byteStream().use { input ->
                    part.outputStream().use { out ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            read += n
                            if (read > UpdateRules.MAX_APK_BYTES) throw UpdateException("The download is too large.")
                            digest.update(buffer, 0, n)
                            out.write(buffer, 0, n)
                            if (total > 0) onProgress((read.toFloat() / total).coerceAtMost(1f))
                        }
                    }
                }
            }
            if (UpdateRules.hex(digest.digest()) != manifest.sha256) {
                throw UpdateException("The file did not match its checksum, so it was not installed.")
            }
            if (!part.renameTo(target)) throw UpdateException("The file could not be saved.")
            target
        } catch (e: Exception) {
            part.delete()
            throw e
        }
    }

    /**
     * A message when [file] is not a newer FRIDAY signed with our key, else null. Android refuses a wrong key anyway; this
     * only gives a clear reason first. When the signer cannot be read on some phone, the check is skipped and Android decides.
     */
    fun problemWith(file: File): String? {
        val pm = app.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else signaturesFlag()
        val archive = pm.getPackageArchiveInfo(file.path, flags) ?: return "That file is not an app."
        if (archive.packageName != app.packageName) return "That file is for a different app."
        val installed = runCatching { pm.getPackageInfo(app.packageName, flags) }.getOrNull()
        if (installed != null && PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            return "That is not a newer version."
        }
        val theirs = signers(archive)
        val ours = installed?.let { signers(it) }.orEmpty()
        if (theirs.isNotEmpty() && ours.isNotEmpty() && theirs.intersect(ours).isEmpty()) {
            return "That file is signed by someone else, so it cannot replace this app."
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun signaturesFlag() = PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { UpdateRules.hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
    }

    /** Opens Android's installer for [file], or the screen where the user allows installs from FRIDAY first. */
    fun install(file: File): InstallStep {
        if (!app.packageManager.canRequestPackageInstalls()) {
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return InstallStep.NEEDS_PERMISSION
        }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", file)
        app.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return InstallStep.STARTED
    }

    private companion object {
        const val MAX_MANIFEST_BYTES = 64 * 1024
    }
}
