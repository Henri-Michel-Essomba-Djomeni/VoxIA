package com.voxia.utils

/**
 * Résout une réponse vocale libre ("oui", "annule", "d'accord bien sûr") en oui/non/ambigu.
 * Utilisé pour confirmer les actions sensibles (appel, alarme, ouverture d'app) avant exécution.
 * Ne reconnaît que le premier mot utile pour éviter qu'une phrase contenant accidentellement
 * "non" plus loin (ex: "oui mais non attends") ne soit mal interprétée dans le mauvais sens.
 */
object ConfirmationParser {
    // "d'accord" -> TextNormalizer tourne l'apostrophe en espace ("d accord"), d'où
    // la forme à deux mots ci-dessous en plus de la forme collée, au cas où l'apostrophe
    // n'a pas été insérée par le moteur STT.
    private val YES = listOf(
        "oui", "ouais", "d accord", "daccord", "ok", "okay", "confirme", "confirmer",
        "yes", "yeah", "yep", "sure", "confirm"
    )
    private val NO = listOf(
        "non", "annule", "annuler", "stop", "arrete", "arreter",
        "no", "nope", "cancel"
    )

    fun parse(text: String): Boolean? {
        val normalized = TextNormalizer.normalize(text)
        if (normalized.isBlank()) return null
        return when {
            matchesAny(normalized, YES) -> true
            matchesAny(normalized, NO) -> false
            else -> null
        }
    }

    /** Vrai si [normalized] est exactement une des [phrases], ou commence par l'une d'elles suivie d'un espace. */
    private fun matchesAny(normalized: String, phrases: List<String>): Boolean =
        phrases.any { phrase -> normalized == phrase || normalized.startsWith("$phrase ") }
}
