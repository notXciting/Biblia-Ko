package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.BuildConfig
import com.example.biblia.R
import com.example.biblia.domain.model.AppLanguage
import com.example.biblia.domain.model.AppSettings
import kotlin.math.roundToInt

/**
 * Settings menu: one row per [SettingsPage] with its icon and a one-line
 * summary of the current values. Each row opens its own short page.
 */
@Composable
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit, onOpenPage: (SettingsPage) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        SettingsTopBar(stringResource(R.string.settings_title), onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            SettingsPage.entries.forEach { page ->
                MenuRow(page, summaryOf(page, settings), onClick = { onOpenPage(page) })
            }
        }
    }
}

/** Back arrow + title, shared by the menu and every sub-page. */
@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MenuRow(page: SettingsPage, summary: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).background(colors.primaryContainer, CircleShape),
            ) {
                Icon(page.icon, contentDescription = null, tint = colors.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(page.title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun summaryOf(page: SettingsPage, s: AppSettings): String = when (page) {
    SettingsPage.READING ->
        readingFontName(s.readingFont) + " · " + stringResource(R.string.settings_font_value, s.fontSizeSp.roundToInt())
    SettingsPage.APPEARANCE ->
        stringResource(themeLabel(s.themeMode)) + " · " + stringResource(accentLabel(s.accent))
    SettingsPage.LISTEN -> stringResource(R.string.settings_listen_summary)
    SettingsPage.APP -> when (s.appLanguage) {
        AppLanguage.FOLLOW_VERSION -> stringResource(R.string.language_follow_version)
        AppLanguage.TAGALOG -> "Tagalog"
        AppLanguage.ENGLISH -> "English"
    } + " · " + (s.uiScale * 100).roundToInt() + "%"
    SettingsPage.BACKUP -> stringResource(R.string.settings_backup_summary)
    SettingsPage.ABOUT -> stringResource(R.string.settings_app_version) + " " + BuildConfig.VERSION_NAME
}
