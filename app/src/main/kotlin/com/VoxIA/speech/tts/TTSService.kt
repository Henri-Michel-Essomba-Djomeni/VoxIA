package com.VoxIA.speech.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.VoxIA.speech.stt.SpeechLanguage
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TTSOptions(
    val urgent: Boolean = false,
    val slow: Boolean = false,
    val onDone: (() -> Unit)? = null
)

class TTSService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_TTS"
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentLanguage = SpeechLanguage.FR
    private val queue = ArrayDeque<Pair<String, TTSOptions>>()
    private var isSpeaking = false
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    fun init(onReady: () -> Unit, onError: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setLanguage(SpeechLanguage.FR)
                isReady = true
                Log.d(TAG, "TTS initialisé ✓")
                onReady()
            } else {
                Log.e(TAG, "Erreur initialisation TTS")
                onError()
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                val cb = utteranceId?.let { callbacks.remove(it) }
                cb?.invoke()
                processQueue()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Erreur TTS utterance: $utteranceId")
                utteranceId?.let { callbacks.remove(it) }
                isSpeaking = false
                processQueue()
            }
        })
    }

    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        if (!isReady) {
            Log.w(TAG, "TTS pas encore prêt")
            return
        }

        if (isSpeaking) {
            queue.addLast(Pair(text, options))
            return
        }

        speakInternal(text, options)
    }

    private fun speakInternal(text: String, options: TTSOptions) {
        isSpeaking = true

        tts?.setSpeechRate(
            when {
                options.urgent -> 1.4f
                options.slow -> 0.75f
                else -> 1.0f
            }
        )
        tts?.setPitch(if (options.urgent) 1.2f else 1.0f)

        val utteranceId = UUID.randomUUID().toString()
        if (options.onDone != null) {
            callbacks[utteranceId] = options.onDone
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d(TAG, "TTS: \"$text\"")
    }

    private fun processQueue() {
        if (queue.isEmpty()) return
        val next = queue.removeFirst()
        speakInternal(next.first, next.second)
    }

    fun setLanguage(language: SpeechLanguage) {
        val locale = when (language) {
            SpeechLanguage.FR -> Locale.FRENCH
            SpeechLanguage.EN -> Locale.ENGLISH
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Langue $language non supportée")
        } else {
            currentLanguage = language
            Log.d(TAG, "Langue TTS → $language")
        }
    }

    fun announceLanguageSwitch(language: SpeechLanguage) {
        val message = when (language) {
            SpeechLanguage.FR -> "Langue changée en français"
            SpeechLanguage.EN -> "Language switched to English"
        }
        setLanguage(language)
        speak(message)
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        queue.clear()
        callbacks.clear()
        Log.d(TAG, "TTS stoppé")
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
        Log.d(TAG, "TTS libéré")
    }

    fun isSpeaking() = isSpeaking
    fun getCurrentLanguage() = currentLanguage
}
