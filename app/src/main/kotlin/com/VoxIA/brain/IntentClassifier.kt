package com.voxia.brain

enum class Intent {
    IDENTIFY_OBJECT,
    READ_DOCUMENT,
    CALL_CONTACT,
    SWITCH_TO_ENGLISH,
    SWITCH_TO_FRENCH,
    SET_REMINDER,
    SET_ALARM,
    TELL_STORY,
    TELL_JOKE,
    DESCRIBE_SURROUNDINGS,
    READ_NOTIFICATION,
    OPEN_APP,
    CALCULATE,
    WHAT_TIME,
    WHAT_DATE,
    BATTERY_STATUS,
    VOLUME_UP,
    VOLUME_DOWN,
    GREETING,
    REPEAT,
    STOP,
    HELP,
    TELL_MOTIVATIONAL,
    WHO_ARE_YOU,
    FALLBACK
}

enum class Language {
    FRENCH,
    ENGLISH,
    UNKNOWN
}

data class PredictionResult(
    val intent: Intent,
    val language: Language,
    val confidence: Float,
    val extractedContact: String? = null,
    val extractedAppName: String? = null,
    val extractedExpression: String? = null
)
