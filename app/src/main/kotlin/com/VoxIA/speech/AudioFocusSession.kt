package com.voxia.speech

/**
 * Tracks one transient audio-focus lease for a sequence of spoken messages.
 *
 * Android integration stays in TTSService; this state holder is deliberately
 * platform-free so denial, reuse and release semantics remain JVM-testable.
 */
internal class AudioFocusSession(
    private val requestFocus: () -> Boolean,
    private val abandonFocus: () -> Unit
) {
    private var held = false

    @Synchronized
    fun acquire(): Boolean {
        if (held) return true
        held = requestFocus()
        return held
    }

    @Synchronized
    fun release() {
        if (!held) return
        held = false
        abandonFocus()
    }

    @Synchronized
    fun isHeld(): Boolean = held
}
