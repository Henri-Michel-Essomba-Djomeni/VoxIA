package com.voxia.brain

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentMapperTest {

    @Test
    fun execute_routesReadingExportCommandsThroughConfirmationRequests() {
        val context = RecordingContext()

        IntentMapper.execute(
            PredictionResult(Intent.COPY_READING_TEXT, Language.FRENCH, confidence = 0.95f),
            context
        )
        IntentMapper.execute(
            PredictionResult(Intent.SHARE_READING_TEXT, Language.FRENCH, confidence = 0.95f),
            context
        )

        assertEquals(
            listOf("requestCopyLastReadingText", "requestShareLastReadingText"),
            context.calls
        )
    }

    @Test
    fun execute_lowConfidenceExportCommandAsksForClarificationOnly() {
        val context = RecordingContext()

        IntentMapper.execute(
            PredictionResult(Intent.COPY_READING_TEXT, Language.FRENCH, confidence = 0.69f),
            context
        )

        assertEquals(listOf("speak"), context.calls)
    }

    @Test
    fun execute_routesSpeechRateCommands() {
        val context = RecordingContext()

        IntentMapper.execute(
            PredictionResult(Intent.READING_SPEED_DOWN, Language.FRENCH, confidence = 0.95f),
            context
        )
        IntentMapper.execute(
            PredictionResult(Intent.READING_SPEED_NORMAL, Language.FRENCH, confidence = 0.95f),
            context
        )
        IntentMapper.execute(
            PredictionResult(Intent.READING_SPEED_UP, Language.FRENCH, confidence = 0.95f),
            context
        )

        assertEquals(
            listOf("decreaseSpeechRate", "resetSpeechRate", "increaseSpeechRate"),
            context.calls
        )
    }

    @Test
    fun execute_englishRequestDoesNotMutateLanguageAndExplainsOfflineLimit() {
        val context = RecordingContext()

        IntentMapper.execute(
            PredictionResult(Intent.SWITCH_TO_ENGLISH, Language.FRENCH, confidence = 0.95f),
            context
        )

        assertEquals(listOf("speak"), context.calls)
        assertEquals(
            "La version hors ligne fonctionne actuellement uniquement en français.",
            context.lastSpoken?.first
        )
    }

    @Test
    fun execute_frenchRequestKeepsAllLayersOnFrench() {
        val context = RecordingContext()

        IntentMapper.execute(
            PredictionResult(Intent.SWITCH_TO_FRENCH, Language.FRENCH, confidence = 0.95f),
            context
        )

        assertEquals(listOf("switchLanguage:FRENCH", "speak"), context.calls)
    }

    private class RecordingContext : VoxiaContext {
        val calls = mutableListOf<String>()
        var lastSpoken: Pair<String, String>? = null

        override fun speak(fr: String, en: String) {
            calls += "speak"
            lastSpoken = fr to en
        }

        override fun repeatLastResponse() {
            calls += "repeatLastResponse"
        }

        override fun speakHelp() {
            calls += "speakHelp"
        }

        override fun speakTime() {
            calls += "speakTime"
        }

        override fun speakDate() {
            calls += "speakDate"
        }

        override fun speakBatteryLevel() {
            calls += "speakBatteryLevel"
        }

        override fun increaseSpeechRate() {
            calls += "increaseSpeechRate"
        }

        override fun decreaseSpeechRate() {
            calls += "decreaseSpeechRate"
        }

        override fun resetSpeechRate() {
            calls += "resetSpeechRate"
        }

        override fun switchLanguage(language: Language) {
            calls += "switchLanguage:$language"
        }

        override fun loadVisionModule() {
            calls += "loadVisionModule"
        }

        override fun captureAndIdentify() {
            calls += "captureAndIdentify"
        }

        override fun describeSurroundings() {
            calls += "describeSurroundings"
        }

        override fun scanProduct() {
            calls += "scanProduct"
        }

        override fun loadOcrModule() {
            calls += "loadOcrModule"
        }

        override fun captureAndRead() {
            calls += "captureAndRead"
        }

        override fun readNextSegment() {
            calls += "readNextSegment"
        }

        override fun readPreviousSegment() {
            calls += "readPreviousSegment"
        }

        override fun requestCopyLastReadingText() {
            calls += "requestCopyLastReadingText"
        }

        override fun requestShareLastReadingText() {
            calls += "requestShareLastReadingText"
        }

        override fun translateVisibleText() {
            calls += "translateVisibleText"
        }

        override fun makeCall(contactName: String?) {
            calls += "makeCall"
        }

        override fun setAlarm(hour: Int?, minute: Int?) {
            calls += "setAlarm"
        }

        override fun setReminder(hour: Int?, minute: Int?, durationMinutes: Int?) {
            calls += "setReminder"
        }

        override fun increaseVolume() {
            calls += "increaseVolume"
        }

        override fun decreaseVolume() {
            calls += "decreaseVolume"
        }

        override fun openApp(appName: String?) {
            calls += "openApp"
        }

        override fun calculate(expression: String?) {
            calls += "calculate"
        }

        override fun tellStory(language: Language) {
            calls += "tellStory"
        }

        override fun tellJoke(language: Language) {
            calls += "tellJoke"
        }

        override fun tellMotivational(language: Language) {
            calls += "tellMotivational"
        }

        override fun readNotifications() {
            calls += "readNotifications"
        }

        override fun stopAll() {
            calls += "stopAll"
        }
    }
}
