package com.nandivex.friday.data

/**
 * Whether the assistant may run right now. The voice session code checks it before it connects to Gemini, so a locked
 * app never opens a session in the background. It stays closed until first-run setup has nothing left to ask; MainActivity
 * opens it.
 */
object AccessGate {
    @Volatile var unlocked: Boolean = false
}
