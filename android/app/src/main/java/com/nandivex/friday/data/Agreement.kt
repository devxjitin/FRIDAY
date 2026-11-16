package com.nandivex.friday.data

/**
 * The agreement shown in setup. Every statement here must match what the app does (see docs/DATA_FLOWS.md). When the
 * text changes in a way that matters, raise [TERMS_VERSION] so users are asked again.
 */
object Agreement {
    data class Section(val title: String, val body: String)

    const val TITLE = "Before you start"
    const val INTRO = "Please read this. It explains what F.R.I.D.A.Y. does on your phone and what it shares."

    val sections: List<Section> = listOf(
        Section(
            "What F.R.I.D.A.Y. does",
            "When you ask by voice, it reads what is on your screen and taps, types, scrolls, swipes, opens apps and changes " +
                "settings for you, the way your own hands would. To do this it needs Android's accessibility permission, " +
                "which you turn on in the next steps. You can turn it off at any time in Android's Settings, Accessibility."
        ),
        Section(
            "What leaves your phone",
            "While a voice session is open, your microphone audio is sent to Google Gemini. When F.R.I.D.A.Y. carries out " +
                "a request it sends the text on your screen, and a screenshot when it needs to look at the screen as a picture. " +
                "That can include private things on the screen at that moment, such as messages, names, numbers, one-time " +
                "codes or amounts. It goes from your phone straight to Google using your own Gemini API key, not through " +
                "Nandivex servers. What Google does with it is set by Google's Gemini API terms and the plan of your key. For " +
                "example, Google may use content sent with a free key to improve its products. Crash reports go to Google " +
                "Firebase Crashlytics."
        ),
        Section(
            "What stays on your phone",
            "Your API key (encrypted), your memories, your conversation history, your settings and your name. Uninstalling " +
                "the app or clearing its data removes them. You can also delete memories and history inside the app."
        ),
        Section(
            "Your responsibility",
            "F.R.I.D.A.Y. can make mistakes. Today it does not ask you before it sends a message, pays or deletes " +
                "something, so say clearly what you want and check the result. Do not ask it to look at a screen you do not " +
                "want Google to see. It cannot read apps that protect their window, such as some banking apps. It is not an " +
                "emergency or medical device: do not rely on it to call for help."
        ),
        Section(
            "Terms",
            "F.R.I.D.A.Y. is made by Nandivex Technologies and is given to you for your own personal use. It is provided " +
                "as it is, with no promise that it will always be available or free of errors. It needs an internet " +
                "connection and a working Gemini API key, which you pay for or use under Google's terms. Questions: " +
                "contact@nandivex.in."
        )
    )

    const val CHECKBOX_LABEL = "I have read this and I agree"
}
