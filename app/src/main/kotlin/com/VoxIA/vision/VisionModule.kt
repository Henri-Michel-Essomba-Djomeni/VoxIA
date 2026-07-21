package com.voxia.vision

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.voxia.utils.PrivacyLog
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionModule(private val context: Context) {

    companion object {
        private const val TAG = "VisionModule"
        private const val CONFIDENCE_THRESHOLD = 0.58f
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(CONFIDENCE_THRESHOLD).build()
    )
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val productCatalog = ProductCatalog.load(context)
    private var initialized = false
    private var initializing = false
    private val readyCallbacks = mutableListOf<(Boolean) -> Unit>()

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null,
        onReady: (Boolean) -> Unit = {}
    ) {
        if (initialized) {
            onReady(true)
            return
        }
        readyCallbacks += onReady
        if (initializing) return
        initializing = true
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                cameraProvider = future.get()
                bindCamera(lifecycleOwner, previewView)
            }.onSuccess {
                initialized = true
                completeInitialization(true)
            }.onFailure { error ->
                PrivacyLog.e(TAG, "Initialisation CameraX impossible", error)
                completeInitialization(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(owner: LifecycleOwner, previewView: androidx.camera.view.PreviewView?) {
        val preview = Preview.Builder().build().also { useCase ->
            previewView?.let { useCase.setSurfaceProvider(it.surfaceProvider) }
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
    }

    private fun completeInitialization(success: Boolean) {
        initializing = false
        val callbacks = readyCallbacks.toList()
        readyCallbacks.clear()
        callbacks.forEach { it(success) }
    }

    fun loadModel(): Boolean = true
    fun releaseModel() = Unit

    fun startDetection(callback: (List<DetectionResult>) -> Unit) {
        captureAnalysis { result ->
            callback(result.labels.mapIndexed { index, label ->
                DetectionResult(index, label.text, label.confidence, BoundingBox(0f, 0f, 1f, 1f))
            })
        }
    }

    fun stopDetection() = Unit

    fun captureAnalysis(callback: (VisionAnalysis) -> Unit) {
        val capture = imageCapture
        if (!initialized || capture == null) {
            callback(VisionAnalysis(error = "Caméra non prête"))
            return
        }
        capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                analyze(imageProxy, callback)
            }

            override fun onError(exception: ImageCaptureException) {
                PrivacyLog.e(TAG, "Capture impossible", exception)
                callback(VisionAnalysis(error = exception.message ?: "Capture impossible"))
            }
        })
    }

    private fun analyze(imageProxy: ImageProxy, callback: (VisionAnalysis) -> Unit) {
        val bitmap = runCatching { imageProxy.toBitmap() }.getOrNull()
        val rotation = imageProxy.imageInfo.rotationDegrees
        imageProxy.close()
        if (bitmap == null) {
            callback(VisionAnalysis(error = "Conversion de l'image impossible"))
            return
        }

        val image = InputImage.fromBitmap(bitmap, rotation)
        val labelsTask = labeler.process(image)
        val barcodeTask = barcodeScanner.process(image)
        val textTask = textRecognizer.process(image)
        Tasks.whenAllComplete(labelsTask, barcodeTask, textTask)
            .addOnCompleteListener {
                val labels = if (labelsTask.isSuccessful) {
                    labelsTask.result
                        .sortedByDescending { it.confidence }
                        .take(8)
                        .map { VisionLabel(it.text, it.confidence) }
                } else {
                    emptyList()
                }
                val codes = if (barcodeTask.isSuccessful) {
                    barcodeTask.result.mapNotNull { it.displayValue ?: it.rawValue }.distinct()
                } else {
                    emptyList()
                }
                val text = if (textTask.isSuccessful) textTask.result.text.trim() else ""
                callback(VisionAnalysis(labels, codes, text, null))
                bitmap.recycle()
            }
    }

    fun buildVoiceDescription(results: List<DetectionResult>, language: String): String {
        val labels = results.map { VisionLabel(it.label, it.confidence) }
        return buildVoiceDescription(VisionAnalysis(labels = labels), language)
    }

    fun buildVoiceDescription(result: VisionAnalysis, language: String, productMode: Boolean = false): String {
        result.error?.let {
            return if (language == "fr") "Je ne peux pas analyser l'image : $it."
            else "I cannot analyze the image: $it."
        }
        if (productMode) {
            ProductVoiceFormatter.build(result, productCatalog, language)?.let { return it }
        }
        val localizedLabels = result.labels.take(5).map {
            val name = if (language == "fr") VisionVocabulary.toFrench(it.text) else it.text
            "$name, ${(it.confidence * 100).toInt()} pour cent"
        }
        val category = ProductKnowledgeBase.inferCategory(result)
        return if (language == "fr") {
            buildString {
                if (localizedLabels.isNotEmpty()) append("Je reconnais : ${localizedLabels.joinToString(", ")}. ")
                if (category != null) append("Catégorie probable : ${category.nameFr}. ")
                if (result.barcodes.isNotEmpty()) append("Code détecté : ${result.barcodes.first()}. ")
                if (productMode && result.text.isNotBlank()) append("Texte de l'étiquette : ${result.text.take(350)}.")
                if (isBlank()) append("Je n'ai pas reconnu assez d'informations. Rapprochez la caméra et améliorez la lumière.")
            }
        } else {
            buildString {
                if (localizedLabels.isNotEmpty()) append("I recognize: ${localizedLabels.joinToString(", ")}. ")
                if (category != null) append("Likely category: ${category.nameEn}. ")
                if (result.barcodes.isNotEmpty()) append("Detected code: ${result.barcodes.first()}. ")
                if (productMode && result.text.isNotBlank()) append("Label text: ${result.text.take(350)}.")
                if (isBlank()) append("I could not recognize enough information. Move closer and improve the light.")
            }
        }
    }

    fun release() {
        cameraProvider?.unbindAll()
        labeler.close()
        barcodeScanner.close()
        textRecognizer.close()
        cameraExecutor.shutdown()
        PrivacyLog.d(TAG, "VisionModule libéré")
    }
}

