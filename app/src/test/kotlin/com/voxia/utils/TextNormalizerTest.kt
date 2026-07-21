package com.voxia.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun normalize_removesAccentsPunctuationAndExtraSpaces() {
        assertEquals("decris l environnement", TextNormalizer.normalize("  Décris l’environnement ! "))
    }

    @Test
    fun normalize_keepsArithmeticCharacters() {
        assertEquals("12.5 + 3/2", TextNormalizer.normalize("12.5 + 3/2"))
    }
}
