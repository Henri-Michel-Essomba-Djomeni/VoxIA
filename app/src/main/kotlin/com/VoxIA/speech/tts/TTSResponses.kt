package com.VoxIA.speech.tts

import com.VoxIA.speech.stt.SpeechLanguage

/**
 * VoxIA — Réponses vocales complètes et robustes
 * Bilingue FR/EN — contexte africain — lecture naturelle
 */
object TTSResponses {

    // ─── DÉMARRAGE ────────────────────────────────────
    fun appReady(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "VOXIA est prêt. Dites VOXIA pour commencer."
        SpeechLanguage.EN -> "VOXIA is ready. Say VOXIA to start."
    }

    fun listening(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Oui ?"
        SpeechLanguage.EN -> "Yes?"
    }

    // ─── F1 : IDENTIFICATION D'OBJETS ─────────────────

    fun objectDetected(objectName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Vous tenez ${translateObjectFR(objectName)}."
        SpeechLanguage.EN -> "You are holding ${objectName}."
    }

    fun multipleObjectsDetected(objects: List<String>, lang: SpeechLanguage): String {
        return when (lang) {
            SpeechLanguage.FR -> {
                val translated = objects.map { translateObjectFR(it) }
                val list = formatList(translated, lang)
                "Je vois plusieurs objets : $list."
            }
            SpeechLanguage.EN -> {
                val list = formatList(objects, lang)
                "I see multiple objects: $list."
            }
        }
    }

    fun objectWithDistance(
        objectName: String,
        distance: String,
        lang: SpeechLanguage
    ) = when (lang) {
        SpeechLanguage.FR ->
            "Je vois ${translateObjectFR(objectName)} à environ $distance."
        SpeechLanguage.EN ->
            "I see $objectName at approximately $distance."
    }

    fun noObjectDetected(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Je ne vois aucun objet clairement. Rapprochez la caméra et réessayez."
        SpeechLanguage.EN ->
            "I cannot see any object clearly. Please move the camera closer and try again."
    }

    fun analyzingObject(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "J'analyse l'objet, un instant."
        SpeechLanguage.EN -> "Analyzing the object, please wait."
    }

    fun lowConfidenceObject(objectName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Je pense que vous tenez ${translateObjectFR(objectName)}, mais je n'en suis pas certain."
        SpeechLanguage.EN ->
            "I think you are holding $objectName, but I'm not entirely sure."
    }

    // ─── TRADUCTION OBJETS → CONTEXTE AFRICAIN ────────

    private fun translateObjectFR(objectName: String): String {
        // Traduction + adaptation contexte local
        return when (objectName.lowercase().trim()) {
            // Nourriture locale
            "banana" -> "une banane"
            "plantain" -> "une banane plantain"
            "corn", "maize" -> "du maïs"
            "cassava" -> "du manioc"
            "yam" -> "de l'igname"
            "groundnut", "peanut" -> "des arachides"
            "palm oil" -> "de l'huile de palme"

            // Boissons
            "bottle" -> "une bouteille"
            "water bottle" -> "une bouteille d'eau"
            "cup", "glass" -> "un verre"

            // Argent
            "money", "bill", "banknote" -> "un billet"
            "coin" -> "une pièce de monnaie"
            "wallet" -> "un portefeuille"

            // Téléphone & tech
            "phone", "smartphone", "mobile" -> "un téléphone"
            "charger" -> "un chargeur"
            "earphone", "headphone" -> "des écouteurs"

            // Documents
            "document", "paper" -> "un document"
            "book" -> "un livre"
            "notebook" -> "un cahier"
            "pen" -> "un stylo"
            "pencil" -> "un crayon"
            "id card", "identity card" -> "une carte d'identité"

            // Objets courants
            "key", "keys" -> "des clés"
            "bag" -> "un sac"
            "spoon" -> "une cuillère"
            "fork" -> "une fourchette"
            "knife" -> "un couteau"
            "plate" -> "une assiette"
            "bowl" -> "un bol"
            "remote" -> "une télécommande"
            "watch" -> "une montre"
            "glasses" -> "des lunettes"
            "mask" -> "un masque"
            "medicine", "pill" -> "un médicament"

            // Par défaut — garde le nom original
            else -> "un objet : $objectName"
        }
    }

