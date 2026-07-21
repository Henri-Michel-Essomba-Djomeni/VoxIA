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

    private class RecordingContext : VoxiaContext {
        val calls = mutableListOf<String>()

        override fun speak(fr: String, en: String) {
            calls += "speak"
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

        override fun switchLanguage(language: Language) {
            calls += "switchLanguage"
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