data class VisionAnalysis(
    val labels: List<VisionLabel> = emptyList(),
    val barcodes: List<String> = emptyList(),
    val text: String = "",
    val error: String? = null
)

data class VisionLabel(val text: String, val confidence: Float)

data class DetectionResult(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class ProductCategory(val id: String, val nameFr: String, val nameEn: String, val keywords: Set<String>)

object ProductKnowledgeBase {
    private val categories = listOf(
        ProductCategory("food", "alimentation", "food", setOf("food", "fruit", "vegetable", "bread", "meal", "snack", "banana", "apple", "rice", "cereal", "pizza", "cake", "sandwich")),
        ProductCategory("drink", "boissons", "drinks", setOf("drink", "beverage", "bottle", "juice", "water", "coffee", "tea", "can", "cup", "milk")),
        ProductCategory("health", "santé et médicaments", "health and medicine", setOf("medicine", "drug", "tablet", "pharmacy", "health", "medical")),
        ProductCategory("hygiene", "hygiène et cosmétiques", "hygiene and cosmetics", setOf("cosmetics", "skin", "soap", "shampoo", "toothpaste", "beauty")),
        ProductCategory("household", "maison et entretien", "household", setOf("furniture", "cleaning", "chair", "table", "kitchen", "home", "tool", "lamp", "bed", "couch")),
        ProductCategory("electronics", "électronique", "electronics", setOf("electronics", "phone", "computer", "laptop", "screen", "device", "battery", "charger", "camera")),
        ProductCategory("clothing", "vêtements et accessoires", "clothing and accessories", setOf("clothing", "fashion", "shoe", "bag", "hat", "glasses", "watch", "belt")),
        ProductCategory("documents", "documents et paiement", "documents and payment", setOf("document", "money", "currency", "card", "book", "paper", "receipt", "ticket")),
        ProductCategory("transport", "transport et signalisation", "transport and signs", setOf("vehicle", "car", "bus", "truck", "bicycle", "road", "traffic", "sign", "train")),
        ProductCategory("agriculture", "agriculture et outils", "agriculture and tools", setOf("plant", "farm", "crop", "tool", "bucket", "container"))
    )

    fun inferCategory(result: VisionAnalysis): ProductCategory? {
        val haystack = (result.labels.map { it.text } + result.text).joinToString(" ").lowercase()
        return categories.map { category -> category to category.keywords.count { haystack.contains(it) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }
}

object VisionVocabulary {
    private val fr = mapOf(
        "person" to "personne", "man" to "homme", "woman" to "femme", "child" to "enfant",
        "baby" to "bébé", "bird" to "oiseau", "cat" to "chat", "dog" to "chien",
        "horse" to "cheval", "sheep" to "mouton", "cow" to "vache", "elephant" to "éléphant",
        "bear" to "ours", "zebra" to "zèbre", "giraffe" to "girafe", "fish" to "poisson",
        "bicycle" to "vélo", "car" to "voiture", "motorcycle" to "moto", "airplane" to "avion",
        "bus" to "bus", "train" to "train", "truck" to "camion", "boat" to "bateau",
        "vehicle" to "véhicule", "traffic light" to "feu tricolore", "stop sign" to "panneau stop",
        "bench" to "banc", "backpack" to "sac à dos", "umbrella" to "parapluie",
        "handbag" to "sac à main", "suitcase" to "valise", "hat" to "chapeau",
        "glasses" to "lunettes", "sunglasses" to "lunettes de soleil", "wallet" to "portefeuille",
        "watch" to "montre", "bottle" to "bouteille", "plate" to "assiette",
        "wine glass" to "verre à vin", "cup" to "tasse", "fork" to "fourchette",
        "knife" to "couteau", "spoon" to "cuillère", "bowl" to "bol", "banana" to "banane",
        "apple" to "pomme", "sandwich" to "sandwich", "orange" to "orange",
        "broccoli" to "brocoli", "carrot" to "carotte", "pizza" to "pizza", "donut" to "donut",
        "cake" to "gâteau", "cookie" to "biscuit", "bread" to "pain", "cheese" to "fromage",
        "egg" to "œuf", "rice" to "riz", "pasta" to "pâtes", "soup" to "soupe",
        "salad" to "salade", "food" to "aliment", "drink" to "boisson",
        "beverage" to "boisson", "water" to "eau", "juice" to "jus", "milk" to "lait",
        "coffee" to "café", "tea" to "thé", "chair" to "chaise", "couch" to "canapé",
        "potted plant" to "plante en pot", "plant" to "plante", "bed" to "lit",
        "dining table" to "table à manger", "table" to "table", "door" to "porte",
        "window" to "fenêtre", "lamp" to "lampe", "pillow" to "oreiller",
        "shelf" to "étagère", "cabinet" to "armoire", "tv" to "télévision",
        "television" to "télévision", "laptop" to "ordinateur portable", "computer" to "ordinateur",
        "mouse" to "souris", "remote" to "télécommande", "keyboard" to "clavier",
        "cell phone" to "téléphone", "mobile phone" to "téléphone portable",
        "phone" to "téléphone", "tablet" to "tablette", "charger" to "chargeur",
        "headphone" to "casque", "speaker" to "enceinte", "camera" to "appareil photo",
        "printer" to "imprimante", "screen" to "écran", "monitor" to "moniteur",
        "book" to "livre", "clock" to "horloge", "vase" to "vase", "scissors" to "ciseaux",
        "teddy bear" to "ours en peluche", "toothbrush" to "brosse à dents",
        "key" to "clé", "keys" to "clés", "pen" to "stylo", "pencil" to "crayon",
        "paper" to "papier", "notebook" to "carnet", "bag" to "sac", "box" to "boîte",
        "basket" to "panier", "rope" to "corde", "candle" to "bougie", "pot" to "marmite",
        "pan" to "poêle", "bucket" to "seau", "tool" to "outil", "hammer" to "marteau",
        "screwdriver" to "tournevis", "money" to "argent", "coin" to "pièce",
        "card" to "carte", "ticket" to "billet", "glove" to "gant", "mask" to "masque",
        "shoe" to "chaussure", "clothing" to "vêtement", "fashion" to "article de mode",
        "towel" to "serviette", "soap" to "savon", "medicine" to "médicament",
        "cosmetics" to "cosmétique", "electronics" to "appareil électronique",
        "meal" to "repas", "dish" to "plat", "building" to "bâtiment",
        "room" to "pièce", "road" to "route", "sky" to "ciel"
    )

    fun toFrench(label: String): String = fr[label.lowercase()] ?: label
}
