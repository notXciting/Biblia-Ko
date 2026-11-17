# Biblia (Tagalog) — PROGRESS

Offline Tagalog Bible app: Kotlin + Jetpack Compose + Material 3, Room + FTS4, DataStore, manual DI, Navigation. Source: Tagalog2012Bible.xml (66 books / 1,189 chapters / 30,692 verses / 179 empty). Package com.example.biblia.

## LATEST ROUND: "v1.2.0 prep" (2026-11-18)

- versionCode 2 / versionName 1.2.0 (app/build.gradle.kts). README updated: ten versions (CSB, HCSB, KJV, NKJV, NLT added to the table), v1.2.0 badge/APK name, ~200 MB storage.
- Release APK built (16.8 MB, debug-key signed), SHA-256 90d94509...7359f3e0a. 49 tests pass. Committed locally.
- NOT pushed / no GitHub release yet: the repo is now PUBLIC (created private; changed outside this session) and holds copyrighted Bible XML — waiting for the user's decision.

## PREVIOUS ROUND: "Version sheet jumps during first import" (2026-11-18)

- BUG: tapping a not-yet-imported version made the version sheet slide down and hide that row, so the progress was invisible. Cause: the progress bar was an AnimatedVisibility BELOW the row (content height grew while animating) and the ModalBottomSheet was half-expanded, which re-anchors/slides whenever content height changes.
- FIX (VersionSheet.kt): sheet opens fully expanded (skipPartiallyExpanded = true) and import progress is DRAWN in the row (drawBehind: 12% primary fill growing left->right + 3dp bottom line, animated) with "Inihahanda… N%" in the subtitle — row height never changes. 49 tests pass; release installed. Not verified on device by me (needs an import on the user's phone) — user to confirm.

## PREVIOUS ROUND: "Five more English versions" (2026-11-18)

- Registered CSB, HCSB, KJV, NKJV, NLT (EnglishCSBBible/EnglishHCSBBible/EnglishKJBible/EnglishNKJBible/EnglishNLTBible.xml). Registry order: MBB12, ASD, then English alphabetically (AMP, CSB, ESV, HCSB, KJV, NIV, NKJV, NLT) — version sheet, About and Compare follow it.
- All 5 files validated: 66 books / 1189 chapters in canonical order (FTS docid ordering relies on it), 31,101-31,103 verses, no empty verses, no nested tags, John 3:16 present.
- Copyright notices: CSB/HCSB/NKJV from the XML headers; KJV "Public domain"; NLT has no header line -> standard Tyndale notice used (confirm edition). HCSB header says "Copyrighted Only For Website" (redistribution caveat, told the user).
- VersionSheet list is now vertically scrollable (10 versions no longer fit). Test renamed to everyVersionHasACopyrightOrPublicDomainNotice (KJV has no "Copyright"). 49 tests pass. Release APK 9.5 -> 16.0 MB; installed on device (not driven — phone may be in use).
- NOT committed/pushed yet (awaiting user). Next GitHub release needs versionCode 2 / versionName 1.1.0.

## PREVIOUS ROUND: "GitHub repo, README, v1.0.0 release" (2026-11-17)

- Private repo: https://github.com/notXciting/Biblia-Ko (branch main). .gitignore also excludes *.log, .kotlin, .claude/settings.local.json; .gitattributes pins gradlew to LF.
- README.md (features, install, version copyrights, tech stack, build steps) with 8 screenshots in docs/screenshots (540px wide).
- Release v1.0.0 (tag on b98a708): asset BibliaKo-v1.0.0.apk (9.5 MB, versionCode 1, minSdk 24, signed with the DEBUG key), SHA-256 32A4AE7E...301B1E. For the next release bump versionCode/versionName in app/build.gradle.kts first.

## PREVIOUS ROUND: "Theme + accent overhaul (contrast-verified)" (2026-11-17)

- Analysis (WCAG, 4.5:1 for accent text): of the old 8 accents only BLUE and RED passed everywhere; ORANGE failed light/sepia/button (2.6-3.1), TEAL light/sepia, GREEN sepia, PURPLE/PINK/INDIGO dark (the 25% lerp toward white was not enough).
- AccentColor now has TWO hand-tuned tones per hue (light, dark; `tone(dark)`): BLUE, INDIGO, PURPLE, PINK, RED, AMBER(Gold, new), BROWN(new), GREEN, TEAL, GRAPHITE(new). ORANGE removed (saved ORANGE falls back to BLUE). Swatches (2 rows of 5) are drawn in the tone the current theme uses.
- ThemeMode (picker order, SYSTEM first): SYSTEM, LIGHT, SEPIA, SAGE(new soft green-gray), DARK, SLATE(new dim blue-gray), MIDNIGHT(new pure black/OLED); `isDark(systemDark)`. ui/theme/Theme.kt: ThemePalette per theme (ThemePalettes) + buildScheme(palette, accent). Theme tiles (4+3 grid) preview the real palette + current accent tone.
- Highlight alpha in dark themes 0.38 -> 0.30 (yellow-on-Slate verse text was 4.4:1; now >= 5.6:1).
- ThemeContrastTest enforces: System first; accent text >= 4.5 and button label >= 4.5 on every theme; verse text >= 4.5 and accent verse numbers >= 3.0 on every highlight; secondary text >= 4.5 on bg and cards. 49 tests pass.
- Release installed; NOT visually checked on device (user asked earlier to not drive the phone while in use).

## PREVIOUS ROUND: "Jump-pulse fix in verse-by-verse layout" (2026-11-17)

- BUG: the jump highlight (pulse) missed the last line of the verse in VERSE_BY_VERSE layout. The draw-phase pulse ended its last line at getCursorRect(range.last + 1); in verse-by-verse that offset is the START of the next paragraph (x = 0), so the last line had zero width. FIX (VerseText drawBehind): edges now use the verse's own first/last character boxes, getBoundingBox(range.first).left / getBoundingBox(range.last).right — correct in both layouts.
- Built, 45 tests pass, release installed. NOT yet verified on device: the phone was in active use (another app came to the foreground mid-test), so device automation was stopped. Verify manually: Verse by verse layout -> search "juan 3:17" -> Go to -> whole verse incl. last line pulses.
- Device state note: the test switched Verse layout to "Bawat talata" (verse by verse).

## PREVIOUS ROUND: "Developer name + grid testament headers" (2026-11-17)

- About: developer shown as "Jee." (AboutSection DEVELOPER).
- Book GRID view now has "Lumang Tipan" / "Bagong Tipan" headers as full-width items (GridItemSpan(maxLineSpan); LazyGrid has no stickyHeader in this Compose version), using the same rule as the list (spansBothTestaments: All + cross-testament themes). SectionHeader got a horizontalPadding param (4dp in grid to align with cards). Verified on device; list view restored.

## PREVIOUS ROUND: "Settings previews use the version in use" (2026-11-17)

- Font-size preview now shows John 3:16 from the SELECTED version (SettingsViewModel.fontPreviewText/fontPreviewReference via ChapterCache; book name in the version's language); the fixed font_preview_* strings remain only as a fallback.
- Verse-layout preview shortened to Psalm 23:1-2. Verified on device with NIV.

## PREVIOUS ROUND: "Tagalog 2015 (ASD) version" (2026-11-17)

- New registry entry right after MBB12: id `asd`, "ASD", "Tagalog 2015 (Ang Salita ng Dios)", assets/bibles/Tagalog2015Bible.xml, Tagalog, copyright "Ang Salita ng Dios (Tagalog Contemporary Bible). Copyright © 2009, 2011, 2014, 2015 by Biblica, Inc." (from the XML header). Version sheet, About ("Mga Bersyon ng Biblia") and Compare all follow registry order, so ASD appears next to MBB12 everywhere.
- File validated: 66 books / 1189 chapters / 30,763 verses (608 empty — handled), books and chapters in canonical order (required by the FTS docid ordering). EnglishBiblesParserTest covers every registry entry, so ASD parsing is tested. 45 tests pass.
- Verified on device: import ~4 s, stays on Juan 3, Tagalog UI + book names, About shows ASD as "ginagamit". MBB12 restored as the selected version afterwards.

## PREVIOUS ROUND: "Verse layout setting" (2026-11-17)

- Settings > "Pagkakaayos ng mga talata / Verse layout": Tuloy-tuloy/Paragraph (default, inline superscript numbers) vs Bawat talata/Verse by verse (one ParagraphStyle per verse, bold accent number + en space, half-line gap = a tiny paragraph with its own small lineHeight containing a zero-width space). Persisted as `verse_layout` (AppSettings.verseLayout, VerseLayout enum); ReaderScreen -> ChapterPage -> ChapterFlowText(layout).
- Both layouts go through buildChapterFlow (VerseText.kt, now internal) and record the same verse ranges, so tap-select, highlights, selection underline, jump pulse/scroll and top-verse tracking work unchanged; gap characters are never inside a verse range.
- Live preview in Settings: the reader's own ChapterFlowText rendering Psalm 23:1-4 from the SELECTED version (SettingsViewModel loads it via ChapterCache), at the user's font size + line spacing, crossfading on switch.
- Tests: VerseLayoutTest (ranges in both layouts, gaps outside verses, paragraph count). 45 tests pass. Verified on device (preview both states, reader verse-by-verse, selection); layout restored to Paragraph afterwards.

## PREVIOUS ROUND: "Icon switcher + verse panel revamp" (2026-11-17)

- App icon picker (Settings > Icon ng app / App icon): Sunrise (default), Night (app_icon_dark.png), Sky (app_icon_sky.png). Manifest: MainActivity has NO launcher filter; three activity-aliases .IconSunrise (enabled) / .IconNight / .IconSky target it. data/AppIconManager enables exactly one (DONT_KILL_APP); persisted as `app_icon` (AppSettings.appIcon, AppIcon enum) and re-applied at startup in BibleApplication.warmUp. Resources per icon: mipmap ic_launcher{_dark,_sky}{,_round,_foreground} + anydpi-v26 adaptive XMLs + colors; drawable-nodpi/icon_preview_{sunrise,dark,sky}.png for the picker (Compose cannot draw adaptive XML). Verified: alias switch on device, app stays alive.
- NOTE: moving the launcher entry from MainActivity to an alias can make some launchers drop an existing home-screen shortcut once (re-add from the app drawer).
- Verse panel (VerseActionBar.kt) redesigned: card sliding up with reference header ("Philippians 3:17-18", ReferenceFormatter) + count + close; 5 highlight swatches (check on the shared color) + remove-highlight; actions Copy / Share / Compare. Compare = CompareSheet listing the selection in every version (installed ones; others show an install hint).
- Highlights: NEW separate Room DB user.db (data/user: HighlightEntity PK book/chapter/verse, version-independent; HighlightRepository; NO destructive migration — user data). Rendered as background spans in VerseText (alpha 0.55 light / 0.38 dark); selection = tint + underline so it shows over highlights. ReaderViewModel owns VerseActions (highlights + compare state).
- Back while verses are selected now clears the selection (previously exited the app). Chapter end spacer 220dp so last verses can scroll above the panel.
- Tests: ReferenceFormatterTest (4). 42 tests pass. Release installed; test highlights removed and Sunrise restored after verification.
- Icon source PNGs moved from app/src/main/assets/images to design/icons/ (not packaged): release APK 12.7 MB -> 8.2 MB. To change an icon, edit the PNG there and regenerate the mipmaps + preview (same System.Drawing approach: art in the central 72/108dp, edge pixels stretched over the margin).

## PREVIOUS ROUND: "Icon, name, search speed, About" (2026-11-17)

- App name "Biblia Ko" (values/strings.xml app_name; launcher uses the default locale).
- Launcher icon from assets/images/app_icon.png: adaptive icon (mipmap-anydpi-v26, background @color/ic_launcher_background) whose opaque foreground puts the art in the visible 72/108dp and extends the art's own edge gradient over the margin (seamless under any mask); legacy rounded-square + round PNGs for API 24-25. Generated per density by a System.Drawing PowerShell script (not kept in the repo; rerun the same approach if the art changes). Verified on device App Info. NOTE: assets/images/app_icon.png itself is still packaged in the APK (~1.5 MB) — move it out of assets/ if not needed at runtime.
- "Search then jump is sometimes slow" ROOT CAUSE (measured on device): the FTS query sorted EVERY match before LIMIT (ORDER BY book, chapter, verse) — "ang" was still running after 3 s, and a jump's chapter load queued behind it. Fixed by FTS-driven CROSS JOIN + ORDER BY f.docid (canonical import order) so SQLite stops at 201 rows: "ang" now < 150 ms on MBB12, worst case (ESV + Tagalog-heavy prefix) < 500 ms incl. 300 ms debounce. Also: jumpTo cancels any running FTS first; FTS skipped below 2 chars; "0 results" hidden while searching; ChapterSkeleton (pulsing paragraph placeholder) if a page's text takes > 150 ms.
- About: app name, version, "Gumawa / Developer: Jee Zor", plus a "Mga Bersyon ng Biblia" card listing all 4 versions (badge, full name, language, copyright, current one marked "ginagamit"). MBB12 copyright moved into the registry so About needs no DB. SettingsViewModel no longer loads version meta.
- 38 tests pass; release build installed (MBB12 restored as the selected version after testing).

## PREVIOUS ROUND: "Better book filters" (2026-11-17)

- BookFilter redesigned as book SETS with a FilterGroup (sheet sections): General (All 66 / OT 39 / NT 27); OT (Law 5, History 12, Poetry & Wisdom 5, Major Prophets 5, Minor Prophets 12); NT (Gospels 4, Paul's Letters 13, General Letters 8, All Letters 21); Themes (Narrative = Gen–Esth + Gospels + Acts 22, Prophecy = prophets + Revelation 18, Prison Letters 4, Pastoral Letters 3, One-Chapter Books 5). Removed the single-book Acts and Revelation filters (now inside Narrative / Prophecy). `bookRange` replaced by `books` / `matches()` / `bookCount`.
- FTS DAO filter changed from BETWEEN to `(:allBooks OR v.book IN (:books))` for non-contiguous themes.
- UI: grouped filter sheet with book count per chip; ActiveFilterBar under the breadcrumb ("Propesiya · 18 aklat ✕", tap to clear; also visible during search); book list keeps OT/NT sticky headers whenever the filtered set spans both testaments.
- BUG FIXED: bottom sheets (filter, versions) rendered in the SYSTEM locale because their window's ComposeView re-provides LocalContext/LocalConfiguration. AppLocale now exposes LocalAppLocaleTag + KeepAppLocale {}; wrap the content of every ModalBottomSheet/Dialog/Popup with it.
- Tests: BookFilterTest (no single-book filter, OT sections partition 1..39, every book reachable via a non-General filter, One-Chapter == books with 1 chapter in the canonical table, theme membership, BookSearch with non-contiguous filter). 38 tests pass.
- Verified on device: grouped sheet in Tagalog; Prophecy list = OT prophets + "Bagong Tipan > Pahayag"; "babilonia" search under Prophecy returns only prophecy books.

## PREVIOUS ROUND: "App language + English book names" (2026-11-17)

- Settings > "Wika ng app / App language": Sundan ang Biblia (default, FOLLOW_VERSION) / Tagalog / English. Persisted as `app_language` (AppSettings.appLanguage, AppLanguage enum).
- UI strings: default values/ = Tagalog, new values-en/strings.xml = full English (KJV John 3:16 as the font preview). Switching is live with no activity restart: ui/AppLocale.kt LocalizedContent provides a ContextWrapper(activity) with locale-specific Resources + LocalConfiguration ("fil" -> default strings, "en" -> values-en). Wrapping the activity keeps startActivity (share) working.
- Book names follow the selected BIBLE's language (independent of the UI language): BookInfo.displayName reads the BookNameLanguage.english snapshot state; alternateName = the other language. Set in MainActivity (before setContent + SideEffect) and in ReaderViewModel.applyVersion (same frame as the text switch). BibleVersion.isEnglish / uiLocaleTag.
- MainActivity/AppRoot read settings once with runBlocking before the first frame, so saved theme/language never flash defaults.
- Tests: LocalizationTest (every values/ key exists in values-en; displayName switching; uiLocaleTag rules). 32 tests pass.
- Verified on device: Settings switches language live; Tagalog UI + English Bible shows English book names; Exodus 5:23 (AMP) -> Exodo 5:23 (MBB12) in Follow mode.

## PREVIOUS ROUND: "English versions + verse-preserving switch" (2026-11-17)

- Registry now has 4 versions: mbb12 (Tagalog), amp (EnglishAmplifiedBible.xml), esv (EnglishESVBible.xml), niv (EnglishNIVBible.xml). All files share the same schema (66 books / 1189 chapters, ~31.1k verses each, no empty verses). BibleVersion gained `copyright`; registry fullName/copyright override the XML header at import (headers are inconsistent: AMP uses name=, ESV packs copyright into translation=, NIV has none). Parser falls back to name= for translation. NIV copyright string is the standard Biblica notice (not in the file) — confirm it matches the edition.
- Each version imports on first selection (~4 s on device, progress bar in the redesigned version sheet), then switches.
- Switching keeps the verse being READ: VerseText reports the verse at the top of the viewport (same probe line the scroll targets, so no drift) to ReaderViewModel.topVerse; applyVersion loads counts + the target chapter for the new version first, then switches in one frame with anchorVerse (silent scroll, no pulse). Near a chapter end, where the scroll is clamped, the anchored verse is kept until the user scrolls.
- Tests: EnglishBiblesParserTest (every registry asset parses to 66/1189 + John 3:16; AMP name= fallback; English copyrights). 29 tests pass. Release APK 6.9 MB.
- Verified on device: Exodus 29:16 MBB12 -> ESV -> NIV -> MBB12 stays on v16 each time.
- Not done (possible follow-ups): book names stay Tagalog in all versions; the top-bar label shows the last tapped/jumped verse, not the scrolled-to one.

## PREVIOUS ROUND: "Instant jump + visual polish" (2026-11-17)

Blank-page-on-jump root causes fixed:
- ChapterPage started with content = null and loaded in a LaunchedEffect -> always >= 1 blank frame. Now starts from ChapterCache.peek() (synchronous); only a true cache miss loads async and fades in (180 ms).
- Selection preload was fire-and-forget, racing the pop. SelectionViewModel.jumpTo() now AWAITS the target chapter into the cache (cap 400 ms), then publishes, then calls onReady (= pop). pick*/open* take an onReady callback; double taps ignored.
- Reader applied the request in a LaunchedEffect after the pop (first frame = old chapter). ReaderViewModel now collects NavigationBus itself on Main.immediate for its whole lifetime, so openAt() runs synchronously inside bus.open(), before popBackStack. openAt is fully in-memory and bumps jumpId; ReaderScreen keys rememberPagerState on jumpId so the pager is built AT the target page.
- VerseText: scroll target now includes the heading offset (onPlaced), scrolls once per pulse, and keeps text invisible (max 300 ms) until positioned -> no visible jump.
- Reader ready no longer waits for DB (chapterCounts starts from BookCatalog.standardChapterCounts); BibleApplication warms Room + last-read chapter on IO at startup. ChapterCache size 7 -> 24.
- Verified on device (release APK): cold start 221 ms; "juan 3:16" jump -> reader positioned + pulsing within 350 ms of the tap.

Visual polish: new neutral palettes (light/dark/sepia) with opaque accent-blended containers; serif reading text; chapter heading = uppercase book label + serif "Kabanata N" + accent rule; flat top bar with hairline + crossfading location title + outlined version chip; small corner prev/next arrows (animated page turn, hidden while verses are selected); selection top row with back button + pill search; direction-aware slide between Books/Chapters/Verses; nav transitions = reader stays as base layer, Selection slides up, Settings slides in from side (emphasized easing); breadcrumb chevrons, hidden during search; empty "Mga Aklat" section hidden in Lahat when the query is a reference; system bar icons follow the app theme.

## PREVIOUS STATE (after "Search scope + speed fixes" round)

Phases 1-5 + perf round complete and verified. This round added Task A (search scope + book search) and Task B (faster jump to a verse). Verse-tap navigation design unchanged: NavigationBus instance in AppContainer + StateFlow replay + popBackStack; SelectionScreen onVerseSelected/onChapterSelected.

### Task A: Search scope + book search

- Scope selector: 3 single-select FilterChips under the search field (ScopeChips.kt) — "Lahat" (ALL, default), "Mga Aklat" (BOOKS), "Teksto" (TEXT). Persisted in DataStore (`search_scope` key, SettingsRepository.setSearchScope, AppSettings.searchScope).
- BookSearch (domain/search/BookSearch.kt): pure Kotlin, synchronous, instant (no debounce, no Android deps). Matches Tagalog name + English name + abbreviation via TextNormalizer (accent/case-insensitive; numbered books with or without space: "1 cor", "1cor", "1 corinto"). Ranking exact(1) > prefix(2) > word-prefix(3, "aw" matches "Mga Awit") > contains(4), ties by book order. Respects BookFilter ranges.
- Lahat scope (smart): when the query is NOT a reference, a "Mga Aklat" section (max 5 books, BookResults.kt) shows FIRST, then FTS verse results below. Reference smart-jump stays top in Lahat and Mga Aklat; when the query IS a valid reference FTS is skipped completely.
- Mga Aklat scope: book names only, no FTS; empty state "Walang aklat na nahanap".
- Teksto scope: previous behavior (FTS only).
- Book result row: number badge, Tagalog name, English name (smaller), chapter count — same style as Books list. Tap: does NOT leave the screen — clears the search field, hides the keyboard, and drills to CHAPTERS of that book (breadcrumb "Mga Aklat > Juan").
- New strings: scope_text ("Teksto"), book_search_empty ("Walang aklat na nahanap").

### Task B: Faster jump to a verse

STEP 1 (measure) — VersePerf (perf/VersePerf.kt): t0 marked at the tap (VerseLevel.onPick -> markTap); stages log "VERSE_PERF <stage>: +<ms>" — (1) bus publish (NavigationBus.open), (2) reader received request (ReaderScreen bus effect), (3) preload started (SelectionViewModel), (4) chapter data ready (ReaderViewModel.loadCurrent), (5) pager at target page (onPageChanged), (6) verse scrolled into view (VerseText). All behind BuildConfig.DEBUG.
Debug-run numbers NOT measurable in this environment (no device attached). Watch them with:
  adb install -r app\build\outputs\apk\debug\app-debug.apk
  adb logcat -s VERSE_PERF
(tap Juan 3:16 in the selection screen; expected stages in the order above, total well under ~200 ms now that every wait was removed)

STEP 2 (fixes applied, each verified with Select-String):
- a) O(1) jump: BookCatalog.standardChapterCounts (canonical 66-entry table, sums to 1189) + flatStart cumulative table + flatIndexOfStandard(); ReaderViewModel.flatIndexOf delegates to it (no loop over 1,189 chapters, no DB query). ZERO animateScrollToPage remain in the codebase; all pager moves (openAt jump and prev/next buttons) use scrollToPage (instant).
- b) PRELOAD: ChapterCache (data/repository/ChapterCache.kt) is the shared LRU(7) in AppContainer (container.chapterCache) + container.applicationScope (SupervisorJob+IO). SelectionViewModel.preloadTarget() fires the target chapter AND the adjacent ones into the cache BEFORE the bus publish/popBackStack; ReaderViewModel.loadChapter reads the same cache, so the reader never shows a loading page for the target chapter.
- c) Instant reference lookup: SelectionViewModel.updateQuery parses ReferenceParser.parse(q) synchronously on EVERY keystroke (no 300 ms debounce, no FTS, no Room round-trip). Only FTS keeps the 300 ms debounce; a valid reference cancels the FTS job entirely. Chapter/verse clamping in ReaderViewModel.openAt now uses an in-memory verse-count table (dao.verseCounts: one GROUP BY query loaded once at init; key = book*1000+chapter) with a DB fallback if not yet loaded.
- d) FTS: prefix terms (term*) unchanged; runFtsNow wraps repo.search in withContext(Dispatchers.IO); LIMIT 201 (200 shown + 1 truncation probe); DAO search now selects ONLY book/chapter/verse/text (VerseSearchRow projection) instead of v.*; old search job is cancelled on every query change (same semantics as flatMapLatest); single DB/FTS connection reused (no rebuild per search).
- e) Scroll-to-verse: VerseText uses state.scrollTo(target) (instant, computed from TextLayoutResult in the layout callback) instead of animateScrollTo, and logs the VERSE_PERF stage right after; the pulse (draw-phase drawBehind) runs in the same frame. No fixed delay() anywhere in the jump path (the only delay left is the 500 ms position-persistence debounce).
- f) Logs: every VERSE_NAV Log.d is now behind `if (BuildConfig.DEBUG)` (tap, publish, collect, openAt, pager jump, popBackStack); VERSE_PERF exists only in debug. No heavy work on the main thread during the jump (preload + FTS on IO scopes; Room dispatches internally). NavHost transitions are explicit 120 ms fades (enter/exit/popEnter/popExit), satisfying the <= 150 ms requirement.

