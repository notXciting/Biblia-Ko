package com.example.biblia.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.versions.VersionRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** "Mga Biblia": every registered version with Load / Unload. */
class ManageBiblesViewModel(container: AppContainer) : ViewModel() {

    private val library = container.versionLibrary

    val sections = VersionRules.sections(BibleVersionRegistry.all)

    /** null until DataStore answers (no flash of "nothing loaded"). */
    val loaded: StateFlow<Set<String>?> = library.loaded.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val importStates = container.importer.states

    var sizes by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    var confirmUnload by mutableStateOf<BibleVersion?>(null); private set
    var unloading by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            BibleVersionRegistry.all.forEach { v -> sizes = sizes + (v.id to library.estimatedMb(v)) }
        }
    }

    fun load(version: BibleVersion) = library.load(version)

    fun askUnload(version: BibleVersion) { confirmUnload = version }

    fun cancelUnload() { confirmUnload = null }

    fun unload() {
        val version = confirmUnload ?: return
        confirmUnload = null
        unloading = version.id
        viewModelScope.launch {
            library.unload(version)
            unloading = null
        }
    }
}
