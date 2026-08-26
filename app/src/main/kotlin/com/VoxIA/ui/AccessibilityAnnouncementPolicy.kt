package com.voxia.ui

internal enum class ResponseOrigin {
    ASSISTANT_SPEECH,
    LOCAL_UI
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

    fun shouldAnnounceState(isAssistantSpeaking: Boolean): Boolean = !isAssistantSpeaking
}
