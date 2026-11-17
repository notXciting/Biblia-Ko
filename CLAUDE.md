# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Biblia Ko": an offline Tagalog/English Bible Android app (package `com.example.biblia`, single `:app` module). Kotlin 2.0.21 + Jetpack Compose (Material 3), Room + FTS4, DataStore, Navigation Compose, and hand-written DI (no Hilt/Koin). JDK 17, compileSdk/targetSdk 35, minSdk 24, AGP 8.13 / Gradle 8.13. The Android SDK path is in `local.properties`.

`PROGRESS.md` is the running log of what has been done, verified results, and what comes NEXT. Read it before starting work and update it when you finish a round. UI strings are in Tagalog (`res/values/strings.xml`).

## Commands (run from repo root, Windows)

```powershell
.\gradlew.bat :app:testDebugUnitTest                      # all JVM unit tests
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.biblia.ReferenceParserTest"   # one class
.\gradlew.bat :app:testDebugUnitTest --tests "*BookSearchTest.someMethod"               # one test
.\gradlew.bat :app:compileDebugKotlin                     # fast compile check
.\gradlew.bat assembleDebug                               # app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat assembleRelease                             # R8 + shrinkResources, signed with debug key
.\gradlew.bat lintDebug
adb logcat -s VERSE_PERF VERSE_NAV                        # debug-only jump instrumentation
```

Test results (JUnit XML) land in `app\build\test-results`. Gradle logs redirected with PowerShell `>` (e.g. `build.log`, `test.log`) are UTF-16; search them with `Select-String`. There are no instrumented (androidTest) tests; JVM tests use `kxml2` as the XmlPullParser implementation, and `unitTests.isReturnDefaultValues = true` lets code call `android.util.Log`.

## Architecture

**DI:** `di/AppContainer.kt`, created by `BibleApplication`, owns every singleton: `BibleDatabase`, `SettingsRepository` (DataStore), `BibleRepository`, `NavigationBus`, `applicationScope` (SupervisorJob + IO), the shared `ChapterCache` (LRU of 24, with a synchronous `peek()`), and `BibleImporter`. ViewModels take the container directly.

**Data import pipeline:** Bible text ships as XML in `app/src/main/assets/bibles/`. The root `Tagalog2012Bible.xml` is the source copy, and the app reads `assets/bibles/tagalog2012.xml`. On first run `BibleImporter` copies the asset to cache, then streams it through a `BibleParser` (`TagalogXmlBibleParser`: XmlPullParser + BOM strip, listener callbacks). It inserts 400-verse batches into `verses`, plus an FTS4 row in `verses_fts` holding accent/case-folded text (`TextNormalizer.fold`) and linked by `verse_id`. Progress is a `StateFlow<Map<versionId, ImportState>>`. A DataStore "imported" flag per version prevents re-import. Partial data is deleted before a retry.

**Adding a Bible version:** drop the XML in `assets/bibles/` and add one `BibleVersion` entry to `data/BibleVersionRegistry.kt`. A new XML format needs a `BibleParser` registered under its `formatId` in `AppContainer.parsers`. Everything is keyed by `versionId`.

**DB schema:** `BibleDatabase` is version 1 with `fallbackToDestructiveMigration()` and `exportSchema = false`. A schema change wipes the DB, and the DataStore imported flag then disagrees with the DB unless you handle it. User data (highlights) lives in a SEPARATE `UserDatabase` (`user.db`, `data/user/`), keyed by book/chapter/verse (version-independent). It must never get a destructive fallback; any schema change there needs a real `Migration`. Never put user data in `BibleDatabase`.

