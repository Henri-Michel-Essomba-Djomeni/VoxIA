package com.voxia.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.voxia.utils.PrivacyLog
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * OCRModule - Lecture de document via ML Kit OCR (F2 - "Lis ce document")
 *
 * Différence avec TextTranslatorModule :
 *   - OCRModule = lire à voix haute sans traduction (F2 natif)
 *   - TextTranslatorModule = lire + traduire (F2-TRANSLATE)
 *
 * Pipeline F2 :
 *   CAMÉRA → contrôle qualité (FrameQualityAnalyzer) → ML Kit OCR → Structuration du texte → TTS
 *
 * Le contrôle qualité est un premier incrément post-capture (voir FrameQualityAnalyzer),
 * pas encore le guidage caméra temps réel visé par PLAN_ACTION_VOXIA.md §7.4/Phase 2.
 *
 * Optimisations :
 *   - Rejet précoce des images trop sombres, trop claires ou floues (pas d'appel ML Kit inutile)
 *   - Détection automatique des blocs de texte (paragraphes, lignes)
 *   - Filtrage des blocs trop courts pour être une lecture utile
 *   - Formatage intelligent pour une lecture fluide
 *   - Capture unique (pas de flux continu) pour économiser la RAM
 *
 * Budget RAM : ~150 Mo (ML Kit OCR chargé à la demande)
 */
class OCRModule(private val context: Context) {

    companion object {
        private const val TAG = "OCRModule"
        private const val MIN_BLOCK_LENGTH = 3     // min chars par bloc pour être retenu
    }

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCapturing = false

    // ML Kit OCR - chargé à la demande
    private var ocrClient = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // =========== INITIALISATION ===========

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null,
        onReady: (Boolean) -> Unit = {}
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            completeCameraInitialization(
                initialize = {
                    cameraProvider = future.get()
                    bindCamera(lifecycleOwner, previewView)
                },
                onFailure = {
                    cameraProvider = null
                    imageCaptureUseCase = null
                    PrivacyLog.e(TAG, "Initialisation OCR impossible")
                },
                onReady = { ready ->
                    if (ready) PrivacyLog.d(TAG, "OCRModule initialisé")
                    onReady(ready)
                }
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView?
    ): Boolean {
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build()
            .also { previewView?.let { pv -> it.setSurfaceProvider(pv.surfaceProvider) } }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            this.imageCaptureUseCase = capture
            return true
        } catch (e: Exception) {
            PrivacyLog.e(TAG, "Erreur liaison caméra")
            return false
        }
    }

    private var imageCaptureUseCase: ImageCapture? = null

    // =========== LECTURE DE DOCUMENT (F2) ===========

    /**
     * Capture une image et extrait le texte pour lecture vocale.
     *
     * @param language Langue de l'interface ("fr" ou "en")
     * @param callback Texte formaté prêt pour TTS
     */
    fun readDocument(
        language: String = "fr",
        callback: (OCRResult) -> Unit
    ) {
        if (isCapturing) return
        isCapturing = true

        val capture = imageCaptureUseCase ?: run {
            callback(OCRResult.Error("Caméra non initialisée"))
            isCapturing = false
            return
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processImageForReading(imageProxy, language, callback)
                    isCapturing = false
                }

                override fun onError(exception: ImageCaptureException) {
                    PrivacyLog.e(TAG, "Erreur capture OCR")
                    callback(OCRResult.Error("Impossible de capturer l'image"))
                    isCapturing = false
                }
            }
        )
    }

    /**
     * OCR sur un Bitmap existant (mode hors-caméra)
     */
    fun readFromBitmap(
        bitmap: Bitmap,
        language: String = "fr",
        callback: (OCRResult) -> Unit
    ) {
        val quality = FrameQualityAnalyzer.analyze(bitmap)
        if (quality !is FrameQuality.Acceptable) {
            callback(buildQualityGuidance(quality, language))
            return
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        performOCR(image) { visionText ->
            val result = buildOCRResult(visionText, language)
            callback(result)
        }
    }

    // =========== PIPELINE OCR ===========

    private fun processImageForReading(
        imageProxy: ImageProxy,
        language: String,
        callback: (OCRResult) -> Unit
    ) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = runCatching { imageProxy.toBitmap() }.getOrNull() ?: run {
            imageProxy.close()
            callback(OCRResult.Error("Impossible de traiter l'image"))
            return
        }
        imageProxy.close()

        val quality = FrameQualityAnalyzer.analyze(bitmap)
        if (quality !is FrameQuality.Acceptable) {
            PrivacyLog.d(TAG, "Capture rejetée avant OCR: qualite=${quality::class.simpleName}")
            callback(buildQualityGuidance(quality, language))
            bitmap.recycle()
            return
        }

        val image = InputImage.fromBitmap(bitmap, rotation)
        performOCR(image) { visionText ->
            val result = buildOCRResult(visionText, language)
            callback(result)
            bitmap.recycle()
        }
    }

    /** Traduit un [FrameQuality] défavorable en consigne vocale actionnable. */
    private fun buildQualityGuidance(quality: FrameQuality, language: String): OCRResult.PoorQuality {
        val message = when (quality) {
            is FrameQuality.TooDark -> if (language == "fr")
                "Image trop sombre. Rapprochez-vous d'une lumière et réessayez."
            else "Image too dark. Move to better light and try again."
            is FrameQuality.TooBright -> if (language == "fr")
                "Image trop claire ou reflet détecté. Réduisez la lumière directe et réessayez."
            else "Image too bright or glare detected. Reduce direct light and try again."
            is FrameQuality.TooBlurry -> if (language == "fr")
                "Image floue. Maintenez le téléphone stable et réessayez."
            else "Image blurry. Hold the phone steady and try again."
            is FrameQuality.Acceptable -> if (language == "fr")
                "Image insuffisante pour la lecture." else "Image not suitable for reading."
        }
        return OCRResult.PoorQuality(quality::class.simpleName ?: "unknown", message)
    }

    private fun performOCR(
        image: InputImage,
        onResult: (Text?) -> Unit
    ) {
        ocrClient.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText)
            }
            .addOnFailureListener { _ ->
                PrivacyLog.e(TAG, "Échec OCR")
                onResult(null)
            }
    }

    /**
     * Structuration du texte OCR pour lecture vocale fluide.
     *
     * ML Kit retourne des blocs → lignes → éléments.
     * On reconstitue une lecture naturelle en triant par position verticale.
     */
    private fun buildOCRResult(visionText: Text?, language: String): OCRResult {
        if (visionText == null || visionText.text.isBlank()) {
            return OCRResult.NoText(
                if (language == "fr") "Aucun texte détecté dans l'image."
                else "No text detected in the image."
            )
        }

        val rawText = visionText.text
        PrivacyLog.d(TAG, "Texte OCR détecté: chars=${rawText.length}")

        // Filtrer et nettoyer les blocs
        val cleanedBlocks = visionText.textBlocks
            .filter { block -> block.text.length >= MIN_BLOCK_LENGTH }
            .sortedBy { it.boundingBox?.top ?: 0 } // Trier haut → bas
            .map { block ->
                block.lines.joinToString(" ") { line ->
                    line.text.trim()
                }
            }
            .filter { it.isNotBlank() }

        if (cleanedBlocks.isEmpty()) {
            return OCRResult.NoText(
                if (language == "fr") "Le texte n'est pas suffisamment lisible."
                else "The text is not legible enough."
            )
        }

        val structuredText = cleanedBlocks.joinToString(". ") { it }
        val segments = DocumentTextSegmenter.segment(cleanedBlocks)
        val wordCount = structuredText.split("\\s+".toRegex()).size
        val estimatedReadingSeconds = (wordCount / 2.5).toInt() // ~150 mots/min

        val voiceIntro = if (language == "fr")
            "J'ai détecté $wordCount mots. Voici le contenu : "
        else
            "I detected $wordCount words. Here is the content: "

        return OCRResult.Success(
            rawText = rawText,
            structuredText = structuredText,
            voiceText = voiceIntro + structuredText,
            segments = segments,
            blockCount = cleanedBlocks.size,
            wordCount = wordCount,
            estimatedReadingSeconds = estimatedReadingSeconds
        )
    }

    // =========== UTILITAIRES ===========

    fun release() {
        ocrClient.close()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        PrivacyLog.d(TAG, "OCRModule libéré")
    }
}

/**
 * Convertit toute exception d'initialisation caméra en résultat terminal unique.
 *
 * Cette logique reste indépendante d'Android pour couvrir le chemin d'échec en test JVM.
 */
internal fun completeCameraInitialization(
    initialize: () -> Boolean,
    onFailure: (Exception) -> Unit = {},
    onReady: (Boolean) -> Unit
) {
    val ready = try {
        initialize()
    } catch (error: Exception) {
        onFailure(error)
        false
    }
    onReady(ready)
}

// =========== DATA CLASSES RÉSULTATS OCR ===========

sealed class OCRResult {
    data class Success(
        val rawText: String,
        val structuredText: String,
        val voiceText: String,       // Texte prêt pour TTS
        val segments: List<String>,
        val blockCount: Int,
        val wordCount: Int,
        val estimatedReadingSeconds: Int
    ) : OCRResult()

    data class NoText(val message: String) : OCRResult()
    data class Error(val message: String) : OCRResult()

    /** Capture rejetée avant OCR par [FrameQualityAnalyzer] : [reason] est le nom du [FrameQuality] défavorable. */
    data class PoorQuality(val reason: String, val message: String) : OCRResult()
}
