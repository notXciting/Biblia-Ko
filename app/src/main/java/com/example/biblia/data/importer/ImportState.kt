package com.example.biblia.data.importer

/** Import progress for one Bible version. */
data class ImportState(
    val versionId: String,
    val phase: Phase = Phase.IDLE,
    val progress: Float = 0f,
) {
    enum class Phase { IDLE, RUNNING, DONE, FAILED }

    val percent: Int get() = (progress * 100).toInt().coerceIn(0, 100)
    val isDone: Boolean get() = phase == Phase.DONE
    val isRunning: Boolean get() = phase == Phase.RUNNING
}
