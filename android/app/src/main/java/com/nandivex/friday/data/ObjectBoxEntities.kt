package com.nandivex.friday.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

/** One finished exchange from a past conversation, kept on the phone so the History page can show it. */
@Entity
data class ConversationTurnEntity(
    @Id var id: Long = 0,
    @Index var sessionId: String = "",
    var userText: String = "",
    var assistantText: String = "",
    @Index var timestampMillis: Long = 0
)