    // ─── F2 : LECTURE DE DOCUMENT ─────────────────────

    fun startingReading(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je lis le document."
        SpeechLanguage.EN -> "Reading the document."
    }

    fun noTextDetected(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Aucun texte détecté. Pointez la caméra vers un document bien éclairé."
        SpeechLanguage.EN ->
            "No text detected. Point the camera at a well-lit document."
    }

    fun readingComplete(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Lecture terminée."
        SpeechLanguage.EN -> "Reading complete."
    }

    fun analyzingDocument(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "J'analyse le document, un instant."
        SpeechLanguage.EN -> "Analyzing the document, please wait."
    }

    fun documentLanguageDetected(detectedLang: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Document détecté en $detectedLang."
        SpeechLanguage.EN -> "Document detected in $detectedLang."
    }

    fun readingSegment(current: Int, total: Int, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Partie $current sur $total."
        SpeechLanguage.EN -> "Part $current of $total."
    }

    fun pauseReading(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Lecture en pause. Dites VOXIA pour continuer."
        SpeechLanguage.EN -> "Reading paused. Say VOXIA to continue."
    }

    fun continueReading(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je continue la lecture."
        SpeechLanguage.EN -> "Continuing the reading."
    }

    // ─── NETTOYAGE TEXTE OCR ──────────────────────────

    fun cleanOCRText(rawText: String): String {
        return rawText
            // Supprimer caractères spéciaux inutiles
            .replace(Regex("[|\\\\@#^*~`]"), "")
            // Normaliser les espaces multiples
            .replace(Regex("\\s+"), " ")
            // Supprimer lignes vides multiples
            .replace(Regex("\\n{3,}"), "\n\n")
            // Corriger ponctuation collée
            .replace(Regex("([.,!?;:])([A-Za-zÀ-ÿ])"), "$1 $2")
            // Supprimer tirets de coupure de mots
            .replace(Regex("-(\\s*\\n\\s*)"), "")
            // Normaliser les guillemets
            .replace("«", "\"").replace("»", "\"")
            .replace("\u2018", "'").replace("\u2019", "'")
            .trim()
    }

    fun segmentTextForTTS(text: String, maxChars: Int = 200): List<String> {
        if (text.length <= maxChars) return listOf(text)

        val segments = mutableListOf<String>()
        // Découper par phrases d'abord
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        var current = StringBuilder()

        for (sentence in sentences) {
            if (current.length + sentence.length > maxChars) {
                if (current.isNotEmpty()) {
                    segments.add(current.toString().trim())
                    current = StringBuilder()
                }
                // Phrase trop longue → découper par virgules
                if (sentence.length > maxChars) {
                    val parts = sentence.split(Regex("(?<=,)\\s+"))
                    for (part in parts) {
                        if (current.length + part.length > maxChars) {
                            if (current.isNotEmpty()) {
                                segments.add(current.toString().trim())
                                current = StringBuilder()
                            }
                        }
                        current.append(part).append(" ")
                    }
                } else {
                    current.append(sentence).append(" ")
                }
            } else {
                current.append(sentence).append(" ")
            }
        }

        if (current.isNotEmpty()) segments.add(current.toString().trim())
        return segments
    }

    // ─── F3 : APPEL RAPIDE ────────────────────────────

    fun callingContact(contactName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Appel de $contactName en cours."
        SpeechLanguage.EN -> "Calling $contactName."
    }

    fun contactNotFound(contactName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Je ne trouve pas $contactName dans vos contacts. Vérifiez le nom et réessayez."
        SpeechLanguage.EN ->
            "I cannot find $contactName in your contacts. Please check the name and try again."
    }

