package com.example.biblia.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.importer.ImportState
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.ui.components.ImportLoadingScreen
import com.example.biblia.ui.listen.VoiceHelpDialog

/**
 * App shell: first-launch import gate ("Inihahanda ang Biblia... 42%") and then
 * the navigation host (reader <-> selection). The import runs on the Application
 * scope, so it survives rotation; process death restarts it only once per version.
 */
@Composable
fun AppRoot(container: AppContainer, modifier: Modifier = Modifier) {
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = remember { runBlocking { container.settingsRepository.initial() } })
    val version = BibleVersionRegistry.byId(settings.versionId)
    val importStates by container.importer.states.collectAsStateWithLifecycle()
    val imported by container.settingsRepository.isImportedFlow(version.id)
        .collectAsStateWithLifecycle(initialValue = false)
    val importState = importStates[version.id] ?: ImportState(version.id)
    val ready = imported || importState.isDone
    val tts by container.tts.state.collectAsStateWithLifecycle()

    LaunchedEffect(version.id) {
        if (!ready) container.importer.ensureImportedAsync(version)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (ready) {
            BibleNavHost(container = container, settings = settings)
        } else {
            ImportLoadingScreen(percent = importState.percent)
        }
        // "Listen" can't speak this language offline (reader, Settings test or status row).
        tts.voiceProblem?.let { problem ->
            VoiceHelpDialog(problem = problem, onDismiss = { container.tts.dismissVoiceProblem() })
        }
    }
}
