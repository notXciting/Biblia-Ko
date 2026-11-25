package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Third-party material in the app: name, copyright line, and the license text from assets. */
private class LicenseItem(val name: String, val notice: String, val asset: String?)

private const val OFL = "SIL Open Font License 1.1"

private val FONTS = listOf(
    LicenseItem("Merriweather", "Copyright 2020 The Merriweather Project Authors · $OFL", "licenses/merriweather_ofl.txt"),
    LicenseItem("Lora", "Copyright 2011 The Lora Project Authors · $OFL", "licenses/lora_ofl.txt"),
    LicenseItem("EB Garamond", "Copyright 2017 The EB Garamond Project Authors · $OFL", "licenses/ebgaramond_ofl.txt"),
    LicenseItem("Noto Sans", "Copyright 2022 The Noto Project Authors · $OFL", "licenses/notosans_ofl.txt"),
    LicenseItem("Atkinson Hyperlegible", "Copyright 2020 Braille Institute of America, Inc. · $OFL", "licenses/atkinson_ofl.txt"),
    LicenseItem("OpenDyslexic", "Copyright (c) 2019 Abbie Gonzalez · $OFL", "licenses/opendyslexic_ofl.txt"),
)

/** Settings > About > "Mga open-source na lisensya". */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val crossRefs = LicenseItem("OpenBible.info Cross References", stringResource(R.string.about_crossrefs_credit), null)
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.licenses_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
            item { SectionTitle(stringResource(R.string.licenses_fonts)) }
            items(FONTS, key = { it.name }) { LicenseCard(it) }
            item { SectionTitle(stringResource(R.string.licenses_data), Modifier.padding(top = 8.dp)) }
            item { LicenseCard(crossRefs) }
        }
    }
}

@Composable
private fun LicenseCard(item: LicenseItem) {
    var open by rememberSaveable(item.name) { mutableStateOf(false) }
    val context = LocalContext.current
    val text by produceState<String?>(null, open) {
        if (open && item.asset != null && value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching { context.assets.open(item.asset).bufferedReader().use { it.readText() } }.getOrNull()
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (item.asset == null) 14.dp else 4.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(item.notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.asset != null) {
                if (open) {
                    Text(
                        text = text.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                TextButton(onClick = { open = !open }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(stringResource(if (open) R.string.licenses_hide else R.string.licenses_show))
                }
            }
        }
    }
}
