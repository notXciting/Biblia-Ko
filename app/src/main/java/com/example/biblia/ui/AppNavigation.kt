package com.example.biblia.ui

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.biblia.BuildConfig
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.ui.reader.ReaderScreen
import com.example.biblia.ui.selection.SelectionScreen
import com.example.biblia.ui.settings.SettingsScreen

private const val TAG = "VERSE_NAV"

/** Routes: reader <-> selection, reader -> settings. */
object Routes {
    const val READER = "reader"
    const val SELECTION = "selection"
    const val SETTINGS = "settings"
}

/** Material "emphasized decelerate" / "accelerate" curves. */
private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val ENTER_MS = 280
private const val EXIT_MS = 180

/**
 * App navigation host. The reader is the base layer: it stays fully visible
 * underneath while Selection (slides up) and Settings (slides in from the
 * side) animate over it, so popping back after a verse pick reveals the
 * reader already positioned on the target verse.
 */
@Composable
fun BibleNavHost(container: AppContainer, settings: AppSettings, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.READER,
        modifier = modifier,
    ) {
        composable(
            Routes.READER,
            exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
            popEnterTransition = { EnterTransition.None },
        ) {
            ReaderScreen(
                container = container,
                settings = settings,
                onOpenSelection = { navController.navigate(Routes.SELECTION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.SELECTION,
            enterTransition = {
                slideInVertically(tween(ENTER_MS, easing = Decelerate)) { it / 10 } +
                    fadeIn(tween(ENTER_MS / 2))
            },
            popExitTransition = {
                slideOutVertically(tween(EXIT_MS, easing = Accelerate)) { it / 10 } +
                    fadeOut(tween(EXIT_MS))
            },
        ) {
            SelectionScreen(
                container = container,
                settings = settings,
                onBack = { navController.popBackStack() },
                onVerseSelected = {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Verse selected -> popBackStack to reader")
                    navController.popBackStack(Routes.READER, inclusive = false)
                },
                onChapterSelected = {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Chapter selected -> popBackStack to reader")
                    navController.popBackStack(Routes.READER, inclusive = false)
                },
            )
        }
        composable(
            Routes.SETTINGS,
            enterTransition = {
                slideInHorizontally(tween(ENTER_MS, easing = Decelerate)) { it / 8 } +
                    fadeIn(tween(ENTER_MS / 2))
            },
            popExitTransition = {
                slideOutHorizontally(tween(EXIT_MS, easing = Accelerate)) { it / 8 } +
                    fadeOut(tween(EXIT_MS))
            },
        ) {
            SettingsScreen(
                container = container,
                settings = settings,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