STEP 3: before/after numbers pending on-device run (command above). Structurally every async wait between tap and scroll was removed: data is preloaded before the pop, the jump is a single instant scrollToPage, and the verse scroll is instant.

### Build/test results (this round)

- :app:testDebugUnitTest — BUILD SUCCESSFUL, 26 tests, 0 failures: BookSearchTest 8 (new: juan/jn/john/1 cor/1cor/mga aw/awit, accent+case, NT filter excludes Genesis, no-match empty, cumulative offset table Genesis 1 = 0, Revelation 22 = 1188, Matthew 1 = 929, John 3 = 999), NavigationBusTest 3, ReferenceParserTest 10, TagalogXmlBibleParserTest 5.
- :app:compileDebugKotlin + assembleDebug + assembleRelease — BUILD SUCCESSFUL.
- APKs: app\build\outputs\apk\debug\app-debug.apk (19.1 MB, 11/17/2026 19:50) and app\build\outputs\apk\release\app-release.apk (2.8 MB, R8 + shrinkResources, 11/17/2026 19:51). Both include ALL of this round's changes.

### Fixes made while landing this round (compile-level)

- Re-added `enum class SelectionLevel` (was lost in the SelectionViewModel rewrite) — Breadcrumb/Broken references fixed.
- SelectionViewModel constructor simplified to (container, initialSettings) — call site updated.
- BookSearch import path corrected to domain.search.
- Duplicate TAG const removed (file-level kept, companion removed); navLog() debug gate added.
- kotlinx.coroutines.flow.collect import restored in ReaderScreen (snapshotFlow collect).
- Canonical chapter-count table corrected (1Pet 5 / 2Pet 3 / Jude 1; Phil 4 / Col 4 / 1Th 5 / 2Th 3 / 1Tim 6 / 2Tit 4 / Tit 3 / Phm 1) — test caught sums 1198 -> 1190 -> 1189.

