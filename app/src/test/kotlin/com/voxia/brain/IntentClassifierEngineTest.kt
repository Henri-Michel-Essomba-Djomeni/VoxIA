package com.voxia.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentClassifierEngineTest {
    private val engine = IntentClassifierEngine()

    @Test
    fun classify_distinguishesVolumeDirections() {
        assertEquals(Intent.VOLUME_UP, engine.classify("Augmente le volume", Language.FRENCH).intent)
        assertEquals(Intent.VOLUME_DOWN, engine.classify("Baisse le volume", Language.FRENCH).intent)
        assertEquals(Intent.VOLUME_DOWN, engine.classify("volume down", Language.ENGLISH).intent)
    }

    @Test
    fun classify_recognizesVisionModes() {
        assertEquals(Intent.IDENTIFY_OBJECT, engine.classify("Identifie cet objet", Language.FRENCH).intent)
        assertEquals(Intent.SCAN_PRODUCT, engine.classify("Scanne ce produit", Language.FRENCH).intent)
        assertEquals(Intent.TRANSLATE_TEXT, engine.classify("Lis et traduis", Language.FRENCH).intent)
    }

    @Test
    fun classify_recognizesDocumentReadingNavigation() {
        assertEquals(Intent.READ_NEXT_SEGMENT, engine.classify("Lis la suite", Language.FRENCH).intent)
        assertEquals(Intent.READ_NEXT_SEGMENT, engine.classify("continue reading", Language.ENGLISH).intent)
        assertEquals(Intent.READ_PREVIOUS_SEGMENT, engine.classify("segment précédent", Language.FRENCH).intent)
        assertEquals(Intent.READ_PREVIOUS_SEGMENT, engine.classify("previous segment", Language.ENGLISH).intent)
    }

    @Test
    fun classify_recognizesDocumentReadingExportCommands() {
        assertEquals(Intent.COPY_READING_TEXT, engine.classify("Copie le texte", Language.FRENCH).intent)
        assertEquals(Intent.COPY_READING_TEXT, engine.classify("copy reading", Language.ENGLISH).intent)
        assertEquals(Intent.SHARE_READING_TEXT, engine.classify("Partage la lecture", Language.FRENCH).intent)
        assertEquals(Intent.SHARE_READING_TEXT, engine.classify("share text", Language.ENGLISH).intent)
    }

    @Test
    fun classify_extractsAlarmTimeAndReminderDuration() {
        val alarm = engine.classify("Mets une alarme à 7 h 30", Language.FRENCH)
        assertEquals(Intent.SET_ALARM, alarm.intent)
        assertEquals(7, alarm.extractedHour)
        assertEquals(30, alarm.extractedMinute)

        val reminder = engine.classify("Rappelle moi dans 2 heures", Language.FRENCH)
        assertEquals(Intent.SET_REMINDER, reminder.intent)
        assertEquals(120, reminder.extractedDurationMinutes)
    }

    @Test
    fun classify_extractsEntities() {
        assertEquals("maman", engine.classify("Appelle maman", Language.FRENCH).extractedContact)
        assertEquals("youtube", engine.classify("Ouvre YouTube", Language.FRENCH).extractedAppName)
        assertEquals("2 plus 2", engine.classify("Calcule 2 plus 2", Language.FRENCH).extractedExpression)
    }

    @Test
    fun classify_unknownPhraseFallsBackWithoutFakeConfidence() {
        val result = engine.classify("phrase totalement inconnue", Language.FRENCH)
        assertEquals(Intent.FALLBACK, result.intent)
        assertEquals(0f, result.confidence)
        assertNull(result.extractedContact)
    }

    @Test
    fun classify_normalizesAccentsAndReturnsHighConfidence() {
        val result = engine.classify("Décris mon environnement", Language.FRENCH)
        assertEquals(Intent.DESCRIBE_SURROUNDINGS, result.intent)
        assertTrue(result.confidence >= 0.9f)
    }
}
