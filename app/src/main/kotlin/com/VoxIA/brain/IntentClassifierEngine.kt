package com.voxia.brain

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import com.voxia.utils.LanguageDetector

class IntentClassifierEngine(private val context: Context) {

    companion object {
        private const val TAG = "IntentClassifierEngine"
        private const val MODEL_FILE = "intent_classifier.tflite"
        private const val VOCAB_FILE = "intent_vocab.json"
        private const val CONFIDENCE_THRESHOLD = 0.70f

        private val KEYWORD_MAP = mapOf(
            "objet" to Intent.IDENTIFY_OBJECT,
            "tiens" to Intent.IDENTIFY_OBJECT,
            "quoi" to Intent.IDENTIFY_OBJECT,
            "vois" to Intent.IDENTIFY_OBJECT,
            "identifier" to Intent.IDENTIFY_OBJECT,
            "object" to Intent.IDENTIFY_OBJECT,
            "holding" to Intent.IDENTIFY_OBJECT,
            "what" to Intent.IDENTIFY_OBJECT,

            "lis" to Intent.READ_DOCUMENT,
            "document" to Intent.READ_DOCUMENT,
            "lettre" to Intent.READ_DOCUMENT,
            "read" to Intent.READ_DOCUMENT,
            "document" to Intent.READ_DOCUMENT,
            "text" to Intent.READ_DOCUMENT,
            "écrit" to Intent.READ_DOCUMENT,

            "appelle" to Intent.CALL_CONTACT,
            "appel" to Intent.CALL_CONTACT,
            "call" to Intent.CALL_CONTACT,
            "phone" to Intent.CALL_CONTACT,
            "contact" to Intent.CALL_CONTACT,
            "maman" to Intent.CALL_CONTACT,
            "papa" to Intent.CALL_CONTACT,

            "anglais" to Intent.SWITCH_TO_ENGLISH,
            "english" to Intent.SWITCH_TO_ENGLISH,
            "switch" to Intent.SWITCH_TO_ENGLISH,

            "français" to Intent.SWITCH_TO_FRENCH,
            "francais" to Intent.SWITCH_TO_FRENCH,
            "french" to Intent.SWITCH_TO_FRENCH,

            "histoire" to Intent.TELL_STORY,
            "raconte" to Intent.TELL_STORY,
            "story" to Intent.TELL_STORY,

            "blague" to Intent.TELL_JOKE,
            "joke" to Intent.TELL_JOKE,
            "drôle" to Intent.TELL_JOKE,
            "funny" to Intent.TELL_JOKE,

            "décris" to Intent.DESCRIBE_SURROUNDINGS,
            "décrire" to Intent.DESCRIBE_SURROUNDINGS,
            "describe" to Intent.DESCRIBE_SURROUNDINGS,
            "surroundings" to Intent.DESCRIBE_SURROUNDINGS,
            "autour" to Intent.DESCRIBE_SURROUNDINGS,

            "heure" to Intent.WHAT_TIME,
            "time" to Intent.WHAT_TIME,
            "heure" to Intent.WHAT_TIME,

            "date" to Intent.WHAT_DATE,
            "jour" to Intent.WHAT_DATE,

            "batterie" to Intent.BATTERY_STATUS,
            "battery" to Intent.BATTERY_STATUS,

            "volume" to Intent.VOLUME_UP,
            "fort" to Intent.VOLUME_UP,
            "plus" to Intent.VOLUME_UP,
            "up" to Intent.VOLUME_UP,
            "down" to Intent.VOLUME_DOWN,
            "moins" to Intent.VOLUME_DOWN,
            "bas" to Intent.VOLUME_DOWN,

            "bonjour" to Intent.GREETING,
            "salut" to Intent.GREETING,
            "hello" to Intent.GREETING,
            "hey" to Intent.GREETING,
            "bonsoir" to Intent.GREETING,

            "répète" to Intent.REPEAT,
            "répéter" to Intent.REPEAT,
            "repeat" to Intent.REPEAT,
            "encore" to Intent.REPEAT,

            "arrête" to Intent.STOP,
            "arrêter" to Intent.STOP,
            "stop" to Intent.STOP,

            "aide" to Intent.HELP,
            "help" to Intent.HELP,

            "motivation" to Intent.TELL_MOTIVATIONAL,
            "encourage" to Intent.TELL_MOTIVATIONAL,
            "motivational" to Intent.TELL_MOTIVATIONAL,

            "qui" to Intent.WHO_ARE_YOU,
            "tu es" to Intent.WHO_ARE_YOU,
            "are you" to Intent.WHO_ARE_YOU
        )
    }

