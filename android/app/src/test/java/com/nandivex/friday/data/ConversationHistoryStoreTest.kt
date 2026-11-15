package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs against a real on-disk ObjectBox store, with the saved conversation history. */
class ConversationHistoryStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun <T> withStore(block: (ConversationHistoryStore) -> T): T =
        MyObjectBox.builder().directory(temporary.newFolder()).build().use { block(ConversationHistoryStore(it)) }

    @Test fun `noise with no real user text is not saved`() = withStore { store ->
        assertNull(store.append("s", "", "hello", 1L))
        assertNull(store.append("s", " a ", "hello", 2L))
        assertEquals(0L, store.count())
        assertNotNull(store.append("s", "hi", "hello", 3L))
        assertEquals(1L, store.count())
    }

    @Test fun `recent returns the newest turns oldest first`() = withStore { store ->
        (1..5).forEach { store.append("s", "question $it", "answer $it", it * 1_000L) }
        assertEquals(listOf("question 3", "question 4", "question 5"), store.recent(3).map { it.userText })
        assertTrue(store.recent(0).isEmpty())
        assertEquals(5, store.recent(50).size)
    }

    @Test fun `turns are kept however old they are`() = withStore { store ->
        store.append("old", "long ago", "yes", 1L)
        store.append("new", "today", "yes", System.currentTimeMillis())
        assertEquals(listOf("long ago", "today"), store.recent(10).map { it.userText })
    }
}
