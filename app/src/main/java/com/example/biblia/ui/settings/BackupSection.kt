package com.example.biblia.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.R
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.backup.BackupData
import com.example.biblia.ui.KeepAppLocale
import com.example.biblia.ui.reader.formatNoteDate

/**
 * "Backup at paglipat": save settings, highlights and notes to a file the user
 * picks (Downloads, Drive, ...), and import such a file on another phone.
 */
@Composable
fun BackupSection(container: AppContainer) {
    val vm: BackupViewModel = viewModel(factory = viewModelFactory { initializer { BackupViewModel(container) } })
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        vm.export(it)
    }
    // Some file managers label .json as text or binary: accept those too.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { vm.read(it) }

    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.backup_title))
        Text(
            text = stringResource(R.string.backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            BackupButton(Icons.Outlined.FileUpload, stringResource(R.string.backup_export), !vm.busy, filled = true,
                modifier = Modifier.weight(1f)) { exportLauncher.launch(vm.suggestedFileName()) }
            BackupButton(Icons.Outlined.FileDownload, stringResource(R.string.backup_import), !vm.busy, filled = false,
                modifier = Modifier.weight(1f)) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
        }
    }

    vm.pendingImport?.let { data -> ImportConfirmDialog(data, onConfirm = vm::confirmImport, onDismiss = vm::cancelImport) }

    val message = vm.message
    val text = message?.let { backupMessageText(it) }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }
}

@Composable
private fun BackupButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    filled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
    if (filled) FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    else OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
}

/** What the file contains, and that importing only adds (nothing is deleted). */
@Composable
private fun ImportConfirmDialog(data: BackupData, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null) },
        title = { KeepAppLocale { Text(stringResource(R.string.backup_import_title)) } },
        text = {
            KeepAppLocale {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Only mention settings when the file has them (they are optional in the format).
                    Text(
                        stringResource(
                            if (data.settings != null) R.string.backup_import_contents else R.string.backup_import_contents_no_settings,
                            data.highlights.size, data.notes.size,
                        ),
                    )
                    if (data.exportedAt > 0) {
                        Text(
                            stringResource(R.string.backup_import_date, formatNoteDate(data.exportedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(
                            if (data.settings != null) R.string.backup_import_merge_note else R.string.backup_import_merge_note_no_settings,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            KeepAppLocale { TextButton(onClick = onConfirm) { Text(stringResource(R.string.backup_import_confirm)) } }
        },
        dismissButton = {
            KeepAppLocale { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        },
    )
}

@Composable
private fun backupMessageText(message: BackupMessage): String = when (message) {
    is BackupMessage.Exported -> stringResource(R.string.backup_exported, message.highlights, message.notes)
    is BackupMessage.Imported -> stringResource(
        if (message.result.settingsApplied) R.string.backup_imported else R.string.backup_imported_no_settings,
        message.result.highlightsAdded, message.result.notesAdded,
    )
    BackupMessage.NotABackup -> stringResource(R.string.backup_not_a_backup)
    BackupMessage.Failed -> stringResource(R.string.backup_failed)
}
