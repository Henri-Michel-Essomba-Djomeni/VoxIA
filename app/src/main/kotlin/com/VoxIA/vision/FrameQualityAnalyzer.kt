package com.voxia.vision

import android.graphics.Bitmap

/**
 * Premier incrément de guidage caméra (voir PLAN_ACTION_VOXIA.md §7.4 et §8 Phase 2).
 *
 * Ce n'est PAS le guidage temps réel continu décrit dans l'architecture cible
 * (flux `ImageAnalysis` + retour vocal/haptique avant capture). C'est un contrôle
 * post-capture, immédiat et local, qui évite de lancer l'OCR ML Kit sur une image
 * manifestement inexploitable (trop sombre, trop claire/reflet, floue) et donne
 * à la place une consigne vocale actionnable.
 *
 * Les seuils ci-dessous sont des heuristiques de démarrage, non calibrées sur un
 * jeu de données réel. Ils doivent être révisés dès que le pilote OCR décrit en
 * §7.4 du plan directeur (100+ captures utilisateurs, vérité terrain) est disponible
 * dans `evaluation/ocr/`. Ne pas présenter ces seuils comme une mesure validée.
 */
object FrameQualityAnalyzer {

    private const val DARK_MEAN_LUMA = 60.0
    private const val BRIGHT_MEAN_LUMA = 235.0
    private const val BLUR_VARIANCE_FLOOR = 12.0
    private const val MAX_SAMPLE_DIMENSION = 160

    fun analyze(bitmap: Bitmap): FrameQuality {
        val scale = MAX_SAMPLE_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height, 1)
        val sample = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap

        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        val quality = analyzePixels(pixels, sample.width, sample.height)
        if (sample !== bitmap) sample.recycle()
        return quality
    }

    /**
     * Logique pure, sans dépendance Android, pour rester testable en JVM classique.
     * [pixels] est un tableau ARGB de taille [width] * [height] (format `Bitmap.getPixels`).
     */
    fun analyzePixels(pixels: IntArray, width: Int, height: Int): FrameQuality {
        require(pixels.size == width * height) { "pixels size must equal width*height" }
        if (width < 3 || height < 3) return FrameQuality.Acceptable(meanLuma = 128.0, blurVariance = Double.MAX_VALUE)

        val luma = DoubleArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            luma[i] = 0.299 * r + 0.587 * g + 0.114 * b
        }
        val meanLuma = luma.average()

        // Variance d'un Laplacien discret simple : proxy classique de netteté.
        // Une image nette a des transitions de bords marquées (variance élevée),
        // une image floue aplatit ces transitions (variance faible).
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val lap = 4 * luma[idx] - luma[idx - 1] - luma[idx + 1] - luma[idx - width] - luma[idx + width]
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)

        return when {
            meanLuma < DARK_MEAN_LUMA -> FrameQuality.TooDark(meanLuma)
            meanLuma > BRIGHT_MEAN_LUMA -> FrameQuality.TooBright(meanLuma)
            variance < BLUR_VARIANCE_FLOOR -> FrameQuality.TooBlurry(variance)
            else -> FrameQuality.Acceptable(meanLuma, variance)
        }
    }
}

sealed class FrameQuality {
    data class Acceptable(val meanLuma: Double, val blurVariance: Double) : FrameQuality()
    data class TooDark(val meanLuma: Double) : FrameQuality()
    data class TooBright(val meanLuma: Double) : FrameQuality()
    data class TooBlurry(val blurVariance: Double) : FrameQuality()
}
