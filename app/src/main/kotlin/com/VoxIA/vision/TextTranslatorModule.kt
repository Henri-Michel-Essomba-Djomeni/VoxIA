package com.voxia.vision

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.voxia.utils.PrivacyLog
import java.util.concurrent.Executors

class TextTranslatorModule(private val context: Context) {
    companion object {
        private const val TAG = "TextTranslatorModule"
        private const val MIN_TEXT_LENGTH = 3
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val languageIdentifier = LanguageIdentification.getClient()
    private val translators = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()
    private var provider: ProcessCameraProvider? = null
    private var capture: ImageCapture? = null
    private var targetLanguage = TranslateLanguage.FRENCH

    fun initialize(
        owner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView? = null,
        targetLang: String = TranslateLanguage.FRENCH,
        onReady: (Boolean) -> Unit = {}
    ) {
        targetLanguage = targetLang
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val success = runCatching {
                provider = future.get()
                val preview = Preview.Builder().build().also { useCase ->
                    previewView?.let { useCase.setSurfaceProvider(it.surfaceProvider) }
                }
                capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider?.unbindAll()
                provider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
            }.onFailure { PrivacyLog.e(TAG, "Initialisation camera impossible", it) }.isSuccess
            onReady(success)
        }, ContextCompat.getMainExecutor(context))
    }

    fun startTranslation(callback: (TranslationResult) -> Unit, targetLang: String = targetLanguage) {
        captureAndTranslate(targetLang, callback)
    }

    fun captureAndTranslate(targetLang: String = targetLanguage, callback: (TranslationResult) -> Unit) {
        val camera = capture ?: run {
            callback(TranslationResult.Error("Caméra non prête"))
            return
        }
        camera.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                val bitmap = runCatching { image.toBitmap() }.getOrNull()
                val rotation = image.imageInfo.rotationDegrees
                image.close()
                if (bitmap == null) callback(TranslationResult.Error("Image illisible"))
                else translateBitmap(bitmap, rotation, targetLang, callback)
            }

            override fun onError(exception: ImageCaptureException) {
                callback(TranslationResult.Error(exception.message ?: "Capture impossible"))
            }
        })
    }

    fun translateFromBitmap(bitmap: Bitmap, targetLang: String = targetLanguage, callback: (TranslationResult) -> Unit) {
        translateBitmap(bitmap, 0, targetLang, callback)
    }

    private fun translateBitmap(bitmap: Bitmap, rotation: Int, targetLang: String, callback: (TranslationResult) -> Unit) {
        recognizer.process(InputImage.fromBitmap(bitmap, rotation))
            .addOnSuccessListener { recognized ->
                val text = recognized.text.trim()
                if (text.length < MIN_TEXT_LENGTH) {
                    callback(TranslationResult.NoText)
                    bitmap.recycle()
                } else identifyAndTranslate(text, targetLang, callback) { bitmap.recycle() }
            }
            .addOnFailureListener {
                bitmap.recycle()
                callback(TranslationResult.Error(it.message ?: "OCR impossible"))
            }
    }

    private fun identifyAndTranslate(text: String, target: String, callback: (TranslationResult) -> Unit, done: () -> Unit) {
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { sourceTag ->
                val source = TranslateLanguage.fromLanguageTag(sourceTag)
                when {
                    source == null || sourceTag == "und" -> {
                        callback(TranslationResult.Success(text, text, sourceTag, target, false, 0f)); done()
                    }
                    source == target -> {
                        callback(TranslationResult.Success(text, text, source, target, false, 1f)); done()
                    }
                    else -> translate(text, source, target, callback, done)
                }
            }
            .addOnFailureListener {
                callback(TranslationResult.Success(text, text, "und", target, false, 0f)); done()
            }
    }

    private fun translate(text: String, source: String, target: String, callback: (TranslationResult) -> Unit, done: () -> Unit) {
        val key = "$source-$target"
        val translator = translators.getOrPut(key) {
            Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build())
        }
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .continueWithTask { translator.translate(text) }
            .addOnSuccessListener {
                callback(TranslationResult.Success(text, it, source, target, true, 1f)); done()
            }
            .addOnFailureListener {
                callback(TranslationResult.ModelNotDownloaded(text, source, target)); done()
            }
    }

    fun buildVoiceMessage(result: TranslationResult, uiLanguage: String): String = when (result) {
        TranslationResult.NoText -> if (uiLanguage == "fr") "Je ne vois aucun texte lisible." else "I cannot see readable text."
        is TranslationResult.Success -> if (result.wasTranslated) {
            if (uiLanguage == "fr") "Texte original : ${result.originalText}. Traduction : ${result.translatedText}"
            else "Original text: ${result.originalText}. Translation: ${result.translatedText}"
        } else if (uiLanguage == "fr") "Le texte dit : ${result.originalText}" else "The text says: ${result.originalText}"
        is TranslationResult.ModelNotDownloaded -> if (uiLanguage == "fr")
            "Le modèle de traduction n'a pas pu être téléchargé. Texte original : ${result.originalText}"
        else "The translation model could not be downloaded. Original text: ${result.originalText}"
        is TranslationResult.UnsupportedPair -> if (uiLanguage == "fr") "Langue non prise en charge. ${result.originalText}" else "Unsupported language. ${result.originalText}"
        is TranslationResult.Error -> if (uiLanguage == "fr") "Erreur de traduction : ${result.message}" else "Translation error: ${result.message}"
    }

    fun release() {
        provider?.unbindAll()
        recognizer.close()
        languageIdentifier.close()
        translators.values.forEach { it.close() }
        translators.clear()
        executor.shutdown()
    }
}

sealed class TranslationResult {
    object NoText : TranslationResult()
    data class Success(val originalText: String, val translatedText: String, val sourceLanguage: String, val targetLanguage: String, val wasTranslated: Boolean, val confidence: Float) : TranslationResult()
    data class UnsupportedPair(val originalText: String, val sourceLanguage: String, val targetLanguage: String) : TranslationResult()
    data class ModelNotDownloaded(val originalText: String, val sourceLanguage: String, val targetLanguage: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}
