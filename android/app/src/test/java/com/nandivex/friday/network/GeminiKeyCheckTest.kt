package com.nandivex.friday.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiKeyCheckTest {
    @Test fun `success means the key is valid`() {
        assertEquals(KeyCheckResult.Valid, GeminiKeyCheck.interpret(200, "{}"))
    }

    @Test fun `a bad key is invalid`() {
        val body = """{"error":{"code":400,"message":"API key not valid.","details":[{"reason":"API_KEY_INVALID"}]}}"""
        assertEquals(KeyCheckResult.Invalid, GeminiKeyCheck.interpret(400, body))
        assertEquals(KeyCheckResult.Invalid, GeminiKeyCheck.interpret(401, null))
        assertEquals(KeyCheckResult.Invalid, GeminiKeyCheck.interpret(403, """{"error":{"details":[{"reason":"API_KEY_INVALID"}]}}"""))
    }

    @Test fun `a forbidden request on a real key is not called invalid`() {
        assertEquals(KeyCheckResult.NotAllowed, GeminiKeyCheck.interpret(403, """{"error":{"status":"PERMISSION_DENIED"}}"""))
    }

    @Test fun `rate limits and server errors are reported as such`() {
        assertEquals(KeyCheckResult.QuotaExceeded, GeminiKeyCheck.interpret(429, null))
        assertEquals(KeyCheckResult.Unexpected(503), GeminiKeyCheck.interpret(503, null))
    }

    @Test fun `every result has a message`() {
        listOf(
            KeyCheckResult.Valid, KeyCheckResult.Invalid, KeyCheckResult.NotAllowed, KeyCheckResult.QuotaExceeded,
            KeyCheckResult.NoConnection, KeyCheckResult.Unexpected(500)
        ).forEach { assertTrue(GeminiKeyCheck.message(it).isNotBlank()) }
    }
}
