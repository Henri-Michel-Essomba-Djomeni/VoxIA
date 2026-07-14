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
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * TextTranslatorModule - Traducteur de texte par caméra pour VOXIA
 * 
 * Fonctionnalité F2-TRANSLATE (extension de F2 - Lecture de document)
 * 
 * Pipeline complet :
 *   CAMÉRA → ML Kit OCR (offline) → Détection langue source → Traduction offline → TTS
 *
 * Cas d'usage :
 *   - Utilisateur filme une pancarte, un menu, un billet, un document
 *   - VOXIA extrait le texte (OCR), détecte la langue, traduit et lit à voix haute
 *
 * Commandes vocales :
 *   FR : "Traduis ce texte", "Lis et traduis", "Qu'est-ce qu'il y a écrit ?"
 *   EN : "Translate this text", "Read and translate", "What does it say?"
 *
 * Architecture mémoire :
 *   - ML Kit OCR Latin : ~10 Mo (même instance que F2)
 *   - ML Kit Translator : ~30 Mo par paire de langues (chargé à la demande)
 *   - Budget total : ~40 Mo (dans le budget VOXIA de 900 Mo)
 *
 * Paires de traduction supportées (offline) :
 *   EN → FR, FR → EN, ES → FR, AR → FR, PT → FR
 *   (Les modèles de traduction ML Kit doivent être téléchargés à l'installation)
 */
class TextTranslatorModule(private val context: Context) {

    companion object {
        private const val TAG = "TextTranslatorModule"

        // Langue cible par défaut (langue de l'utilisateur)
        private const val DEFAULT_TARGET = TranslateLanguage.FRENCH

        // Seuil minimal de texte pour déclencher la traduction
        private const val MIN_TEXT_LENGTH = 5

        // Paires supportées offline → à télécharger au premier lancement
        val SUPPORTED_PAIRS = listOf(
            Pair(TranslateLanguage.ENGLISH, TranslateLanguage.FRENCH),
            Pair(TranslateLanguage.FRENCH, TranslateLanguage.ENGLISH),
            Pair(TranslateLanguage.SPANISH, TranslateLanguage.FRENCH),
            Pair(TranslateLanguage.PORTUGUESE, TranslateLanguage.FRENCH),
            Pair(TranslateLanguage.ARABIC, TranslateLanguage.FRENCH),
        )
    }

    // =========== ÉTAT ===========
    private var isActive = false
    private var targetLanguage = DEFAULT_TARGET
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var resultCallback: ((TranslationResult) -> Unit)? = null

    // ML Kit OCR (réutilise l'instance de F2 si déjà chargée)
    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Cache des traducteurs pour éviter les rechargements répétés
    private val translatorCache = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    // Détecteur de langue ML Kit (offline)
    private val languageIdentifier by lazy {
        com.google.mlkit.nl.languageid.LanguageIdentification.getClient()
    }

    // =========== INITIALISATION CAMÉRA ===========

    /**
     * Initialise la caméra pour la traduction en temps réel.
     * Mode : capture à la demande (pas de flux continu pour économiser la RAM)
     */
    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null,
        targetLang: String = DEFAULT_TARGET
    ) {
        this.targetLanguage = targetLang
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera(lifecycleOwner, previewView)
            Log.d(TAG, "TextTranslatorModule initialisé. Langue cible: $targetLang")
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView?
    ) {
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build()
            .also { previewView?.let { pv -> it.setSurfaceProvider(pv.surfaceProvider) } }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isActive) {
                        processFrameForOCR(imageProxy)
                    } else {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur liaison caméra: ${e.message}")
        }
    }

    // =========== PIPELINE PRINCIPAL ===========

    /**
     * Active la traduction : la prochaine frame stable sera traitée
     *
     * @param callback Résultat contenant le texte original et la traduction
     * @param targetLang Langue cible (par défaut français)
     */
    fun startTranslation(
        callback: (TranslationResult) -> Unit,
        targetLang: String = targetLanguage
    ) {
        this.targetLanguage = targetLang
        this.resultCallback = callback
        this.isActive = true
        Log.d(TAG, "Traduction activée. Cible: $targetLang")
    }

    /**
     * Traduction directe depuis un Bitmap (capture photo)
     * Mode non-streaming : idéal pour le cas "je prends une photo et traduis"
     */
    fun translateFromBitmap(
        bitmap: Bitmap,
        targetLang: String = targetLanguage,
        callback: (TranslationResult) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        extractTextFromImage(image) { rawText ->
            if (rawText.isNullOrBlank() || rawText.length < MIN_TEXT_LENGTH) {
                callback(TranslationResult.NoText)
                return@extractTextFromImage
            }
            detectAndTranslate(rawText, targetLang, callback)
        }
    }

    // =========== OCR ===========

    private fun processFrameForOCR(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmapSafe() ?: run { imageProxy.close(); return }
        val image = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
        imageProxy.close()

        extractTextFromImage(image) { rawText ->
            if (!rawText.isNullOrBlank() && rawText.length >= MIN_TEXT_LENGTH) {
                isActive = false // Arrêter après la première détection réussie
                detectAndTranslate(rawText, targetLanguage) { result ->
                    resultCallback?.invoke(result)
                    resultCallback = null
                }
            }
        }
    }

    /**
     * Extraction OCR via ML Kit Latin (offline)
     * Supporte : Latin (FR, EN, ES, PT, DE, IT...), suffisant pour l'Afrique francophone
     */
    private fun extractTextFromImage(
        image: InputImage,
        onResult: (String?) -> Unit
    ) {
        latinRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extracted = visionText.text.trim()
                Log.d(TAG, "OCR extrait (${extracted.length} chars): ${extracted.take(100)}")
                onResult(extracted)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Échec OCR: ${e.message}")
                onResult(null)
            }
    }

    // =========== DÉTECTION DE LANGUE ===========

    /**
     * Détecte la langue source puis lance la traduction
     */
    private fun detectAndTranslate(
        rawText: String,
        targetLang: String,
        callback: (TranslationResult) -> Unit
    ) {
        languageIdentifier.identifyLanguage(rawText)
            .addOnSuccessListener { sourceLang ->
                Log.d(TAG, "Langue détectée: $sourceLang")

                when {
                    sourceLang == "und" -> {
                        // Langue non identifiée → retourner le texte brut
                        callback(TranslationResult.Success(
                            originalText = rawText,
                            translatedText = rawText,
                            sourceLanguage = "inconnu",
                            targetLanguage = targetLang,
                            wasTranslated = false,
                            confidence = 0f
                        ))
                    }
                    sourceLang == targetLang -> {
                        // Déjà dans la langue cible → pas besoin de traduire
                        callback(TranslationResult.Success(
                            originalText = rawText,
                            translatedText = rawText,
                            sourceLanguage = sourceLang,
                            targetLanguage = targetLang,
                            wasTranslated = false,
                            confidence = 1f
                        ))
                    }
                    isPairSupported(sourceLang, targetLang) -> {
                        translateText(rawText, sourceLang, targetLang, callback)
                    }
                    else -> {
                        // Paire non supportée → retourner le texte brut avec info
                        callback(TranslationResult.UnsupportedPair(
                            originalText = rawText,
                            sourceLanguage = sourceLang,
                            targetLanguage = targetLang
                        ))
                    }
                }
            }
            .addOnFailureListener {
                // Si la détection échoue, tenter une traduction EN→FR par défaut
                translateText(rawText, TranslateLanguage.ENGLISH, targetLang, callback)
            }
    }

    // =========== TRADUCTION ===========

    /**
     * Traduction ML Kit offline.
     * Les modèles sont mis en cache pour éviter les rechargements.
     * Téléchargement automatique au premier usage (Wi-Fi requis une seule fois).
     */
    private fun translateText(
        text: String,
        sourceLang: String,
        targetLang: String,
        callback: (TranslationResult) -> Unit
    ) {
        val cacheKey = "$sourceLang→$targetLang"
        val translator = translatorCache.getOrPut(cacheKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }

        // Vérifier si le modèle est téléchargé, sinon le télécharger
        val conditions = com.google.mlkit.common.model.DownloadConditions.Builder()
            .requireWifi() // Télécharger uniquement en Wi-Fi
            .build()

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        Log.d(TAG, "Traduction réussie: ${translatedText.take(80)}")
                        callback(TranslationResult.Success(
                            originalText = text,
                            translatedText = translatedText,
                            sourceLanguage = sourceLang,
                            targetLanguage = targetLang,
                            wasTranslated = true,
                            confidence = 1f
                        ))
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Échec traduction: ${e.message}")
                        callback(TranslationResult.Error(e.message ?: "Erreur inconnue"))
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Modèle non disponible offline: ${e.message}")
                // Fallback : retourner le texte brut
                callback(TranslationResult.ModelNotDownloaded(
                    originalText = text,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang
                ))
            }
    }

    // =========== MESSAGES VOCAUX ===========

    /**
     * Construit le message vocal à partir du résultat de traduction
     *
     * @param result Résultat de la traduction
     * @param uiLanguage Langue de l'interface VOXIA ("fr" ou "en")
     * @return Texte à synthétiser vocalement
     */
    fun buildVoiceMessage(result: TranslationResult, uiLanguage: String): String {
        return when (result) {
            is TranslationResult.NoText -> {
                if (uiLanguage == "fr") "Je ne vois pas de texte dans l'image."
                else "I cannot see any text in the image."
            }
            is TranslationResult.Success -> {
                if (result.wasTranslated) {
                    val langName = getLanguageName(result.sourceLanguage, uiLanguage)
                    if (uiLanguage == "fr")
                        "Texte en $langName : ${result.originalText}. " +
                        "Traduction : ${result.translatedText}"
                    else
                        "Text in $langName: ${result.originalText}. " +
                        "Translation: ${result.translatedText}"
                } else {
                    // Même langue, pas besoin de traduire
                    if (uiLanguage == "fr") "Le texte dit : ${result.originalText}"
                    else "The text says: ${result.originalText}"
                }
            }
            is TranslationResult.UnsupportedPair -> {
                val langName = getLanguageName(result.sourceLanguage, uiLanguage)
                if (uiLanguage == "fr")
                    "Texte détecté en $langName. La traduction de cette langue n'est pas encore disponible. " +
                    "Le texte dit : ${result.originalText}"
                else
                    "Text detected in $langName. Translation from this language is not yet available. " +
                    "The text says: ${result.originalText}"
            }
            is TranslationResult.ModelNotDownloaded -> {
                if (uiLanguage == "fr")
                    "Le modèle de traduction n'est pas encore téléchargé. " +
                    "Connectez-vous au Wi-Fi pour le télécharger. " +
                    "Le texte brut dit : ${result.originalText}"
                else
                    "Translation model is not downloaded yet. " +
                    "Connect to Wi-Fi to download it. " +
                    "The raw text says: ${result.originalText}"
            }
            is TranslationResult.Error -> {
                if (uiLanguage == "fr") "Une erreur s'est produite lors de la traduction."
                else "An error occurred during translation."
            }
        }
    }

    // =========== UTILITAIRES ===========

    private fun isPairSupported(source: String, target: String): Boolean {
        return SUPPORTED_PAIRS.any { it.first == source && it.second == target }
    }

    private fun getLanguageName(langCode: String, uiLang: String): String {
        return if (uiLang == "fr") {
            when (langCode) {
                "en" -> "anglais"
                "fr" -> "français"
                "es" -> "espagnol"
                "pt" -> "portugais"
                "ar" -> "arabe"
                "de" -> "allemand"
                "zh" -> "chinois"
                else -> langCode
            }
        } else {
            when (langCode) {
                "en" -> "English"
                "fr" -> "French"
                "es" -> "Spanish"
                "pt" -> "Portuguese"
                "ar" -> "Arabic"
                "de" -> "German"
                "zh" -> "Chinese"
                else -> langCode
            }
        }
    }

    /**
     * Précharge les modèles de traduction les plus courants en arrière-plan.
     * À appeler au démarrage de l'app quand le Wi-Fi est disponible.
     */
    fun preloadTranslationModels() {
        SUPPORTED_PAIRS.forEach { (src, tgt) ->
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(src)
                .setTargetLanguage(tgt)
                .build()
            val translator = Translation.getClient(options)
            val conditions = com.google.mlkit.common.model.DownloadConditions.Builder()
                .requireWifi()
                .build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Log.d(TAG, "Modèle $src→$tgt téléchargé")
                    translatorCache["$src→$tgt"] = translator
                }
                .addOnFailureListener {
                    Log.w(TAG, "Téléchargement $src→$tgt échoué (pas de Wi-Fi ?)")
                }
        }
    }

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
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 85, out)
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            Log.e(TAG, "Erreur conversion bitmap: ${e.message}")
            null
        }
    }

    fun release() {
        isActive = false
        resultCallback = null
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
        latinRecognizer.close()
        languageIdentifier.close()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        Log.d(TAG, "TextTranslatorModule libéré")
    }
}

// =========== SEALED CLASS RÉSULTATS ===========

sealed class TranslationResult {
    object NoText : TranslationResult()

    data class Success(
        val originalText: String,
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String,
        val wasTranslated: Boolean,
        val confidence: Float
    ) : TranslationResult()

    data class UnsupportedPair(
        val originalText: String,
        val sourceLanguage: String,
        val targetLanguage: String
    ) : TranslationResult()

    data class ModelNotDownloaded(
        val originalText: String,
        val sourceLanguage: String,
        val targetLanguage: String
    ) : TranslationResult()

    data class Error(val message: String) : TranslationResult()
}
