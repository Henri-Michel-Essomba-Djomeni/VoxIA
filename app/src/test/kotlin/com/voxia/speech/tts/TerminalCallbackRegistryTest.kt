package com.voxia.speech.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalCallbackRegistryTest {

    @Test
    fun complete_invokesRegisteredTerminalCallback() {
        val registry = TerminalCallbackRegistry()
        var invocationCount = 0
        registry.register("utterance-1") { invocationCount++ }

        registry.complete("utterance-1")

        assertEquals(1, invocationCount)
    }

    @Test
    fun complete_invokesCallbackAtMostOnce() {
        val registry = TerminalCallbackRegistry()
        var invocationCount = 0
        registry.register("utterance-1") { invocationCount++ }

        registry.complete("utterance-1")
        registry.complete("utterance-1")

        assertEquals(1, invocationCount)
    }
}
