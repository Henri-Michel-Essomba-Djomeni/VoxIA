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
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * VisionModule - Module complet de détection d'objets pour VOXIA
 * 
 * Responsabilités :
 *  - Initialiser CameraX en preview + analyse d'images
 *  - Charger/décharger YOLOv8n INT8 à la demande (gestion mémoire dynamique)
 *  - Exécuter l'inférence TFLite sur chaque frame
 *  - Post-traitement NMS + décodage des 50 classes VOXIA
 *  - Retourner les résultats via callback pour le VoiceAssistantService
 *
 * Usage (depuis VoiceAssistantService) :
 *   val vision = VisionModule(context)
 *   vision.initialize(lifecycleOwner, previewView)
 *   vision.startDetection { results -> speakResults(results) }
 *   vision.release()  // libère YOLOv8n de la RAM
 */
class VisionModule(private val context: Context) {

    companion object {
        private const val TAG = "VisionModule"
        private const val MODEL_FILE = "yolov8n_int8.tflite"
        private const val INPUT_SIZE = 416          // taille d'entrée du modèle
        private const val NUM_CLASSES = 50          // 50 classes VOXIA
        private const val CONF_THRESHOLD = 0.45f   // seuil de confiance
        private const val IOU_THRESHOLD = 0.45f    // seuil IoU pour NMS
        private const val MAX_DETECTIONS = 10       // max objets renvoyés

        // 50 classes VOXIA (30 COCO + 20 OpenImages)
        val VOXIA_CLASSES = arrayOf(
            // COCO 30 classes
            "personne", "velo", "voiture", "moto", "avion",
            "bus", "train", "camion", "bateau", "feux_circulation",
            "bouteille", "verre", "tasse", "fourchette", "couteau",
            "cuillere", "bol", "banane", "pomme", "sandwich",
            "chaise", "canape", "ecran", "ordinateur_portable", "souris",
            "telephone", "livre", "horloge", "sac", "valise",
            // OpenImages 20 classes
            "billet_banque", "piece_monnaie", "cle", "lunettes", "chapeau",
            "panier", "jerrican", "seau", "machette", "lanterne",
            "sachet", "bougie", "pile", "stylo", "carnet",
            "masque", "gant", "ceinture", "portefeuille", "carte_sim"
        )
    }

