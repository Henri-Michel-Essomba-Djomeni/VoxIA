package com.voxia.vision

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionModule(private val context: Context) {

    companion object {
        private const val TAG = "VisionModule"

        private val LABEL_FR = mapOf(
            // Personnes & animaux
            "person" to "personne", "man" to "homme", "woman" to "femme",
            "child" to "enfant", "baby" to "bébé",
            "bird" to "oiseau", "cat" to "chat", "dog" to "chien",
            "horse" to "cheval", "sheep" to "mouton", "cow" to "vache",
            "elephant" to "éléphant", "bear" to "ours",
            "zebra" to "zèbre", "giraffe" to "girafe",
            "butterfly" to "papillon", "insect" to "insecte",
            "fish" to "poisson",

            // Véhicules
            "bicycle" to "vélo", "car" to "voiture", "motorcycle" to "moto",
            "airplane" to "avion", "bus" to "bus", "train" to "train",
            "truck" to "camion", "boat" to "bateau",
            "bicycle" to "vélo", "scooter" to "scooter",
            "helicopter" to "hélicoptère", "ambulance" to "ambulance",

            // Signalisation
            "traffic light" to "feu tricolore", "fire hydrant" to "bouche d'incendie",
            "stop sign" to "panneau stop", "parking meter" to "parcmètre",
            "bench" to "banc",

            // Accessoires
            "backpack" to "sac à dos", "umbrella" to "parapluie",
            "handbag" to "sac à main", "tie" to "cravate",
            "suitcase" to "valise", "hat" to "chapeau",
            "glasses" to "lunettes", "sunglasses" to "lunettes de soleil",
            "wallet" to "portefeuille", "watch" to "montre",
            "belt" to "ceinture",

            // Équipement sportif
            "frisbee" to "frisbee", "skis" to "skis",
            "snowboard" to "snowboard", "sports ball" to "ballon",
            "kite" to "cerf-volant", "baseball bat" to "batte",
            "baseball glove" to "gant", "skateboard" to "skateboard",
            "surfboard" to "planche de surf", "tennis racket" to "raquette",
            "ball" to "balle", "racket" to "raquette",

            // Nourriture & boissons
            "bottle" to "bouteille", "plate" to "assiette",
            "wine glass" to "verre à vin", "cup" to "tasse",
            "fork" to "fourchette", "knife" to "couteau",
            "spoon" to "cuillère", "bowl" to "bol",
            "banana" to "banane", "apple" to "pomme",
            "sandwich" to "sandwich", "orange" to "orange",
            "broccoli" to "brocoli", "carrot" to "carotte",
            "hot dog" to "hot dog", "pizza" to "pizza",
            "donut" to "donut", "cake" to "gâteau",
            "cookie" to "biscuit", "bread" to "pain",
            "cheese" to "fromage", "egg" to "œuf",
            "rice" to "riz", "pasta" to "pâtes",
            "soup" to "soupe", "salad" to "salade",
            "water" to "eau", "juice" to "jus",
            "milk" to "lait", "coffee" to "café",
            "tea" to "thé", "beer" to "bière",

            // Maison & meubles
            "chair" to "chaise", "couch" to "canapé",
            "potted plant" to "plante en pot", "bed" to "lit",
            "dining table" to "table à manger", "toilet" to "toilette",
            "table" to "table", "door" to "porte",
            "window" to "fenêtre", "mirror" to "miroir",
            "lamp" to "lampe", "pillow" to "oreiller",
            "curtain" to "rideau", "rug" to "tapis",
            "shelf" to "étagère", "drawer" to "tiroir",
            "cabinet" to "armoire",

            // Électronique
            "tv" to "télévision", "laptop" to "ordinateur portable",
            "mouse" to "souris", "remote" to "télécommande",
            "keyboard" to "clavier", "cell phone" to "téléphone",
            "phone" to "téléphone", "computer" to "ordinateur",
            "tablet" to "tablette", "charger" to "chargeur",
            "earphone" to "écouteurs", "headphone" to "casque",
            "speaker" to "enceinte", "camera" to "appareil photo",
            "printer" to "imprimante", "screen" to "écran",
            "television" to "télévision", "monitor" to "moniteur",

            // Objets divers
            "book" to "livre", "clock" to "horloge",
            "vase" to "vase", "scissors" to "ciseaux",
            "teddy bear" to "ours en peluche",
            "hair drier" to "sèche-cheveux",
            "toothbrush" to "brosse à dents",
            "key" to "clé", "keys" to "clés",
            "pen" to "stylo", "pencil" to "crayon",
            "paper" to "papier", "notebook" to "carnet",
            "bag" to "sac", "box" to "boîte",
            "basket" to "panier", "rope" to "corde",
            "lighter" to "briquet", "candle" to "bougie",
            "spoon" to "cuillère", "pot" to "marmite",
            "pan" to "poêle", "bucket" to "seau",
            "tool" to "outil", "hammer" to "marteau",
            "screwdriver" to "tournevis", "knife" to "couteau",
            "money" to "argent", "coin" to "pièce",
            "card" to "carte", "ticket" to "billet",
            "glove" to "gant", "mask" to "masque",
            "shoe" to "chaussure", "clothing" to "vêtement",
            "towel" to "serviette", "soap" to "savon"
        )
    }

    private var objectDetector: ObjectDetector? = null
    private var isDetecting = false
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var detectionCallback: ((List<DetectionResult>) -> Unit)? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(lifecycleOwner, previewView)
            Log.d(TAG, "CameraX initialisé")
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView?
    ) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val preview = Preview.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .build()
            .also { previewView?.let { pv -> it.setSurfaceProvider(pv.surfaceProvider) } }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isDetecting) {
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

    fun loadModel(): Boolean {
        if (objectDetector != null) return true
        return try {
            val options = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableClassification()
                .build()
            objectDetector = ObjectDetection.getClient(options)
            Log.d(TAG, "ML Kit Object Detection prêt (~10 Mo, 400+ classes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Échec chargement ML Kit: ${e.message}")
            false
        }
    }

    fun releaseModel() {
        objectDetector?.close()
        objectDetector = null
        Log.d(TAG, "ML Kit libéré")
    }

    fun startDetection(callback: (List<DetectionResult>) -> Unit) {
        if (objectDetector == null && !loadModel()) {
            callback(emptyList())
            return
        }
        detectionCallback = callback
        isDetecting = true
        Log.d(TAG, "Détection démarrée")
    }

    fun stopDetection() {
        isDetecting = false
        detectionCallback = null
        Log.d(TAG, "Détection arrêtée")
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = objectDetector ?: run { imageProxy.close(); return }

        detector.process(inputImage)
            .addOnSuccessListener { objects ->
                val results = objects.mapNotNull { obj ->
                    val label = obj.labels.firstOrNull() ?: return@mapNotNull null
                    DetectionResult(
                        classId = 0,
                        label = label.text,
                        confidence = label.confidence,
                        boundingBox = BoundingBox(
                            x1 = obj.boundingBox.left.toFloat() / imageProxy.width.coerceAtLeast(1),
                            y1 = obj.boundingBox.top.toFloat() / imageProxy.height.coerceAtLeast(1),
                            x2 = obj.boundingBox.right.toFloat() / imageProxy.width.coerceAtLeast(1),
                            y2 = obj.boundingBox.bottom.toFloat() / imageProxy.height.coerceAtLeast(1)
                        )
                    )
                }
                if (results.isNotEmpty()) {
                    detectionCallback?.invoke(results)
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    fun buildVoiceDescription(results: List<DetectionResult>, language: String): String {
        if (results.isEmpty()) {
            return if (language == "fr") "Je ne détecte aucun objet clairement."
            else "I cannot detect any object clearly."
        }
        val top = results.take(5)
        return if (language == "fr") {
            val items = top.joinToString(", ") {
                val label = LABEL_FR[it.label.lowercase()] ?: it.label
                "$label (${(it.confidence * 100).toInt()}%)"
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

    fun release() {
        stopDetection()
        releaseModel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "VisionModule libéré")
    }
}

data class DetectionResult(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)
