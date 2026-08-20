package com.voxia.language

import com.voxia.brain.Language
import com.voxia.speech.stt.SpeechLanguage

/**
 * Politique de langue de la V0 hors ligne.
 *
 * L'interface, la reconnaissance, la synthese et les reponses applicatives
 * restent en francais tant que le parcours anglais complet n'est pas livre et
 * valide hors ligne.
 */
object OfflineLanguagePolicy {
    val brainLanguage: Language = Language.FRENCH
    val speechLanguage: SpeechLanguage = SpeechLanguage.FR

    fun normalize(language: Language): Language =
        if (language == brainLanguage) language else brainLanguage

    fun normalize(language: SpeechLanguage): SpeechLanguage =
        if (language == speechLanguage) language else speechLanguage

    fun isSupported(language: Language): Boolean = language == brainLanguage
}
