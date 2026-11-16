package com.nandivex.friday.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.BuildConfig
import com.nandivex.friday.ui.theme.AssistantSurfaceElevated
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/** Profile: the name FRIDAY uses for you (set in Settings) and this phone. There is no account or activation key. */
@Composable
fun ProfilePage(ownerName: String, modifier: Modifier = Modifier) {
    val name = ownerName.trim().takeIf { it.isNotEmpty() }
    PageColumn(modifier) {
        PageCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(name)
                Spacer(Modifier.height(12.dp))
                Text(name ?: "Your profile", color = AssistantTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center)
                if (name == null) {
                    Text("Add your name in Settings.", color = AssistantTextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        PageLabel("This phone")
        PageCard {
            PageRow("Device", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
            PageRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            PageRow("App version", BuildConfig.VERSION_NAME)
        }
    }
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
