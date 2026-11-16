package com.nandivex.friday.data

/**
 * Whether the assistant may run right now. The voice session code checks it before it connects to Gemini, so a locked
 * app never opens a session in the background. It is open today; the first-run setup (Sprint 5.1) will close it until
 * setup is finished.
 */
object AccessGate {
    @Volatile var unlocked: Boolean = true
}
