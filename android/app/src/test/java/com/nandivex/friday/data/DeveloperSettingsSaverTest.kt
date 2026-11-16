package com.nandivex.friday.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeveloperSettingsSaverTest {
    private class FakeKeys(
        var api: String? = "old-api",
        var failApiWrite: Boolean = false,
        var failRestore: Boolean = false
    ) : ApiKeyStorage {
        val writes = mutableListOf<String>()
        override fun getApiKey() = api
        override fun setApiKey(key: String): Boolean {
            writes += "api=$key"
            if (failApiWrite || (failRestore && key == "old-api")) return false
            api = key; return true
        }
        override fun clearApiKey(): Boolean { writes += "api=clear"; if (failRestore) return false; api = null; return true }
    }

    private suspend fun failure(block: suspend () -> Unit): DeveloperSaveFailedException = try {
        block(); fail("expected DeveloperSaveFailedException"); error("unreachable")
    } catch (e: DeveloperSaveFailedException) { e }

    @Test fun `success writes the changed key and then settings`() = runBlocking {
        val keys = FakeKeys()
        var settingsWritten = false
        saveDeveloperConfiguration(keys, "new-api") { settingsWritten = true }
        assertEquals(listOf("api=new-api"), keys.writes)
        assertEquals("new-api", keys.api)
        assertTrue(settingsWritten)
    }

    @Test fun `an unchanged key is not rewritten`() = runBlocking {
        val keys = FakeKeys()
        saveDeveloperConfiguration(keys, null) { }
        assertTrue(keys.writes.isEmpty())
        assertEquals("old-api", keys.api)
    }

    @Test fun `settings failure restores the changed key`() = runBlocking {
        val keys = FakeKeys()
        val e = failure { saveDeveloperConfiguration(keys, "new-api") { throw java.io.IOException("disk") } }
        assertTrue(e.rollbackComplete)
        assertEquals("old-api", keys.api)
    }

    @Test fun `a failed key write is reported and the old key stays`() = runBlocking {
        val keys = FakeKeys(failApiWrite = true)
        var settingsWritten = false
        failure { saveDeveloperConfiguration(keys, "new-api") { settingsWritten = true } }
        assertFalse(settingsWritten)
        assertEquals("old-api", keys.api)
    }

    @Test fun `a key that did not exist before is cleared on failure`() = runBlocking {
        val keys = FakeKeys(api = null)
        failure { saveDeveloperConfiguration(keys, "new-api") { throw java.io.IOException("disk") } }
        assertNull(keys.api)
    }

    @Test fun `a failed restore is reported`() = runBlocking {
        val keys = FakeKeys(failRestore = true)
        val e = failure { saveDeveloperConfiguration(keys, "new-api") { throw java.io.IOException("disk") } }
        assertFalse(e.rollbackComplete)
    }

    @Test fun `cancellation restores the key and is rethrown`() = runBlocking {
        val keys = FakeKeys()
        try {
            saveDeveloperConfiguration(keys, "new-api") { throw CancellationException("stop") }
            fail("expected cancellation")
        } catch (e: CancellationException) {
            assertEquals("old-api", keys.api)
        }
    }
}