    fun multipleContactsFound(contactName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Plusieurs contacts correspondent à $contactName. Lequel voulez-vous appeler ?"
        SpeechLanguage.EN ->
            "Multiple contacts match $contactName. Which one would you like to call?"
    }

    fun callCancelled(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Appel annulé."
        SpeechLanguage.EN -> "Call cancelled."
    }

    fun callFailed(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Impossible de passer l'appel. Vérifiez votre réseau."
        SpeechLanguage.EN -> "Unable to make the call. Please check your network."
    }

    // ─── CHANGEMENT DE LANGUE ─────────────────────────

    fun languageSwitched(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Langue changée en français."
        SpeechLanguage.EN -> "Language switched to English."
    }

    fun askLanguagePreference(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Quelle langue préférez-vous ? Dites français ou anglais."
        SpeechLanguage.EN ->
            "Which language do you prefer? Say French or English."
    }

    // ─── ERREURS & FALLBACKS ──────────────────────────

    fun didNotUnderstand(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je n'ai pas compris. Pouvez-vous répéter ?"
        SpeechLanguage.EN -> "I didn't understand. Could you repeat that?"
    }

    fun lowConfidenceTranscription(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Je n'ai pas bien entendu. Parlez plus près du microphone."
        SpeechLanguage.EN ->
            "I didn't hear clearly. Please speak closer to the microphone."
    }

    fun unknownCommand(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Commande non reconnue. Dites VOXIA pour réessayer."
        SpeechLanguage.EN ->
            "Command not recognized. Say VOXIA to try again."
    }

    fun processingError(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Une erreur s'est produite. Réessayez."
        SpeechLanguage.EN -> "An error occurred. Please try again."
    }

    fun timeout(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "Délai dépassé. Dites VOXIA pour recommencer."
        SpeechLanguage.EN ->
            "Timeout. Say VOXIA to start again."
    }

    // ─── PERMISSIONS ──────────────────────────────────

    fun micPermissionNeeded(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "J'ai besoin d'accéder au microphone pour fonctionner. Veuillez autoriser l'accès."
        SpeechLanguage.EN ->
            "I need microphone access to work. Please grant the permission."
    }

    fun cameraPermissionNeeded(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "J'ai besoin d'accéder à la caméra pour identifier les objets."
        SpeechLanguage.EN ->
            "I need camera access to identify objects."
    }

    fun contactPermissionNeeded(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR ->
            "J'ai besoin d'accéder à vos contacts pour passer des appels."
        SpeechLanguage.EN ->
            "I need contacts access to make calls."
    }

    // ─── UTILITAIRES ──────────────────────────────────

    private fun formatList(items: List<String>, lang: SpeechLanguage): String {
        return when {
            items.isEmpty() -> ""
            items.size == 1 -> items[0]
            else -> {
                val separator = if (lang == SpeechLanguage.FR) " et " else " and "
                items.dropLast(1).joinToString(", ") + separator + items.last()
            }
        }
    }

    // ─── PHRASES D'ENTRAÎNEMENT (collecte accents) ────

    val frenchTrainingPhrases = listOf(
        "Qu'est-ce que je tiens ?",
        "Qu'est-ce que je tiens dans ma main ?",
        "Lis ce document",
        "Lis cette lettre",
        "Lis ce que tu vois",
        "Appelle maman",
        "Appelle papa",
        "Appelle mon frère",
        "Appelle ma sœur",
        "Passe en anglais",
        "Qu'est-ce que c'est ?",
        "Dis-moi ce que tu vois",
        "Aide-moi à lire ce texte"
    )

    val englishTrainingPhrases = listOf(
        "What am I holding?",
        "What am I holding in my hand?",
        "Read this document",
        "Read this letter",
        "Read what you see",
        "Call mom",
        "Call dad",
        "Call my brother",
        "Call my sister",
        "Switch to French",
        "What is this?",
        "Tell me what you see",
        "Help me read this text"
    )
}