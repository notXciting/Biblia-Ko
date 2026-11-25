package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.BuildConfig
import com.example.biblia.R
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry

/** About: app, developer, then every Bible version with its copyright. */
@Composable
fun AboutSection(currentVersionId: String, onOpenLicenses: () -> Unit, modifier: Modifier = Modifier, showTitle: Boolean = true) {
    Column(modifier.fillMaxWidth()) {
        if (showTitle) SectionTitle(stringResource(R.string.settings_about))
        AboutCard {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            LabeledValue(stringResource(R.string.settings_app_version), BuildConfig.VERSION_NAME)
            Spacer(Modifier.height(10.dp))
            LabeledValue(stringResource(R.string.settings_developer), DEVELOPER)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_crossrefs_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenLicenses, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(stringResource(R.string.licenses_title))
            }
        }
        SectionTitle(stringResource(R.string.settings_bible_versions), Modifier.padding(top = 12.dp))
        AboutCard {
            BibleVersionRegistry.all.forEachIndexed { index, version ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                VersionInfo(version, inUse = version.id == currentVersionId)
            }
        }
    }
}

private const val DEVELOPER = "Jee."

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun VersionInfo(version: BibleVersion, inUse: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (inUse) colors.primary else colors.surfaceContainerHighest),
        ) {
            Text(
                text = version.shortName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (inUse) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = version.catalogName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Text(
                text = if (inUse) {
                    version.language + " · " + stringResource(R.string.settings_version_in_use)
                } else {
                    version.language
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (inUse) colors.primary else colors.onSurfaceVariant,
            )
            if (version.copyright.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = version.copyright,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
