package com.VoxIA.speech.tts

import com.VoxIA.speech.stt.SpeechLanguage

/**
 * Toutes les réponses vocales de VoxIA
 * Bilingue FR/EN — utilisé par TTSService
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
        SpeechLanguage.FR -> "Vous tenez $objectName."
        SpeechLanguage.EN -> "You are holding $objectName."
    }

    fun multipleObjectsDetected(objects: List<String>, lang: SpeechLanguage): String {
        val list = objects.joinToString(", ")
        return when (lang) {
            SpeechLanguage.FR -> "Je vois plusieurs objets : $list."
            SpeechLanguage.EN -> "I see multiple objects: $list."
        }
    }

    fun noObjectDetected(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je ne vois aucun objet clairement. Rapprochez la caméra."
        SpeechLanguage.EN -> "I cannot see any object clearly. Please move the camera closer."
    }

    fun analyzingObject(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "J'analyse l'objet, un instant."
        SpeechLanguage.EN -> "Analyzing the object, please wait."
    }

    // ─── F2 : LECTURE DE DOCUMENT ─────────────────────
    fun startingReading(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je lis le document."
        SpeechLanguage.EN -> "Reading the document."
    }

    fun noTextDetected(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Aucun texte détecté. Pointez la caméra vers un document."
        SpeechLanguage.EN -> "No text detected. Please point the camera at a document."
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

    // ─── F3 : APPEL RAPIDE ────────────────────────────
    fun callingContact(contactName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Appel de $contactName en cours."
        SpeechLanguage.EN -> "Calling $contactName."
    }

    fun contactNotFound(contactName: String, lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je ne trouve pas $contactName dans vos contacts."
        SpeechLanguage.EN -> "I cannot find $contactName in your contacts."
    }

    fun callCancelled(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Appel annulé."
        SpeechLanguage.EN -> "Call cancelled."
    }

    // ─── CHANGEMENT DE LANGUE ─────────────────────────
    fun languageSwitched(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Langue changée en français."
        SpeechLanguage.EN -> "Language switched to English."
    }

    // ─── ERREURS & FALLBACKS ──────────────────────────
    fun didNotUnderstand(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je n'ai pas compris. Pouvez-vous répéter ?"
        SpeechLanguage.EN -> "I didn't understand. Could you repeat?"
    }

    fun lowConfidence(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Je n'ai pas bien entendu. Parlez plus près du micro."
        SpeechLanguage.EN -> "I didn't hear clearly. Please speak closer to the microphone."
    }

    fun unknownCommand(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Commande non reconnue. Dites VOXIA pour recommencer."
        SpeechLanguage.EN -> "Command not recognized. Say VOXIA to try again."
    }

    fun processingError(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Une erreur s'est produite. Réessayez."
        SpeechLanguage.EN -> "An error occurred. Please try again."
    }

    // ─── MODE HORS LIGNE ──────────────────────────────
    fun offlineMode(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "Mode hors ligne activé. Toutes les fonctions sont disponibles."
        SpeechLanguage.EN -> "Offline mode activated. All features are available."
    }

    // ─── PERMISSIONS ──────────────────────────────────
    fun micPermissionNeeded(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "J'ai besoin d'accéder au microphone pour fonctionner."
        SpeechLanguage.EN -> "I need microphone access to work properly."
    }

    fun cameraPermissionNeeded(lang: SpeechLanguage) = when (lang) {
        SpeechLanguage.FR -> "J'ai besoin d'accéder à la caméra pour identifier les objets."
        SpeechLanguage.EN -> "I need camera access to identify objects."
    }

    // ─── ACCENTS LOCAUX ───────────────────────────────
    // Phrases de collecte données pour fine-tuning
    val frenchTrainingPhrases = listOf(
        "Qu'est-ce que je tiens ?",
        "Lis ce document",
        "Appelle maman",
        "Appelle papa",
        "Qu'est-ce que c'est ?",
        "Lis cette lettre",
        "Appelle mon frère",
        "Passe en anglais",
        "Qu'est-ce que je tiens dans ma main ?",
        "Lis ce que tu vois"
    )

    val englishTrainingPhrases = listOf(
        "What am I holding?",
        "Read this document",
        "Call mom",
        "Call dad",
        "What is this?",
        "Read this letter",
        "Call my brother",
        "Switch to French",
        "What am I holding in my hand?",
        "Read what you see"
    )
}