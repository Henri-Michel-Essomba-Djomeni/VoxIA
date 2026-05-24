package com.voxia.brain

object LanguageDetector {

    // Mots clés français
    private val frenchKeywords = listOf(
        "lis", "quoi", "est", "que", "qui", "comment", "appelle",
        "dis", "donne", "mets", "fais", "passe", "raconte", "répète",
        "arrête", "aide", "bonjour", "salut", "bonsoir", "merci",
        "oui", "non", "heure", "date", "batterie", "volume", "calcule",
        "décris", "ouvre", "réveille", "rappelle", "silence", "encore"
    )

    // Mots clés anglais
    private val englishKeywords = listOf(
        "read", "what", "is", "call", "tell", "give", "set", "make",
        "switch", "repeat", "stop", "help", "hello", "hey", "hi",
        "thanks", "yes", "no", "time", "date", "battery", "volume",
        "calculate", "describe", "open", "wake", "remind", "again",
        "identify", "analyze", "scan", "speak", "show"
    )

    fun detect(text: String): Language {
        val words = text.lowercase().split(" ")

        var frenchScore = 0
        var englishScore = 0

        for (word in words) {
            if (frenchKeywords.contains(word)) frenchScore++
            if (englishKeywords.contains(word)) englishScore++
        }

        return when {
            frenchScore > englishScore -> Language.FRENCH
            englishScore > frenchScore -> Language.ENGLISH
            else -> Language.UNKNOWN
        }
    }

    fun isFrench(text: String): Boolean {
        return detect(text) == Language.FRENCH
    }

    fun isEnglish(text: String): Boolean {
        return detect(text) == Language.ENGLISH
    }
}