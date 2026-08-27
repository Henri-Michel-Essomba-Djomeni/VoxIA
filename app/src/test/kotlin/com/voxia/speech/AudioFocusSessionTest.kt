package com.voxia.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusSessionTest {

    @Test
    fun ducking_interruptsSpeechToPreventConcurrentVoices() {
        assertTrue(AudioFocusInterruptionPolicy.shouldInterrupt(AudioFocusEvent.DUCK))
    }

    @Test
    fun permanentAndTransientLoss_interruptSpeech() {
        assertTrue(AudioFocusInterruptionPolicy.shouldInterrupt(AudioFocusEvent.LOSS))
        assertTrue(AudioFocusInterruptionPolicy.shouldInterrupt(AudioFocusEvent.TRANSIENT_LOSS))
    }

    @Test
    fun gainAndUnknownEvents_doNotInterruptSpeech() {
        assertFalse(AudioFocusInterruptionPolicy.shouldInterrupt(AudioFocusEvent.GAIN))
        assertFalse(AudioFocusInterruptionPolicy.shouldInterrupt(AudioFocusEvent.UNKNOWN))
    }

    @Test
    fun acquire_reusesHeldFocusUntilRelease() {
        var requests = 0
        var abandons = 0
        val session = AudioFocusSession(
            requestFocus = { requests++; true },
            abandonFocus = { abandons++ }
        )

        assertTrue(session.acquire())
        assertTrue(session.acquire())
        session.release()
        session.release()

        assertEquals(1, requests)
        assertEquals(1, abandons)
        assertFalse(session.isHeld())
    }

    @Test
    fun acquire_retriesAfterFocusDenial() {
        var requests = 0
        val session = AudioFocusSession(
            requestFocus = { ++requests >= 2 },
            abandonFocus = {}
        )

        assertFalse(session.acquire())
        assertTrue(session.acquire())

        assertEquals(2, requests)
        assertTrue(session.isHeld())
    }

    @Test
    fun release_doesNotAbandonDeniedFocus() {
        var abandons = 0
        val session = AudioFocusSession(
            requestFocus = { false },
            abandonFocus = { abandons++ }
        )

        assertFalse(session.acquire())
        session.release()

        assertEquals(0, abandons)
    }
}
