package com.VoxIA.speech.stt

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.json.JSONObject

// Langue supportée
enum class SpeechLanguage { FR, EN }

// Résultat de transcription
data class STTResult(
    val text: String,
    val language: SpeechLanguage,
    val isFinal: Boolean,
    val confidence: Float = 0f
)

class VoskSTTService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_STT"
        // Chemins des modèles dans assets/
        private const val MODEL_FR = "vosk-model-small-fr"
        private const val MODEL_EN = "vosk-model-small-en"
    }

    private var modelFR: Model? = null
    private var modelEN: Model? = null
    private var speechService: SpeechService? = null
    private var currentLanguage = SpeechLanguage.FR
    private var isListening = false

    // ─── CHARGER LES MODÈLES ──────────────────────────
    suspend fun loadModels() {
        Log.d(TAG, "Chargement modèle FR...")
        modelFR = Model(getModelPath(MODEL_FR))
        Log.d(TAG, "Modèle FR chargé ✓")

        Log.d(TAG, "Chargement modèle EN...")
        modelEN = Model(getModelPath(MODEL_EN))
        Log.d(TAG, "Modèle EN chargé ✓")
    }

    // ─── CHARGER UN SEUL MODÈLE (économie RAM) ────────
    suspend fun loadModel(language: SpeechLanguage) {
        when (language) {
            SpeechLanguage.FR -> {
                if (modelFR == null) {
                    Log.d(TAG, "Chargement modèle FR...")
                    modelFR = Model(getModelPath(MODEL_FR))
                    Log.d(TAG, "Modèle FR chargé ✓")
                }
                // Libérer EN si chargé
                modelEN?.close()
                modelEN = null
            }
            SpeechLanguage.EN -> {
                if (modelEN == null) {
                    Log.d(TAG, "Chargement modèle EN...")
                    modelEN = Model(getModelPath(MODEL_EN))
                    Log.d(TAG, "Modèle EN chargé ✓")
                }
                // Libérer FR si chargé
                modelFR?.close()
                modelFR = null
            }
        }
        currentLanguage = language
    }

    // ─── DÉMARRER L'ÉCOUTE ────────────────────────────
    fun startListening(
        language: SpeechLanguage = currentLanguage,
        onResult: (STTResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening) {
            Log.w(TAG, "Déjà en écoute")
            return
        }

        val model = when (language) {
            SpeechLanguage.FR -> modelFR
            SpeechLanguage.EN -> modelEN
        }

        if (model == null) {
            onError("Modèle $language non chargé")
            return
        }

        try {
            val recognizer = Recognizer(model, 16000.0f)

            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(object : RecognitionListener {

                // Résultat partiel (streaming temps réel)
                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = extractText(it, "partial")
                        if (text.isNotEmpty()) {
                            onResult(STTResult(
                                text = text,
                                language = language,
                                isFinal = false
                            ))
                        }
                    }
                }

                // Résultat final
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = extractText(it, "text")
                        if (text.isNotEmpty()) {
                            onResult(STTResult(
                                text = text,
                                language = language,
                                isFinal = true
                            ))
                        }
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let {
                        val text = extractText(it, "text")
                        if (text.isNotEmpty()) {
                            onResult(STTResult(
                                text = text,
                                language = language,
                                isFinal = true
                            ))
                        }
                    }
                    isListening = false
                }

                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Erreur STT: ${exception?.message}")
                    onError(exception?.message ?: "Erreur inconnue")
                    isListening = false
                }

                override fun onTimeout() {
                    Log.w(TAG, "Timeout STT")
                    isListening = false
                }
            })

            isListening = true
            Log.d(TAG, "Écoute démarrée en $language")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur démarrage: ${e.message}")
            onError(e.message ?: "Erreur démarrage STT")
        }
    }

    // ─── ARRÊTER L'ÉCOUTE ─────────────────────────────
    fun stopListening() {
        speechService?.stop()
        isListening = false
        Log.d(TAG, "Écoute arrêtée")
    }

    // ─── CHANGER DE LANGUE (< 2s) ─────────────────────
    suspend fun switchLanguage(language: SpeechLanguage) {
        val wasListening = isListening
        if (wasListening) stopListening()

        loadModel(language)
        currentLanguage = language
        Log.d(TAG, "Langue changée vers $language")
    }

    // ─── LIBÉRER LA MÉMOIRE ───────────────────────────
    fun release() {
        stopListening()
        speechService?.shutdown()
        modelFR?.close()
        modelEN?.close()
        modelFR = null
        modelEN = null
        Log.d(TAG, "Mémoire libérée")
    }

    // ─── UTILITAIRES ──────────────────────────────────
    private fun getModelPath(modelName: String): String {
        return "${context.filesDir.absolutePath}/$modelName"
    }

    private fun extractText(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun isCurrentlyListening() = isListening
    fun getCurrentLanguage() = currentLanguage
}