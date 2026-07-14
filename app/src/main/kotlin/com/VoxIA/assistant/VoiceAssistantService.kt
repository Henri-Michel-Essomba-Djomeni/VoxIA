package com.voxia.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.voxia.brain.Intent
import com.voxia.brain.IntentMapper
import com.voxia.brain.Language
import com.voxia.brain.PredictionResult
import com.voxia.brain.VoxiaContext
import com.voxia.brain.VoxiaResponses
import com.voxia.speech.SpeechManager
import com.voxia.speech.stt.STTResult
import com.voxia.speech.stt.SpeechLanguage
import com.voxia.utils.MemoryManager
import com.voxia.vision.OCRModule
import com.voxia.vision.OCRResult
import com.voxia.vision.VisionModule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.AudioManager

class VoiceAssistantService : Service(), VoxiaContext {

    companion object {
        private const val TAG = "VoiceAssistantService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voxia_voice_channel"
    }

    private lateinit var speechManager: SpeechManager
    private var visionModule: VisionModule? = null
    private var ocrModule: OCRModule? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: androidx.camera.view.PreviewView? = null

    private var currentLanguage = Language.FRENCH
    private var lastResponse: Pair<String, String> = Pair("", "")
    private var lastTranscript: String = ""

    private var currentSpeechLanguage: SpeechLanguage
        get() = when (currentLanguage) {
            Language.FRENCH -> SpeechLanguage.FR
            Language.ENGLISH -> SpeechLanguage.EN
            Language.UNKNOWN -> SpeechLanguage.FR
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VOXIA")
            .setContentText("À l'écoute...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build())

        speechManager = SpeechManager(this)
        speechManager.init(
            onReady = { Log.d(TAG, "SpeechManager prêt") },
            onStateChange = { state -> Log.d(TAG, "État vocal: $state") },
            onTranscript = { result -> handleTranscript(result) },
            onCommandDetected = { text, lang -> handleCommand(text, lang) }
        )

        MemoryManager.load("app")
        MemoryManager.load("tts")
        MemoryManager.load("intent")
    }

    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
    }

    fun setPreviewView(view: androidx.camera.view.PreviewView) {
        previewView = view
    }

    private fun handleTranscript(result: STTResult) {
        lastTranscript = result.text
    }

    private fun handleCommand(text: String, language: SpeechLanguage) {
        val lang = when (language) {
            SpeechLanguage.FR -> Language.FRENCH
            SpeechLanguage.EN -> Language.ENGLISH
        }
        val result = PredictionResult(
            intent = Intent.FALLBACK,
            language = lang,
            confidence = 0.85f
        )
        IntentMapper.execute(result, this)
    }

    override fun speak(fr: String, en: String) {
        lastResponse = Pair(fr, en)
        val text = when (currentLanguage) {
            Language.FRENCH -> fr
            Language.ENGLISH -> en
            Language.UNKNOWN -> fr
        }
        speechManager.speak(text)
    }

    override fun repeatLastResponse() {
        speak(lastResponse.first, lastResponse.second)
    }

    override fun speakHelp() {
        val response = VoxiaResponses.help(currentLanguage)
        speak(response.first, response.second)
    }

