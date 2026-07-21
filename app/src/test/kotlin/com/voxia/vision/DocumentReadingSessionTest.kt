package com.voxia.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentReadingSessionTest {

    @Test
    fun fromSegments_discardsBlankSegments() {
        val session = DocumentReadingSession.fromSegments(listOf("  ", "Premier segment", "", "Second segment"))

        assertEquals("Premier segment", session?.current()?.text)
        assertEquals(2, session?.current()?.total)
    }

    @Test
    fun nextAndPrevious_moveWithinBounds() {
        val session = DocumentReadingSession.fromSegments(listOf("A", "B"))!!

        assertEquals("A", session.current()?.text)
        assertEquals("B", session.next()?.text)
        assertTrue(session.isAtEnd())
        assertNull(session.next())
        assertEquals("A", session.previous()?.text)
        assertFalse(session.isAtEnd())
        assertNull(session.previous())
    }

    @Test
    fun segment_groupsShortBlocksAndSplitsLongBlocks() {
        val longBlock = (1..12).joinToString(" ") { "mot$it" }

        val segments = DocumentTextSegmenter.segment(
            blocks = listOf("Titre court", "Deuxieme bloc lisible", longBlock),
            maxWordsPerSegment = 5
        )

        assertEquals("Titre court. Deuxieme bloc lisible", segments[0])
        assertEquals("mot1 mot2 mot3 mot4 mot5", segments[1])
        assertEquals("mot11 mot12", segments[3])
    }
}
