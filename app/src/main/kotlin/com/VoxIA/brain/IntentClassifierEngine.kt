package com.voxia.brain

import android.content.Context
import android.util.Log
import com.voxia.utils.LanguageDetector

class IntentClassifierEngine(private val context: Context) {

    companion object {
        private const val TAG = "IntentClassifierEngine"
        private const val CONFIDENCE_THRESHOLD = 0.70f

        private val KEYWORD_MAP = mapOf(
            "objet" to Intent.IDENTIFY_OBJECT,
            "tiens" to Intent.IDENTIFY_OBJECT,
            "quoi" to Intent.IDENTIFY_OBJECT,
            "vois" to Intent.IDENTIFY_OBJECT,
            "identifier" to Intent.IDENTIFY_OBJECT,
            "object" to Intent.IDENTIFY_OBJECT,
            "holding" to Intent.IDENTIFY_OBJECT,

            "lis" to Intent.READ_DOCUMENT,
            "document" to Intent.READ_DOCUMENT,
            "lettre" to Intent.READ_DOCUMENT,
            "read" to Intent.READ_DOCUMENT,
            "text" to Intent.READ_DOCUMENT,
            "écrit" to Intent.READ_DOCUMENT,

            "appelle" to Intent.CALL_CONTACT,
            "appel" to Intent.CALL_CONTACT,
            "call" to Intent.CALL_CONTACT,
            "phone" to Intent.CALL_CONTACT,

            "anglais" to Intent.SWITCH_TO_ENGLISH,
            "english" to Intent.SWITCH_TO_ENGLISH,

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
            "es" to Intent.WHO_ARE_YOU,
            "you" to Intent.WHO_ARE_YOU
        )

        private val EXTRACTION_PATTERNS = mapOf(
            Intent.CALL_CONTACT to listOf(
                Regex("(?:appelle|appel|call|phone|contact)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE),
                Regex("(?:appelle|call)\\s+moi\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            ),
            Intent.OPEN_APP to listOf(
                Regex("(?:ouvre|lance|open)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            ),
            Intent.CALCULATE to listOf(
                Regex("(?:calcule|calculate)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            )
        )
    }

    fun loadModel(): Boolean {
        Log.d(TAG, "Classifieur par motifs chargé (0 Mo)")
        return true
    }

    fun classify(text: String, detectedLanguage: Language = Language.UNKNOWN): PredictionResult {
        return classifyWithPatterns(text, detectedLanguage)
    }

    private fun classifyWithPatterns(text: String, detectedLanguage: Language): PredictionResult {
        val lower = text.lowercase().trim()
        val words = lower.split("\\s+".toRegex())
        val scores = mutableMapOf<Intent, Int>()

        for (word in words) {
            KEYWORD_MAP.forEach { (keyword, intent) ->
                if (word == keyword || lower.contains(keyword)) {
                    scores[intent] = (scores[intent] ?: 0) + 1
                }
            }
        }

        if (scores.isEmpty()) {
            val lang = if (detectedLanguage != Language.UNKNOWN) detectedLanguage
            else LanguageDetector.detect(text)
            return PredictionResult(intent = Intent.FALLBACK, language = lang, confidence = 0.5f)
        }

        val bestIntent = scores.maxByOrNull { it.value }?.key ?: Intent.FALLBACK
        val maxScore = scores.values.max()
        val totalScore = scores.values.sum()
        val confidence = (maxScore.toFloat() / totalScore.coerceAtLeast(1)).coerceAtMost(1f)

        val lang = if (detectedLanguage != Language.UNKNOWN) detectedLanguage
        else LanguageDetector.detect(text)

        val extracted = extractEntities(text, bestIntent)

        return PredictionResult(
            intent = bestIntent,
            language = lang,
            confidence = confidence,
            extractedContact = extracted["contact"],
            extractedAppName = extracted["app"],
            extractedExpression = extracted["expression"]
        )
    }

    private fun extractEntities(text: String, intent: Intent): Map<String, String?> {
        val patterns = EXTRACTION_PATTERNS[intent] ?: return emptyMap()
        val result = mutableMapOf<String, String?>()

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val value = match.groupValues.getOrNull(1)?.trim()
            if (!value.isNullOrBlank()) {
                when (intent) {
                    Intent.CALL_CONTACT -> result["contact"] = value
                    Intent.OPEN_APP -> result["app"] = value
                    Intent.CALCULATE -> result["expression"] = value.take(50)
                    else -> {}
                }
                break
            }
        }
        return result
    }

    fun release() {
        Log.d(TAG, "Classifieur libéré")
    }
}
