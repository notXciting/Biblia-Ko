package com.example.biblia.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.backup.BackupData
import com.example.biblia.domain.backup.BackupFormatException
import com.example.biblia.domain.backup.ImportResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One-shot outcome shown as a message, then cleared. */
sealed interface BackupMessage {
    data class Exported(val highlights: Int, val notes: Int) : BackupMessage
    data class Imported(val result: ImportResult) : BackupMessage
    data object NotABackup : BackupMessage
    data object Failed : BackupMessage
}

/** Settings > Backup: export to / import from a file the user picks. */
class BackupViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.backupRepository

    /** A read, valid backup waiting for the user's "Import" confirmation. */
    var pendingImport by mutableStateOf<BackupData?>(null); private set
    var message by mutableStateOf<BackupMessage?>(null); private set
    var busy by mutableStateOf(false); private set

    /** "BibliaKo-backup-2026-11-24.json" */
    fun suggestedFileName(): String =
        "BibliaKo-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"

    fun export(uri: Uri?) {
        uri ?: return
        run { repo.export(uri).let { BackupMessage.Exported(it.highlights.size, it.notes.size) } }
    }

    fun read(uri: Uri?) {
        uri ?: return
        run { pendingImport = repo.read(uri); null }
    }

    fun confirmImport() {
        val data = pendingImport ?: return
        pendingImport = null
        run { BackupMessage.Imported(repo.apply(data)) }
    }

    fun cancelImport() { pendingImport = null }

    fun consumeMessage() { message = null }

    private fun run(block: suspend () -> BackupMessage?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            message = try {
                block()
            } catch (e: BackupFormatException) {
                BackupMessage.NotABackup
            } catch (e: Exception) {
                BackupMessage.Failed
            }
            busy = false
        }
    }
}
