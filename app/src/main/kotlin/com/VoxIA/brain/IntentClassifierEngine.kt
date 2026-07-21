package com.voxia.brain

import com.voxia.utils.LanguageDetector
import com.voxia.utils.PrivacyLog
import com.voxia.utils.TextNormalizer

class IntentClassifierEngine {

    companion object {
        private const val TAG = "IntentClassifierEngine"

        private data class Rule(val intent: Intent, val phrases: List<String>)

        private fun rule(intent: Intent, vararg phrases: String) = Rule(intent, phrases.toList())

        private val RULES = listOf(
            rule(Intent.SCAN_PRODUCT, "scanne ce produit", "scan produit", "quel est ce produit", "scan this product", "what product is this", "code barre", "barcode"),
            rule(Intent.TRANSLATE_TEXT, "traduis ce texte", "lis et traduis", "traduire le texte", "translate this text", "read and translate", "what does it say"),
            rule(Intent.IDENTIFY_OBJECT, "identifie cet objet", "quel est cet objet", "qu est ce que je tiens", "que vois tu", "identify this object", "what is this", "what am i holding"),
            rule(Intent.DESCRIBE_SURROUNDINGS, "decris autour de moi", "decris mon environnement", "decris la scene", "que vois tu autour", "describe my surroundings", "describe the scene", "what is around me"),
            rule(Intent.READ_DOCUMENT, "lis ce document", "lis ce texte", "qu est ce qui est ecrit", "lecture document", "read this document", "read this text", "read aloud"),
            rule(Intent.READ_NEXT_SEGMENT, "lis la suite", "segment suivant", "continue la lecture", "next segment", "continue reading", "read next"),
            rule(Intent.READ_PREVIOUS_SEGMENT, "segment precedent", "lis le precedent", "retour lecture", "previous segment", "read previous", "go back"),
            rule(Intent.READING_SPEED_UP, "lis plus vite", "lecture plus rapide", "parle plus vite", "read faster", "speak faster"),
            rule(Intent.READING_SPEED_DOWN, "lis plus lentement", "lecture plus lente", "parle plus lentement", "read slower", "speak slower"),
            rule(Intent.READING_SPEED_NORMAL, "vitesse normale", "lecture normale", "vitesse de lecture normale", "normal speed", "reset reading speed"),
            rule(Intent.COPY_READING_TEXT, "copie le texte", "copie la lecture", "copy text", "copy reading"),
            rule(Intent.SHARE_READING_TEXT, "partage le texte", "partage la lecture", "share text", "share reading"),
            rule(Intent.CALL_CONTACT, "appelle", "appel", "contacte", "telephone a", "call", "phone"),
            rule(Intent.SWITCH_TO_ENGLISH, "parle anglais", "passe en anglais", "switch to english", "speak english"),
            rule(Intent.SWITCH_TO_FRENCH, "parle francais", "passe en francais", "switch to french", "speak french"),
            rule(Intent.SET_REMINDER, "rappelle moi", "cree un rappel", "mets un rappel", "remind me", "set reminder"),
            rule(Intent.SET_ALARM, "mets une alarme", "regle une alarme", "reveille moi", "set alarm", "wake me"),
            rule(Intent.TELL_STORY, "raconte une histoire", "histoire", "tell me a story", "story"),
            rule(Intent.TELL_JOKE, "raconte une blague", "blague", "fais moi rire", "tell me a joke", "joke"),
            rule(Intent.READ_NOTIFICATION, "lis mes notifications", "notifications", "read my notifications", "read notifications"),
            rule(Intent.OPEN_APP, "ouvre", "lance", "demarre l application", "open", "launch"),
            rule(Intent.CALCULATE, "calcule", "combien font", "combien fait", "calculate", "what is"),
            rule(Intent.WHAT_TIME, "quelle heure", "donne moi l heure", "what time", "time is it"),
            rule(Intent.WHAT_DATE, "quelle date", "quel jour", "date sommes nous", "what date", "what day"),
            rule(Intent.BATTERY_STATUS, "niveau de batterie", "combien de batterie", "batterie", "battery level", "battery status"),
            rule(Intent.VOLUME_UP, "augmente le volume", "monte le son", "plus fort", "volume up", "louder"),
            rule(Intent.VOLUME_DOWN, "baisse le volume", "diminue le son", "moins fort", "volume down", "quieter"),
            rule(Intent.GREETING, "bonjour", "bonsoir", "salut", "hello", "hey", "good morning", "good evening"),
            rule(Intent.REPEAT, "repete", "dis le encore", "encore une fois", "repeat", "say it again"),
            rule(Intent.STOP, "arrete voxia", "tais toi", "stop voxia", "stop listening", "arrete"),
            rule(Intent.HELP, "aide", "que peux tu faire", "commandes disponibles", "help", "what can you do"),
            rule(Intent.TELL_MOTIVATIONAL, "motive moi", "encourage moi", "motivation", "motivate me", "encourage me"),
            rule(Intent.WHO_ARE_YOU, "qui es tu", "presente toi", "who are you", "what are you")
        )

        private val KEYWORDS = mapOf(
            Intent.IDENTIFY_OBJECT to setOf(
                "objet", "tiens", "quoi", "vois", "voir", "identifier", "identifie", "identify",
                "object", "holding", "main", "trouve", "trouver", "cherche", "chercher", "regarde",
                "regarder", "reconnais", "reconnaitre", "reconnait", "montre", "montrer", "capture",
                "analyse", "analyser", "scan", "scanne", "what"
            ),
            Intent.SCAN_PRODUCT to setOf("produit", "barcode", "code barre", "prix", "emballage", "scan produit"),
            Intent.TRANSLATE_TEXT to setOf("traduis", "traduire", "translate", "translation", "langue"),
            Intent.READ_DOCUMENT to setOf(
                "lis", "lire", "dit", "dis", "document", "texte", "text", "lettre", "courrier",
                "page", "journal", "livre", "book", "read", "ecrit", "ecrire", "affiche",
                "panneau", "enseigne", "menu", "menus", "etiquette"
            ),
            Intent.READ_NEXT_SEGMENT to setOf("suite", "suivant", "continue", "continuer", "next segment", "continue reading"),
            Intent.READ_PREVIOUS_SEGMENT to setOf("precedent", "precedente", "retour", "previous segment", "read previous"),
            Intent.READING_SPEED_UP to setOf("plus vite", "plus rapide", "read faster", "speak faster", "speed up"),
            Intent.READING_SPEED_DOWN to setOf("plus lentement", "plus lente", "read slower", "speak slower", "slow down"),
            Intent.READING_SPEED_NORMAL to setOf("vitesse normale", "lecture normale", "normal speed", "reset speed"),
            Intent.COPY_READING_TEXT to setOf("copie", "copy reading", "copy text", "clipboard"),
            Intent.SHARE_READING_TEXT to setOf("partage", "partager", "share reading", "share text"),
            Intent.CALL_CONTACT to setOf(
                "appelle", "appel", "appeler", "appellez", "telephone", "tel", "call", "phone",
                "contact", "contacte", "contacter", "joindre", "compose"
            ),
            Intent.SWITCH_TO_ENGLISH to setOf("anglais", "english", "switch", "anglaise", "anglois"),
            Intent.SWITCH_TO_FRENCH to setOf("francais", "french", "francaise"),
            Intent.SET_REMINDER to setOf("rappelle", "rappel", "remind", "reminder"),
            Intent.SET_ALARM to setOf("alarme", "reveil", "reveille", "alarm"),
            Intent.TELL_STORY to setOf("histoire", "raconte", "raconter", "story", "conte", "conter", "fable", "legende", "recit"),
            Intent.TELL_JOKE to setOf("blague", "joke", "marrant", "comique", "fun", "funny", "humour", "rire", "ris"),
            Intent.DESCRIBE_SURROUNDINGS to setOf(
                "decris", "decrire", "describe", "surroundings", "environnement", "autour",
                "alentours", "piece", "salle", "dehors", "scene"
            ),
            Intent.READ_NOTIFICATION to setOf("notification", "notifications", "message", "messages"),
            Intent.OPEN_APP to setOf("ouvre", "lance", "open", "demarre", "application", "app"),
            Intent.CALCULATE to setOf("calcule", "calculer", "calculate", "combien", "plus", "moins", "fois", "divise"),
            Intent.WHAT_TIME to setOf("heure", "heures", "time", "horloge", "temps", "moment"),
            Intent.WHAT_DATE to setOf("date", "jour", "mois", "annee", "aujourd", "today", "calendar", "calendrier", "semaine"),
            Intent.BATTERY_STATUS to setOf("batterie", "battery", "charge", "energie", "pourcentage", "power", "pile", "niveau"),
            Intent.VOLUME_UP to setOf("volume", "fort", "plus", "up", "augmente", "augmenter", "monte", "monter", "haut", "son"),
            Intent.VOLUME_DOWN to setOf("down", "moins", "bas", "baisse", "baisser", "descends", "descendre", "silence", "doucement"),
            Intent.GREETING to setOf("bonjour", "salut", "hello", "hey", "bonsoir", "coucou", "hi", "salam"),
            Intent.REPEAT to setOf("repete", "repeter", "repeat", "encore", "redis", "redire", "autre", "ressayez"),
            Intent.STOP to setOf("arrete", "arreter", "stop", "tais", "taisez", "ferme", "fermer", "suffit", "calme", "assez", "quitte"),
            Intent.HELP to setOf("aide", "aider", "help", "capable", "peux", "peut", "sais", "fonctions", "commandes", "possibilites"),
            Intent.TELL_MOTIVATIONAL to setOf("motivation", "motiver", "motivational", "encourage", "encourager", "inspire", "inspirer", "force", "courage"),
            Intent.WHO_ARE_YOU to setOf("qui es tu", "who are you", "presente", "nom", "name")
        )
    }

