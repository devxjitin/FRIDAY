package com.nandivex.friday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.data.LicenseKey
import com.nandivex.friday.data.LicenseStatus
import com.nandivex.friday.ui.components.AppIconTile
import com.nandivex.friday.ui.theme.AppButtonShape
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantAccentFill
import com.nandivex.friday.ui.theme.AssistantBackground
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantFieldBackground
import com.nandivex.friday.ui.theme.AssistantOutline
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import com.nandivex.friday.viewmodel.AccessState

/** The activated key as the Profile page shows it. */
data class LicenseSummary(val key: String, val status: LicenseStatus) {
    val maskedKey: String get() = LicenseKey.masked(key)
}

/** Shown while the key is being checked, so the assistant never flashes before the lock. */
@Composable
fun AccessCheckingScreen() {
    AccessFrame {
        CircularProgressIndicator(color = AssistantAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ActivationScreen(state: AccessState.NeedsKey, storeUrl: String, onActivate: (String) -> Unit, onOpenStore: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    AccessFrame {
        Heading("Activate F.R.I.D.A.Y.", "Enter your activation key. It works on one phone at a time.")
        Spacer(Modifier.height(24.dp))
        TextField(
            value = input,
            onValueChange = { input = it.take(40) },
            placeholder = { Text("ULTR-XXXX-XXXX-XXXX-XXXX", color = AssistantTextMuted) },
            singleLine = true,
            enabled = !state.busy,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) onActivate(input) }),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AssistantFieldBackground,
                unfocusedContainerColor = AssistantFieldBackground,
                disabledContainerColor = AssistantFieldBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = AssistantAccent,
                focusedTextColor = AssistantTextPrimary,
                unfocusedTextColor = AssistantTextPrimary,
                disabledTextColor = AssistantTextSecondary
            ),
            modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, AssistantOutline), RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = if (state.busy) "Activating…" else "Activate",
            enabled = !state.busy && input.isNotBlank(),
            onClick = { onActivate(input) }
        )
        state.error?.let { ErrorLine(it) }
        if (storeUrl.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onOpenStore) { Text("Don't have a key? Buy one on the store", color = AssistantAccent) }
        }
    }
}

/**
 * Shown on every open until FRIDAY's accessibility service is on. FRIDAY is a hands-free phone control app, so the home
 * screen stays closed without it. This is also the prominent disclosure Google Play asks for: it says what the service
 * reads and does, and the user agrees with a button before Android's own settings screen opens.
 */
@Composable
fun PhoneControlScreen(onAgree: () -> Unit, onOpenAppInfo: () -> Unit = {}) {
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
        // An app installed from a file or the web (not from a store) has its accessibility switch locked on Android 13 and newer.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            Spacer(Modifier.height(22.dp))
            DisclosureLine(
                "If the switch is greyed out",
                "Android locks it for apps that were not installed from a store. Tap Open app info, then the three dots at the top right, " +
                    "then Allow restricted settings. Come back here and turn F.R.I.D.A.Y. on."
            )
            TextButton(onClick = onOpenAppInfo) { Text("Open app info", color = AssistantAccent) }
        }
    }
}

@Composable
private fun DisclosureLine(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(text, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun AccessFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(AssistantBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
private fun Heading(title: String, subtitle: String) {
    AppIconTile(56.dp)
    Spacer(Modifier.height(18.dp))
    Text(title, color = AssistantTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = AppButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = com.nandivex.friday.ui.theme.AssistantButtonFill, contentColor = com.nandivex.friday.ui.theme.AssistantOnButtonFill),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
    ) { Text(text) }
}

@Composable
private fun ErrorLine(text: String) {
    Spacer(Modifier.height(14.dp))
    Text(text, color = AssistantError, fontSize = 13.sp, textAlign = TextAlign.Center)
}
