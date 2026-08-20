package com.voxia.speech.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voxia.language.OfflineLanguagePolicy
import com.voxia.utils.PrivacyLog
import java.util.Locale

enum class SpeechLanguage { FR, EN }

data class STTResult(
    val text: String,
    val language: SpeechLanguage,
    val isFinal: Boolean,
    val confidence: Float = 0f
)

internal object STTResultDispatcher {
    const val EMPTY_FINAL_RESULT_ERROR =
        "Je n'ai pas compris. Réessayez en parlant distinctement."

    fun dispatchFinal(
        matches: List<String>,
        confidences: FloatArray?,
        language: SpeechLanguage,
        onResult: (STTResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val recognized = matches.withIndex().firstOrNull { it.value.isNotBlank() }
        if (recognized == null) {
            onError(EMPTY_FINAL_RESULT_ERROR)
            return
        }

        onResult(
            STTResult(
                text = recognized.value,
                language = language,
                isFinal = true,
                confidence = confidences?.getOrNull(recognized.index) ?: 0f
            )
        )
    }
}

class AndroidSpeechRecognizerSTTService(private val context: Context) {

    companion object {
        private const val TAG = "AndroidSpeechRecognizerSTT"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var currentLanguage = SpeechLanguage.FR
    private var isListening = false
    private var fallbackAttempted = false

    suspend fun loadModels(): Boolean = loadModel(currentLanguage)

    suspend fun loadModel(language: SpeechLanguage): Boolean {
        currentLanguage = OfflineLanguagePolicy.normalize(language)
        return SpeechRecognizer.isRecognitionAvailable(context).also { available ->
            if (!available) {
                PrivacyLog.w(TAG, "SpeechRecognizer Android indisponible sur cet appareil")
            }
        }
    }

    fun startListening(
        language: SpeechLanguage = currentLanguage,
        onResult: (STTResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening) {
            PrivacyLog.w(TAG, "Déjà en écoute")
            return
        }

        val effectiveLanguage = OfflineLanguagePolicy.normalize(language)
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Reconnaissance vocale Android indisponible")
                return@post
            }

            currentLanguage = effectiveLanguage
            fallbackAttempted = false
            launchRecognizer(
                language = effectiveLanguage,
                useOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context),
                preferOffline = true,
                onResult = onResult,
                onError = onError
            )
        }
    }

    private fun launchRecognizer(
        language: SpeechLanguage,
        useOnDevice: Boolean,
        preferOffline: Boolean,
        onResult: (STTResult) -> Unit,
        onError: (String) -> Unit
    ) {
        recognizer?.destroy()
        recognizer = if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = recognizer?.apply {
            setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        // 12 = langue non prise en charge, 13 = pack de langue indisponible.
                        // Certains appareils annoncent le moteur local avant que le pack FR soit installé.
                        if ((error == 12 || error == 13) && !fallbackAttempted) {
                            fallbackAttempted = true
                            PrivacyLog.w(TAG, "Pack vocal local indisponible ($error), repli vers le moteur système")
                            mainHandler.postDelayed({
                                launchRecognizer(
                                    language = language,
                                    useOnDevice = false,
                                    preferOffline = false,
                                    onResult = onResult,
                                    onError = onError
                                )
                            }, 200L)
                            return
                        }
                        onError(humanReadableError(error))
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val confidences = results
                            ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        STTResultDispatcher.dispatchFinal(
                            matches = matches,
                            confidences = confidences,
                            language = language,
                            onResult = onResult,
                            onError = onError
                        )
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val text = matches.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            onResult(STTResult(text, language, isFinal = false))
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        recognizer?.startListening(buildRecognizerIntent(language, preferOffline))
        PrivacyLog.d(
            TAG,
            "Écoute SpeechRecognizer démarrée en $language " +
                "(local=$useOnDevice, horsLigne=$preferOffline)"
        )
    }

    fun stopListening() {
        mainHandler.post {
            recognizer?.stopListening()
            isListening = false
            PrivacyLog.d(TAG, "Écoute arrêtée")
        }
    }

    suspend fun switchLanguage(language: SpeechLanguage) {
        if (isListening) stopListening()
        currentLanguage = OfflineLanguagePolicy.normalize(language)
        PrivacyLog.d(TAG, "Langue STT -> $currentLanguage")
    }

    fun release() {
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
            isListening = false
            PrivacyLog.d(TAG, "STT libéré")
        }
    }

    fun isCurrentlyListening() = isListening

    fun getCurrentLanguage() = currentLanguage

    private fun buildRecognizerIntent(language: SpeechLanguage, preferOffline: Boolean): Intent {
        val locale = when (language) {
            SpeechLanguage.FR -> Locale.FRENCH
            SpeechLanguage.EN -> Locale.ENGLISH
        }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
        }
    }

    private fun humanReadableError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Le microphone n'a pas pu être utilisé."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "L'autorisation du microphone est requise."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "La reconnaissance vocale n'est pas disponible hors ligne et le réseau est inaccessible."
        SpeechRecognizer.ERROR_NO_MATCH -> STTResultDispatcher.EMPTY_FINAL_RESULT_ERROR
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "La reconnaissance vocale est occupée. Réessayez."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Je n'ai entendu aucune parole."
        12, 13 -> "Le pack vocal français n'est pas installé sur ce téléphone."
        else -> "La reconnaissance vocale a rencontré une erreur ($error)."
    }
}

@Deprecated(
    message = "Use AndroidSpeechRecognizerSTTService. This adapter wraps Android SpeechRecognizer, not Vosk.",
    replaceWith = ReplaceWith("AndroidSpeechRecognizerSTTService")
)
typealias VoskSTTService = AndroidSpeechRecognizerSTTService
