package com.voxia.vision

import org.junit.Assert.assertTrue
import org.junit.Test

class FrameQualityAnalyzerTest {

    private val width = 40
    private val height = 40

    private fun solidFrame(gray: Int): IntArray {
        val argb = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        return IntArray(width * height) { argb }
    }

    private fun checkerboardFrame(): IntArray {
        return IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            val gray = if ((x + y) % 2 == 0) 0 else 255
            (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
    }

    @Test
    fun analyzePixels_flagsUniformDarkFrameAsTooDark() {
        val result = FrameQualityAnalyzer.analyzePixels(solidFrame(10), width, height)
        assertTrue(result is FrameQuality.TooDark)
    }

    @Test
    fun analyzePixels_flagsUniformBrightFrameAsTooBright() {
        val result = FrameQualityAnalyzer.analyzePixels(solidFrame(250), width, height)
        assertTrue(result is FrameQuality.TooBright)
    }

    @Test
    fun analyzePixels_flagsFlatMidToneFrameAsTooBlurry() {
        // Un cadre uniforme de tonalité moyenne n'a aucune transition de bord :
        // variance du Laplacien nulle, donc considéré flou/sans texture exploitable.
        val result = FrameQualityAnalyzer.analyzePixels(solidFrame(128), width, height)
        assertTrue(result is FrameQuality.TooBlurry)
    }

    @Test
    fun analyzePixels_acceptsHighContrastSharpFrame() {
        val result = FrameQualityAnalyzer.analyzePixels(checkerboardFrame(), width, height)
        assertTrue(result is FrameQuality.Acceptable)
    }
}
