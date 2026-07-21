package com.voxia.speech

import android.content.Context
import com.voxia.utils.PrivacyLog
import com.voxia.speech.stt.AndroidSpeechRecognizerSTTService
import com.voxia.speech.stt.SpeechLanguage
import com.voxia.speech.stt.STTResult
import com.voxia.speech.tts.TTSService
import com.voxia.speech.tts.TTSOptions
import com.voxia.speech.wakeword.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// État global du module vocal
enum class SpeechState {
    IDLE,       // en attente du wake word
    LISTENING,  // écoute active
    PROCESSING, // traitement en cours
    SPEAKING    // réponse vocale
}

class SpeechManager(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_SpeechManager"
    }

    private val stt = AndroidSpeechRecognizerSTTService(context)
    private val tts = TTSService(context)
    private val wakeWord = WakeWordService(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var state = SpeechState.IDLE
    private var currentLanguage = SpeechLanguage.FR
    private var onStateChange: ((SpeechState) -> Unit)? = null
    private var onTranscript: ((STTResult) -> Unit)? = null
    private var onCommandDetected: ((String, SpeechLanguage) -> Unit)? = null
    private var wakeWordEnabled = false
    private var recognitionInitialized = false

    // ─── INITIALISER TOUT ─────────────────────────────
    fun init(
        onReady: () -> Unit,
        onStateChange: (SpeechState) -> Unit,
        onTranscript: (STTResult) -> Unit,
        onCommandDetected: (String, SpeechLanguage) -> Unit
    ) {
        this.onStateChange = onStateChange
        this.onTranscript = onTranscript
        this.onCommandDetected = onCommandDetected

        // 1. Init TTS
        tts.init(
            onReady = {
                initializeRecognition(onReady)
            },
            onError = {
                PrivacyLog.e(TAG, "Erreur init TTS")
                initializeRecognition(onReady)
            }
        )
    }

    private fun initializeRecognition(onReady: () -> Unit) {
        if (recognitionInitialized) return
        recognitionInitialized = true
        scope.launch {
            val sttReady = stt.loadModel(SpeechLanguage.FR)
            wakeWord.onWakeWord { onWakeWordDetected() }
            wakeWordEnabled = wakeWord.start()
            val readyMessage = when {
                sttReady && wakeWordEnabled -> "VOXIA est prêt. Dites VOXIA ou utilisez le bouton Parler."
                sttReady -> "VOXIA est prêt. Appuyez sur le bouton Parler."
                else -> "La reconnaissance vocale n'est pas disponible. Utilisez les boutons de l'écran."
            }
            if (tts.isAvailable()) tts.speak(readyMessage)
            onReady()
        }
    }

    // ─── WAKE WORD DÉTECTÉ ────────────────────────────
    private fun onWakeWordDetected() {
        listenForCommand()
    }

    fun listenForCommand() {
        if (state != SpeechState.IDLE) return

        wakeWord.pause()
        val prompt = if (currentLanguage == SpeechLanguage.FR) "Oui ?" else "Yes?"
        if (tts.isAvailable()) {
            setState(SpeechState.SPEAKING)
            tts.speak(prompt, TTSOptions(onDone = { startCommandRecognition() }))
        } else startCommandRecognition()
    }

    private fun startCommandRecognition() {
        setState(SpeechState.LISTENING)
        stt.startListening(
                language = currentLanguage,
                onResult = { result ->
                    onTranscript?.invoke(result)
                    if (result.isFinal && result.text.isNotEmpty()) handleTranscript(result)
                },
                onError = { error ->
                    PrivacyLog.e(TAG, "Erreur STT: $error")
                    val msg = if (currentLanguage == SpeechLanguage.FR)
                        "Je n'ai pas compris. Appuyez sur Parler pour réessayer."
                    else "I didn't understand. Tap Speak to try again."
                    speak(msg)
                }
            )
    }

    // ─── TRAITER LA TRANSCRIPTION ─────────────────────
    private fun handleTranscript(result: STTResult) {
        setState(SpeechState.PROCESSING)
        stt.stopListening()

        val text = result.text.lowercase()

        // Détection commande switch de langue
        if (text.contains("switch to english") || text.contains("parle anglais")) {
            switchLanguage(SpeechLanguage.EN)
            setState(SpeechState.IDLE)
            return
        }
        if (text.contains("parle français") || text.contains("switch to french")) {
            switchLanguage(SpeechLanguage.FR)
            setState(SpeechState.IDLE)
            return
        }

        // Envoyer la commande au Brain
        onCommandDetected?.invoke(result.text, currentLanguage)
        // Ne pas override SPEAKING (action synchrone comme speakTime)
        // ni laisser en LISTENING si l'action est asynchrone (vision)
        if (state == SpeechState.PROCESSING) {
            setState(SpeechState.IDLE)
        }
    }

    // ─── RÉPONDRE VOCALEMENT ──────────────────────────
    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        setState(SpeechState.SPEAKING)
        tts.speak(text, options.copy(
            onDone = {
                options.onDone?.invoke()
                setState(SpeechState.IDLE)
                resumeWakeWordIfAvailable()
            }
        ))
    }

    // ─── CHANGER DE LANGUE ────────────────────────────
    fun switchLanguage(language: SpeechLanguage) {
        scope.launch {
            stt.switchLanguage(language)
            tts.setLanguage(language)
            currentLanguage = language
            tts.announceLanguageSwitch(language)
            PrivacyLog.d(TAG, "Langue globale -> $language")
        }
    }

    // ─── ÉTAT ─────────────────────────────────────────
    private fun setState(newState: SpeechState) {
        val oldState = state
        state = newState

        if (newState == SpeechState.LISTENING && oldState == SpeechState.IDLE) {
            wakeWord.pause()
        } else if (newState == SpeechState.IDLE && oldState != SpeechState.IDLE) {
            wakeWord.resume()
        }

        onStateChange?.invoke(newState)
        PrivacyLog.d(TAG, "État -> $newState")
    }

    // ─── LIBÉRER ──────────────────────────────────────
    fun release() {
        wakeWord.stop()
        stt.release()
        tts.release()
        PrivacyLog.d(TAG, "SpeechManager libéré")
    }

    fun getState() = state
    fun getCurrentLanguage() = currentLanguage

    fun cancelListening() {
        stt.stopListening()
        tts.stop()
        setState(SpeechState.IDLE)
        resumeWakeWordIfAvailable()
    }

    private fun resumeWakeWordIfAvailable() {
        if (wakeWordEnabled && !wakeWord.isRunning()) wakeWord.start()
    }
}
