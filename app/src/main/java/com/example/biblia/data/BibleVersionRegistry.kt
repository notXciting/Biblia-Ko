package com.example.biblia.data

/**
 * One installed/importable Bible translation. [copyright] (when non-blank)
 * overrides whatever the XML header carries, since headers are inconsistent
 * across files (some have no copyright, some pack it into the name).
 */
data class BibleVersion(
    val id: String,
    val shortName: String,
    val fullName: String,
    val assetPath: String,
    val language: String,
    val copyright: String = "",
    val formatId: String = FORMAT_TAGALOG_XML,
) {
    /** English Bibles show English book names (and UI, when following the version). */
    val isEnglish: Boolean get() = language == "English"

    /** BCP-47 tag for the app UI when the language setting follows the version. */
    val uiLocaleTag: String get() = if (isEnglish) "en" else "fil"

    companion object {
        const val FORMAT_TAGALOG_XML = "tagalog-xml"
    }
}

/**
 * Registry of all Bible versions the app knows about. Adding a new version
 * requires ONLY: dropping the XML into `app/src/main/assets/bibles/` and
 * adding one entry here. Everything else (import, search, switching) is
 * generic. Each version is imported on first selection.
 */
object BibleVersionRegistry {

    val all: List<BibleVersion> = listOf(
        BibleVersion(
            id = "mbb12",
            shortName = "MBB12",
            fullName = "Tagalog 2012 (Magandang Balita Biblia)",
            assetPath = "bibles/tagalog2012.xml",
            language = "Tagalog",
            copyright = "New Testament © 2012 Philippine Bible Society. Old Testament © 2005 Philippine Bible Society.",
        ),
        BibleVersion(
            id = "asd",
            shortName = "ASD",
            fullName = "Tagalog 2015 (Ang Salita ng Dios)",
            assetPath = "bibles/Tagalog2015Bible.xml",
            language = "Tagalog",
            copyright = "Ang Salita ng Dios (Tagalog Contemporary Bible). Copyright © 2009, 2011, 2014, 2015 by Biblica, Inc.",
        ),
        BibleVersion(
            id = "amp",
            shortName = "AMP",
            fullName = "Amplified Bible",
            assetPath = "bibles/EnglishAmplifiedBible.xml",
            language = "English",
            copyright = "Copyright © 2015 by The Lockman Foundation, La Habra, CA 90631. All rights reserved.",
        ),
        BibleVersion(
            id = "esv",
            shortName = "ESV",
            fullName = "English Standard Version",
            assetPath = "bibles/EnglishESVBible.xml",
            language = "English",
            copyright = "The Holy Bible, English Standard Version. ESV® Text Edition: 2016. Copyright © 2001 by Crossway Bibles.",
        ),
        BibleVersion(
            id = "niv",
            shortName = "NIV",
            fullName = "New International Version",
            assetPath = "bibles/EnglishNIVBible.xml",
            language = "English",
            copyright = "Holy Bible, New International Version®, NIV®. Copyright © 1973, 1978, 1984, 2011 by Biblica, Inc.®",
        ),
    )

    val default: BibleVersion get() = all.first()

    fun byId(id: String): BibleVersion =
        all.firstOrNull { it.id == id } ?: default
}
