package com.example.biblia.ui.bibles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.R
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.importer.ImportState
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.versions.VersionRules
import com.example.biblia.ui.KeepAppLocale
import com.example.biblia.ui.listen.languageLabel

/**
 * "Mga Biblia" (Manage Bibles): every version by language, with Load / Unload.
 * Loading keeps going when the user leaves; switching Bibles stays in the
 * reader's quick picker.
 */
@Composable
fun ManageBiblesScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit) {
    val vm: ManageBiblesViewModel = viewModel(factory = viewModelFactory { initializer { ManageBiblesViewModel(container) } })
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val states by vm.importStates.collectAsStateWithLifecycle()
    val loadedSet = loaded.orEmpty()
    val importing = states.filterValues { it.isRunning }.keys
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Column {
                Text(stringResource(R.string.bibles_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (loaded != null) {
                    Text(
                        text = stringResource(R.string.bibles_summary, loadedSet.size, loadedSet.sumOf { vm.sizes[it] ?: 0 }),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (loaded == null) return@Column
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.weight(1f).navigationBarsPadding()) {
            vm.sections.forEach { (language, versions) ->
                item(key = "header-$language") {
                    Text(
                        text = languageLabel(language),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(versions, key = { it.id }) { version ->
                    val state = states[version.id] ?: ImportState(version.id)
                    BibleRow(
                        version = version,
                        sizeMb = vm.sizes[version.id],
                        state = state,
                        action = actionFor(version, settings.versionId, loadedSet, state, vm.unloading),
                        canUnload = VersionRules.canUnload(version.id, settings.versionId, loadedSet, importing),
                        onLoad = { vm.load(version) },
                        onUnload = { vm.askUnload(version) },
                    )
                }
            }
        }
    }
    vm.confirmUnload?.let { version ->
        UnloadDialog(version, vm.sizes[version.id], onConfirm = vm::unload, onDismiss = vm::cancelUnload)
    }
}

private fun actionFor(v: BibleVersion, currentId: String, loaded: Set<String>, state: ImportState, unloading: String?) = when {
    unloading == v.id -> BibleRowAction.UNLOADING
    state.isRunning -> BibleRowAction.PROGRESS
    v.id == currentId -> BibleRowAction.IN_USE
    v.id in loaded -> BibleRowAction.UNLOAD
    else -> BibleRowAction.LOAD
}

@Composable
private fun UnloadDialog(version: BibleVersion, sizeMb: Int?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { KeepAppLocale { Text(stringResource(R.string.bibles_unload_title, version.shortName)) } },
        text = { KeepAppLocale { Text(stringResource(R.string.bibles_unload_body, sizeMb ?: 8)) } },
        confirmButton = { KeepAppLocale { TextButton(onClick = onConfirm) { Text(stringResource(R.string.bibles_unload)) } } },
        dismissButton = { KeepAppLocale { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } } },
    )
}