    override fun speakTime() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = format.format(Date())
        speak(
            "Il est $time.",
            "The time is $time."
        )
    }

    override fun speakDate() {
        val format = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        val date = format.format(Date())
        speak(
            "Nous sommes le $date.",
            "Today is $date."
        )
    }

    override fun speakBatteryLevel() {
        val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        speak(
            if (percent >= 0) "Batterie à $percent pour cent." else "Impossible de lire la batterie.",
            if (percent >= 0) "Battery at $percent percent." else "Unable to read battery level."
        )
    }

    override fun switchLanguage(language: Language) {
        currentLanguage = language
        speechManager.switchLanguage(
            when (language) {
                Language.FRENCH -> SpeechLanguage.FR
                Language.ENGLISH -> SpeechLanguage.EN
                Language.UNKNOWN -> SpeechLanguage.FR
            }
        )
    }

    override fun loadVisionModule() {
        val owner = lifecycleOwner ?: return
        if (visionModule == null) {
            if (!MemoryManager.canLoad("vision")) {
                MemoryManager.unload("ocr")
            }
            MemoryManager.load("vision")
            visionModule = VisionModule(this)
            visionModule?.initialize(owner)
            visionModule?.loadModel()
        }
    }

    override fun captureAndIdentify() {
        loadVisionModule()
        visionModule?.let { vision ->
            vision.startDetection { results ->
                val vm = vision
                vm.stopDetection()
                val voice = vm.buildVoiceDescription(results, when (currentLanguage) {
                    Language.FRENCH -> "fr"
                    else -> "en"
                })
                speak(voice, voice)
                vm.releaseModel()
                visionModule = null
                MemoryManager.unload("vision")
            }
        }
    }

    override fun describeSurroundings() {
        captureAndIdentify()
    }

    override fun loadOcrModule() {
        val owner = lifecycleOwner ?: return
        if (ocrModule == null) {
            if (!MemoryManager.canLoad("ocr")) {
                MemoryManager.unload("vision")
            }
            MemoryManager.load("ocr")
            ocrModule = OCRModule(this)
            ocrModule?.initialize(owner)
        }
    }

    override fun captureAndRead() {
        loadOcrModule()
        ocrModule?.let { ocr ->
            ocr.readDocument(when (currentLanguage) {
                Language.FRENCH -> "fr"
                else -> "en"
            }) { result ->
                when (result) {
                    is OCRResult.Success -> {
                        speak(
                            result.voiceText,
                            result.voiceText
                        )
                    }
                    is OCRResult.NoText -> {
                        speak(result.message, result.message)
                    }
                    is OCRResult.Error -> {
                        speak(
                            "Erreur de lecture: ${result.message}",
                            "Reading error: ${result.message}"
                        )
                    }
                }
                ocrModule = null
                MemoryManager.unload("ocr")
            }
        }
    }

    override fun makeCall(contactName: String?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            speak(
                "Je n'ai pas la permission d'appeler. Veuillez l'autoriser dans les paramètres.",
                "I don't have call permission. Please grant it in settings."
            )
            return
        }

        if (contactName.isNullOrBlank()) {
            speak("Quel contact voulez-vous appeler ?", "Which contact would you like to call?")
            return
        }

        val number = findContactNumber(contactName)
        if (number != null) {
            speak(
                "Appel de $contactName en cours.",
                "Calling $contactName."
            )
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            speak(
                "Je ne trouve pas $contactName dans vos contacts.",
                "I cannot find $contactName in your contacts."
            )
        }
    }

    private fun findContactNumber(name: String): String? {
        val resolver: ContentResolver = contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(c.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ))
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur recherche contact: ${e.message}")
            null
        } finally {
            cursor?.close()
        }
    }

    override fun setAlarm() {
        speak(
            "Désolé, la fonctionnalité d'alarme n'est pas encore implémentée.",
            "Sorry, alarm functionality is not yet implemented."
        )
    }

    override fun setReminder() {
        speak(
            "Désolé, la fonctionnalité de rappel n'est pas encore implémentée.",
            "Sorry, reminder functionality is not yet implemented."
        )
    }

    override fun increaseVolume() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current + 1).coerceAtMost(max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        speak(
            "Volume augmenté à $newVolume.",
            "Volume increased to $newVolume."
        )
    }

    override fun decreaseVolume() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current - 1).coerceAtLeast(0)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        speak(
            "Volume diminué à $newVolume.",
            "Volume decreased to $newVolume."
        )
    }

    override fun openApp(appName: String?) {
        speak(
            "Désolé, l'ouverture d'applications n'est pas encore implémentée.",
            "Sorry, opening apps is not yet implemented."
        )
    }

    override fun calculate(expression: String?) {
        speak(
            "Désolé, le calcul n'est pas encore implémenté.",
            "Sorry, calculation is not yet implemented."
        )
    }

    override fun tellStory(language: Language) {
        val response = VoxiaResponses.story(language)
        speak(response.first, response.second)
    }

    override fun tellJoke(language: Language) {
        val response = VoxiaResponses.joke(language)
        speak(response.first, response.second)
    }

    override fun tellMotivational(language: Language) {
        val response = VoxiaResponses.motivational(language)
        speak(response.first, response.second)
    }

    override fun readNotifications() {
        speak(
            "Désolé, la lecture des notifications n'est pas encore implémentée.",
            "Sorry, reading notifications is not yet implemented."
        )
    }

    override fun stopAll() {
        speechManager.release()
        visionModule?.release()
        ocrModule?.release()
        MemoryManager.unloadAll()
        stopSelf()
    }

    fun getCurrentLanguage(): Language = currentLanguage

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VOXIA Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service vocal VOXIA en arrière-plan"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speechManager.release()
        visionModule?.release()
        ocrModule?.release()
        MemoryManager.unloadAll()
        super.onDestroy()
    }
}
