package com.nandivex.friday.data

/** Lines the home screen shows while idle. Short and plain; a new one is picked for each session. */
val HOME_GREETINGS: List<String> = listOf(
    "How can I help?",
    "What do you need?",
    "Tell me what to do.",
    "Ready when you are.",
    "Go ahead, I'm listening.",
    "What would you like to do?",
    "Say what you need."
)

/** A random home greeting. */
fun randomHomeGreeting(): String = HOME_GREETINGS.random()
