package com.voxia.brain

object VoxiaResponses {

    // Réponses de salutation
    fun greeting(language: Language): Pair<String, String> = Pair(
        "Bonjour ! Je suis VOXIA. Comment puis-je vous aider ?",
        "Hello ! I am VOXIA. How can I help you ?"
    )

    // Réponses d'aide
    fun help(language: Language): Pair<String, String> = Pair(
        "Je peux identifier des objets, lire des documents, passer des appels, " +
        "régler des alarmes, raconter des histoires et bien plus encore.",
        "I can identify objects, read documents, make calls, " +
        "set alarms, tell stories and much more."
    )

    // Réponses fallback
    fun fallback(language: Language): Pair<String, String> = Pair(
        "Je n'ai pas compris. Pouvez-vous répéter s'il vous plaît ?",
        "I did not understand. Could you please repeat ?"
    )

    // Réponses confiance faible
    fun lowConfidence(language: Language): Pair<String, String> = Pair(
        "Je ne suis pas sûr d'avoir compris. Pouvez-vous reformuler ?",
        "I am not sure I understood. Could you rephrase ?"
    )

    // Réponses changement de langue
    fun switchedToFrench(): Pair<String, String> = Pair(
        "Très bien, je passe en français.",
        "Okay, switching to French."
    )

    fun switchedToEnglish(): Pair<String, String> = Pair(
        "D'accord, je passe en anglais.",
        "Okay, switching to English."
    )

    // Présentation
    fun whoAreYou(language: Language): Pair<String, String> = Pair(
        "Je suis VOXIA, votre assistant vocal intelligent. " +
        "Je fonctionne entièrement sans internet.",
        "I am VOXIA, your smart voice assistant. " +
        "I work completely without internet."
    )

    // Blagues
    fun joke(language: Language): Pair<String, String> = Pair(
        "Pourquoi les plongeurs plongent-ils toujours en arrière ? " +
        "Parce que s'ils plongeaient en avant, ils tomberaient dans le bateau !",
        "Why do scientists never knock on doors ? " +
        "Because they do not want to interrupt any reaction !"
    )

    // Histoires
    fun story(language: Language): Pair<String, String> = Pair(
        "Il était une fois, dans un village africain, un jeune garçon " +
        "qui rêvait de voir le monde. Chaque soir, il écoutait les étoiles " +
        "lui raconter des histoires de pays lointains...",
        "Once upon a time, in an African village, a young girl " +
        "dreamed of changing the world. Every night, she would look " +
        "at the stars and imagine a future full of possibilities..."
    )

    // Citations motivantes
    fun motivational(language: Language): Pair<String, String> = Pair(
        "Chaque jour est une nouvelle opportunité de devenir " +
        "la meilleure version de vous-même. Vous êtes capable !",
        "Every day is a new opportunity to become " +
        "the best version of yourself. You are capable !"
    )

    // Arrêt
    fun stop(language: Language): Pair<String, String> = Pair(
        "D'accord, j'arrête.",
        "Okay, stopping."
    )

    // Langue non reconnue
    fun unknownLanguage(): Pair<String, String> = Pair(
        "Je n'ai pas reconnu la langue. Parlez en français ou en anglais.",
        "I did not recognize the language. Please speak in French or English."
    )
}