package com.VoxIA.speech.wakeword

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

class WakeWordService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_WakeWord"
        private const val SAMPLE_RATE = 16000
        private const val WAKE_WORD = "voxia"
        private const val MODEL_FR = "vosk-model-small-fr"
    }

    private var model: Model? = null
    private var sharedModel: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var isActive = false
    private var isPaused = false
    private var restartPending = false
    private val listeners = mutableListOf<() -> Unit>()

    fun setModel(model: Model) {
        sharedModel = model
    }

    fun start(onError: ((String) -> Unit)? = null) {
        if (isActive) return

        try {
            val modelPath = File(context.filesDir, MODEL_FR).absolutePath
            if (!File(modelPath).exists()) {
                Log.w(TAG, "Modèle non trouvé — fallback mode DEV")
                startDevMode()
                return
            }

            model = sharedModel ?: Model(modelPath)
            recognizer = Recognizer(model, SAMPLE_RATE.toFloat(), "[\"$WAKE_WORD\"]")
            speechService = SpeechService(recognizer, SAMPLE_RATE.toFloat())

            isActive = true
            startListening()
            Log.d(TAG, "Wake word Vosk actif — en attente de '$WAKE_WORD'")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur init: ${e.message}")
            startDevMode()
        }
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            if (isPaused) return
            val text = extractText(hypothesis ?: "", "partial")
            if (text.contains(WAKE_WORD, ignoreCase = true)) {
                Log.d(TAG, "Wake word détecté: '$text'")
                onDetected()
            }
        }

        override fun onResult(hypothesis: String?) {
            val text = extractText(hypothesis ?: "", "text")
            if (text.contains(WAKE_WORD, ignoreCase = true)) {
                Log.d(TAG, "Wake word détecté (final): '$text'")
                onDetected()
                return
            }
            scheduleRestart()
        }

        override fun onFinalResult(hypothesis: String?) {
            scheduleRestart()
        }

        override fun onError(exception: Exception?) {
            Log.e(TAG, "Erreur: ${exception?.message}")
            scheduleRestart(500)
        }

        override fun onTimeout() {
            scheduleRestart()
        }
    }

    private fun startListening() {
        if (isPaused || !isActive) return
        speechService?.stop()
        speechService?.startListening(listener)
    }

    private fun scheduleRestart(delayMs: Long = 0) {
        if (restartPending || !isActive || isPaused) return
        restartPending = true
        speechService?.stop()
        Thread {
            if (delayMs > 0) Thread.sleep(delayMs)
            restartPending = false
            if (isActive && !isPaused) startListening()
        }.start()
    }

    private fun onDetected() {
        speechService?.stop()
        isPaused = true
        listeners.forEach { it.invoke() }
    }

    private fun extractText(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "").trim().lowercase()
        } catch (_: Exception) { "" }
    }

    fun pause() {
        isPaused = true
        speechService?.stop()
    }

    fun resume() {
        isPaused = false
        if (isActive) startListening()
    }

    fun onWakeWord(callback: () -> Unit): () -> Unit {
        listeners.add(callback)
        return { listeners.remove(callback) }
    }

    private fun startDevMode() {
        isActive = true
        Thread {
            while (isActive) {
                Thread.sleep(30000)
                if (isActive && !isPaused) {
                    Log.d(TAG, "Wake word simulé")
                    listeners.forEach { it.invoke() }
                }
            }
        }.start()
    }

    fun stop() {
        isActive = false
        speechService?.stop()
        speechService?.shutdown()
        recognizer?.close()
        model?.close()
        speechService = null
        recognizer = null
        if (sharedModel == null) model?.close()
        model = null
        listeners.clear()
        Log.d(TAG, "WakeWord arrêté")
    }

    fun isRunning() = isActive
}
