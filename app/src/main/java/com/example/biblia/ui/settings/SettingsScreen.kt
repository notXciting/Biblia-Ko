package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.R
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings

/**
 * Settings: theme tiles, accent swatches, font size (live preview) and line
 * spacing — every change persists to DataStore and applies live app-wide.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    settings: AppSettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } },
    )
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
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
            Spacer(Modifier.height(16.dp))
            AppIconSection(
                current = settings.appIcon,
                onSelect = { icon ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setAppIcon(icon)
                },
            )
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.settings_theme))
            ThemeTiles(
                current = settings.themeMode,
                accent = settings.accent,
                onSelect = { mode ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setThemeMode(mode)
                },
            )
            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_accent_color))
            AccentSwatches(
                current = settings.accent,
                darkTheme = settings.themeMode.isDark(isSystemInDarkTheme()),
                onSelect = { color ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setAccent(color)
                },
            )
            Spacer(Modifier.height(8.dp))
            FontSizeSection(
                fontSizeSp = settings.fontSizeSp,
                lineSpacing = settings.lineSpacing,
                // John 3:16 in the Bible currently in use (fixed text as fallback).
                previewReference = vm.fontPreviewReference,
                previewText = vm.fontPreviewText,
                onChange = { vm.setFontSizeSp(it) },
            )
            Spacer(Modifier.height(8.dp))
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
                previewVerses = vm.previewVerses,
                previewReference = vm.previewReference,
                fontSizeSp = settings.fontSizeSp,
                lineSpacingMultiplier = settings.lineSpacing.multiplier,
                onSelect = { value ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setVerseLayout(value)
                },
            )
            Spacer(Modifier.height(8.dp))
            ListenSection(
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
            )
            Spacer(Modifier.height(8.dp))
            BackupSection(container = container)
            Spacer(Modifier.height(8.dp))
            AboutSection(currentVersionId = settings.versionId)
        }
    }
}
