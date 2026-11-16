package com.nandivex.friday.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nandivex.friday.data.Agreement
import com.nandivex.friday.data.AssistantOptions
import com.nandivex.friday.data.MAX_ASSISTANT_NAME_LENGTH
import com.nandivex.friday.data.SetupStep
import com.nandivex.friday.ui.components.SettingsDropdown
import com.nandivex.friday.ui.components.assistantTextFieldColors
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantSuccess
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import com.nandivex.friday.viewmodel.SetupUiState

private const val KEY_PAGE_URL = "https://aistudio.google.com/apikey"
private const val BILLING_PAGE_URL = "https://ai.google.dev/gemini-api/docs/billing"

/** Shown for the moment it takes to read the phone's saved answers. */
@Composable
fun SetupLoadingScreen() {
    AccessFrame {
        CircularProgressIndicator(color = AssistantAccent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

/** What the setup screens can ask the app to do. */
class SetupActions(
    val onWelcomeNext: () -> Unit,
    val onAgreementAccepted: () -> Unit,
    val onDecline: () -> Unit,
    val onNameChanged: (String) -> Unit,
    val onLanguageChanged: (String) -> Unit,
    val onAboutYouNext: () -> Unit,
    val onKeyEdited: () -> Unit,
    val onCheckAndSaveKey: (String) -> Unit,
    val onKeyNext: () -> Unit,
    val onVoiceChanged: (String) -> Unit,
    val onVoiceNext: () -> Unit,
    val onPermissionsNext: () -> Unit,
    val onOpenAppSettings: () -> Unit
)

/** One screen per setup step. Step 7, phone control, and the final screen are shown by the caller after these. */
@Composable
fun SetupScreen(state: SetupUiState, actions: SetupActions) {
    when (state.step) {
        SetupStep.WELCOME -> WelcomeStep(actions)
        SetupStep.AGREEMENT -> AgreementStep(actions)
        SetupStep.ABOUT_YOU -> AboutYouStep(state, actions)
        SetupStep.API_KEY -> ApiKeyStep(state, actions)
        SetupStep.VOICE -> VoiceStep(state, actions)
        SetupStep.PERMISSIONS -> PermissionsStep(actions)
        null -> Unit
    }
}

@Composable
private fun WelcomeStep(actions: SetupActions) {
    AccessFrame {
        Heading("Your phone, on voice command", "F.R.I.D.A.Y. lets you use your Android phone without touching it. Setup takes about two minutes.")
        Spacer(Modifier.height(20.dp))
        Text("By Nandivex Technologies", color = AssistantTextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Get started", enabled = true, onClick = actions.onWelcomeNext)
    }
}

@Composable
private fun AgreementStep(actions: SetupActions) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    AccessFrame {
        Heading(Agreement.TITLE, Agreement.INTRO)
        Spacer(Modifier.height(18.dp))
        Agreement.sections.forEach { DisclosureLine(it.title, it.body) }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreed, onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = AssistantAccent)
            )
            Spacer(Modifier.size(10.dp))
            Text(Agreement.CHECKBOX_LABEL, color = AssistantTextPrimary, fontSize = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton("Accept and continue", enabled = agreed, onClick = actions.onAgreementAccepted)
        TextButton(onClick = actions.onDecline, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Decline and close the app", color = AssistantTextSecondary)
        }
    }
}

@Composable
private fun AboutYouStep(state: SetupUiState, actions: SetupActions) {
    AccessFrame {
        Heading("About you", "What should F.R.I.D.A.Y. call you? It uses this name when it talks to you.")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.ownerName,
            onValueChange = { actions.onNameChanged(it.take(MAX_ASSISTANT_NAME_LENGTH)) },
            singleLine = true,
            label = { Text("Your name") },
            colors = assistantTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.onAboutYouNext() }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { actions.onNameChanged("Boss") }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Use \"Boss\" instead", color = AssistantAccent)
        }
        state.error?.let { ErrorText(it) }
        Spacer(Modifier.height(10.dp))
        Text("Language you speak", color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        SettingsDropdown(
            value = AssistantOptions.languageLabel(state.languageCode),
            options = AssistantOptions.languages.map { it.code to it.label },
            selectedId = state.languageCode,
            onSelect = actions.onLanguageChanged
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Next", enabled = state.ownerName.isNotBlank(), onClick = actions.onAboutYouNext)
    }
}

@Composable
private fun ApiKeyStep(state: SetupUiState, actions: SetupActions) {
    val context = LocalContext.current
    var key by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    AccessFrame {
        Heading(
            "Your Gemini API key",
            "F.R.I.D.A.Y. uses Google Gemini to understand you. You bring your own key, so your voice goes from your phone straight to Google."
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.trim().take(200); actions.onKeyEdited() },
            singleLine = true,
            label = { Text(if (state.hasApiKey) "Replace the saved key (optional)" else "Paste your API key") },
            colors = assistantTextFieldColors(),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (key.isNotBlank()) actions.onCheckAndSaveKey(key) }),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Hide key" else "Show key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KEY_PAGE_URL))) } },
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text("Get a key from Google AI Studio", color = AssistantAccent) }
        TextButton(
            onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BILLING_PAGE_URL))) } },
            modifier = Modifier.heightIn(min = 48.dp)
        ) { Text("How to turn on billing", color = AssistantAccent) }
        state.keyMessage?.let { if (state.keyMessageIsError) ErrorText(it) else SuccessText(it) }
        if (state.hasApiKey && state.keyMessage == null && key.isEmpty()) SuccessText("A key is already saved on this phone.")
        Spacer(Modifier.height(10.dp))
        if (key.isNotBlank()) {
            PrimaryButton(
                if (state.checkingKey) "Checking with Google…" else "Check and save key",
                enabled = !state.checkingKey, onClick = { actions.onCheckAndSaveKey(key) }
            )
        } else {
            PrimaryButton("Next", enabled = state.hasApiKey, onClick = actions.onKeyNext)
        }
        if (key.isNotBlank() && state.hasApiKey && state.keyMessage != null && !state.keyMessageIsError) {
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Next", enabled = true, onClick = actions.onKeyNext)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Use a key from a Google project with billing turned on. F.R.I.D.A.Y. can send what is on your screen, and Google's terms do not " +
                "allow personal information on the free tier. Google charges your project for use; Nandivex does not. The app cannot check " +
                "whether billing is on. The key is stored encrypted on this phone and used only to call Gemini.",
            color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VoiceStep(state: SetupUiState, actions: SetupActions) {
    AccessFrame {
        Heading("Choose a voice", "This is how F.R.I.D.A.Y. sounds. You can change it later in Settings, and you will hear it in your first conversation.")
        Spacer(Modifier.height(20.dp))
        SettingsDropdown(
            value = AssistantOptions.voices.firstOrNull { it.name == state.voiceName }
                ?.let { "${it.name}, ${it.gender}, ${it.style}" } ?: state.voiceName,
            options = AssistantOptions.voices.map { it.name to "${it.name}, ${it.gender}, ${it.style}" },
            selectedId = state.voiceName,
            onSelect = actions.onVoiceChanged
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Next", enabled = true, onClick = actions.onVoiceNext)
    }
}

@Composable
private fun PermissionsStep(actions: SetupActions) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    // Read the live state on every recomposition; the counter and the resume hook below trigger one.
    @Suppress("UNUSED_EXPRESSION") refresh
    val micOn = granted(Manifest.permission.RECORD_AUDIO)
    val notificationsNeeded = Build.VERSION.SDK_INT >= 33
    val notificationsOn = !notificationsNeeded || granted("android.permission.POST_NOTIFICATIONS")

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AccessFrame {
        Heading("Two permissions", "F.R.I.D.A.Y. needs the microphone to hear you. Notifications are optional.")
        Spacer(Modifier.height(20.dp))
        PermissionRow(
            title = "Microphone",
            why = "So F.R.I.D.A.Y. can hear what you say while a session is open. Required.",
            on = micOn,
            onAllow = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
        if (notificationsNeeded) {
            PermissionRow(
                title = "Notifications",
                why = "Shows the small \"F.R.I.D.A.Y. is listening\" notice while a session is open. Optional.",
                on = notificationsOn,
                onAllow = { notificationLauncher.launch("android.permission.POST_NOTIFICATIONS") }
            )
        }
        if (!micOn) {
            Text(
                "If Android no longer shows the question, allow the microphone in the app's settings.",
                color = AssistantTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
            )
            TextButton(onClick = actions.onOpenAppSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Open app settings", color = AssistantAccent)
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Next", enabled = micOn, onClick = actions.onPermissionsNext)
    }
}

@Composable
private fun PermissionRow(title: String, why: String, on: Boolean, onAllow: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(title, color = AssistantTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (on) Text("Allowed", color = AssistantSuccess, fontSize = 14.sp)
            else TextButton(onClick = onAllow, modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow", color = AssistantAccent) }
        }
        Text(why, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** The last screen, shown once after phone control is on. */
@Composable
fun SetupDoneScreen(onStart: () -> Unit) {
    AccessFrame {
        Heading("You are all set", "Tap the microphone and talk. For example:")
        Spacer(Modifier.height(18.dp))
        listOf("\"Turn on the torch.\"", "\"Turn Wi-Fi off.\"", "\"Open the Clock app.\"", "\"Set the volume to half.\"").forEach {
            Text(it, color = AssistantTextPrimary, fontSize = 17.sp, modifier = Modifier.padding(vertical = 5.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Anything on your screen can be read and tapped, so say clearly what you want and check the result.",
            color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Start using F.R.I.D.A.Y.", enabled = true, onClick = onStart)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = AssistantError, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SuccessText(text: String) {
    Text(text, color = AssistantSuccess, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp))
}
