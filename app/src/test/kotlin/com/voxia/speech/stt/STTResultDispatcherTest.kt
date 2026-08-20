package com.voxia.speech.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class STTResultDispatcherTest {

    @Test
    fun dispatchFinal_emptyResults_reportsTerminalError() {
        var result: STTResult? = null
        var error: String? = null

        STTResultDispatcher.dispatchFinal(
            matches = emptyList(),
            confidences = null,
            language = SpeechLanguage.FR,
            onResult = { result = it },
            onError = { error = it }
        )

        assertNull(result)
        assertEquals(STTResultDispatcher.EMPTY_FINAL_RESULT_ERROR, error)
    }

    @Test
    fun dispatchFinal_blankFirstMatch_usesFirstUsableResultAndItsConfidence() {
        var result: STTResult? = null
        var error: String? = null

        STTResultDispatcher.dispatchFinal(
            matches = listOf("  ", "bonjour VoxIA"),
            confidences = floatArrayOf(0.1f, 0.85f),
            language = SpeechLanguage.FR,
            onResult = { result = it },
            onError = { error = it }
        )

        assertNull(error)
        assertEquals("bonjour VoxIA", result?.text)
        assertEquals(0.85f, result?.confidence ?: 0f, 0f)
        assertEquals(true, result?.isFinal)
    }
}