    fun loadModel(): Boolean {
        PrivacyLog.d(TAG, "Classifieur rules baseline actif (${RULES.size} intentions, ${KEYWORDS.values.sumOf { it.size }} mots clés)")
        return true
    }

    fun classify(text: String, detectedLanguage: Language = Language.UNKNOWN): PredictionResult {
        val normalized = TextNormalizer.normalize(text)
        val language = detectedLanguage.takeUnless { it == Language.UNKNOWN } ?: LanguageDetector.detect(text)
        if (normalized.isBlank()) return PredictionResult(Intent.FALLBACK, language, 0f)

        val scored = buildList {
            RULES.forEach { rule ->
                val best = rule.phrases.maxOfOrNull { phraseScore(normalized, it) } ?: 0
                if (best > 0) add(rule.intent to best)
            }
            KEYWORDS.forEach { (intent, keywords) ->
                val score = keywordScore(normalized, keywords)
                if (score > 0) add(intent to score)
            }
        }
            .groupBy({ it.first }, { it.second })
            .map { (intent, scores) -> intent to scores.max() }
            .sortedByDescending { it.second }

        val best = scored.firstOrNull()
            ?: return PredictionResult(Intent.FALLBACK, language, 0f)
        val secondScore = scored.getOrNull(1)?.second ?: 0
        val confidence = when {
            best.second >= 100 -> 0.99f
            best.second >= 20 && secondScore == 0 -> 0.92f
            best.second >= 10 && secondScore == 0 -> 0.85f
            secondScore == 0 -> 0.70f
            else -> (best.second.toFloat() / (best.second + secondScore)).coerceIn(0.5f, 0.95f)
        }
        val time = extractTime(normalized)

        return PredictionResult(
            intent = best.first,
            language = language,
            confidence = confidence,
            extractedContact = if (best.first == Intent.CALL_CONTACT) {
                extractAfterCommand(normalized, listOf("appelle", "appel", "contacte", "telephone a", "call", "phone"))
            } else null,
            extractedAppName = if (best.first == Intent.OPEN_APP) {
                extractAfterCommand(normalized, listOf("ouvre", "lance", "demarre l application", "open", "launch"))
            } else null,
            extractedExpression = if (best.first == Intent.CALCULATE) {
                extractAfterCommand(normalized, listOf("calcule", "combien font", "combien fait", "calculate", "what is"))
            } else null,
            extractedHour = time?.first,
            extractedMinute = time?.second,
            extractedDurationMinutes = extractDurationMinutes(normalized)
        )
    }

