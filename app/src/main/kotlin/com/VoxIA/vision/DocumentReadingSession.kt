package com.voxia.vision

data class ReadingPosition(
    val index: Int,
    val total: Int,
    val text: String
) {
    val number: Int = index + 1
}

class DocumentReadingSession private constructor(private val segments: List<String>) {
    private var currentIndex = 0

    fun current(): ReadingPosition? = positionAt(currentIndex)

    fun next(): ReadingPosition? {
        if (currentIndex >= segments.lastIndex) return null
        currentIndex += 1
        return current()
    }

    fun previous(): ReadingPosition? {
        if (currentIndex <= 0) return null
        currentIndex -= 1
        return current()
    }

    fun isAtEnd(): Boolean = segments.isEmpty() || currentIndex >= segments.lastIndex

    private fun positionAt(index: Int): ReadingPosition? =
        segments.getOrNull(index)?.let { ReadingPosition(index, segments.size, it) }

    companion object {
        fun fromSegments(segments: List<String>): DocumentReadingSession? {
            val clean = segments.map { it.trim() }.filter { it.isNotBlank() }
            return if (clean.isEmpty()) null else DocumentReadingSession(clean)
        }
    }
}

object DocumentTextSegmenter {
    fun segment(blocks: List<String>, maxWordsPerSegment: Int = 55): List<String> {
        val cleanBlocks = blocks.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanBlocks.isEmpty()) return emptyList()

        val segments = mutableListOf<String>()
        val current = mutableListOf<String>()
        var wordCount = 0

        fun flush() {
            if (current.isNotEmpty()) {
                segments += current.joinToString(". ")
                current.clear()
                wordCount = 0
            }
        }

        cleanBlocks.forEach { block ->
            val blockWords = block.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (blockWords.size > maxWordsPerSegment) {
                flush()
                blockWords.chunked(maxWordsPerSegment).forEach { chunk ->
                    segments += chunk.joinToString(" ")
                }
                return@forEach
            }
            if (wordCount > 0 && wordCount + blockWords.size > maxWordsPerSegment) flush()
            current += block
            wordCount += blockWords.size
        }
        flush()
        return segments
    }
}
