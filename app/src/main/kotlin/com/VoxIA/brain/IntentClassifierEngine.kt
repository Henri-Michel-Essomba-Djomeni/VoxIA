package com.voxia.brain

import android.content.Context
import android.util.Log
import com.voxia.utils.LanguageDetector

class IntentClassifierEngine(private val context: Context) {

    companion object {
        private const val TAG = "IntentClassifierEngine"
        private const val CONFIDENCE_THRESHOLD = 0.70f

        private val KEYWORD_MAP = mapOf(
            // ── IDENTIFIER OBJET ──
            "objet" to Intent.IDENTIFY_OBJECT,
            "tiens" to Intent.IDENTIFY_OBJECT,
            "quoi" to Intent.IDENTIFY_OBJECT,
            "vois" to Intent.IDENTIFY_OBJECT,
            "voir" to Intent.IDENTIFY_OBJECT,
            "identifier" to Intent.IDENTIFY_OBJECT,
            "identifie" to Intent.IDENTIFY_OBJECT,
            "identify" to Intent.IDENTIFY_OBJECT,
            "object" to Intent.IDENTIFY_OBJECT,
            "holding" to Intent.IDENTIFY_OBJECT,
            "main" to Intent.IDENTIFY_OBJECT,
            "trouve" to Intent.IDENTIFY_OBJECT,
            "trouver" to Intent.IDENTIFY_OBJECT,
            "cherche" to Intent.IDENTIFY_OBJECT,
            "chercher" to Intent.IDENTIFY_OBJECT,
            "regarde" to Intent.IDENTIFY_OBJECT,
            "regarder" to Intent.IDENTIFY_OBJECT,
            "reconnais" to Intent.IDENTIFY_OBJECT,
            "reconnaître" to Intent.IDENTIFY_OBJECT,
            "reconnait" to Intent.IDENTIFY_OBJECT,
            "montre" to Intent.IDENTIFY_OBJECT,
            "montrer" to Intent.IDENTIFY_OBJECT,
            "capture" to Intent.IDENTIFY_OBJECT,
            "analyse" to Intent.IDENTIFY_OBJECT,
            "analyser" to Intent.IDENTIFY_OBJECT,
            "scan" to Intent.IDENTIFY_OBJECT,
            "scanne" to Intent.IDENTIFY_OBJECT,
            "what" to Intent.IDENTIFY_OBJECT,

            // ── LIRE DOCUMENT ──
            "lis" to Intent.READ_DOCUMENT,
            "lire" to Intent.READ_DOCUMENT,
            "dit" to Intent.READ_DOCUMENT,
            "dis" to Intent.READ_DOCUMENT,
            "document" to Intent.READ_DOCUMENT,
            "texte" to Intent.READ_DOCUMENT,
            "text" to Intent.READ_DOCUMENT,
            "lettre" to Intent.READ_DOCUMENT,
            "courrier" to Intent.READ_DOCUMENT,
            "page" to Intent.READ_DOCUMENT,
            "journal" to Intent.READ_DOCUMENT,
            "livre" to Intent.READ_DOCUMENT,
            "book" to Intent.READ_DOCUMENT,
            "read" to Intent.READ_DOCUMENT,
            "écrit" to Intent.READ_DOCUMENT,
            "ecrit" to Intent.READ_DOCUMENT,
            "écrire" to Intent.READ_DOCUMENT,
            "affiche" to Intent.READ_DOCUMENT,
            "panneau" to Intent.READ_DOCUMENT,
            "enseigne" to Intent.READ_DOCUMENT,
            "menus" to Intent.READ_DOCUMENT,
            "menu" to Intent.READ_DOCUMENT,
            "étiquette" to Intent.READ_DOCUMENT,
            "etiquette" to Intent.READ_DOCUMENT,

            // ── APPELER ──
            "appelle" to Intent.CALL_CONTACT,
            "appel" to Intent.CALL_CONTACT,
            "appeler" to Intent.CALL_CONTACT,
            "appellez" to Intent.CALL_CONTACT,
            "téléphone" to Intent.CALL_CONTACT,
            "telephone" to Intent.CALL_CONTACT,
            "tel" to Intent.CALL_CONTACT,
            "call" to Intent.CALL_CONTACT,
            "phone" to Intent.CALL_CONTACT,
            "contact" to Intent.CALL_CONTACT,
            "contacte" to Intent.CALL_CONTACT,
            "contacter" to Intent.CALL_CONTACT,
            "joindre" to Intent.CALL_CONTACT,
            "compose" to Intent.CALL_CONTACT,

            // ── CHANGEMENT LANGUE ──
            "anglais" to Intent.SWITCH_TO_ENGLISH,
            "english" to Intent.SWITCH_TO_ENGLISH,
            "switch" to Intent.SWITCH_TO_ENGLISH,
            "anglaise" to Intent.SWITCH_TO_ENGLISH,
            "en" to Intent.SWITCH_TO_ENGLISH,
            "anglois" to Intent.SWITCH_TO_ENGLISH,

            "français" to Intent.SWITCH_TO_FRENCH,
            "francais" to Intent.SWITCH_TO_FRENCH,
            "french" to Intent.SWITCH_TO_FRENCH,
            "française" to Intent.SWITCH_TO_FRENCH,
            "francaise" to Intent.SWITCH_TO_FRENCH,
            "fr" to Intent.SWITCH_TO_FRENCH,

            // ── HISTOIRE ──
            "histoire" to Intent.TELL_STORY,
            "raconte" to Intent.TELL_STORY,
            "raconter" to Intent.TELL_STORY,
            "story" to Intent.TELL_STORY,
            "conte" to Intent.TELL_STORY,
            "conter" to Intent.TELL_STORY,
            "fable" to Intent.TELL_STORY,
            "légende" to Intent.TELL_STORY,
            "legende" to Intent.TELL_STORY,
            "récit" to Intent.TELL_STORY,
            "recit" to Intent.TELL_STORY,

            // ── BLAGUE ──
            "blague" to Intent.TELL_JOKE,
            "joke" to Intent.TELL_JOKE,
            "marrant" to Intent.TELL_JOKE,
            "comique" to Intent.TELL_JOKE,
            "fun" to Intent.TELL_JOKE,
            "funny" to Intent.TELL_JOKE,
            "humour" to Intent.TELL_JOKE,
            "rire" to Intent.TELL_JOKE,
            "ris" to Intent.TELL_JOKE,

            // ── DÉCRIRE ENVIRONNEMENT ──
            "décris" to Intent.DESCRIBE_SURROUNDINGS,
            "décrire" to Intent.DESCRIBE_SURROUNDINGS,
            "decris" to Intent.DESCRIBE_SURROUNDINGS,
            "decrire" to Intent.DESCRIBE_SURROUNDINGS,
            "describe" to Intent.DESCRIBE_SURROUNDINGS,
            "surroundings" to Intent.DESCRIBE_SURROUNDINGS,
            "environnement" to Intent.DESCRIBE_SURROUNDINGS,
            "autour" to Intent.DESCRIBE_SURROUNDINGS,
            "alentours" to Intent.DESCRIBE_SURROUNDINGS,
            "ou" to Intent.DESCRIBE_SURROUNDINGS,
            "suis" to Intent.DESCRIBE_SURROUNDINGS,
            "trouve" to Intent.DESCRIBE_SURROUNDINGS,
            "pièce" to Intent.DESCRIBE_SURROUNDINGS,
            "piece" to Intent.DESCRIBE_SURROUNDINGS,
            "salle" to Intent.DESCRIBE_SURROUNDINGS,
            "dehors" to Intent.DESCRIBE_SURROUNDINGS,

            // ── HEURE ──
            "heure" to Intent.WHAT_TIME,
            "heures" to Intent.WHAT_TIME,
            "time" to Intent.WHAT_TIME,
            "horloge" to Intent.WHAT_TIME,
            "temps" to Intent.WHAT_TIME,
            "moment" to Intent.WHAT_TIME,

            // ── DATE ──
            "date" to Intent.WHAT_DATE,
            "jour" to Intent.WHAT_DATE,
            "mois" to Intent.WHAT_DATE,
            "année" to Intent.WHAT_DATE,
            "annee" to Intent.WHAT_DATE,
            "aujourd" to Intent.WHAT_DATE,
            "today" to Intent.WHAT_DATE,
            "calendar" to Intent.WHAT_DATE,
            "calendrier" to Intent.WHAT_DATE,
            "semaine" to Intent.WHAT_DATE,

            // ── BATTERIE ──
            "batterie" to Intent.BATTERY_STATUS,
            "battery" to Intent.BATTERY_STATUS,
            "charge" to Intent.BATTERY_STATUS,
            "énergie" to Intent.BATTERY_STATUS,
            "energie" to Intent.BATTERY_STATUS,
            "pourcentage" to Intent.BATTERY_STATUS,
            "power" to Intent.BATTERY_STATUS,
            "pile" to Intent.BATTERY_STATUS,
            "niveau" to Intent.BATTERY_STATUS,

            // ── VOLUME ──
            "volume" to Intent.VOLUME_UP,
            "fort" to Intent.VOLUME_UP,
            "plus" to Intent.VOLUME_UP,
            "up" to Intent.VOLUME_UP,
            "augmente" to Intent.VOLUME_UP,
            "augmenter" to Intent.VOLUME_UP,
            "monte" to Intent.VOLUME_UP,
            "monter" to Intent.VOLUME_UP,
            "haut" to Intent.VOLUME_UP,
            "son" to Intent.VOLUME_UP,
            "down" to Intent.VOLUME_DOWN,
            "moins" to Intent.VOLUME_DOWN,
            "bas" to Intent.VOLUME_DOWN,
            "baisse" to Intent.VOLUME_DOWN,
            "baisser" to Intent.VOLUME_DOWN,
            "descends" to Intent.VOLUME_DOWN,
            "descendre" to Intent.VOLUME_DOWN,
            "silence" to Intent.VOLUME_DOWN,
            "doucement" to Intent.VOLUME_DOWN,

            // ── SALUTATION ──
            "bonjour" to Intent.GREETING,
            "salut" to Intent.GREETING,
            "hello" to Intent.GREETING,
            "hey" to Intent.GREETING,
            "bonsoir" to Intent.GREETING,
            "bon matin" to Intent.GREETING,
            "coucou" to Intent.GREETING,
            "hi" to Intent.GREETING,
            "salam" to Intent.GREETING,
            "bonne" to Intent.GREETING,

            // ── RÉPÉTER ──
            "répète" to Intent.REPEAT,
            "répéter" to Intent.REPEAT,
            "repete" to Intent.REPEAT,
            "repeter" to Intent.REPEAT,
            "repeat" to Intent.REPEAT,
            "encore" to Intent.REPEAT,
            "redis" to Intent.REPEAT,
            "redire" to Intent.REPEAT,
            "autre" to Intent.REPEAT,
            "ressayez" to Intent.REPEAT,

            // ── STOP ──
            "arrête" to Intent.STOP,
            "arrêter" to Intent.STOP,
            "arrete" to Intent.STOP,
            "arreter" to Intent.STOP,
            "stop" to Intent.STOP,
            "tais" to Intent.STOP,
            "taisez" to Intent.STOP,
            "ferme" to Intent.STOP,
            "fermer" to Intent.STOP,
            "suffit" to Intent.STOP,
            "calmez" to Intent.STOP,
            "calme" to Intent.STOP,
            "assez" to Intent.STOP,
            "quitte" to Intent.STOP,

            // ── AIDE ──
            "aide" to Intent.HELP,
            "aider" to Intent.HELP,
            "help" to Intent.HELP,
            "capable" to Intent.HELP,
            "peux" to Intent.HELP,
            "peut" to Intent.HELP,
            "sais" to Intent.HELP,
            "fonctions" to Intent.HELP,
            "commandes" to Intent.HELP,
            "possibilités" to Intent.HELP,

            // ── MOTIVATION ──
            "motivation" to Intent.TELL_MOTIVATIONAL,
            "motiver" to Intent.TELL_MOTIVATIONAL,
            "motivational" to Intent.TELL_MOTIVATIONAL,
            "encourage" to Intent.TELL_MOTIVATIONAL,
            "encourager" to Intent.TELL_MOTIVATIONAL,
            "inspire" to Intent.TELL_MOTIVATIONAL,
            "inspirer" to Intent.TELL_MOTIVATIONAL,
            "force" to Intent.TELL_MOTIVATIONAL,
            "courage" to Intent.TELL_MOTIVATIONAL,

            // ── QUI ES-TU ──
            "qui" to Intent.WHO_ARE_YOU,
            "es" to Intent.WHO_ARE_YOU,
            "you" to Intent.WHO_ARE_YOU,
            "t'es" to Intent.WHO_ARE_YOU,
            "nom" to Intent.WHO_ARE_YOU,
            "appelle" to Intent.WHO_ARE_YOU,
            "name" to Intent.WHO_ARE_YOU,
            "are" to Intent.WHO_ARE_YOU,
            "présente" to Intent.WHO_ARE_YOU,
            "presente" to Intent.WHO_ARE_YOU,
            "ta" to Intent.WHO_ARE_YOU
        )

        private val EXTRACTION_PATTERNS = mapOf(
            Intent.CALL_CONTACT to listOf(
                Regex("(?:appelle|appel|call|phone|contact|téléphone|telephone|contacte|compose|joindre)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE),
                Regex("(?:appelle|call)\\s+(?:moi|nous)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE),
                Regex("(?:au|aux)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            ),
            Intent.OPEN_APP to listOf(
                Regex("(?:ouvre|lance|open|démarre|demarre|lance|lance)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            ),
            Intent.CALCULATE to listOf(
                Regex("(?:calcule|calculer|calculate|combien\\s+font|combiens?\\s+fait)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
            )
        )
    }

    fun loadModel(): Boolean {
        Log.d(TAG, "Classifieur par motifs chargé (${KEYWORD_MAP.size} entrées)")
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
            val value = match.groupValues.getOrNull(1)?.trim()?.replace(Regex("(mon|ma|mes|le|la|les|à|a|de|du|des)$", RegexOption.IGNORE_CASE), "")?.trim()
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