## STANDING FACTS (unchanged)

- Toolchain: Gradle 8.13, AGP 8.13.0, Kotlin 2.0.21 + Compose plugin, KSP 2.0.21-1.0.28, Room 2.6.1, Compose BOM 2024.11.00, navigation 2.8.4, compileSdk 35, minSdk 24, SDK C:\Android\Sdk (local.properties), JDK 17. unitTests.isReturnDefaultValues = true (Log.d on JVM).
- Data: streaming XmlPullParser + BOM strip; 400-verse batched import with StateFlow progress + DataStore imported flag; FTS4 over accent-folded norm_text joined via verse_id; BookCatalog + BibleVersionRegistry; manual DI in AppContainer (database, settingsRepository, bibleRepository, navigationBus, applicationScope, chapterCache, importer).
- Reader: pager over flat chapter index (beyondViewportPageCount = 1), shared LRU cache + neighbor prefetch, draw-phase pulse, superscript verse numbers + tap select + copy/share, position restore, version sheet with import-wait.
- Settings: theme tiles, 8 accents, 12-32sp slider, line spacing, view mode, search scope, About — all DataStore-persisted via BibleTheme around the NavHost.
- Tooling constraints (still in force): no edit tool on existing files (full rewrites; PowerShell here-string Set-Content); files < ~200 lines; never retry the same call twice; gradle output redirected to build.log/test.log; errors via Select-String -Pattern 'e: ' or error; JUnit XML under app\build\test-results; shell output sometimes not captured — verify from disk/logs.

## NEXT

1. User verifies on device: install debug APK, `adb logcat -s VERSE_PERF VERSE_NAV`, tap Juan 3:16 (list + grid), try search scopes ("juan" -> book section first, "juan 3:16" -> smart-jump, Mga Aklat scope -> no FTS), confirm instant jump + pulse.
2. On "continue" -> Phase 6: lintDebug, accessibility/insets/dark review, self-review, README.md, final summary. Optionally strip VERSE_NAV/VERSE_PERF logs (currently gated to debug builds, safe to ship).
