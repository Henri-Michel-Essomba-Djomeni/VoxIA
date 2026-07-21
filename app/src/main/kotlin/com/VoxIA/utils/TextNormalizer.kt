package com.voxia.utils

import java.text.Normalizer

object TextNormalizer {
    fun normalize(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('’', ' ')
        .replace('\'', ' ')
        .replace(Regex("[^a-z0-9:+*/().,\\-\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
