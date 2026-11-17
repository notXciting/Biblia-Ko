# Keep source file and line numbers in stack traces.
-keepattributes SourceFile,LineNumberTable

# Room: keep generated database implementations.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# XmlPullParser (used by the Bible parser)
-dontwarn org.xmlpull.**
