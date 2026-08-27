package com.voxia.ui

import com.voxia.speech.SpeechState

internal enum class ResponseOrigin {
    ASSISTANT_SPEECH,
    LOCAL_UI
}

internal enum class StateAnnouncementTiming {
    IMMEDIATE,
    DELAYED,
    NONE
}

/**
 * VOXIA TTS owns assistant responses. TalkBack owns controls, dialogs and
 * local UI-only feedback. Mirrored assistant text must therefore stay silent
 * as a live region, otherwise both engines announce the same response.
 */
internal object AccessibilityAnnouncementPolicy {
    fun originForAssistantEvent(wasSpoken: Boolean): ResponseOrigin =
        if (wasSpoken) ResponseOrigin.ASSISTANT_SPEECH else ResponseOrigin.LOCAL_UI

    fun shouldAnnounce(origin: ResponseOrigin): Boolean = origin == ResponseOrigin.LOCAL_UI

    fun stateAnnouncementTiming(state: SpeechState?): StateAnnouncementTiming = when (state) {
        SpeechState.IDLE,
        SpeechState.LISTENING -> StateAnnouncementTiming.IMMEDIATE

        SpeechState.PROCESSING -> StateAnnouncementTiming.DELAYED
        SpeechState.SPEAKING,
        null -> StateAnnouncementTiming.NONE
    }
}
