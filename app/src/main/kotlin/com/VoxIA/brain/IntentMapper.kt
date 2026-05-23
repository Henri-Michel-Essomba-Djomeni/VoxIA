package com.voxia.brain

// Mapping intention → action à exécuter
object IntentMapper {

    fun execute(result: PredictionResult, context: VoxiaContext) {
        
        // Vérifier le seuil de confiance
        if (result.confidence < 0.70f) {
            context.speak(
                fr = "Pouvez-vous répéter s'il vous plaît ?",
                en = "Could you please repeat ?"
            )
            return
        }

        when (result.intent) {

            Intent.IDENTIFY_OBJECT -> {
                // Déclenche le module Vision (Dev1)
                context.loadVisionModule()
                context.captureAndIdentify()
            }

            Intent.READ_DOCUMENT -> {
                // Déclenche OCR ML Kit (Dev3)
                context.loadOcrModule()
                context.captureAndRead()
            }

            Intent.CALL_CONTACT -> {
                // Déclenche appel Android (Dev4)
                context.makeCall(result.extractedContact)
            }

            Intent.SWITCH_TO_ENGLISH -> {
                // Change modèle STT (Dev2)
                context.switchLanguage(Language.ENGLISH)
                context.speak(
                    fr = "Langue changée vers l'anglais",
                    en = "Language switched to English"
                )
            }

            Intent.SWITCH_TO_FRENCH -> {
                // Change modèle STT (Dev2)
                context.switchLanguage(Language.FRENCH)
                context.speak(
                    fr = "Langue changée vers le français",
                    en = "Langue changée vers le français"
                )
            }

            Intent.SET_ALARM -> {
                context.setAlarm()
            }

            Intent.SET_REMINDER -> {
                context.setReminder()
            }

            Intent.WHAT_TIME -> {
                context.speakTime()
            }

            Intent.WHAT_DATE -> {
                context.speakDate()
            }

            Intent.BATTERY_STATUS -> {
                context.speakBatteryLevel()
            }

            Intent.VOLUME_UP -> {
                context.increaseVolume()
            }

            Intent.VOLUME_DOWN -> {
                context.decreaseVolume()
            }

            Intent.TELL_STORY -> {
                context.tellStory(result.language)
            }

            Intent.TELL_JOKE -> {
                context.tellJoke(result.language)
            }

            Intent.DESCRIBE_SURROUNDINGS -> {
                context.loadVisionModule()
                context.describeSurroundings()
            }

            Intent.READ_NOTIFICATION -> {
                context.readNotifications()
            }

            Intent.OPEN_APP -> {
                context.openApp(result.extractedAppName)
            }

            Intent.CALCULATE -> {
                context.calculate(result.extractedExpression)
            }

            Intent.GREETING -> {
                context.speak(
                    fr = "Bonjour ! Comment puis-je vous aider ?",
                    en = "Hello ! How can I help you ?"
                )
            }

            Intent.REPEAT -> {
                context.repeatLastResponse()
            }

            Intent.STOP -> {
                context.stopAll()
            }

            Intent.HELP -> {
                context.speakHelp()
            }

            Intent.TELL_MOTIVATIONAL -> {
                context.tellMotivational(result.language)
            }

            Intent.WHO_ARE_YOU -> {
                context.speak(
                    fr = "Je suis VOXIA, votre assistant vocal offline.",
                    en = "I am VOXIA, your offline voice assistant."
                )
            }

            Intent.FALLBACK -> {
                context.speak(
                    fr = "Je n'ai pas compris. Pouvez-vous répéter ?",
                    en = "I did not understand. Could you repeat ?"
                )
            }
        }
    }
}