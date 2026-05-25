package com.voxia.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
 *   CAMÉRA → Stabilisation → ML Kit OCR → Structuration du texte → TTS
 *
 * Optimisations :
 *   - Détection automatique des blocs de texte (paragraphes, lignes)
 *   - Filtrage du texte fragmenté ou peu fiable (confiance < seuil)
 *   - Formatage intelligent pour une lecture fluide
 *   - Capture unique (pas de flux continu) pour économiser la RAM
 *
 * Budget RAM : ~150 Mo (ML Kit OCR chargé à la demande)
 */
class OCRModule(private val context: Context) {

    companion object {
        private const val TAG = "OCRModule"
        private const val MIN_CONFIDENCE = 0.7f    // seuil de confiance OCR
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
        previewView: androidx.camera.view.PreviewView? = null
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera(lifecycleOwner, previewView)
            Log.d(TAG, "OCRModule initialisé")
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView?
    ) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Erreur liaison caméra: ${e.message}")
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
                    Log.e(TAG, "Erreur capture: ${exception.message}")
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
        val bitmap = imageProxy.toBitmapSafe() ?: run {
            imageProxy.close()
            callback(OCRResult.Error("Impossible de traiter l'image"))
            return
        }
        imageProxy.close()

        val image = InputImage.fromBitmap(bitmap, 0)
        performOCR(image) { visionText ->
            val result = buildOCRResult(visionText, language)
            callback(result)
        }
    }

    private fun performOCR(
        image: InputImage,
        onResult: (Text?) -> Unit
    ) {
        ocrClient.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Échec OCR: ${e.message}")
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
        Log.d(TAG, "Texte OCR brut: ${rawText.take(200)}")

        // Filtrer et nettoyer les blocs
        val cleanedBlocks = visionText.textBlocks
            .filter { block ->
                block.text.length >= MIN_BLOCK_LENGTH &&
                (block.confidence ?: 1f) >= MIN_CONFIDENCE
            }
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
            blockCount = cleanedBlocks.size,
            wordCount = wordCount,
            estimatedReadingSeconds = estimatedReadingSeconds
        )
    }

    // =========== UTILITAIRES ===========

    private fun ImageProxy.toBitmapSafe(): Bitmap? {
        return try {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            null
        }
    }

    fun release() {
        ocrClient.close()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "OCRModule libéré")
    }
}

// =========== DATA CLASSES RÉSULTATS OCR ===========

sealed class OCRResult {
    data class Success(
        val rawText: String,
        val structuredText: String,
        val voiceText: String,       // Texte prêt pour TTS
        val blockCount: Int,
        val wordCount: Int,
        val estimatedReadingSeconds: Int
    ) : OCRResult()

    data class NoText(val message: String) : OCRResult()
    data class Error(val message: String) : OCRResult()
}
