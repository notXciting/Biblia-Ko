package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.ThemeMode

/** One Settings sub-page. Every change persists to DataStore and applies live app-wide. */
@Composable
fun SettingsPageScreen(
    container: AppContainer,
    settings: AppSettings,
    page: SettingsPage,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container) } })
    // Font size while the slider is dragged, so the pinned preview grows live.
    var liveSize by remember(settings.fontSizeSp) { mutableFloatStateOf(settings.fontSizeSp) }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        SettingsTopBar(stringResource(page.title), onBack)
        if (page == SettingsPage.READING) {
            ReadingPreview(
                verses = vm.previewVerses,
                reference = vm.previewReference,
                fontSizeSp = liveSize,
                lineSpacingMultiplier = settings.lineSpacing.multiplier,
                layout = settings.verseLayout,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            when (page) {
                SettingsPage.READING -> ReadingPage(vm, settings, onLiveSize = { liveSize = it })
                SettingsPage.APPEARANCE -> AppearancePage(vm, settings)
                SettingsPage.LISTEN -> ListenSection(
                    prefs = settings.tts,
                    languages = vm.ttsLanguages,
                    voices = vm.ttsVoices,
                    status = vm.ttsStatus,
                    onRefresh = { vm.refreshVoices() },
                    onVoiceHelp = { language, status -> vm.showVoiceHelp(language, status) },
                    onRate = { vm.setTtsRate(it) },
                    onPitch = { vm.setTtsPitch(it) },
                    onVoice = { language, name -> vm.setTtsVoice(language, name) },
                    onSpeakNumbers = { vm.setTtsSpeakNumbers(it) },
                    onTest = { vm.testVoice(BibleVersionRegistry.byId(settings.versionId).language) },
                    showTitle = false,
                )
                SettingsPage.APP -> AppPage(vm, settings)
                SettingsPage.BACKUP -> BackupSection(container = container, showTitle = false)
                SettingsPage.ABOUT -> AboutSection(currentVersionId = settings.versionId, onOpenLicenses = onOpenLicenses, showTitle = false)
            }
        }
    }
}

@Composable
private fun ColumnScope.ReadingPage(vm: SettingsViewModel, settings: AppSettings, onLiveSize: (Float) -> Unit) {
    val haptics = LocalHapticFeedback.current
    ReadingFontSection(
        current = settings.readingFont,
        sample = vm.fontPreviewText,
        onSelect = { font ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setReadingFont(font)
        },
    )
    Spacer(Modifier.height(8.dp))
    FontSizeSection(fontSizeSp = settings.fontSizeSp, onLive = onLiveSize, onChange = { vm.setFontSizeSp(it) })
    LineSpacingSection(
        current = settings.lineSpacing,
        onSelect = { value ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setLineSpacing(value)
        },
    )
    Spacer(Modifier.height(8.dp))
    VerseLayoutSection(
        current = settings.verseLayout,
        onSelect = { value ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setVerseLayout(value)
        },
    )
    Spacer(Modifier.height(8.dp))
    CrossRefSettingsSection(
        show = settings.showCrossRefs,
        font = settings.crossRefFont,
        onShow = { vm.setShowCrossRefs(it) },
        onFont = { vm.setCrossRefFont(it) },
    )
}

@Composable
private fun ColumnScope.AppearancePage(vm: SettingsViewModel, settings: AppSettings) {
    val haptics = LocalHapticFeedback.current
    SectionTitle(stringResource(com.example.biblia.R.string.settings_theme))
    ThemeTiles(
        current = settings.themeMode,
        accent = settings.accent,
        onSelect = { mode ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setThemeMode(mode)
        },
    )
    if (settings.themeMode == ThemeMode.PAPER) {
        PaperGrainRow(checked = settings.paperGrain, onChange = { vm.setPaperGrain(it) })
    }
    Spacer(Modifier.height(16.dp))
    SectionTitle(stringResource(com.example.biblia.R.string.settings_accent_color))
    AccentSwatches(
        current = settings.accent,
        darkTheme = settings.themeMode.isDark(isSystemInDarkTheme()),
        onSelect = { color ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setAccent(color)
        },
    )
    Spacer(Modifier.height(16.dp))
    AppIconSection(
        current = settings.appIcon,
        onSelect = { icon ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setAppIcon(icon)
        },
    )
}

@Composable
private fun ColumnScope.AppPage(vm: SettingsViewModel, settings: AppSettings) {
    val haptics = LocalHapticFeedback.current
    LanguageSection(
        current = settings.appLanguage,
        versionLanguage = BibleVersionRegistry.byId(settings.versionId).language,
        onSelect = { value ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.setAppLanguage(value)
        },
    )
    Spacer(Modifier.height(8.dp))
    UiScaleSection(current = settings.uiScale, onChange = { vm.setUiScale(it) })
    Spacer(Modifier.height(8.dp))
    BarSizeSection(current = settings.barSize, onSelect = { vm.setBarSize(it) })
}
