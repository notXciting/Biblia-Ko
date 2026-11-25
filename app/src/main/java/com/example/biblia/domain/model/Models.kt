package com.example.biblia.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which language book names are shown in. Follows the selected Bible version
 * (an English Bible shows "Exodus", the Tagalog one "Exodo"). Snapshot state:
 * every composable that reads [BookInfo.displayName] recomposes when it flips.
 */
object BookNameLanguage {
    var english by mutableStateOf(false)
}

/** One book of the Bible (1..66). */
@Immutable
data class BookInfo(
    val number: Int,
    val tagalogName: String,
    val englishName: String,
    val abbreviation: String,
    val testament: Testament,
    val section: BookSection,
) {
    /** Name in the current Bible version's language. */
    val displayName: String get() = if (BookNameLanguage.english) englishName else tagalogName

    /** The name in the other language (shown as a secondary hint). */
    val alternateName: String get() = if (BookNameLanguage.english) tagalogName else englishName
}

enum class Testament { OLD, NEW }

/** Canonical sections used for filtering and grouping. */
enum class BookSection(val bookFrom: Int, val bookTo: Int) {
    LAW(1, 5),
    HISTORY(6, 17),
    POETRY(18, 22),
    MAJOR_PROPHETS(23, 27),
    MINOR_PROPHETS(28, 39),
    GOSPELS(40, 43),
    ACTS(44, 44),
    PAULS_LETTERS(45, 57),
    GENERAL_LETTERS(58, 65),
    REVELATION(66, 66);

    fun contains(book: Int): Boolean = book in bookFrom..bookTo

    companion object {
        fun of(book: Int): BookSection = entries.first { it.contains(book) }
    }
}

/** A single verse. Empty [text] means the translation omits this verse. */
@Immutable
data class Verse(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
) {
    val isEmpty: Boolean get() = text.isBlank()
}

@Immutable
data class ChapterContent(
    val versionId: String,
    val book: Int,
    val chapter: Int,
    val verses: List<Verse>,
) {
    /** Combined verses of this chapter: text verse -> last verse it covers. */
    val spanEnds: Map<Int, Int> by lazy { VerseSpans.ends(verses) }
}

/** Copyright/translation metadata stored per version at import time. */
@Immutable
data class VersionMeta(
    val versionId: String,
    val translation: String,
    val copyright: String,
    val link: String,
)

@Immutable
data class VerseReference(val book: Int, val chapter: Int, val verse: Int? = null)

/** How filters are grouped in the filter sheet. */
enum class FilterGroup { GENERAL, OLD_TESTAMENT, NEW_TESTAMENT, THEMES }

/**
 * Book filter for the selection screen (book list, book search and full-text
 * search). Every filter holds several books: single-book "sections" (Acts,
 * Revelation) were folded into meaningful groups (Narrative, Prophecy).
 * Theme filters may be non-contiguous and span both testaments.
 */
enum class BookFilter(val group: FilterGroup, books: Iterable<Int>?) {
    ALL(FilterGroup.GENERAL, null),
    OLD_TESTAMENT(FilterGroup.GENERAL, 1..39),
    NEW_TESTAMENT(FilterGroup.GENERAL, 40..66),

    LAW(FilterGroup.OLD_TESTAMENT, 1..5),
    HISTORY(FilterGroup.OLD_TESTAMENT, 6..17),
    POETRY(FilterGroup.OLD_TESTAMENT, 18..22),
    MAJOR_PROPHETS(FilterGroup.OLD_TESTAMENT, 23..27),
    MINOR_PROPHETS(FilterGroup.OLD_TESTAMENT, 28..39),

    GOSPELS(FilterGroup.NEW_TESTAMENT, 40..43),
    PAULS_LETTERS(FilterGroup.NEW_TESTAMENT, 45..57),
    GENERAL_LETTERS(FilterGroup.NEW_TESTAMENT, 58..65),
    ALL_LETTERS(FilterGroup.NEW_TESTAMENT, 45..65),

    /** Story books: Genesis–Esther, the Gospels and Acts. */
    NARRATIVE(FilterGroup.THEMES, (1..17) + (40..44)),
    /** All prophets plus Revelation. */
    PROPHECY(FilterGroup.THEMES, (23..39) + 66),
    /** Ephesians, Philippians, Colossians, Philemon. */
    PRISON_LETTERS(FilterGroup.THEMES, listOf(49, 50, 51, 57)),
    /** 1 Timothy, 2 Timothy, Titus. */
    PASTORAL_LETTERS(FilterGroup.THEMES, 54..56),
    /** Obadiah, Philemon, 2 John, 3 John, Jude. */
    ONE_CHAPTER(FilterGroup.THEMES, listOf(31, 57, 63, 64, 65));

    /** Book numbers in canonical order; null = every book. */
    val books: List<Int>? = books?.sorted()

    private val bookSet: Set<Int>? = books?.toSet()

    val bookCount: Int get() = books?.size ?: 66

