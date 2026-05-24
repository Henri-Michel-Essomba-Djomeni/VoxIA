package com.voxia.brain

object IntentMapper {

    fun execute(result: PredictionResult, context: VoxiaContext) {

        // Vérifier le seuil de confiance
        if (result.confidence < 0.70f) {
            val response = VoxiaResponses.lowConfidence(result.language)
            context.speak(fr = response.first, en = response.second)
            return
        }

        when (result.intent) {

            Intent.IDENTIFY_OBJECT -> {
                context.loadVisionModule()
                context.captureAndIdentify()
            }

            Intent.READ_DOCUMENT -> {
                context.loadOcrModule()
                context.captureAndRead()
            }

            Intent.CALL_CONTACT -> {
                context.makeCall(result.extractedContact)
            }

            Intent.SWITCH_TO_ENGLISH -> {
                context.switchLanguage(Language.ENGLISH)
                val response = VoxiaResponses.switchedToEnglish()
                context.speak(fr = response.first, en = response.second)
            }

            Intent.SWITCH_TO_FRENCH -> {
                context.switchLanguage(Language.FRENCH)
                val response = VoxiaResponses.switchedToFrench()
                context.speak(fr = response.first, en = response.second)
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
                val response = VoxiaResponses.story(result.language)
                context.speak(fr = response.first, en = response.second)
            }

            Intent.TELL_JOKE -> {
                val response = VoxiaResponses.joke(result.language)
                context.speak(fr = response.first, en = response.second)
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
                val response = VoxiaResponses.greeting(result.language)
                context.speak(fr = response.first, en = response.second)
            }

            Intent.REPEAT -> {
                context.repeatLastResponse()
            }

            Intent.STOP -> {
                val response = VoxiaResponses.stop(result.language)
                context.speak(fr = response.first, en = response.second)
                context.stopAll()
            }

            Intent.HELP -> {
                val response = VoxiaResponses.help(result.language)
                context.speak(fr = response.first, en = response.second)
            }

            Intent.TELL_MOTIVATIONAL -> {
                val response = VoxiaResponses.motivational(result.language)
                context.speak(fr = response.first, en = response.second)
            }

            Intent.WHO_ARE_YOU -> {
                val response = VoxiaResponses.whoAreYou(result.language)
                context.speak(fr = response.first, en = response.second)
            }

            Intent.FALLBACK -> {
                val response = VoxiaResponses.fallback(result.language)
                context.speak(fr = response.first, en = response.second)
            }
        }
    }
}