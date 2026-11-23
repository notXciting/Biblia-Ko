package com.example.biblia.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.domain.model.AppLanguage
import com.example.biblia.domain.model.AppSettings
import java.util.Locale

/** Effective UI locale tag for the settings ("fil" = default Tagalog strings). */
fun uiLocaleTag(settings: AppSettings): String = when (settings.appLanguage) {
    AppLanguage.TAGALOG -> "fil"
    AppLanguage.ENGLISH -> "en"
    AppLanguage.FOLLOW_VERSION -> BibleVersionRegistry.byId(settings.versionId).uiLocaleTag
}

/**
 * Runs [content] with string resources resolved in [localeTag], switching
 * live (no activity restart). The provided context still wraps the ACTIVITY,
 * so startActivity (share sheet), toasts, etc. keep working; only resources
 * and configuration are swapped.
 *
 * Also applies the app's UI size ([uiScale], Settings > "Laki ng app"): the
 * density is the window's REAL density (from the configuration, which this
 * never changes) times the scale, so nesting never scales twice. dp and sp
 * both follow it; the reader's own text size stays relative on top.
 */
@Composable
fun LocalizedContent(localeTag: String, uiScale: Float = LocalUiScale.current, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val (context, config) = remember(localeTag, base, baseConfig) {
        val locale = Locale.forLanguageTag(localeTag)
        val config = Configuration(baseConfig).apply { setLocale(locale) }
        LocalizedContext(base, base.createConfigurationContext(config).resources) to config
    }
    val density = remember(baseConfig.densityDpi, baseConfig.fontScale, uiScale) {
        Density(baseConfig.densityDpi / 160f * uiScale, baseConfig.fontScale)
    }
    CompositionLocalProvider(
        LocalContext provides context,
        LocalConfiguration provides config,
        LocalAppLocaleTag provides localeTag,
        LocalUiScale provides uiScale,
        LocalDensity provides density,
        content = content,
    )
}

/** The app's UI locale tag, so separate windows can re-apply it. */
val LocalAppLocaleTag = staticCompositionLocalOf { "fil" }

/** The app's UI size multiplier (1 = phone default), re-applied by [KeepAppLocale]. */
val LocalUiScale = staticCompositionLocalOf { 1f }

/**
 * Bottom sheets / dialogs / popups render in their own window, whose
 * ComposeView re-provides LocalContext, LocalConfiguration and LocalDensity
 * from the SYSTEM. Wrap a sheet's content with this to keep the app language
 * AND the app's UI size inside it.
 */
@Composable
fun KeepAppLocale(content: @Composable () -> Unit) {
    LocalizedContent(LocalAppLocaleTag.current, LocalUiScale.current, content)
}

private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}
