package com.VoxIA.speech

import android.content.Context
import android.util.Log
import com.VoxIA.speech.stt.SpeechLanguage
import com.VoxIA.speech.stt.VoskSTTService
import com.VoxIA.speech.stt.STTResult
import com.VoxIA.speech.tts.TTSService
import com.VoxIA.speech.tts.TTSOptions
import com.VoxIA.speech.wakeword.WakeWordService
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

    private val stt = VoskSTTService(context)
    private val tts = TTSService(context)
    private val wakeWord = WakeWordService(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var state = SpeechState.IDLE
    private var currentLanguage = SpeechLanguage.FR
    private var onStateChange: ((SpeechState) -> Unit)? = null
    private var onTranscript: ((STTResult) -> Unit)? = null
    private var onCommandDetected: ((String, SpeechLanguage) -> Unit)? = null

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
                // 2. Charger modèle STT FR par défaut
                scope.launch {
                    stt.loadModel(SpeechLanguage.FR)

                    // 3. Démarrer wake word
                    wakeWord.start()
                    wakeWord.onWakeWord {
                        onWakeWordDetected()
                    }

                    Log.d(TAG, "SpeechManager prêt ✓")
                    tts.speak("VOXIA est prêt. Dites VOXIA pour commencer.")
                    onReady()
                }
            },
            onError = {
                Log.e(TAG, "Erreur init TTS")
            }
        )
    }

    // ─── WAKE WORD DÉTECTÉ ────────────────────────────
    private fun onWakeWordDetected() {
        if (state != SpeechState.IDLE) return

        setState(SpeechState.LISTENING)
        val prompt = if (currentLanguage == SpeechLanguage.FR) "Oui ?" else "Yes?"
        tts.speak(prompt, TTSOptions(urgent = false))

        // Démarrer écoute STT
        stt.startListening(
            language = currentLanguage,
            onResult = { result ->
                onTranscript?.invoke(result)
                if (result.isFinal && result.text.isNotEmpty()) {
                    handleTranscript(result)
                }
            },
            onError = { error ->
                Log.e(TAG, "Erreur STT: $error")
                val msg = if (currentLanguage == SpeechLanguage.FR)
                    "Je n'ai pas compris. Réessayez."
                else "I didn't understand. Please try again."
                tts.speak(msg)
                setState(SpeechState.IDLE)
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

        // Envoyer la commande au Brain (Dev3)
        onCommandDetected?.invoke(result.text, currentLanguage)
        setState(SpeechState.IDLE)
    }

    // ─── RÉPONDRE VOCALEMENT ──────────────────────────
    fun speak(text: String, options: TTSOptions = TTSOptions()) {
        setState(SpeechState.SPEAKING)
        tts.speak(text, options.copy(
            onDone = {
                options.onDone?.invoke()
                setState(SpeechState.IDLE)
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
            Log.d(TAG, "Langue globale → $language")
        }
    }

    // ─── ÉTAT ─────────────────────────────────────────
    private fun setState(newState: SpeechState) {
        state = newState
        onStateChange?.invoke(newState)
        Log.d(TAG, "État → $newState")
    }

    // ─── LIBÉRER ──────────────────────────────────────
    fun release() {
        wakeWord.stop()
        stt.release()
        tts.release()
        Log.d(TAG, "SpeechManager libéré")
    }

    fun getState() = state
    fun getCurrentLanguage() = currentLanguage
}