**Launcher icons:** the launcher entry is one of three `activity-alias`es (`.IconSunrise`/`.IconNight`/`.IconSky` → `MainActivity`, which has no launcher filter); `data/AppIconManager` enables exactly one. Each icon has mipmaps + an adaptive XML, plus a `drawable-nodpi/icon_preview_*.png` for the Settings picker (Compose can't draw adaptive-icon XML). The full-size source art is in `design/icons/` (outside the app module, so it is not packaged); regenerate the mipmaps from it if an icon changes.

**Book/chapter math:** `data/catalog/BookCatalog` holds the canonical 66-book table (Tagalog/English names, abbreviations, `standardChapterCounts` summing to 1189) and cumulative `flatStart` offsets. The reader's `HorizontalPager` pages over a *flat chapter index* (0..1188), and `flatIndexOfStandard()` maps book/chapter to a page in O(1). Verse-count clamping uses an in-memory `book*1000+chapter` table loaded once from the DAO.

**Screens and navigation:** `ui/AppNavigation.kt` defines a NavHost with three routes: `reader` (the start destination and base layer, kept visible with `KeepUntilTransitionsFinished`), `selection` (slides up) and `settings` (slides in from the side). `BibleTheme` wraps the NavHost and applies DataStore settings. The selection screen does not navigate with arguments when the user picks a verse or chapter. Instead `SelectionViewModel.jumpTo()`:
1. cancels any running full-text search (frees the DB),
2. awaits the target chapter into `ChapterCache` (capped at 400 ms), warms its neighbors,
3. publishes an `OpenVerseRequest` to `NavigationBus`, then calls `onReady` (= `popBackStack`).

`ReaderViewModel` itself collects the bus for its whole lifetime on Main.immediate, so `openAt` runs synchronously inside `bus.open()`, before the pop. `openAt` is in-memory only and bumps `jumpId`; `ReaderScreen` keys its `PagerState` on `jumpId`, so the pager is built AT the target page. `ChapterPage` starts from `ChapterCache.peek()` (text in the first frame); a real cache miss shows `ChapterSkeleton` after 150 ms. `VerseText` scrolls the verse into view while hidden, then runs a draw-phase pulse. Keep this jump path free of delays and animations (prev/next arrows may animate).

**Search (`domain/search`, all pure Kotlin and unit-tested):**
- `ReferenceParser` parses references like "juan 3:16" synchronously on every keystroke. A valid reference becomes a smart-jump and cancels FTS.
- `BookSearch` matches book names instantly with ranking exact > prefix > word-prefix > contains, and respects `BookFilter`.
- FTS goes through `SearchQueryBuilder` (prefix terms), with a 300 ms debounce, `LIMIT 201` (200 shown + a truncation probe), a 2-character minimum, and runs on IO. The DAO query drives from `verses_fts` with `CROSS JOIN` and `ORDER BY f.docid`: docids are in canonical order because each version is imported Genesis→Revelation, so SQLite streams and stops at the limit. Don't change it back to `ORDER BY book, chapter, verse` (that sorted every match: >3 s for "ang").
- Search scope (Lahat / Mga Aklat / Teksto) is persisted in DataStore.

**Listen (TTS):** `domain/tts/TtsController` is pure Kotlin (JVM-tested in `TtsControllerTest`); Android `TextToSpeech` and audio focus sit behind `TtsEngine` / `PlaybackFocus` (`data/tts`). The controller lives in `AppContainer.tts` on a Main scope and owns the reading position; the reader only renders `TtsState` (spoken-verse tint, ListenBar, following chapter rollovers). The voice follows `BibleVersion.language` through `TtsLocales`, and only installed, network-free voices are used (`VoiceCheck`). If there is no engine, no voice, or only online voices, the controller raises a `VoiceProblem`, which `AppRoot` shows as `VoiceHelpDialog`. Never add `READ_PHONE_STATE`: calls are handled through audio focus.

**Debug instrumentation:** the `perf/VersePerf` stages and the `VERSE_NAV` logs are wrapped in `if (BuildConfig.DEBUG)`. Keep new logging gated the same way.

**Localization:** `values/` = Tagalog (default), `values-en/` = English; every key must exist in both (LocalizationTest). The UI language is applied live by `ui/AppLocale.kt` (`LocalizedContent`), not by Android's per-app locale. A `ModalBottomSheet`/`Dialog`/`Popup` gets its own window that resets to the SYSTEM locale, so wrap its content in `KeepAppLocale { }`. Book names come from `BookInfo.displayName`, which follows the selected Bible's language through `BookNameLanguage`, independently of the UI language.

**Themes and accents:** `ui/theme/Theme.kt` builds the color scheme from a `ThemePalette` (one per `ThemeMode`) plus the accent's tone for that brightness (`AccentColor.tone(dark)`, two hand-tuned tones per hue). `ThemeContrastTest` enforces WCAG contrast for every theme × accent × highlight combination. Run it after touching any color, and keep the highlight alphas in `ChapterPage` (0.55 light / 0.30 dark) in sync with the test.

## Conventions

- Keep source files under about 200 lines. Large screens are split into small per-feature files (see `ui/selection/*Level.kt`, `*Views.kt`).
- Persist every user setting through `SettingsRepository` / `AppSettings`, not ad-hoc preferences.
