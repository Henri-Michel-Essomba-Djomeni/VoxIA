package com.voxia.utils

import android.util.Log
import com.voxia.assistant.BuildConfig

object PrivacyLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, message) else Log.w(tag, releaseSafe(message))
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG && throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, releaseSafe(message))
        }
    }

    private fun releaseSafe(message: String): String =
        message.replace(Regex("[\\r\\n]+"), " ").take(160)
}
