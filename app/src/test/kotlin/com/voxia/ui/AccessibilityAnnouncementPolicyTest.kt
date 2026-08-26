package com.voxia.ui

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
    fun listeningState_isAnnouncedForAccessibility() {
        assertTrue(AccessibilityAnnouncementPolicy.shouldAnnounceState(isAssistantSpeaking = false))
    }

    @Test
    fun speakingState_doesNotCompeteWithAssistantSpeech() {
        assertFalse(AccessibilityAnnouncementPolicy.shouldAnnounceState(isAssistantSpeaking = true))
    }
}
