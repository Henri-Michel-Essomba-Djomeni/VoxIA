package com.voxia.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechStatePolicyTest {

    @Test
    fun completedFinalSpeech_returnsToIdle() {
        assertTrue(
            SpeechStatePolicy.shouldReturnToIdle(
                currentState = SpeechState.SPEAKING,
                hasPendingSpeech = false
            )
        )
    }

    @Test
    fun completedPrompt_doesNotOverwriteListeningState() {
        assertFalse(
            SpeechStatePolicy.shouldReturnToIdle(
                currentState = SpeechState.LISTENING,
                hasPendingSpeech = false
            )
        )
    }

    @Test
    fun completedQueuedSpeech_keepsSpeakingState() {
        assertFalse(
            SpeechStatePolicy.shouldReturnToIdle(
                currentState = SpeechState.SPEAKING,
                hasPendingSpeech = true
            )
        )
    }

    @Test
    fun successfulPrompt_startsListening() {
        assertEquals(
            PromptTerminalAction.START_LISTENING,
            SpeechPromptPolicy.terminalAction(succeeded = true)
        )
    }

    @Test
    fun failedPrompt_returnsToIdleWithoutOpeningMicrophone() {
        assertEquals(
            PromptTerminalAction.RETURN_TO_IDLE,
            SpeechPromptPolicy.terminalAction(succeeded = false)
        )
    }
}
