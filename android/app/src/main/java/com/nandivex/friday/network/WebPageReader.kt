package com.nandivex.friday.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Reads a web page as plain text on the phone: no server in between. Only public http(s) pages are fetched, never
 * addresses on the phone itself or the user's local network. The text is other people's content, so the caller marks
 * it untrusted.
 */
object WebPageReader {
    private const val MAX_DOWNLOAD_BYTES = 2_000_000L
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val http = HttpClients.shared.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun read(url: String, maxChars: Int): String = withContext(Dispatchers.IO) {
        val target = url.trim().toHttpUrlOrNull()
        if (target == null || target.scheme !in setOf("http", "https")) {
            return@withContext "status=failed\nThat is not a web address I can open. Use a full http or https link."
        }
        if (!isPublicHost(target.host)) {
            return@withContext "status=failed\nThat address is on this phone or a private network, so it is not opened."
        }
        try {
            val request = Request.Builder().url(target).header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,text/plain;q=0.9,*/*;q=0.5").build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "status=failed\nThe page answered with error ${response.code}."
                // A redirect may have landed somewhere private; check where the request really ended up.
                if (!isPublicHost(response.request.url.host)) {
                    return@withContext "status=failed\nThat page redirected to a private address, so it is not opened."
                }
                val type = response.header("Content-Type").orEmpty().lowercase()
                if (type.isNotEmpty() && !type.startsWith("text/") && "html" !in type && "xml" !in type) {
                    return@withContext "status=failed\nThat link is not a web page (it is $type)."
                }
                val body = response.peekBody(MAX_DOWNLOAD_BYTES).string()
                val page = extract(body, response.request.url.toString(), maxChars)
                if (page.text.isBlank()) "status=failed\nThe page has no readable text."
                else "status=ok\nTitle: ${page.title}\nAddress: ${response.request.url}\n\n${page.text}"
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            "status=failed\nThe page could not be read."
        }
    }

    internal data class Page(val title: String, val text: String)

    /** The main text of an HTML page: paragraphs and headings of the article if there is one, without menus and scripts. */
    internal fun extract(html: String, baseUrl: String, maxChars: Int): Page {
        val doc = Jsoup.parse(html, baseUrl)
        doc.select("script,style,noscript,svg,iframe,nav,footer,aside,form,button,header nav").remove()
        val root = doc.selectFirst("article") ?: doc.selectFirst("main") ?: doc.selectFirst("[role=main]") ?: doc.body()
        val lines = ArrayList<String>()
        root.select("h1,h2,h3,h4,p,li,pre,blockquote,td,th").forEach { element ->
            val text = element.text().trim()
            if (text.length >= 2 && lines.lastOrNull() != text) lines.add(text)
        }
        val joined = if (lines.isEmpty()) root.text().trim() else lines.joinToString("\n")
        return Page(doc.title().trim(), joined.take(maxChars))
    }

    /** False for loopback, private, link-local and unresolvable hosts. */
    internal fun isPublicHost(host: String): Boolean {
        if (host.isBlank() || host.equals("localhost", ignoreCase = true) || host.endsWith(".local", ignoreCase = true)) return false
        return try {
            InetAddress.getAllByName(host).none {
                it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress || it.isAnyLocalAddress || it.isMulticastAddress
            }
        } catch (e: Exception) {
            false
        }
    }
}
