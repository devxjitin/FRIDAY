package com.nandivex.friday.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nandivex.friday.BuildConfig
import com.nandivex.friday.data.AccountProfile
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantButtonFill
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantOnButtonFill
import com.nandivex.friday.ui.theme.AssistantSuccess
import com.nandivex.friday.ui.theme.AssistantSurfaceElevated
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import com.nandivex.friday.viewmodel.AccountViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Profile: the account (Google sign-in, name, year of birth, verified email) and this phone. Signed out, it offers
 * sign-in. Signed in without a profile, it asks for the name and year of birth. Otherwise it shows the profile with
 * Edit, Sign out and Delete account.
 */
@Composable
fun ProfilePage(ownerName: String, modifier: Modifier = Modifier, accountViewModel: AccountViewModel = viewModel()) {
    val state by accountViewModel.state.collectAsState()
    val activity = LocalContext.current.findActivity()
    var confirmDelete by remember { mutableStateOf(false) }
    val profile = state.profile
    val shownName = (profile?.name ?: ownerName).trim().takeIf { it.isNotEmpty() }

    PageColumn(modifier) {
        PageCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(shownName)
                Spacer(Modifier.height(12.dp))
                Text(shownName ?: "Your profile", color = AssistantTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center)
                state.email?.let { Text(it, color = AssistantTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)) }
            }
        }

        when {
            !state.available -> {
                PageLabel("Account")
                PageCard { Text("Sign-in is not available in this build.", color = AssistantTextSecondary, fontSize = 14.sp) }
            }
            !state.signedIn -> {
                PageLabel("Account")
                PageCard {
                    Text("Sign in with Google", color = AssistantTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Your Google account confirms your email. F.R.I.D.A.Y. then saves your name, year of birth and email in your own " +
                            "account, which only you can read. Your Gemini key, memories and conversations stay on this phone.",
                        color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    FilledAction("Sign in with Google", enabled = !state.busy && activity != null) { activity?.let(accountViewModel::signIn) }
                    ErrorLine(state.error)
                }
            }
            state.loadingProfile -> {
                PageCard { Text("Loading your profile…", color = AssistantTextSecondary, fontSize = 14.sp) }
            }
            state.needsProfile || state.editing -> {
                PageLabel(if (profile == null) "About you" else "Edit profile")
                ProfileForm(
                    initialName = profile?.name ?: ownerName,
                    initialYear = profile?.yearOfBirth?.toString().orEmpty(),
                    busy = state.busy, error = state.error,
                    onSave = accountViewModel::saveProfile,
                    onCancel = if (profile != null) accountViewModel::cancelEditing else null
                )
                if (profile == null) {
                    TextButton(onClick = accountViewModel::signOut, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Sign out", color = AssistantAccent)
                    }
                }
            }
            profile != null -> {
                PageLabel("Account")
                PageCard {
                    PageRow("Name", profile.name)
                    PageRow("Email", profile.email)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Email status", color = AssistantTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("Verified by Google", color = AssistantSuccess, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    accountViewModel.bandLabel()?.let { PageRow("Age range", it) }
                    memberSince(profile)?.let { PageRow("Member since", it) }
                }
                Spacer(Modifier.height(12.dp))
                ErrorLine(state.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedAction("Edit", Modifier.weight(1f), enabled = !state.busy, onClick = accountViewModel::startEditing)
                    OutlinedAction("Sign out", Modifier.weight(1f), enabled = !state.busy, onClick = accountViewModel::signOut)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { confirmDelete = true }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Delete account", color = AssistantError)
                }
            }
            else -> ErrorLine(state.error)
        }

        PageLabel("This phone")
        PageCard {
            PageRow("Device", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
            PageRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            PageRow("App version", BuildConfig.VERSION_NAME)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "Your name, year of birth and email are deleted from your F.R.I.D.A.Y. account and you are signed out. " +
                        "You will be asked to choose your Google account once more to confirm. This cannot be undone. " +
                        "Memories and conversations on this phone are not deleted; clear them in Storage."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; activity?.let(accountViewModel::deleteAccount) }) {
                    Text("Delete", color = AssistantError)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileForm(
    initialName: String,
    initialYear: String,
    busy: Boolean,
    error: String?,
    onSave: (String, String) -> Unit,
    onCancel: (() -> Unit)?
) {
    var name by remember(initialName) { mutableStateOf(initialName.takeUnless { it == "Boss" }.orEmpty()) }
    var year by remember(initialYear) { mutableStateOf(initialYear) }
    PageCard {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text("Name") },
            colors = assistantTextFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = year, onValueChange = { year = it.filter(Char::isDigit).take(4) }, singleLine = true, label = { Text("Year of birth") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = assistantTextFieldColors(), modifier = Modifier.fillMaxWidth()
        )
        Text(
            "F.R.I.D.A.Y. is for people aged 18 or older, because Google's Gemini terms require it. Your age range also sets how it speaks to you.",
            color = AssistantTextMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp)
        )
        ErrorLine(error)
        Spacer(Modifier.height(14.dp))
        FilledAction(if (busy) "Saving…" else "Save", enabled = !busy) { onSave(name, year) }
        if (onCancel != null) {
            TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun ErrorLine(error: String?) {
    if (error != null) Text(error, color = AssistantError, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun FilledAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AssistantButtonFill, contentColor = AssistantOnButtonFill)
    ) { Text(text) }
}

@Composable
private fun OutlinedAction(text: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(24.dp)) {
        Text(text, color = AssistantTextPrimary)
    }
}

private fun memberSince(profile: AccountProfile): String? = profile.memberSinceMillis?.let {
    DateTimeFormatter.ofPattern("MMMM yyyy").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
}

/** A round tile with the first letters of the name, or a plain person mark when there is no name. */
@Composable
private fun Avatar(name: String?) {
    val initials = name?.split(Regex("\\s+"))?.filter { it.isNotBlank() }?.take(2)
        ?.joinToString("") { it.first().uppercase() }.orEmpty()
    Box(Modifier.size(72.dp).clip(CircleShape).background(AssistantSurfaceElevated), contentAlignment = Alignment.Center) {
        if (initials.isNotEmpty()) {
            Text(initials, color = AssistantTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = AssistantTextSecondary, modifier = Modifier.size(34.dp))
        }
    }
}
