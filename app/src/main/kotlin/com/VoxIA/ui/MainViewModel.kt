package com.voxia.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class VoxiaState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class MainViewModel : ViewModel() {

    private val _state = MutableLiveData(VoxiaState.IDLE)
    val state: LiveData<VoxiaState> = _state

    private val _currentLanguage = MutableLiveData("FR")
    val currentLanguage: LiveData<String> = _currentLanguage

    private val _transcript = MutableLiveData("")
    val transcript: LiveData<String> = _transcript

    private val _response = MutableLiveData("")
    val response: LiveData<String> = _response

    private val _isVisionActive = MutableLiveData(false)
    val isVisionActive: LiveData<Boolean> = _isVisionActive

    private val _isOcrActive = MutableLiveData(false)
    val isOcrActive: LiveData<Boolean> = _isOcrActive

    fun setState(state: VoxiaState) {
        _state.value = state
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun setTranscript(text: String) {
        _transcript.value = text
    }

    fun setResponse(text: String) {
        _response.value = text
    }

    fun setVisionActive(active: Boolean) {
        _isVisionActive.value = active
    }

    fun setOcrActive(active: Boolean) {
        _isOcrActive.value = active
    }
}
