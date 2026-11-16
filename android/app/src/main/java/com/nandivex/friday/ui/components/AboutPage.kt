package com.nandivex.friday.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.nandivex.friday.data.Changelog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.BuildConfig
import com.nandivex.friday.ui.theme.AppButtonShape
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantAccentFill
import com.nandivex.friday.ui.theme.AssistantAccentAlt
import com.nandivex.friday.ui.theme.AssistantError
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary
import java.text.DateFormat
import java.util.Date

/** About: what this app is and which build is installed. */
@Composable
fun AboutPage(
    assistantName: String,
    modifier: Modifier = Modifier
) {
    PageColumn(modifier) {
        PageCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIconTile(72.dp)
                Spacer(Modifier.height(14.dp))
                Text(assistantName, color = AssistantTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Your personal voice assistant", color = AssistantTextSecondary, fontSize = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "Version ${BuildConfig.VERSION_NAME}", color = AssistantTextMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp).combinedClickable(
                        onClick = {},
                        // Test builds only: a long press on the version crashes the app so the Crashlytics report can be checked.
                        onLongClick = { if (BuildConfig.BUILD_TYPE != "release") throw RuntimeException("Crashlytics test crash") }
                    )
                )
            }
        }

        PageLabel("This build")
        PageCard {
            PageRow("Version", BuildConfig.VERSION_NAME)
            PageRow("Build", BuildConfig.VERSION_CODE.toString())
            PageRow("Package", BuildConfig.APPLICATION_ID)
            PageRow("Device", Build.MODEL)
            PageRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }

        WhatsNew()

        PageLabel("Developer")
        PageCard {
            Text("Developed by Nandivex Technologies", color = AssistantTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("contact@nandivex.in", color = AssistantTextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }

        PageLabel("Privacy")
        PageCard {
            Text(
                "Your memories and settings stay on this phone. What you say, and the screen text or picture F.R.I.D.A.Y. reads to carry out your request, goes to Google Gemini with your own API key.",
                color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 21.sp
            )
        }
    }
}

/** What changed in the newest version only, read from the changelog shipped inside the app. */
@Composable
private fun WhatsNew() {
    val context = LocalContext.current
    val latest = remember {
        runCatching { Changelog.parse(context.assets.open("changelog.md").bufferedReader().use { it.readText() }) }
            .getOrDefault(emptyList()).firstOrNull()
    }
    latest?.let { entry ->
        PageLabel("What's new")
        PageCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Version ${entry.version}", color = AssistantTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (entry.date.isNotBlank()) {
                    Text(entry.date, color = AssistantTextMuted, fontSize = 13.sp)
                }
            }
            entry.sections.forEach { section ->
                PageLabel(section.title, color = AssistantAccent, top = 14.dp, bottom = 2.dp)
                // Each change as its own bullet, so long lines don't run into each other.
                section.items.forEach { item ->
                    Row(Modifier.padding(top = 6.dp)) {
                        Text("•", color = AssistantTextMuted, fontSize = 14.sp, lineHeight = 20.sp,
                            modifier = Modifier.padding(end = 10.dp))
                        Text(item, color = AssistantTextSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, spinner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (spinner) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AssistantAccent)
        Text(text, color = AssistantTextPrimary, fontSize = 15.sp)
    }
}
