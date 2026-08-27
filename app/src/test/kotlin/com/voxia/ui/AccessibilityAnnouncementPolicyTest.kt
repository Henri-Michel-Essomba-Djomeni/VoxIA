package com.voxia.ui

import com.voxia.speech.SpeechState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityAnnouncementPolicyTest {

    @Test
    fun unspokenAssistantEvent_isOwnedByTalkBack() {
        assertTrue(
            AccessibilityAnnouncementPolicy.shouldAnnounce(
                AccessibilityAnnouncementPolicy.originForAssistantEvent(wasSpoken = false)
            )
        )
    }

    @Test
    fun assistantSpeech_isNotRepeatedByTalkBackLiveAnnouncement() {
        assertFalse(
            AccessibilityAnnouncementPolicy.shouldAnnounce(ResponseOrigin.ASSISTANT_SPEECH)
        )
    }

    @Test
    fun localUiFeedback_isAnnouncedForAccessibility() {
        assertTrue(
            AccessibilityAnnouncementPolicy.shouldAnnounce(ResponseOrigin.LOCAL_UI)
        )
    }

    @Test
    fun idleAndListeningStates_areAnnouncedImmediately() {
        assertEquals(
            StateAnnouncementTiming.IMMEDIATE,
            AccessibilityAnnouncementPolicy.stateAnnouncementTiming(SpeechState.IDLE)
        )
        assertEquals(
            StateAnnouncementTiming.IMMEDIATE,
            AccessibilityAnnouncementPolicy.stateAnnouncementTiming(SpeechState.LISTENING)
        )
    }

    @Test
    fun processingState_isDelayedToAvoidCompetingWithImmediateResponse() {
        assertEquals(
            StateAnnouncementTiming.DELAYED,
            AccessibilityAnnouncementPolicy.stateAnnouncementTiming(SpeechState.PROCESSING)
        )
    }

    @Test
    fun speakingState_doesNotCompeteWithAssistantSpeech() {
        assertEquals(
            StateAnnouncementTiming.NONE,
            AccessibilityAnnouncementPolicy.stateAnnouncementTiming(SpeechState.SPEAKING)
        )
    }

    @Test
    fun unknownState_isNotAnnouncedAsReady() {
        assertEquals(
            StateAnnouncementTiming.NONE,
            AccessibilityAnnouncementPolicy.stateAnnouncementTiming(null)
        )
    }
}
