package com.voxia.speech.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalCallbackRegistryTest {

    @Test
    fun complete_invokesRegisteredTerminalCallback() {
        val registry = TerminalCallbackRegistry()
        var invocationCount = 0
        registry.register("utterance-1", onDone = { invocationCount++ }, onError = null)

        registry.complete("utterance-1", succeeded = true)

        assertEquals(1, invocationCount)
    }

    @Test
    fun complete_invokesCallbackAtMostOnce() {
        val registry = TerminalCallbackRegistry()
        var invocationCount = 0
        registry.register("utterance-1", onDone = { invocationCount++ }, onError = null)

        registry.complete("utterance-1", succeeded = true)
        registry.complete("utterance-1", succeeded = true)

        assertEquals(1, invocationCount)
    }

    @Test
    fun failure_invokesErrorWithoutInvokingSuccessCallback() {
        var successes = 0
        var failures = 0
        val registry = TerminalCallbackRegistry()
        registry.register(
            "utterance-1",
            onDone = { successes++ },
            onError = { failures++ }
        )

        registry.complete("utterance-1", succeeded = false)

        assertEquals(0, successes)
        assertEquals(1, failures)
    }
}