    private var tfliteInterpreter: Interpreter? = null
    private var useTFLite = false

    fun loadModel(): Boolean {
        return try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            tfliteInterpreter = Interpreter(modelBuffer)
            useTFLite = true
            Log.d(TAG, "Modèle TFLite chargé")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Modèle TFLite non trouvé, utilisation du classifieur par mots-clés: ${e.message}")
            useTFLite = false
            true
        }
    }

    fun classify(text: String, detectedLanguage: Language = Language.UNKNOWN): PredictionResult {
        if (useTFLite && tfliteInterpreter != null) {
            return classifyWithTFLite(text, detectedLanguage)
        }
        return classifyWithKeywords(text, detectedLanguage)
    }

    private fun classifyWithKeywords(text: String, detectedLanguage: Language): PredictionResult {
        val words = text.lowercase().trim().split("\\s+".toRegex())
        val scores = mutableMapOf<Intent, Int>()

        for (word in words) {
            KEYWORD_MAP.forEach { (keyword, intent) ->
                if (word == keyword || text.lowercase().contains(keyword)) {
                    scores[intent] = (scores[intent] ?: 0) + 1
                }
            }
        }

        if (scores.isEmpty()) {
            val lang = if (detectedLanguage != Language.UNKNOWN) detectedLanguage
            else LanguageDetector.detect(text)
            return PredictionResult(
                intent = Intent.FALLBACK,
                language = lang,
                confidence = 0.5f
            )
        }

        val bestIntent = scores.maxByOrNull { it.value }?.key ?: Intent.FALLBACK
        val maxScore = scores.values.max()
        val totalScore = scores.values.sum()
        val confidence = (maxScore.toFloat() / totalScore.coerceAtLeast(1)).coerceAtMost(1f)

        val lang = if (detectedLanguage != Language.UNKNOWN) detectedLanguage
        else LanguageDetector.detect(text)

        val contactName = extractContact(text, lang)
        val appName = extractAppName(text, lang)
        val expression = extractExpression(text, lang)

        return PredictionResult(
            intent = bestIntent,
            language = lang,
            confidence = confidence,
            extractedContact = contactName,
            extractedAppName = appName,
            extractedExpression = expression
        )
    }

    private fun classifyWithTFLite(text: String, detectedLanguage: Language): PredictionResult {
        return classifyWithKeywords(text, detectedLanguage)
    }

    private fun extractContact(text: String, language: Language): String? {
        val lower = text.lowercase()
        val patterns = if (language == Language.FRENCH) {
            listOf("appelle ", "appel ", "contacte ")
        } else {
            listOf("call ", "phone ", "contact ")
        }
        for (pattern in patterns) {
            val idx = lower.indexOf(pattern)
            if (idx >= 0) {
                val after = text.substring(idx + pattern.length).trim()
                val name = after.split("\\s+".toRegex()).take(2).joinToString(" ")
                if (name.isNotBlank() && name.length > 1) return name
            }
        }
        return null
    }

    private fun extractAppName(text: String, language: Language): String? {
        val lower = text.lowercase()
        val patterns = if (language == Language.FRENCH) {
            listOf("ouvre ", "lance ")
        } else {
            listOf("open ")
        }
        for (pattern in patterns) {
            val idx = lower.indexOf(pattern)
            if (idx >= 0) {
                val after = text.substring(idx + pattern.length).trim()
                val app = after.split("\\s+".toRegex()).first()
                if (app.isNotBlank()) return app
            }
        }
        return null
    }

    private fun extractExpression(text: String, language: Language): String? {
        val lower = text.lowercase()
        val patterns = if (language == Language.FRENCH) {
            listOf("calcule ")
        } else {
            listOf("calculate ")
        }
        for (pattern in patterns) {
            val idx = lower.indexOf(pattern)
            if (idx >= 0) {
                return text.substring(idx + pattern.length).trim().take(50)
            }
        }
        return null
    }

    fun release() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }
}
