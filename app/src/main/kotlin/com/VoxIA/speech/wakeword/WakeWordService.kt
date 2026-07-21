package com.voxia.speech.wakeword

import android.content.Context
import com.voxia.utils.PrivacyLog

class WakeWordService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_WakeWord"
    }

    private val listeners = mutableListOf<() -> Unit>()
    private var isActive = false

    fun start(onError: ((String) -> Unit)? = null): Boolean {
        PrivacyLog.i(
            TAG,
            "Wake word non embarqué dans l'alpha; utiliser le bouton Parler"
        )
        isActive = false
        return false
    }

    fun pause() {
        isActive = false
    }

    fun resume() {
        // No-op: wake word intentionally disabled until an evaluated engine is selected.
    }

    fun stop() {
        isActive = false
        PrivacyLog.d(TAG, "WakeWord arrêté")
    }

    fun onWakeWord(callback: () -> Unit): () -> Unit {
        listeners.add(callback)
        return { listeners.remove(callback) }
    }

    fun isRunning(): Boolean = isActive
}
