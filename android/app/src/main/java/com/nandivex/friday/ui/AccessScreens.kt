package com.nandivex.friday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.ui.components.AppIconTile
import com.nandivex.friday.ui.theme.AppButtonShape
import com.nandivex.friday.ui.theme.AssistantBackground
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/**
 * Shown on every open until FRIDAY's accessibility service is on. FRIDAY is a hands-free phone control app, so the home
 * screen stays closed without it. This is also the prominent disclosure Google Play asks for: it says what the service
 * reads and does, and the user agrees with a button before Android's own settings screen opens.
 */
@Composable
fun PhoneControlScreen(onAgree: () -> Unit) {
    AccessFrame {
        Heading(
            "Turn on phone control",
            "F.R.I.D.A.Y. runs your phone by voice. For that it needs Android's accessibility permission."
        )
        Spacer(Modifier.height(22.dp))
        DisclosureLine("What it does", "When you ask by voice, it reads what is on the screen and taps, types, scrolls, swipes and presses Back, Home or Recents for you.")
        DisclosureLine("What is shared", "The screen text it reads is sent to Google Gemini only to carry out your request. It is not stored on our servers or sold.")
        DisclosureLine("Your choice", "You can turn it off at any time in Settings, Accessibility, F.R.I.D.A.Y.")
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = "Agree and turn on", enabled = true, onClick = onAgree)
        Spacer(Modifier.height(10.dp))
        Text(
            "On the next screen, open F.R.I.D.A.Y. and switch it on. Then come back here.",
            color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun DisclosureLine(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(text, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
internal fun AccessFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(AssistantBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
internal fun Heading(title: String, subtitle: String) {
    AppIconTile(56.dp)
    Spacer(Modifier.height(18.dp))
    Text(title, color = AssistantTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
}

@Composable
internal fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = AppButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = com.nandivex.friday.ui.theme.AssistantButtonFill, contentColor = com.nandivex.friday.ui.theme.AssistantOnButtonFill),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
    ) { Text(text) }
}
