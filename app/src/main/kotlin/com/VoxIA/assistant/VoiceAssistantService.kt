package com.voxia.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.voxia.brain.IntentClassifierEngine
import com.voxia.brain.IntentMapper
import com.voxia.brain.Language
import com.voxia.brain.VoxiaContext
import com.voxia.brain.VoxiaResponses
import com.voxia.speech.SpeechManager
import com.voxia.speech.stt.STTResult
import com.voxia.speech.stt.SpeechLanguage
import com.voxia.speech.tts.TTSOptions
import com.voxia.utils.ArithmeticEvaluator
import com.voxia.utils.MemoryManager
import com.voxia.utils.PrivacyLog
import com.voxia.vision.DocumentReadingSession
import com.voxia.vision.OCRModule
import com.voxia.vision.OCRResult
import com.voxia.vision.ReadingPosition
import com.voxia.vision.TextTranslatorModule
import com.voxia.vision.VisionModule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class VoiceAssistantService : LifecycleService(), VoxiaContext {

    companion object {
        private const val TAG = "VoiceAssistantService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voxia_voice_channel"
        const val ACTION_EVENT = "com.voxia.assistant.EVENT"
        const val EXTRA_STATE = "state"
        const val EXTRA_TRANSCRIPT = "transcript"
        const val EXTRA_RESPONSE = "response"
        const val EXTRA_PERMISSION = "permission"
    }

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    private val binder = LocalBinder()

    private lateinit var speechManager: SpeechManager
    private lateinit var intentClassifier: IntentClassifierEngine
    private var visionModule: VisionModule? = null
    private var ocrModule: OCRModule? = null
    private var translatorModule: TextTranslatorModule? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: androidx.camera.view.PreviewView? = null

    private var currentLanguage = Language.FRENCH
    private var lastResponse: Pair<String, String> = Pair("", "")
    private var lastTranscript: String = ""
    private var pendingPermissionAction: (() -> Unit)? = null
    private var activeActionToken = 0
    private val interactionHandler = Handler(Looper.getMainLooper())
    private val confirmationTransactions = ConfirmationTransactionStore(
        nowMillis = SystemClock::elapsedRealtime
    )
    private val contactChoices = ContactChoiceStore(
        nowMillis = SystemClock::elapsedRealtime
    )
    private val contactNameRequests = ContactNameRequestStore(
        nowMillis = SystemClock::elapsedRealtime
    )
    private var readingSession: DocumentReadingSession? = null
    private var lastReadingText: String = ""

    private val currentSpeechLanguage: SpeechLanguage
        get() = when (currentLanguage) {
            Language.FRENCH -> SpeechLanguage.FR
            Language.ENGLISH -> SpeechLanguage.EN
            Language.UNKNOWN -> SpeechLanguage.FR
        }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        promoteToForegroundIfAudioAllowed()

        intentClassifier = IntentClassifierEngine()
        intentClassifier.loadModel()

        speechManager = SpeechManager(this)
        speechManager.init(
            onReady = { PrivacyLog.d(TAG, "SpeechManager prêt") },
            onStateChange = { state ->
                PrivacyLog.d(TAG, "État vocal: $state")
                publishEvent(state = state.name)
            },
            onTranscript = { result -> handleTranscript(result) },
            onCommandDetected = { text, lang -> handleCommand(text, lang) }
        )

        MemoryManager.load("app")
        MemoryManager.load("tts")
        MemoryManager.load("android_stt")
        MemoryManager.load("intent")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForegroundIfAudioAllowed()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun promoteToForegroundIfAudioAllowed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("VOXIA")
                .setContentText("À l'écoute...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
        )
    }

    fun listenOnce() = speechManager.listenForCommand()

    fun cancelCurrentAction() {
        clearPendingInteraction()
        invalidateActiveAction()
        releaseActiveModules()
        speechManager.cancelListening()
        val message = if (currentLanguage == Language.FRENCH) "Action annulée." else "Action cancelled."
        publishEvent(state = "IDLE", response = message)
    }

    fun retryPendingPermissionAction() {
        val action = pendingPermissionAction
        pendingPermissionAction = null
        action?.invoke()
    }

    fun clearPendingPermissionAction() {
        pendingPermissionAction = null
    }

    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
    }

    fun setPreviewView(view: androidx.camera.view.PreviewView) {
        previewView = view
    }

    private fun handleTranscript(result: STTResult) {
        lastTranscript = result.text
        publishEvent(transcript = result.text)
    }

    private fun handleCommand(text: String, language: SpeechLanguage) {
        val lang = when (language) {
            SpeechLanguage.FR -> Language.FRENCH
            SpeechLanguage.EN -> Language.ENGLISH
        }

        val confirmationResolution = confirmationTransactions.resolve(text)
        when (confirmationResolution) {
            is ConfirmationResolution.Confirmed -> Unit
            is ConfirmationResolution.Cancelled -> speak("D'accord, annulé.", "Okay, cancelled.")
            is ConfirmationResolution.Expired -> speak(
                "La confirmation a expiré. Veuillez recommencer la commande.",
                "The confirmation expired. Please repeat the command."
            )
            is ConfirmationResolution.Ambiguous -> speak(
                "Dites oui pour confirmer ou non pour annuler.",
                "Say yes to confirm or no to cancel."
            )
            ConfirmationResolution.None -> Unit
        }
        if (confirmationResolution !is ConfirmationResolution.None) return

        val contactResolution = contactChoices.resolve(text)
        when (val resolution = contactResolution) {
            is ContactChoiceResolution.Selected -> requestCallConfirmation(resolution.candidate)
            is ContactChoiceResolution.Cancelled -> speak("D'accord, appel annulé.", "Okay, call cancelled.")
            is ContactChoiceResolution.Expired -> speak(
                "Le choix du contact a expiré. Veuillez recommencer.",
                "The contact choice expired. Please start again."
            )
            is ContactChoiceResolution.Ambiguous -> speak(
                "Plusieurs numéros portent ce nom. Dites le numéro du choix.",
                "Several numbers use that name. Say the choice number."
            )
            is ContactChoiceResolution.Invalid -> speak(
                "Choix non reconnu. Dites un numéro de la liste ou annulez.",
                "Choice not recognized. Say a number from the list or cancel."
            )
            ContactChoiceResolution.None -> Unit
        }
        if (contactResolution !is ContactChoiceResolution.None) return

        val contactNameResolution = contactNameRequests.resolve(text)
        when (val resolution = contactNameResolution) {
            is ContactNameResolution.Provided -> makeCall(resolution.name)
            is ContactNameResolution.Cancelled -> speak("D'accord, appel annulé.", "Okay, call cancelled.")
            is ContactNameResolution.Expired -> speak(
                "La demande de contact a expiré. Veuillez recommencer.",
                "The contact request expired. Please start again."
            )
            is ContactNameResolution.Invalid -> speak(
                "Je n'ai pas compris le nom. Dites le nom du contact ou annulez.",
                "I did not understand the name. Say the contact name or cancel."
            )
            ContactNameResolution.None -> Unit
        }
        if (contactNameResolution !is ContactNameResolution.None) return

        val result = intentClassifier.classify(text, lang)
        PrivacyLog.d(
            TAG,
            "Commande classée: intent=${result.intent}, language=${result.language}, " +
                "confidence=${result.confidence}, chars=${text.length}"
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
        publishEvent(response = text)
        speechManager.speak(text)
    }

    private fun publishEvent(
        state: String? = null,
        transcript: String? = null,
        response: String? = null,
        permission: String? = null
    ) {
        sendBroadcast(Intent(ACTION_EVENT).setPackage(packageName).apply {
            state?.let { putExtra(EXTRA_STATE, it) }
            transcript?.let { putExtra(EXTRA_TRANSCRIPT, it) }
            response?.let { putExtra(EXTRA_RESPONSE, it) }
            permission?.let { putExtra(EXTRA_PERMISSION, it) }
        })
    }

    private fun requestConfirmation(promptFr: String, promptEn: String, onConfirm: () -> Unit) {
        contactChoices.clear()
        contactNameRequests.clear()
        val token = confirmationTransactions.begin(onConfirm)
        interactionHandler.postDelayed({
            if (confirmationTransactions.expire(token)) {
                speak(
                    "La confirmation a expiré. Veuillez recommencer la commande.",
                    "The confirmation expired. Please repeat the command."
                )
            }
        }, SENSITIVE_ACTION_TIMEOUT_MS)
        speak(promptFr, promptEn)
    }

    private fun beginActiveAction(): Int {
        activeActionToken += 1
        releaseActiveModules()
        readingSession = null
        lastReadingText = ""
        return activeActionToken
    }

    private fun invalidateActiveAction() {
        activeActionToken += 1
    }

    private fun isActiveAction(token: Int): Boolean = token == activeActionToken

    private fun clearPendingInteraction() {
        confirmationTransactions.clear()
        contactChoices.clear()
        contactNameRequests.clear()
        interactionHandler.removeCallbacksAndMessages(null)
        pendingPermissionAction = null
    }

    private fun ensurePermission(permission: String, action: () -> Unit): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return true
        pendingPermissionAction = action
        publishEvent(permission = permission)
        val label = when (permission) {
            Manifest.permission.CAMERA -> "la caméra"
            Manifest.permission.READ_CONTACTS -> "les contacts"
            else -> "cette autorisation"
        }
        speak("J'ai besoin de l'autorisation pour $label.", "I need permission to use $label.")
        return false
    }

    override fun repeatLastResponse() {
        readingSession?.current()?.let {
            speakReadingPosition(it)
            return
        }
        speak(lastResponse.first, lastResponse.second)
    }

    override fun speakHelp() {
        val response = VoxiaResponses.help(currentLanguage)
        speak(response.first, response.second)
    }

    override fun speakTime() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = format.format(Date())
        speak("Il est $time.", "The time is $time.")
    }

    override fun speakDate() {
        val format = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
        val date = format.format(Date())
        speak("Nous sommes le $date.", "Today is $date.")
    }

    override fun speakBatteryLevel() {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        speak(
            if (percent >= 0) "Batterie à $percent pour cent." else "Impossible de lire la batterie.",
            if (percent >= 0) "Battery at $percent percent." else "Unable to read battery level."
        )
    }

    override fun increaseSpeechRate() {
        announceSpeechRate(speechManager.increaseSpeechRate())
    }

    override fun decreaseSpeechRate() {
        announceSpeechRate(speechManager.decreaseSpeechRate())
    }

    override fun resetSpeechRate() {
        announceSpeechRate(speechManager.resetSpeechRate())
    }

    private fun announceSpeechRate(rate: Float) {
        val percent = (rate * 100).roundToInt()
        speak(
            "Vitesse de lecture réglée à $percent pour cent.",
            "Reading speed set to $percent percent."
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
        if (!ensurePermission(Manifest.permission.CAMERA) { loadVisionModule() }) return
        if (visionModule == null) {
            MemoryManager.unload("ocr")
            MemoryManager.load("vision")
            visionModule = VisionModule(this)
        }
    }

    override fun captureAndIdentify() {
        if (!ensurePermission(Manifest.permission.CAMERA) { captureAndIdentify() }) return
        val token = beginActiveAction()
        loadVisionModule()
        val vision = visionModule ?: return
        vision.initialize(lifecycleOwner ?: this, previewView) { ready ->
            if (!isActiveAction(token)) return@initialize
            if (!ready) {
                speak("Impossible d'ouvrir la caméra.", "Unable to open the camera.")
                releaseVision(vision)
            } else {
                vision.captureAnalysis { result ->
                    if (!isActiveAction(token)) return@captureAnalysis
                    val voice = vision.buildVoiceDescription(result, if (currentLanguage == Language.FRENCH) "fr" else "en")
                    speak(voice, voice)
                    releaseVision(vision)
                }
            }
        }
    }

    override fun describeSurroundings() {
        captureAndIdentify()
    }

    override fun scanProduct() {
        if (!ensurePermission(Manifest.permission.CAMERA) { scanProduct() }) return
        val token = beginActiveAction()
        loadVisionModule()
        val vision = visionModule ?: return
        vision.initialize(lifecycleOwner ?: this, previewView) { ready ->
            if (!isActiveAction(token)) return@initialize
            if (!ready) {
                speak("Impossible d'ouvrir la caméra.", "Unable to open the camera.")
                releaseVision(vision)
            } else {
                vision.captureAnalysis { result ->
                    if (!isActiveAction(token)) return@captureAnalysis
                    val voice = vision.buildVoiceDescription(result, if (currentLanguage == Language.FRENCH) "fr" else "en", productMode = true)
                    speak(voice, voice)
                    releaseVision(vision)
                }
            }
        }
    }

    private fun releaseVision(module: VisionModule) {
        runCatching { module.release() }.onFailure {
            PrivacyLog.e(TAG, "Libération vision impossible")
        }
        if (visionModule === module) visionModule = null
        MemoryManager.unload("vision")
    }

    override fun loadOcrModule() {
        if (!ensurePermission(Manifest.permission.CAMERA) { loadOcrModule() }) return
        if (ocrModule == null) {
            MemoryManager.unload("vision")
            MemoryManager.load("ocr")
            ocrModule = OCRModule(this)
        }
    }

    override fun captureAndRead() {
        if (!ensurePermission(Manifest.permission.CAMERA) { captureAndRead() }) return
        val token = beginActiveAction()
        loadOcrModule()
        val ocr = ocrModule ?: return
        ocr.initialize(lifecycleOwner ?: this, previewView) { ready ->
            if (!isActiveAction(token)) return@initialize
            if (!ready) {
                speak("Impossible d'ouvrir la caméra.", "Unable to open the camera.")
                releaseOcr(ocr)
            } else {
                ocr.readDocument(if (currentLanguage == Language.FRENCH) "fr" else "en") { result ->
                    if (!isActiveAction(token)) return@readDocument
                    when (result) {
                        is OCRResult.Success -> startDocumentReading(result)
                        is OCRResult.NoText -> speak(result.message, result.message)
                        is OCRResult.PoorQuality -> speak(result.message, result.message)
                        is OCRResult.Error -> speak(
                            "Erreur de lecture: ${result.message}",
                            "Reading error: ${result.message}"
                        )
                    }
                    releaseOcr(ocr)
                }
            }
        }
    }

    private fun startDocumentReading(result: OCRResult.Success) {
        val session = DocumentReadingSession.fromSegments(result.segments)
        if (session == null) {
            speak(result.voiceText, result.voiceText)
            return
        }

        readingSession = session
        lastReadingText = result.structuredText
        val position = session.current()
        if (position == null) {
            speak(result.voiceText, result.voiceText)
        } else {
            speakReadingPosition(position, result.wordCount)
        }
    }

    override fun readNextSegment() {
        val session = readingSession ?: run {
            speak(
                "Aucune lecture de document n'est en cours.",
                "No document reading is in progress."
            )
            return
        }
        val next = session.next()
        if (next == null) {
            speak(
                "Fin du texte. Dites précédent pour revenir ou répète pour relire ce segment.",
                "End of text. Say previous to go back or repeat to hear this segment again."
            )
        } else {
            speakReadingPosition(next)
        }
    }

    override fun readPreviousSegment() {
        val session = readingSession ?: run {
            speak(
                "Aucune lecture de document n'est en cours.",
                "No document reading is in progress."
            )
            return
        }
        val previous = session.previous()
        if (previous == null) {
            speak(
                "Vous êtes déjà au début du texte.",
                "You are already at the beginning of the text."
            )
        } else {
            speakReadingPosition(previous)
        }
    }

    private fun speakReadingPosition(position: ReadingPosition, wordCount: Int? = null) {
        val introFr = wordCount?.let { "J'ai détecté $it mots. " }.orEmpty()
        val introEn = wordCount?.let { "I detected $it words. " }.orEmpty()
        speak(
            "${introFr}Segment ${position.number} sur ${position.total}. ${position.text}",
            "${introEn}Segment ${position.number} of ${position.total}. ${position.text}"
        )
    }

    override fun requestCopyLastReadingText() {
        if (lastReadingText.isBlank()) {
            copyLastReadingText()
            return
        }
        requestConfirmation(
            "Le texte reconnu peut contenir des informations personnelles. Copier dans le presse-papiers ? Dites oui ou non.",
            "The recognized text may contain personal information. Copy it to the clipboard? Say yes or no."
        ) {
            copyLastReadingText()
        }
    }

    override fun requestShareLastReadingText() {
        if (lastReadingText.isBlank()) {
            shareLastReadingText()
            return
        }
        requestConfirmation(
            "Le texte reconnu peut contenir des informations personnelles. Partager avec une autre application ? Dites oui ou non.",
            "The recognized text may contain personal information. Share it with another app? Say yes or no."
        ) {
            shareLastReadingText()
        }
    }

    fun copyLastReadingText() {
        if (lastReadingText.isBlank()) {
            speak(
                "Aucun texte OCR récent à copier.",
                "No recent OCR text to copy."
            )
            return
        }
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("VOXIA OCR", lastReadingText))
        speak(
            "Texte reconnu copié dans le presse-papiers.",
            "Recognized text copied to the clipboard."
        )
    }

    fun shareLastReadingText() {
        if (lastReadingText.isBlank()) {
            speak(
                "Aucun texte OCR récent à partager.",
                "No recent OCR text to share."
            )
            return
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, lastReadingText)
        }
        val chooser = Intent.createChooser(sendIntent, "Partager le texte VOXIA").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(chooser)
        speak(
            "Choisissez l'application avec laquelle partager le texte.",
            "Choose the app to share the text with."
        )
    }

    private fun releaseOcr(module: OCRModule) {
        runCatching { module.release() }.onFailure {
            PrivacyLog.e(TAG, "Libération OCR impossible")
        }
        if (ocrModule === module) ocrModule = null
        MemoryManager.unload("ocr")
    }

    override fun translateVisibleText() {
        if (!ensurePermission(Manifest.permission.CAMERA) { translateVisibleText() }) return
        val token = beginActiveAction()
        val translator = TextTranslatorModule(this)
        translatorModule = translator
        val target = if (currentLanguage == Language.FRENCH) "fr" else "en"
        translator.initialize(lifecycleOwner ?: this, previewView, target) { ready ->
            if (!isActiveAction(token)) return@initialize
            if (!ready) {
                speak("Impossible d'ouvrir la caméra.", "Unable to open the camera.")
                releaseTranslator(translator)
            } else {
                translator.captureAndTranslate(target) { result ->
                    if (!isActiveAction(token)) return@captureAndTranslate
                    val voice = translator.buildVoiceMessage(result, target)
                    speak(voice, voice)
                    releaseTranslator(translator)
                }
            }
        }
    }

    private fun releaseTranslator(module: TextTranslatorModule) {
        runCatching { module.release() }.onFailure {
            PrivacyLog.e(TAG, "Libération traduction impossible")
        }
        if (translatorModule === module) translatorModule = null
    }

    private fun releaseActiveModules() {
        visionModule?.let { releaseVision(it) }
        ocrModule?.let { releaseOcr(it) }
        translatorModule?.let { releaseTranslator(it) }
    }

    override fun makeCall(contactName: String?) {
        if (contactName.isNullOrBlank()) {
            requestContactName(
                "Quel contact voulez-vous appeler ?",
                "Which contact would you like to call?"
            )
            return
        }

        if (!ensurePermission(Manifest.permission.READ_CONTACTS) { makeCall(contactName) }) return

        when (val match = selectContactMatch(contactName, findContactNumbers(contactName))) {
            ContactMatch.NotFound -> speak(
                "Je ne trouve pas $contactName dans vos contacts.",
                "I cannot find $contactName in your contacts."
            )
            is ContactMatch.Unique -> requestCallConfirmation(match.candidate)
            is ContactMatch.RequiresChoice -> requestContactChoice(match.candidates)
            is ContactMatch.TooMany -> {
                requestContactName(
                    "J'ai trouvé ${match.count} correspondances. Dites un nom plus précis.",
                    "I found ${match.count} matches. Say a more precise name."
                )
            }
        }
    }

    private fun requestContactName(promptFr: String, promptEn: String) {
        confirmationTransactions.clear()
        contactChoices.clear()
        val token = contactNameRequests.begin()
        speak(promptFr, promptEn)
        interactionHandler.postDelayed({
            if (contactNameRequests.expire(token)) {
                speak(
                    "La demande de contact a expiré. Veuillez recommencer.",
                    "The contact request expired. Please start again."
                )
            }
        }, CONTACT_NAME_TIMEOUT_MS)
    }

    private fun requestContactChoice(candidates: List<ContactCandidate>) {
        confirmationTransactions.clear()
        contactNameRequests.clear()
        val token = contactChoices.begin(candidates)
        val choicesFr = candidates.mapIndexed { index, candidate ->
            "${index + 1}, ${candidate.spokenDescriptionFr()}"
        }.joinToString(". ")
        val choicesEn = candidates.mapIndexed { index, candidate ->
            "${index + 1}, ${candidate.spokenDescriptionEn()}"
        }.joinToString(". ")
        speak(
            "Plusieurs contacts correspondent. $choicesFr. Dites le numéro du choix ou annulez.",
            "Several contacts match. $choicesEn. Say the choice number or cancel."
        )
        interactionHandler.postDelayed({
            if (contactChoices.expire(token)) {
                speak(
                    "Le choix du contact a expiré. Veuillez recommencer.",
                    "The contact choice expired. Please start again."
                )
            }
        }, CONTACT_CHOICE_TIMEOUT_MS)
    }

    private fun requestCallConfirmation(candidate: ContactCandidate) {
        requestConfirmation(
            "Voulez-vous appeler ${candidate.spokenDescriptionFr()} ? Dites oui ou non.",
            "Do you want to call ${candidate.spokenDescriptionEn()}? Say yes or no."
        ) {
            speak(
                "J'ouvre le composeur pour ${candidate.displayName}. Confirmez l'appel à l'écran.",
                "Opening the dialer for ${candidate.displayName}. Confirm the call on screen."
            )
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.fromParts("tel", candidate.number, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    private fun findContactNumbers(name: String): List<ContactCandidate> {
        val resolver: ContentResolver = contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        return try {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val displayNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
                buildList {
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(displayNameIndex).orEmpty().trim()
                        val number = cursor.getString(numberIndex).orEmpty().trim()
                        if (displayName.isBlank() || number.isBlank()) continue
                        val type = cursor.getInt(typeIndex)
                        val customLabel = cursor.getString(labelIndex)
                        val typeLabel = ContactsContract.CommonDataKinds.Phone
                            .getTypeLabel(resources, type, customLabel)
                            .toString()
                        add(ContactCandidate(displayName, number, typeLabel))
                    }
                }
            }.orEmpty()
        } catch (e: Exception) {
            PrivacyLog.e(TAG, "Recherche contact impossible")
            emptyList()
        }
    }

    override fun setAlarm(hour: Int?, minute: Int?) {
        if (hour == null) {
            speak("Indiquez l'heure, par exemple : mets une alarme à 7 heures 30.", "Please specify a time, for example: set an alarm at 7 30.")
            return
        }
        requestConfirmation(
            "Confirmez-vous une alarme à %02d:%02d ? Dites oui ou non.".format(hour, minute ?: 0),
            "Set an alarm for %02d:%02d? Say yes or no.".format(hour, minute ?: 0)
        ) {
            launchExternal(
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute ?: 0)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "VOXIA")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                },
                "Alarme préparée pour %02d:%02d.".format(hour, minute ?: 0),
                "Alarm prepared for %02d:%02d.".format(hour, minute ?: 0)
            )
        }
    }

    override fun setReminder(hour: Int?, minute: Int?, durationMinutes: Int?) {
        when {
            durationMinutes != null -> requestConfirmation(
                "Confirmez-vous un minuteur de $durationMinutes minutes ? Dites oui ou non.",
                "Set a $durationMinutes minute timer? Say yes or no."
            ) {
                launchExternal(
                    Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, durationMinutes * 60)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Rappel VOXIA")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    },
                    "Minuteur préparé pour $durationMinutes minutes.",
                    "Timer prepared for $durationMinutes minutes."
                )
            }
            hour != null -> setAlarm(hour, minute)
            else -> speak("Indiquez une heure ou une durée pour le rappel.", "Please specify a time or duration for the reminder.")
        }
    }

    override fun increaseVolume() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current + 1).coerceAtMost(max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        speak("Volume augmenté à $newVolume.", "Volume increased to $newVolume.")
    }

    override fun decreaseVolume() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (current - 1).coerceAtLeast(0)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        speak("Volume diminué à $newVolume.", "Volume decreased to $newVolume.")
    }

    override fun openApp(appName: String?) {
        if (appName.isNullOrBlank()) {
            speak("Quelle application voulez-vous ouvrir ?", "Which app would you like to open?")
            return
        }
        val launcherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = packageManager.queryIntentActivities(launcherQuery, 0)
            .map { it to it.loadLabel(packageManager).toString() }
            .sortedBy { it.second.length }
            .firstOrNull { (_, label) -> label.contains(appName, ignoreCase = true) }
        val launchIntent = match?.first?.activityInfo?.packageName?.let(packageManager::getLaunchIntentForPackage)
        if (launchIntent == null) {
            speak("Je ne trouve pas l'application $appName.", "I cannot find the app $appName.")
        } else {
            requestConfirmation(
                "Ouvrir ${match.second} ? Dites oui ou non.",
                "Open ${match.second}? Say yes or no."
            ) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                speak("Ouverture de ${match.second}.", "Opening ${match.second}.")
            }
        }
    }

    override fun calculate(expression: String?) {
        val result = ArithmeticEvaluator.evaluate(expression)
        if (result == null) {
            speak("Je n'ai pas reconnu une expression arithmétique valide.", "I did not recognize a valid arithmetic expression.")
        } else {
            val formatted = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                "%.4f".format(Locale.US, result).trimEnd('0').trimEnd('.')
            }
            speak("Le résultat est $formatted.", "The result is $formatted.")
        }
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
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        if (!enabled) {
            speak("Autorisez VOXIA dans l'accès aux notifications, puis réessayez.", "Enable notification access for VOXIA, then try again.")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val notifications = NotificationRepository.latest()
        if (notifications.isEmpty()) {
            speak("Aucune notification récente.", "No recent notifications.")
        } else {
            val fr = notifications.take(5).joinToString(". ") { "${it.app}: ${it.title}. ${it.text}" }
            speak("Voici vos notifications. $fr", "Here are your notifications. $fr")
        }
    }

    private fun launchExternal(intent: Intent, fr: String, en: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) == null) {
            speak("Aucune application compatible n'est installée.", "No compatible app is installed.")
        } else {
            startActivity(intent)
            speak(fr, en)
        }
    }

    override fun stopAll() {
        clearPendingInteraction()
        readingSession = null
        lastReadingText = ""
        invalidateActiveAction()
        releaseActiveModules()
        speechManager.cancelListening()
        val message = if (currentLanguage == Language.FRENCH) "D'accord, j'arrête." else "Okay, stopping."
        publishEvent(response = message)
        speechManager.speak(message, TTSOptions(onDone = {
            Handler(Looper.getMainLooper()).post { stopSelf() }
        }))
    }

    fun getCurrentLanguage(): Language = currentLanguage

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VOXIA Voice Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Service vocal VOXIA en arrière-plan"
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        clearPendingInteraction()
        invalidateActiveAction()
        releaseActiveModules()
        speechManager.release()
        intentClassifier.release()
        MemoryManager.unloadAll()
        super.onDestroy()
    }
}
