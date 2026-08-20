package com.voxia.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OCRInitializationTest {

    @Test
    fun completeCameraInitialization_reportsSuccessOnce() {
        val results = mutableListOf<Boolean>()

        completeCameraInitialization(
            initialize = { true },
            onReady = results::add
        )

        assertEquals(listOf(true), results)
    }

    @Test
    fun completeCameraInitialization_convertsExceptionToFailureOnce() {
        val results = mutableListOf<Boolean>()
        var failureReported = false

        completeCameraInitialization(
            initialize = { throw IllegalStateException("camera unavailable") },
            onFailure = { failureReported = true },
            onReady = results::add
        )

        assertTrue(failureReported)
        assertEquals(listOf(false), results)
    }

    @Test
    fun completeCameraInitialization_preservesBindingFailure() {
        var ready = true

        completeCameraInitialization(
            initialize = { false },
            onReady = { ready = it }
        )

        assertFalse(ready)
    }
}
