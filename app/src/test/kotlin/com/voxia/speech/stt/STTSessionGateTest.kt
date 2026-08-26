package com.voxia.speech.stt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class STTSessionGateTest {

    @Test
    fun finalResult_allowsOnlyOneTerminalCallback() {
        val gate = STTSessionGate()
        val sessionId = gate.begin()

        assertTrue(gate.acceptTerminal(sessionId))
        assertFalse(gate.acceptTerminal(sessionId))
        assertFalse(gate.hasActiveSession())
    }

    @Test
    fun deliberateStop_suppressesLateClientError() {
        val gate = STTSessionGate()
        val sessionId = gate.begin()

        gate.cancel()

        assertFalse(gate.acceptTerminal(sessionId))
        assertFalse(gate.isActive(sessionId))
    }

    @Test
    fun newSession_rejectsCallbacksFromPreviousRecognizer() {
        val gate = STTSessionGate()
        val previousSessionId = gate.begin()
        val currentSessionId = gate.begin()

        assertFalse(gate.acceptTerminal(previousSessionId))
        assertTrue(gate.acceptTerminal(currentSessionId))
    }

    @Test
    fun fallback_invalidatesLocalRecognizerAndKeepsSystemRecognizerActive() {
        val gate = STTSessionGate()
        val localSessionId = gate.begin()

        val systemSessionId = gate.continueWithFallback(localSessionId)

        assertNotNull(systemSessionId)
        assertFalse(gate.acceptTerminal(localSessionId))
        assertTrue(gate.isActive(systemSessionId!!))
        assertTrue(gate.acceptTerminal(systemSessionId))
    }

    @Test
    fun cancellationDuringFallback_suppressesDelayedSystemRecognizer() {
        val gate = STTSessionGate()
        val localSessionId = gate.begin()
        val systemSessionId = gate.continueWithFallback(localSessionId)

        gate.cancel()

        assertNotNull(systemSessionId)
        assertFalse(gate.isActive(systemSessionId!!))
        assertFalse(gate.acceptTerminal(systemSessionId))
    }

    @Test
    fun staleRecognizer_cannotStartAnotherFallback() {
        val gate = STTSessionGate()
        val localSessionId = gate.begin()
        val systemSessionId = gate.continueWithFallback(localSessionId)

        val duplicateFallbackId = gate.continueWithFallback(localSessionId)

        assertNotNull(systemSessionId)
        assertNull(duplicateFallbackId)
        assertTrue(gate.isActive(systemSessionId!!))
    }
}
