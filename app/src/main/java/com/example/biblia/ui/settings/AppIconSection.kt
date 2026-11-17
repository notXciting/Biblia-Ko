package com.example.biblia.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.AppIcon

/** Launcher icon picker: three previews; the chosen one gets an accent ring. */
@Composable
fun AppIconSection(current: AppIcon, onSelect: (AppIcon) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_app_icon))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppIcon.entries.forEach { icon ->
                IconTile(icon, selected = icon == current, onClick = { onSelect(icon) }, Modifier.weight(1f))
            }
        }
        Text(
            text = stringResource(R.string.settings_app_icon_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun IconTile(icon: AppIcon, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, label = "iconRing")
    val shape = RoundedCornerShape(24)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Image(
            painter = painterResource(previewOf(icon)),
            contentDescription = stringResource(labelOf(icon)),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(1f)
                .border(BorderStroke(ring, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent), shape)
                .padding(5.dp)
                .clip(shape)
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(labelOf(icon)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun previewOf(icon: AppIcon): Int = when (icon) {
    AppIcon.SUNRISE -> R.drawable.icon_preview_sunrise
    AppIcon.NIGHT -> R.drawable.icon_preview_dark
    AppIcon.SKY -> R.drawable.icon_preview_sky
}

private fun labelOf(icon: AppIcon): Int = when (icon) {
    AppIcon.SUNRISE -> R.string.app_icon_sunrise
    AppIcon.NIGHT -> R.string.app_icon_night
    AppIcon.SKY -> R.string.app_icon_sky
}
