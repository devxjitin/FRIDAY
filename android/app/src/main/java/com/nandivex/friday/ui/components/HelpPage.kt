package com.nandivex.friday.ui.components

import androidx.compose.runtime.getValue
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Email
import androidx.compose.ui.platform.LocalContext
import com.nandivex.friday.BuildConfig
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.ui.theme.AssistantSurface
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/** Where support emails go, from the Contact card and the About page. */
const val DEVELOPER_EMAIL = "contact@nandivex.in"

/** "Pixel 9, Android 16 (API 36)": the phone details a bug report and a support email carry. */
fun deviceSummary(): String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}, " +
    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

/** One question and its answer on the Help page. */
private class Faq(val question: String, val answer: String)

private val Faqs = listOf(
    Faq(
        "Why does F.R.I.D.A.Y. need accessibility access?",
        "Android only lets an app read the screen and tap, type or scroll for you through its accessibility service. " +
            "F.R.I.D.A.Y. uses it to do what you ask by voice. Turn it on in Settings, Accessibility. You can turn it off there at any time, and F.R.I.D.A.Y. stops controlling the phone."
    ),
    Faq(
        "What is sent to Google Gemini?",
        "What you say, and the replies. When a request needs the screen, the text on it, and sometimes a picture of it, is sent too. " +
            "That text can include private content such as messages. Nothing goes through Nandivex servers. " +
            "Google handles it under the terms of your Gemini API plan."
    ),
    Faq(
        "Which Gemini key do I need?",
        "Your own key from Google AI Studio, on a Google project with billing turned on. Google's terms do not allow personal information on the unpaid tier, " +
            "and F.R.I.D.A.Y. can send screen content. The app cannot check whether billing is on; keeping it on is your responsibility."
    ),
    Faq(
        "It does not hear me. What can I do?",
        "Check that the microphone permission is on for F.R.I.D.A.Y., that no other app is using the microphone, and that you have an internet connection. " +
            "Then tap the microphone button and try again."
    ),
    Faq(
        "How do I stop F.R.I.D.A.Y.?",
        "Tap the microphone button to end the session. To stop it controlling the phone altogether, turn off its accessibility service in Android settings."
    ),
    Faq(
        "How do I delete what it saved?",
        "Open Storage and clear conversations or memories, or use the History and Memory pages. Removing the Gemini key is in Settings. " +
            "Uninstalling the app removes everything it stored on the phone."
    )
)

/**
 * Help and support: search the common questions, email support, report a problem or suggest a feature.
 * [onRequest] gets "bug" or "feature" and opens that form.
 */
@Composable
fun HelpPage(onRequest: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<Int?>(null) }
    val shown = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) Faqs.indices.toList()
        else Faqs.indices.filter { Faqs[it].question.contains(q, ignoreCase = true) || Faqs[it].answer.contains(q, ignoreCase = true) }
    }
    PageColumn(modifier) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Search help", color = AssistantTextMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AssistantTextMuted) },
            shape = RoundedCornerShape(28.dp), colors = assistantTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        PageLabel("Common questions")
        if (shown.isEmpty()) {
            Text("No question matches. Write to us below and we will help.", color = AssistantTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(4.dp))
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AssistantSurface)) {
                shown.forEachIndexed { n, i ->
                    val expanded = open == i || query.isNotBlank()
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable(role = Role.Button) { open = if (open == i) null else i }
                            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Faqs[i].question, color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null, tint = AssistantTextMuted
                            )
                        }
                        if (expanded) Text(Faqs[i].answer, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (n < shown.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(com.nandivex.friday.ui.theme.AssistantOutline))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        PageLabel("Contact us")
        HelpAction(
            Icons.Outlined.Email, "Email support",
            "Write to $DEVELOPER_EMAIL. Your app version and phone model are added to the message, and you can change it before sending."
        ) { emailDeveloper(context) }
        Spacer(Modifier.height(12.dp))
        HelpAction(
            Icons.Outlined.BugReport, "Report a problem",
            "Something broke or works the wrong way? Tell us what happened."
        ) { onRequest("bug") }
        Spacer(Modifier.height(12.dp))
        HelpAction(
            Icons.Outlined.Extension, "Suggest a feature",
            "Tell us what you would like F.R.I.D.A.Y. to do. It opens your email app with the message filled in."
        ) { onRequest("feature") }
    }
}

/** A card that shrinks a little and lights its border while it is pressed. */
@Composable
private fun HelpAction(icon: ImageVector, title: String, text: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "helpScale")
    val border by animateColorAsState(if (pressed) AssistantTextMuted else Color.Transparent, label = "helpBorder")
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().scale(scale).clip(shape).background(AssistantSurface).border(BorderStroke(1.dp, border), shape)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(com.nandivex.friday.ui.theme.AssistantSurfaceElevated), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = AssistantTextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text, color = AssistantTextSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.size(8.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AssistantTextMuted)
    }
}

/** Opens the phone's email app with a support email to the developer; copies the address when there is no email app. */
private fun emailDeveloper(context: Context) {
    val subject = "F.R.I.D.A.Y. support (v${BuildConfig.VERSION_NAME})"
    val body = "\n\n\n---\nApp version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nPhone: ${deviceSummary()}"
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Email", DEVELOPER_EMAIL))
        Toast.makeText(context, "No email app found. Address copied: $DEVELOPER_EMAIL", Toast.LENGTH_LONG).show()
    }
}
