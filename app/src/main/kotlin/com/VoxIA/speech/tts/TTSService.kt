package com.voxia.speech.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.voxia.language.OfflineLanguagePolicy
import com.voxia.speech.AudioFocusEvent
import com.voxia.speech.AudioFocusInterruptionPolicy
import com.voxia.speech.AudioFocusSession
import com.voxia.speech.stt.SpeechLanguage
import com.voxia.utils.PrivacyLog
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class TerminalCallbackRegistry {
    private data class Callbacks(
        val onDone: (() -> Unit)?,
        val onError: (() -> Unit)?
    )

    private val callbacks = ConcurrentHashMap<String, Callbacks>()

    fun register(utteranceId: String, onDone: (() -> Unit)?, onError: (() -> Unit)?) {
        if (onDone != null || onError != null) callbacks[utteranceId] = Callbacks(onDone, onError)
    }

    fun complete(utteranceId: String?, succeeded: Boolean) {
        val terminalCallbacks = utteranceId?.let(callbacks::remove) ?: return
        if (succeeded) terminalCallbacks.onDone?.invoke() else terminalCallbacks.onError?.invoke()
    }

    fun clear() {
        callbacks.clear()
    }
}

data class TTSOptions(
    val urgent: Boolean = false,
    val slow: Boolean = false,
    val onDone: (() -> Unit)? = null,
    val onError: (() -> Unit)? = null
)

@Suppress("OVERRIDE_DEPRECATION")
class TTSService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_TTS"
        private const val MIN_SPEECH_RATE = 0.7f
        private const val MAX_SPEECH_RATE = 1.4f
        private const val MIN_EFFECTIVE_RATE = 0.5f
        private const val MAX_EFFECTIVE_RATE = 2.0f
    }

    @Volatile
    private var tts: TextToSpeech? = null
    @Volatile
    private var isReady = false
    @Volatile
    private var initializationFailed = false
    private var currentLanguage = SpeechLanguage.FR
    private var speechRateMultiplier = 1.0f
    private val queue = ArrayDeque<Pair<String, TTSOptions>>()
    private var isSpeaking = false
    private var currentUtteranceId: String? = null
    private val callbacks = TerminalCallbackRegistry()
    private var onInterrupted: (() -> Unit)? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        val event = when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> AudioFocusEvent.GAIN
            AudioManager.AUDIOFOCUS_LOSS -> AudioFocusEvent.LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusEvent.TRANSIENT_LOSS
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AudioFocusEvent.DUCK
            else -> AudioFocusEvent.UNKNOWN
        }
        if (AudioFocusInterruptionPolicy.shouldInterrupt(event)) {
            interruptForAudioFocusLoss()
        }
    }
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(false)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener(audioFocusListener)
        .build()
    private val audioFocusSession = AudioFocusSession(
        requestFocus = {
            audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        },
        abandonFocus = { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    )

    fun init(onReady: () -> Unit, onError: () -> Unit, onInterrupted: () -> Unit = {}) {
        this.onInterrupted = onInterrupted
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setLanguage(SpeechLanguage.FR)
                isReady = true
                initializationFailed = false
                tts?.setAudioAttributes(audioAttributes)
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
                finishUtterance(utteranceId, succeeded = true)
            }

            override fun onError(utteranceId: String?) {
                PrivacyLog.e(TAG, "Erreur TTS utterance")
                finishUtterance(utteranceId, succeeded = false)
            }
        })
    }

    @Synchronized
    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        if (!isReady) {
            if (initializationFailed) {
                options.onError?.invoke()
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

    @Synchronized
    private fun speakInternal(text: String, options: TTSOptions) {
        if (!audioFocusSession.acquire()) {
            PrivacyLog.e(TAG, "Focus audio refusé; utterance abandonnée")
            options.onError?.invoke()
            if (!isSpeaking) processQueueOrReleaseFocus()
            return
        }
        isSpeaking = true

        val optionRate = when {
            options.urgent -> 1.4f
            options.slow -> 0.75f
            else -> 1.0f
        }
        val effectiveRate = (speechRateMultiplier * optionRate)
            .coerceIn(MIN_EFFECTIVE_RATE, MAX_EFFECTIVE_RATE)
        tts?.setSpeechRate(effectiveRate)
        tts?.setPitch(if (options.urgent) 1.2f else 1.0f)

        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        callbacks.register(utteranceId, options.onDone, options.onError)

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            PrivacyLog.e(TAG, "Le moteur TTS a refusé l'utterance")
            finishUtterance(utteranceId, succeeded = false)
            return
        }
        PrivacyLog.d(TAG, "TTS parle: chars=${text.length}")
    }

    @Synchronized
    private fun finishUtterance(utteranceId: String?, succeeded: Boolean) {
        if (utteranceId == null || utteranceId != currentUtteranceId) return
        currentUtteranceId = null
        isSpeaking = false
        callbacks.complete(utteranceId, succeeded)
        if (!isSpeaking) processQueueOrReleaseFocus()
    }

    @Synchronized
    private fun processQueue() {
        if (queue.isEmpty()) return
        val next = queue.removeFirst()
        speakInternal(next.first, next.second)
    }

    @Synchronized
    private fun processQueueOrReleaseFocus() {
        if (queue.isEmpty()) audioFocusSession.release() else processQueue()
    }

    @Synchronized
    fun setLanguage(language: SpeechLanguage) {
        val effectiveLanguage = OfflineLanguagePolicy.normalize(language)
        val locale = when (effectiveLanguage) {
            SpeechLanguage.FR -> Locale.FRENCH
            SpeechLanguage.EN -> Locale.ENGLISH
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED) {
            PrivacyLog.e(TAG, "Langue $effectiveLanguage non supportée")
        } else {
            currentLanguage = effectiveLanguage
            PrivacyLog.d(TAG, "Langue TTS -> $effectiveLanguage")
        }
    }

    fun announceLanguageSwitch(language: SpeechLanguage) {
        setLanguage(language)
        speak(
            if (language == SpeechLanguage.FR) {
                "VOXIA est déjà en français."
            } else {
                "La version hors ligne fonctionne actuellement uniquement en français."
            }
        )
    }

    @Synchronized
    fun stop() {
        currentUtteranceId = null
        isSpeaking = false
        queue.clear()
        callbacks.clear()
        tts?.stop()
        audioFocusSession.release()
        PrivacyLog.d(TAG, "TTS stoppé")
    }

    @Synchronized
    private fun interruptForAudioFocusLoss() {
        if (!isSpeaking && queue.isEmpty()) return
        PrivacyLog.d(TAG, "TTS interrompu par une perte de focus audio")
        stop()
        onInterrupted?.invoke()
    }

    @Synchronized
    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
        PrivacyLog.d(TAG, "TTS libéré")
    }

    @Synchronized
    fun isSpeaking() = isSpeaking
    @Synchronized
    fun hasPendingSpeech() = isSpeaking || queue.isNotEmpty()
    fun isAvailable() = isReady
    fun getCurrentLanguage() = currentLanguage

    @Synchronized
    fun adjustSpeechRate(delta: Float): Float {
        speechRateMultiplier = (speechRateMultiplier + delta).coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        return speechRateMultiplier
    }

    @Synchronized
    fun resetSpeechRate(): Float {
        speechRateMultiplier = 1.0f
        return speechRateMultiplier
    }

    @Synchronized
    fun getSpeechRateMultiplier() = speechRateMultiplier
}
