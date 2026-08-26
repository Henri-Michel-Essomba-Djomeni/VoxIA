package com.voxia.speech

/** Pure transition rules shared by asynchronous speech callbacks. */
internal object SpeechStatePolicy {
    fun shouldReturnToIdle(currentState: SpeechState, hasPendingSpeech: Boolean): Boolean =
        currentState == SpeechState.SPEAKING && !hasPendingSpeech
}

internal enum class PromptTerminalAction {
    START_LISTENING,
    RETURN_TO_IDLE
}

internal object SpeechPromptPolicy {
    fun terminalAction(succeeded: Boolean): PromptTerminalAction =
        if (succeeded) PromptTerminalAction.START_LISTENING else PromptTerminalAction.RETURN_TO_IDLE
}
