package com.nandivex.friday.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPageReaderTest {
    @Test
    fun `extract keeps the article text and drops menus scripts and footers`() {
        val html = """
            <html><head><title> Match report </title><script>var x = 1;</script></head>
            <body><nav><li>Home</li><li>Sport</li></nav>
            <article><h1>Final result</h1><p>India won by 5 wickets.</p><p>India won by 5 wickets.</p><ul><li>Kohli 82</li></ul></article>
            <footer><p>Copyright</p></footer></body></html>
        """.trimIndent()
        val page = WebPageReader.extract(html, "https://example.com/a", 1_000)
        assertEquals("Match report", page.title)
        assertEquals("Final result\nIndia won by 5 wickets.\nKohli 82", page.text)
    }

    @Test
    fun `extract without an article uses the body and respects the limit`() {
        val page = WebPageReader.extract("<body><p>abcdefghij</p></body>", "https://example.com", 4)
        assertEquals("abcd", page.text)
    }

    @Test
    fun `private and local hosts are refused`() {
        assertFalse(WebPageReader.isPublicHost("localhost"))
        assertFalse(WebPageReader.isPublicHost("127.0.0.1"))
        assertFalse(WebPageReader.isPublicHost("192.168.1.10"))
        assertFalse(WebPageReader.isPublicHost("10.0.0.5"))
        assertFalse(WebPageReader.isPublicHost("169.254.1.1"))
        assertFalse(WebPageReader.isPublicHost("printer.local"))
        assertTrue(WebPageReader.isPublicHost("8.8.8.8"))
    }
}
