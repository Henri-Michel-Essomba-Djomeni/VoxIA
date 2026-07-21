package com.voxia.brain

@Suppress("UNUSED_PARAMETER")
object VoxiaResponses {

    // Réponses de salutation
    fun greeting(language: Language): Pair<String, String> = Pair(
        "Bonjour ! Je suis VOXIA. Comment puis-je vous aider ?",
        "Hello ! I am VOXIA. How can I help you ?"
    )

    // Réponses d'aide
    fun help(language: Language): Pair<String, String> = Pair(
        "Je peux identifier une image, scanner un produit, lire ou traduire un document, " +
        "régler la vitesse de lecture, ouvrir une application, préparer un appel, calculer, régler une alarme, lire vos notifications et vous assister.",
        "I can identify an image, scan a product, read or translate a document, " +
        "adjust reading speed, open an app, prepare a call, calculate, set an alarm, read notifications and assist you."
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
        "Je suis VOXIA, votre assistant vocal et visuel. Mes fonctions principales sont embarquées; " +
        "la traduction et certains moteurs vocaux peuvent nécessiter une connexion lors de leur première utilisation.",
        "I am VOXIA, your voice and visual assistant. My main features are bundled; " +
        "translation and some speech engines may need a connection on first use."
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