    // =========== ÉTAT DU MODULE ===========
    private var tfliteInterpreter: Interpreter? = null
    private var isModelLoaded = false
    private var isDetecting = false
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var detectionCallback: ((List<DetectionResult>) -> Unit)? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Buffers pré-alloués pour éviter les allocations en boucle d'inférence
    private val inputBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 1) // INT8
            .apply { order(ByteOrder.nativeOrder()) }
    }

    // =========== INITIALISATION CAMÉRA ===========

    /**
     * Initialise CameraX avec preview et analyse d'images.
     * Doit être appelé depuis le thread principal.
     *
     * @param lifecycleOwner Activité ou Fragment propriétaire
     * @param previewView Vue de prévisualisation (peut être null si pas d'UI)
     */
    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner, previewView)
            Log.d(TAG, "CameraX initialisé avec succès")
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView?
    ) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Preview (facultatif pour les malvoyants, utile pour debug)
        val preview = Preview.Builder()
            .setTargetResolution(android.util.Size(INPUT_SIZE, INPUT_SIZE))
            .build()
            .also { previewView?.let { pv -> it.setSurfaceProvider(pv.surfaceProvider) } }

        // Analyse d'images pour l'inférence IA
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(INPUT_SIZE, INPUT_SIZE))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isDetecting && isModelLoaded) {
                        processFrame(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Échec liaison CameraX: ${e.message}")
        }
    }

    // =========== GESTION DU MODÈLE YOLOv8n ===========

    /**
     * Charge le modèle YOLOv8n INT8 en mémoire.
     * À appeler uniquement quand F1 est demandé (gestion RAM dynamique).
     * Budget RAM : ~180 Mo
     */
    fun loadModel(): Boolean {
        if (isModelLoaded) return true
        return try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // Tenter l'accélération NNAPI (Snapdragon/MediaTek)
                setUseNNAPI(true)
            }
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            tfliteInterpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            Log.d(TAG, "YOLOv8n INT8 chargé en mémoire (~180 Mo)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Échec chargement YOLOv8n: ${e.message}")
            false
        }
    }

    /**
     * Libère le modèle de la RAM.
     * Toujours appeler cette méthode après F1 pour libérer ~180 Mo.
     */
    fun releaseModel() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
        isModelLoaded = false
        System.gc()
        Log.d(TAG, "YOLOv8n déchargé de la RAM")
    }

    // =========== DÉTECTION D'OBJETS (F1) ===========

    /**
     * Démarre la détection continue d'objets.
     * Le callback est appelé à chaque frame avec les objets détectés.
     *
     * @param callback Fonction appelée avec la liste des objets détectés
     */
    fun startDetection(callback: (List<DetectionResult>) -> Unit) {
        if (!isModelLoaded) {
            Log.w(TAG, "Modèle non chargé. Chargement en cours...")
            if (!loadModel()) {
                callback(emptyList())
                return
            }
        }
        detectionCallback = callback
        isDetecting = true
        Log.d(TAG, "Détection d'objets démarrée")
    }

    /**
     * Arrête la détection continue.
     */
    fun stopDetection() {
        isDetecting = false
        detectionCallback = null
        Log.d(TAG, "Détection d'objets arrêtée")
    }

    /**
     * Détection unique sur un Bitmap (pour les captures ponctuelles).
     *
     * @param bitmap Image à analyser
     * @return Liste des objets détectés
     */
    fun detectOnBitmap(bitmap: Bitmap): List<DetectionResult> {
        if (!isModelLoaded) return emptyList()
        val preprocessed = preprocessBitmap(bitmap)
        return runInference(preprocessed)
    }

    // =========== PIPELINE D'INFÉRENCE ===========

    private fun processFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() ?: run {
            imageProxy.close()
            return
        }
        val preprocessed = preprocessBitmap(bitmap)
        val results = runInference(preprocessed)
        if (results.isNotEmpty()) {
            detectionCallback?.invoke(results)
        }
        imageProxy.close()
    }

    /**
     * Prétraitement : redimensionnement + normalisation INT8 [-128, 127]
     */
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            // INT8 quantisé : (valeur / 255.0 - 0.5) * 2 → [-128, 127]
            inputBuffer.put(((pixel shr 16 and 0xFF) - 128).toByte()) // R
            inputBuffer.put(((pixel shr 8 and 0xFF) - 128).toByte())  // G
            inputBuffer.put(((pixel and 0xFF) - 128).toByte())         // B
        }
        if (scaled != bitmap) scaled.recycle()
        return inputBuffer
    }

    /**
     * Inférence TFLite + décodage YOLOv8 + NMS
     *
     * Sortie brute YOLOv8n : [1, 4 + NUM_CLASSES, 3549]
     * Format : (cx, cy, w, h, cls0, cls1, ..., cls49) pour chaque ancre
     */
    private fun runInference(inputBuffer: ByteBuffer): List<DetectionResult> {
        val interpreter = tfliteInterpreter ?: return emptyList()

        // Buffer de sortie YOLOv8n : [1, 54, 3549]
        val numAnchors = 3549 // pour imgsz=416
        val outputBuffer = Array(1) {
            Array(4 + NUM_CLASSES) { FloatArray(numAnchors) }
        }

        try {
            interpreter.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inférence: ${e.message}")
            return emptyList()
        }

        return postProcess(outputBuffer[0], numAnchors)
    }

    /**
     * Post-traitement : décodage des boîtes + filtrage confiance + NMS
     */
    private fun postProcess(
        output: Array<FloatArray>,
        numAnchors: Int
    ): List<DetectionResult> {
        val candidates = mutableListOf<DetectionResult>()

        for (i in 0 until numAnchors) {
            val cx = output[0][i]
            val cy = output[1][i]
            val w  = output[2][i]
            val h  = output[3][i]

            // Trouver la classe avec la probabilité maximale
            var maxScore = 0f
            var maxClass = 0
            for (c in 0 until NUM_CLASSES) {
                val score = output[4 + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClass = c
                }
            }

            if (maxScore >= CONF_THRESHOLD) {
                // Convertir cx,cy,w,h → x1,y1,x2,y2 normalisés [0,1]
                val x1 = (cx - w / 2f).coerceIn(0f, 1f)
                val y1 = (cy - h / 2f).coerceIn(0f, 1f)
                val x2 = (cx + w / 2f).coerceIn(0f, 1f)
                val y2 = (cy + h / 2f).coerceIn(0f, 1f)

                candidates.add(
                    DetectionResult(
                        classId = maxClass,
                        label = VOXIA_CLASSES.getOrElse(maxClass) { "inconnu" },
                        confidence = maxScore,
                        boundingBox = BoundingBox(x1, y1, x2, y2)
                    )
                )
            }
        }

        return applyNMS(candidates)
    }

    /**
     * Non-Maximum Suppression pour éliminer les doublons
     */
    private fun applyNMS(candidates: List<DetectionResult>): List<DetectionResult> {
        val sorted = candidates.sortedByDescending { it.confidence }
        val selected = mutableListOf<DetectionResult>()

        for (candidate in sorted) {
            if (selected.size >= MAX_DETECTIONS) break
            val shouldKeep = selected.none { existing ->
                computeIoU(existing.boundingBox, candidate.boundingBox) > IOU_THRESHOLD &&
                existing.classId == candidate.classId
            }
            if (shouldKeep) selected.add(candidate)
        }
        return selected
    }

    private fun computeIoU(a: BoundingBox, b: BoundingBox): Float {
        val interX1 = max(a.x1, b.x1)
        val interY1 = max(a.y1, b.y1)
        val interX2 = min(a.x2, b.x2)
        val interY2 = min(a.y2, b.y2)
        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        val aArea = (a.x2 - a.x1) * (a.y2 - a.y1)
        val bArea = (b.x2 - b.x1) * (b.y2 - b.y1)
        return if (aArea + bArea - interArea == 0f) 0f
        else interArea / (aArea + bArea - interArea)
    }

    // =========== UTILITAIRES ===========

    /**
     * Convertit un ImageProxy (format YUV_420_888) en Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap? {
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
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Conversion ImageProxy → Bitmap échouée: ${e.message}")
            null
        }
    }

    /**
     * Libération complète des ressources (camera + modèle)
     */
    fun release() {
        stopDetection()
        releaseModel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "VisionModule libéré")
    }

    /**
     * Génère un message vocal descriptif des objets détectés (FR ou EN)
     *
     * @param results Liste des détections
     * @param language "fr" ou "en"
     * @return Phrase à synthétiser vocalement
     */
    fun buildVoiceDescription(results: List<DetectionResult>, language: String): String {
        if (results.isEmpty()) {
            return if (language == "fr") "Je ne détecte aucun objet clairement."
            else "I cannot detect any object clearly."
        }
        val top = results.take(3)
        return if (language == "fr") {
            val items = top.joinToString(", ") {
                "${it.label} (${(it.confidence * 100).toInt()}%)"
            }
            if (top.size == 1) "Je détecte : $items."
            else "Je détecte ${top.size} objets : $items."
        } else {
            val items = top.joinToString(", ") {
                "${it.label} at ${(it.confidence * 100).toInt()}%"
            }
            if (top.size == 1) "I detect: $items."
            else "I detect ${top.size} objects: $items."
        }
    }
}

// =========== DATA CLASSES ===========

data class DetectionResult(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val x1: Float,  // gauche normalisée [0,1]
    val y1: Float,  // haut normalisée [0,1]
    val x2: Float,  // droite normalisée [0,1]
    val y2: Float   // bas normalisée [0,1]
)
