package com.voxia.speech.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.voxia.speech.stt.SpeechLanguage
import com.voxia.utils.PrivacyLog
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TTSOptions(
    val urgent: Boolean = false,
    val slow: Boolean = false,
    val onDone: (() -> Unit)? = null
)

@Suppress("OVERRIDE_DEPRECATION")
class TTSService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_TTS"
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var initializationFailed = false
    private var currentLanguage = SpeechLanguage.FR
    private val queue = ArrayDeque<Pair<String, TTSOptions>>()
    private var isSpeaking = false
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    fun init(onReady: () -> Unit, onError: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setLanguage(SpeechLanguage.FR)
                isReady = true
                initializationFailed = false
                PrivacyLog.d(TAG, "TTS initialisé")
                onReady()
                processQueue()
            } else {
                initializationFailed = true
                PrivacyLog.e(TAG, "Erreur initialisation TTS")
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
                utteranceId?.let { callbacks.remove(it) }
                PrivacyLog.e(TAG, "Erreur TTS utterance")
                isSpeaking = false
                processQueue()
            }
        })
    }

    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        if (!isReady) {
            if (initializationFailed) {
                options.onDone?.invoke()
                return
            }
            queue.add(Pair(text, options))
            PrivacyLog.d(TAG, "TTS pas encore prêt; message mis en attente")
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
        PrivacyLog.d(TAG, "TTS parle: chars=${text.length}")
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
            PrivacyLog.e(TAG, "Langue $language non supportée")
        } else {
            currentLanguage = language
            PrivacyLog.d(TAG, "Langue TTS -> $language")
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
        PrivacyLog.d(TAG, "TTS stoppé")
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
        PrivacyLog.d(TAG, "TTS libéré")
    }

    fun isSpeaking() = isSpeaking
    fun isAvailable() = isReady
    fun getCurrentLanguage() = currentLanguage
}
