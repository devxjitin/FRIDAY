package com.nandivex.friday.data

import io.objectbox.BoxStore

/** A finished exchange as read back from long-term memory. */
data class StoredTurn(
    val id: Long,
    val sessionId: String,
    val userText: String,
    val assistantText: String,
    val timestampMillis: Long
)

/**
 * Every finished exchange, kept on the phone with no expiry (the user deletes them). This is the long-term memory the
 * assistant searches; [ConversationStore] only keeps the last few minutes for the notification line.
 */
class ConversationHistoryStore(store: BoxStore) {
    private val box = store.boxFor(ConversationTurnEntity::class.java)

    companion object {
        /** Exchanges whose user text is shorter than this are noise (a cough, "uh") and are not worth remembering. */
        const val MIN_USER_CHARS = 2
        /** Most characters of one exchange kept for display. */
        const val MAX_TURN_CHARS = 2_000
        private const val TEXT_BYTES_ESTIMATE = 1_024L

    }

    /** Saves one exchange. Returns its id, or null when it was noise (no real user text). */
    fun append(sessionId: String, userText: String, assistantText: String, timestampMillis: Long): Long? {
        val user = userText.trim()
        if (user.length < MIN_USER_CHARS) return null
        return box.put(
            ConversationTurnEntity(
                sessionId = sessionId,
                userText = user.take(MAX_TURN_CHARS),
                assistantText = assistantText.trim().take(MAX_TURN_CHARS),
                timestampMillis = timestampMillis
            )
        )
    }

    /**
     * The newest [count] exchanges, oldest first, so they read as a conversation. Exchanges of [excludeSessionId]
     * (the session in progress, already in the model's context) are left out.
     */
    fun recent(count: Int, excludeSessionId: String? = null): List<StoredTurn> {
        if (count <= 0) return emptyList()
        val builder = if (excludeSessionId == null) box.query()
        else box.query(ConversationTurnEntity_.sessionId.notEqual(excludeSessionId))
        val query = builder.orderDesc(ConversationTurnEntity_.timestampMillis).build()
        return try {
            query.find(0, count.toLong()).map { it.toStored() }.reversed()
        } finally {
            query.close()
        }
    }

    fun count(): Long = box.count()

    /** Deletes one exchange. Returns false when it was already gone. */
    fun delete(id: Long): Boolean = box.remove(id)

    /** Deletes everything. */
    fun clearAll() = box.removeAll()

    /** About how much space the saved exchanges take: each one is counted as about 1 KB. An estimate for the Storage page. */
    fun approxBytes(): Long = box.count() * TEXT_BYTES_ESTIMATE

    fun get(id: Long): StoredTurn? = box.get(id)?.toStored()

    /** The turns with these ids, in the same order; ids that no longer exist are left out. */
    fun getAll(ids: List<Long>): List<StoredTurn> {
        if (ids.isEmpty()) return emptyList()
        val byId = box.get(ids).filterNotNull().associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toStored() }
    }

    /** Every turn of one session, oldest first. */
    fun sessionTurns(sessionId: String): List<StoredTurn> {
        val query = box.query(ConversationTurnEntity_.sessionId.equal(sessionId))
            .order(ConversationTurnEntity_.timestampMillis).build()
        return try {
            query.find().map { it.toStored() }
        } finally {
            query.close()
        }
    }

    private fun ConversationTurnEntity.toStored() = StoredTurn(
        id = id,
        sessionId = sessionId,
        userText = userText,
        assistantText = assistantText,
        timestampMillis = timestampMillis
    )
}