    fun matches(book: Int): Boolean = bookSet == null || book in bookSet
}

/**
 * Reading themes, in picker order (System first). Light family: LIGHT, SEPIA,
 * SAGE. Dark family: DARK, SLATE (dim blue-gray), MIDNIGHT (pure black, OLED).
 */
enum class ThemeMode(private val dark: Boolean?) {
    SYSTEM(null),
    LIGHT(false),
    SEPIA(false),
    SAGE(false),
    DARK(true),
    SLATE(true),
    MIDNIGHT(true);

    /** Whether this theme renders dark ([systemDark] decides for SYSTEM). */
    fun isDark(systemDark: Boolean): Boolean = dark ?: systemDark
}

enum class LineSpacing(val multiplier: Float) {
    COMPACT(1.15f),
    NORMAL(1.4f),
    RELAXED(1.75f),
}

enum class ViewMode { LIST, GRID }

/** How the reader lays out verses: one flowing paragraph, or one verse per paragraph. */
enum class VerseLayout { PARAGRAPH, VERSE_BY_VERSE }

/**
 * Accent colors, each with two hand-tuned tones of the same hue: [light] for
 * light themes and [dark] for dark themes. Every tone is verified (WCAG) at
 * >= 4.5:1 as text on every theme background and behind button labels, and
 * >= 3:1 for verse numbers on every highlight color. ORANGE was removed: an
 * orange dark enough for light backgrounds is brown (failed at 2.6-3.1:1).
 */
enum class AccentColor(val light: Long, val dark: Long) {
    BLUE(0xFF1A5FB4, 0xFF8AB4F8),
    INDIGO(0xFF3949AB, 0xFFA9B3F5),
    PURPLE(0xFF6A1B9A, 0xFFD1A3F0),
    PINK(0xFFA3134F, 0xFFF59CC0),
    RED(0xFFB3261E, 0xFFF2A09A),
    AMBER(0xFF8A5300, 0xFFF4C25B),
    BROWN(0xFF6D4C41, 0xFFD8B4A3),
    GREEN(0xFF2E6B30, 0xFF86CF8A),
    TEAL(0xFF00695C, 0xFF4DD0C1),
    GRAPHITE(0xFF455A64, 0xFFB4C3CC);

    fun tone(dark: Boolean): Long = if (dark) this.dark else light
}

/** Search scope in the selection screen (remembered in DataStore). */
enum class SearchScope { ALL, BOOKS, TEXT }

/** Highlighter colors (ARGB). Stored by name in the user database. */
enum class HighlightColor(val argb: Long) {
    YELLOW(0xFFFFD84D),
    GREEN(0xFF7DDB8A),
    BLUE(0xFF7CC0FF),
    PINK(0xFFFF9EC4),
    PURPLE(0xFFC4A8FF),
}

/** Launcher icon variant; [aliasName] is the activity-alias in the manifest. */
enum class AppIcon(val aliasName: String) {
    SUNRISE("IconSunrise"),
    NIGHT("IconNight"),
    SKY("IconSky"),
}

/** UI language: follow the selected Bible version, or a fixed language. */
enum class AppLanguage { FOLLOW_VERSION, TAGALOG, ENGLISH }

/** All persisted user preferences (DataStore-backed). */
@Immutable
data class AppSettings(
    val appLanguage: AppLanguage = AppLanguage.FOLLOW_VERSION,
    val appIcon: AppIcon = AppIcon.SUNRISE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.BLUE,
    val fontSizeSp: Float = 18f,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val verseLayout: VerseLayout = VerseLayout.PARAGRAPH,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchScope: SearchScope = SearchScope.ALL,
    val versionId: String = "mbb12",
    val book: Int = 1,
    val chapter: Int = 1,
    val verse: Int = 0,
    val tts: com.example.biblia.domain.tts.TtsPrefs = com.example.biblia.domain.tts.TtsPrefs(),
    /** Whole-app UI size multiplier (Settings > "Laki ng app"); 1 = phone default. */
    val uiScale: Float = 1f,
    /** Order of the notes list ("Mga Tala"). */
    val noteSort: NoteSort = NoteSort.RECENT,
    /** Cross-references in the verse panel (Settings > "Mga kaugnay na talata"). */
    val showCrossRefs: Boolean = true,
    val crossRefFont: CrossRefFont = CrossRefFont.SERIF_ITALIC,
)

/** How cross-reference text is set in the verse panel. */
enum class CrossRefFont { SERIF_ITALIC, SANS }

/** Range and step of the app UI size setting. */
object UiScale {
    const val MIN = 0.8f
    const val MAX = 1.3f
    const val STEP = 0.05f
    const val DEFAULT = 1f

    /** Clamps and snaps to the 5% grid (so 1.0 stays exactly 1.0). */
    fun clamp(value: Float): Float = (Math.round(value.coerceIn(MIN, MAX) / STEP) * STEP).let {
        Math.round(it * 100f) / 100f
    }
}
