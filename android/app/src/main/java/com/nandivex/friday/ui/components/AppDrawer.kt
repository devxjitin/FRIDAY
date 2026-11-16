package com.nandivex.friday.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandivex.friday.ui.theme.AssistantAccent
import com.nandivex.friday.ui.theme.AssistantOutline
import com.nandivex.friday.ui.theme.AssistantSelectedBg
import com.nandivex.friday.ui.theme.AssistantSurface
import com.nandivex.friday.ui.theme.AssistantSurfaceElevated
import com.nandivex.friday.ui.theme.AssistantTextMuted
import com.nandivex.friday.ui.theme.AssistantTextPrimary
import com.nandivex.friday.ui.theme.AssistantTextSecondary

/** Pages reachable from the left menu. */
enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", AssistantIcons.Home),
    ABILITIES("What it can do", Icons.Outlined.Bolt),
    HISTORY("History", Icons.Outlined.History),
    MEMORY("Memory", Icons.Outlined.Psychology),
    PROFILE("Profile", Icons.Outlined.AccountCircle),
    SETTINGS("Settings", AssistantIcons.Settings),
    USAGE("Usage", Icons.Outlined.Insights),
    STORAGE("Storage", Icons.Outlined.Inventory2),
    DEVELOPER("Developer", Icons.Outlined.Code),
    HELP("Help and support", Icons.AutoMirrored.Outlined.HelpOutline),
    ABOUT("About", Icons.Outlined.Info)
}

/** Height of the slim bar at the top of every page; pages reserve this much space under it. */
val AppTopBarHeight = 56.dp

private val RowHeight = 48.dp

/** The menu in reading order: the basics, then your data, your account, the app itself, and help. */
private val DrawerGroups: List<Pair<String?, List<NavTab>>> = listOf(
    null to listOf(NavTab.HOME, NavTab.ABILITIES, NavTab.HISTORY, NavTab.MEMORY),
    "Account" to listOf(NavTab.PROFILE),
    "App" to listOf(NavTab.SETTINGS, NavTab.USAGE, NavTab.STORAGE),
    "Support" to listOf(NavTab.HELP, NavTab.ABOUT)
)

@Composable
fun AppDrawerContent(
    selected: NavTab,
    /** The name set at setup; blank shows only the app name. */
    userName: String,
    appVersion: String,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    Column(modifier.statusBarsPadding().navigationBarsPadding()) {
        DrawerHeader(userName, onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            DrawerGroups.forEach { (title, tabs) ->
                if (title != null) NavSection(title)
                tabs.forEach { tab -> NavRow(tab, tab == selected) { onSelect(tab) } }
            }
            Spacer(Modifier.height(16.dp))
        }
        DrawerFooter(appVersion)
    }
}

@Composable
private fun NavSection(title: String) {
    Text(
        text = title.uppercase(),
        color = AssistantTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun NavRow(tab: NavTab, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val tint = if (selected) AssistantAccent else AssistantTextPrimary
    Row(
        Modifier.fillMaxWidth().height(RowHeight).clip(shape)
            .then(if (selected) Modifier.background(AssistantSelectedBg, shape) else Modifier)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
        Spacer(Modifier.size(16.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** The app mark with the user's name and the app name, and a close button where the menu is a sheet. */
@Composable
private fun DrawerHeader(userName: String, onClose: (() -> Unit)?) {
    val name = userName.trim()
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIconTile(48.dp)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (name.isEmpty()) com.nandivex.friday.data.DISPLAY_NAME else name,
                color = AssistantTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (name.isNotEmpty()) {
                Text(com.nandivex.friday.data.DISPLAY_NAME, color = AssistantTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close menu", tint = AssistantTextSecondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Pinned at the bottom: the app version and who makes it. */
@Composable
private fun DrawerFooter(appVersion: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AssistantOutline))
        Spacer(Modifier.height(12.dp))
        Text("Version $appVersion", color = AssistantTextSecondary, fontSize = 13.sp)
        Text("from Nandivex Technologies", color = AssistantTextSecondary, fontSize = 13.sp)
    }
}

/** Three thin lines of different length, left aligned: the menu button used on every page. */
@Composable
private fun MenuLinesIcon(tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.size(24.dp)) {
        val stroke = 1.7.dp.toPx()
        val left = 3.dp.toPx()
        val lines = listOf(6.dp.toPx() to 18.dp.toPx(), 12.dp.toPx() to 12.dp.toPx(), 18.dp.toPx() to 7.dp.toPx())
        lines.forEach { (y, length) ->
            drawLine(tint, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(left + length, y),
                strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/** Slim bar above every page: a thin menu button on the left, the page title in a serif in the middle, the theme button on the right. */
@Composable
fun AppTopBar(
    title: String,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppTopBarHeight,
    /** When set, a day/night button is shown at the right end of the bar. */
    onToggleTheme: (() -> Unit)? = null,
    isDarkTheme: Boolean = false
) {
    Box(modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp)) {
        Text(
            title,
            color = AssistantTextPrimary,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp)
        )
        if (showMenuButton) {
            IconButton(onClick = onMenuClick, modifier = Modifier.align(Alignment.CenterStart)) {
                MenuLinesIcon(AssistantTextPrimary)
            }
        }
        if (onToggleTheme != null) {
            IconButton(onClick = onToggleTheme, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to day theme" else "Switch to night theme",
                    tint = AssistantTextPrimary, modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Drawer surface colour, shared by the modal and permanent variants. */
val AppDrawerContainerColor: Color get() = com.nandivex.friday.ui.theme.AssistantBackground
