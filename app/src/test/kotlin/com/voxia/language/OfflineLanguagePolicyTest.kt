package com.voxia.language

import com.voxia.brain.Language
import com.voxia.speech.stt.SpeechLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLanguagePolicyTest {

    @Test
    fun normalize_keepsBrainAndSpeechLayersOnFrench() {
        assertEquals(Language.FRENCH, OfflineLanguagePolicy.normalize(Language.FRENCH))
        assertEquals(Language.FRENCH, OfflineLanguagePolicy.normalize(Language.ENGLISH))
        assertEquals(Language.FRENCH, OfflineLanguagePolicy.normalize(Language.UNKNOWN))
        assertEquals(SpeechLanguage.FR, OfflineLanguagePolicy.normalize(SpeechLanguage.FR))
        assertEquals(SpeechLanguage.FR, OfflineLanguagePolicy.normalize(SpeechLanguage.EN))
    }

    @Test
    fun isSupported_acceptsOnlyFrenchForOfflineV0() {
        assertTrue(OfflineLanguagePolicy.isSupported(Language.FRENCH))
        assertFalse(OfflineLanguagePolicy.isSupported(Language.ENGLISH))
        assertFalse(OfflineLanguagePolicy.isSupported(Language.UNKNOWN))
    }
}
