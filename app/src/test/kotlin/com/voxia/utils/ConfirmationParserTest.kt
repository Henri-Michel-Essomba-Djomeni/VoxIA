package com.voxia.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfirmationParserTest {
    @Test
    fun parse_recognizesFrenchAndEnglishYes() {
        assertEquals(true, ConfirmationParser.parse("Oui"))
        assertEquals(true, ConfirmationParser.parse("d'accord vas-y"))
        assertEquals(true, ConfirmationParser.parse("Yes please"))
    }

    @Test
    fun parse_recognizesFrenchAndEnglishNo() {
        assertEquals(false, ConfirmationParser.parse("Non"))
        assertEquals(false, ConfirmationParser.parse("annule ça"))
        assertEquals(false, ConfirmationParser.parse("Cancel that"))
    }

    @Test
    fun parse_returnsNullForAmbiguousOrUnrelatedText() {
        assertNull(ConfirmationParser.parse("quelle heure est-il"))
        assertNull(ConfirmationParser.parse(""))
    }
}
