package com.voxia.brain

// Interface que Dev4 doit implémenter
// Ce fichier définit le contrat entre Brain et le reste de l'app
interface VoxiaContext {

    // Parole
    fun speak(fr: String, en: String)
    fun repeatLastResponse()
    fun speakHelp()
    fun speakTime()
    fun speakDate()
    fun speakBatteryLevel()

    // Langue
    fun switchLanguage(language: Language)

    // Vision (Dev1)
    fun loadVisionModule()
    fun captureAndIdentify()
    fun describeSurroundings()

    // OCR (Dev3)
    fun loadOcrModule()
    fun captureAndRead()

    // Appel (Dev4)
    fun makeCall(contactName: String?)

    // Alarme et rappel
    fun setAlarm()
    fun setReminder()

    // Volume
    fun increaseVolume()
    fun decreaseVolume()

    // Applications
    fun openApp(appName: String?)

    // Calcul
    fun calculate(expression: String?)

    // Contenu vocal
    fun tellStory(language: Language)
    fun tellJoke(language: Language)
    fun tellMotivational(language: Language)

    // Notifications
    fun readNotifications()

    // Système
    fun stopAll()
}