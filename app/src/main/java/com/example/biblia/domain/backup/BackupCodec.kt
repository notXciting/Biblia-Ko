package com.example.biblia.domain.backup

import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.UiScale
import com.example.biblia.domain.tts.TtsPrefs
import org.json.JSONArray
import org.json.JSONObject

/**
 * The backup file: plain JSON, readable and future-proof. Unknown fields are
 * ignored and missing ones fall back to defaults, so newer/older app versions
 * can read each other's files. [decode] throws [BackupFormatException] for
 * anything that isn't a Biblia Ko backup.
 */
object BackupCodec {

    const val APP_ID = "biblia-ko"
    const val FORMAT = 1

    fun encode(data: BackupData): String = JSONObject().apply {
        put("app", APP_ID)
        put("format", FORMAT)
        put("exportedAt", data.exportedAt)
        put("appVersion", data.appVersion)
        data.settings?.let { put("settings", settingsToJson(it)) }
        put("highlights", JSONArray().apply {
            data.highlights.forEach { h ->
                put(JSONObject().put("book", h.book).put("chapter", h.chapter).put("verse", h.verse)
                    .put("color", h.color).put("updatedAt", h.updatedAt))
            }
        })
        put("notes", JSONArray().apply {
            data.notes.forEach { n ->
                put(JSONObject().put("book", n.book).put("chapter", n.chapter)
                    .put("verses", JSONArray(n.verses.sorted())).put("text", n.text)
                    .put("createdAt", n.createdAt).put("updatedAt", n.updatedAt))
            }
        })
    }.toString(2)

    fun decode(raw: String): BackupData {
        val root = runCatching { JSONObject(raw.trimStart('﻿')) }.getOrNull() ?: throw BackupFormatException("not JSON")
        if (root.optString("app") != APP_ID) throw BackupFormatException("not a Biblia Ko backup")
        if (root.optInt("format", 0) !in 1..FORMAT) throw BackupFormatException("made by a newer app version")
        return BackupData(
            exportedAt = root.optLong("exportedAt"),
            appVersion = root.optString("appVersion"),
            settings = root.optJSONObject("settings")?.let(::settingsFromJson),
            highlights = objects(root.optJSONArray("highlights")).mapNotNull { h ->
                val book = h.optInt("book"); val chapter = h.optInt("chapter"); val verse = h.optInt("verse")
                val color = h.optString("color")
                if (book !in 1..66 || chapter < 1 || verse < 1 || color.isBlank()) null
                else HighlightBackup(book, chapter, verse, color, h.optLong("updatedAt"))
            },
            notes = objects(root.optJSONArray("notes")).mapNotNull { n ->
                val verses = n.optJSONArray("verses")?.let { a -> (0 until a.length()).map { a.optInt(it) } }
                    .orEmpty().filter { it > 0 }.toSortedSet()
                val book = n.optInt("book"); val chapter = n.optInt("chapter"); val text = n.optString("text").trim()
                if (book !in 1..66 || chapter < 1 || verses.isEmpty() || text.isEmpty()) null
                else NoteBackup(book, chapter, verses, text, n.optLong("createdAt"), n.optLong("updatedAt"))
            },
        )
    }

    private fun objects(array: JSONArray?): List<JSONObject> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull { array.optJSONObject(it) }

    private fun settingsToJson(s: AppSettings) = JSONObject().apply {
        put("appLanguage", s.appLanguage.name); put("appIcon", s.appIcon.name)
        put("themeMode", s.themeMode.name); put("accent", s.accent.name)
        put("fontSizeSp", s.fontSizeSp.toDouble()); put("lineSpacing", s.lineSpacing.name)
        put("verseLayout", s.verseLayout.name); put("viewMode", s.viewMode.name)
        put("searchScope", s.searchScope.name); put("uiScale", s.uiScale.toDouble())
        put("noteSort", s.noteSort.name)
        put("ttsRate", s.tts.rate.toDouble()); put("ttsPitch", s.tts.pitch.toDouble())
        put("ttsSpeakVerseNumbers", s.tts.speakVerseNumbers)
        put("ttsVoices", JSONObject(s.tts.voices))
    }

    /** Missing or unknown values keep the default; numbers are clamped to valid ranges. */
    private fun settingsFromJson(o: JSONObject): AppSettings {
        val d = AppSettings()
        val voices = o.optJSONObject("ttsVoices")
        return d.copy(
            appLanguage = enumOf(o, "appLanguage", d.appLanguage),
            appIcon = enumOf(o, "appIcon", d.appIcon),
            themeMode = enumOf(o, "themeMode", d.themeMode),
            accent = enumOf(o, "accent", d.accent),
            fontSizeSp = o.optDouble("fontSizeSp", d.fontSizeSp.toDouble()).toFloat().coerceIn(12f, 32f),
            lineSpacing = enumOf(o, "lineSpacing", d.lineSpacing),
            verseLayout = enumOf(o, "verseLayout", d.verseLayout),
            viewMode = enumOf(o, "viewMode", d.viewMode),
            searchScope = enumOf(o, "searchScope", d.searchScope),
            uiScale = UiScale.clamp(o.optDouble("uiScale", d.uiScale.toDouble()).toFloat()),
            noteSort = enumOf(o, "noteSort", d.noteSort),
            tts = TtsPrefs(
                rate = o.optDouble("ttsRate", 1.0).toFloat().coerceIn(TtsPrefs.MIN_RATE, TtsPrefs.MAX_RATE),
                pitch = o.optDouble("ttsPitch", 1.0).toFloat().coerceIn(TtsPrefs.MIN_PITCH, TtsPrefs.MAX_PITCH),
                voices = voices?.keys()?.asSequence()?.associateWith { voices.optString(it) }
                    ?.filterValues { it.isNotBlank() }.orEmpty(),
                speakVerseNumbers = o.optBoolean("ttsSpeakVerseNumbers", false),
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumOf(o: JSONObject, key: String, default: T): T =
        o.optString(key).takeIf { it.isNotEmpty() }?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}

class BackupFormatException(message: String) : Exception(message)