    private fun phraseScore(text: String, rawPhrase: String): Int {
        val phrase = TextNormalizer.normalize(rawPhrase)
        if (text == phrase) return 100
        val padded = " $text "
        val candidate = " $phrase "
        if (padded.contains(candidate)) return 20 + phrase.split(' ').size * 4
        return 0
    }

    private fun keywordScore(text: String, keywords: Set<String>): Int {
        val padded = " $text "
        return keywords.sumOf { rawKeyword ->
            val keyword = TextNormalizer.normalize(rawKeyword)
            when {
                keyword.contains(' ') && padded.contains(" $keyword ") -> 8 + keyword.split(' ').size
                padded.contains(" $keyword ") -> 4
                else -> 0
            }
        }
    }

    private fun extractAfterCommand(text: String, commands: List<String>): String? {
        commands.sortedByDescending { it.length }.forEach { command ->
            val marker = "$command "
            val index = text.indexOf(marker)
            if (index >= 0) {
                return text.substring(index + marker.length)
                    .trim()
                    .removeSuffix(" s il te plait")
                    .removeSuffix(" please")
                    .takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractTime(text: String): Pair<Int, Int>? {
        val match = Regex("(?:a |at )?(\\d{1,2})(?:\\s*(?:h|:|heures?|hours?)\\s*(\\d{1,2})?)").find(text)
            ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        return if (hour in 0..23 && minute in 0..59) hour to minute else null
    }

    private fun extractDurationMinutes(text: String): Int? {
        val match = Regex("(\\d+)\\s*(minutes?|mins?|heures?|hours?)").find(text) ?: return null
        val value = match.groupValues[1].toIntOrNull() ?: return null
        return if (match.groupValues[2].startsWith("h")) value * 60 else value
    }

    fun release() = Unit
}
