package com.VoxIA.speech.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.VoxIA.speech.stt.SpeechLanguage
import java.util.Locale
import java.util.UUID

// Options de synthèse vocale
data class TTSOptions(
    val urgent: Boolean = false,    // alerte → voix rapide
    val slow: Boolean = false,      // lecture document → voix lente
    val onDone: (() -> Unit)? = null
)

class TTSService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_TTS"
    }

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentLanguage = SpeechLanguage.FR
    private val queue = mutableListOf<Pair<String, TTSOptions>>()
    private var isSpeaking = false

    // ─── INITIALISER ──────────────────────────────────
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

        // Listener pour savoir quand la parole est terminée
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                processQueue()
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Erreur TTS utterance")
                isSpeaking = false
                processQueue()
            }
        })
    }

    // ─── PARLER ───────────────────────────────────────
    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        if (!isReady) {
            Log.w(TAG, "TTS pas encore prêt")
            return
        }

        if (isSpeaking) {
            queue.add(Pair(text, options))
            return
        }

        _speak(text, options)
    }

    private fun _speak(text: String, options: TTSOptions) {
        isSpeaking = true

        // Vitesse selon le contexte
        val speed = when {
            options.urgent -> 1.4f
            options.slow   -> 0.75f
            else           -> 1.0f
        }

        val pitch = if (options.urgent) 1.2f else 1.0f

        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)

        val utteranceId = UUID.randomUUID().toString()

        // Callback onDone
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    isSpeaking = false
                    options.onDone?.invoke()
                    processQueue()
                }
            }
            override fun onError(id: String?) {
                isSpeaking = false
                processQueue()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        Log.d(TAG, "TTS parle: \"$text\"")
    }

    // ─── FILE D'ATTENTE ───────────────────────────────
    private fun processQueue() {
        if (queue.isEmpty()) return
        val next = queue.removeAt(0)
        _speak(next.first, next.second)
    }

    // ─── CHANGER DE LANGUE ────────────────────────────
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

    // ─── ANNONCER CHANGEMENT DE LANGUE ────────────────
    fun announceLanguageSwitch(language: SpeechLanguage) {
        val message = when (language) {
            SpeechLanguage.FR -> "Langue changée en français"
            SpeechLanguage.EN -> "Language switched to English"
        }
        setLanguage(language)
        speak(message)
    }

    // ─── ANNONCER MODE RÉSEAU ─────────────────────────
    fun announceNetworkMode(isOnline: Boolean, language: SpeechLanguage) {
        val message = when {
            isOnline && language == SpeechLanguage.FR ->
                "Connexion détectée. Mode haute qualité activé."
            isOnline && language == SpeechLanguage.EN ->
                "Connection detected. High quality mode activated."
            !isOnline && language == SpeechLanguage.FR ->
                "Mode hors ligne activé."
            else ->
                "Offline mode activated."
        }
        speak(message)
    }

    // ─── STOPPER ──────────────────────────────────────
    fun stop() {
        tts?.stop()
        isSpeaking = false
        queue.clear()
        Log.d(TAG, "TTS stoppé")
    }

    // ─── LIBÉRER ──────────────────────────────────────
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