package com.voxia.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.voxia.assistant.R
import com.voxia.assistant.VoiceAssistantService

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var stateText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var responseText: TextView
    private var service: VoiceAssistantService? = null
    private var bound = false
    private var binding = false
    private var receiverRegistered = false
    private var pendingCameraAction: (() -> Unit)? = null
    private val pendingServiceActions = PendingActionQueue<VoiceAssistantService>()
    private var listenAfterAudioPermission = false
    private var notificationPermissionPromptedThisSession = false

    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startAndBindService()
            requestNotificationPermissionIfNeeded()
            if (listenAfterAudioPermission) {
                runWhenServiceReady { it.listenOnce() }
            }
        } else {
            updateResponse(R.string.permission_audio_required)
        }
        listenAfterAudioPermission = false
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            previewView.visibility = View.VISIBLE
            val uiAction = pendingCameraAction
            pendingCameraAction = null
            if (uiAction != null) uiAction.invoke() else runWhenServiceReady { it.retryPendingPermissionAction() }
        } else {
            pendingCameraAction = null
            service?.clearPendingPermissionAction()
            updateResponse(R.string.permission_camera_denied)
        }
    }

    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runWhenServiceReady { it.retryPendingPermissionAction() }
        else {
            service?.clearPendingPermissionAction()
            updateResponse(R.string.permission_contacts_denied)
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) updateResponse(R.string.permission_notifications_denied)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binding = false
            val connectedService = (binder as? VoiceAssistantService.LocalBinder)?.getService()
            if (connectedService == null) {
                bound = false
                pendingServiceActions.clear()
                updateResponse(R.string.service_unavailable)
                return
            }

            service = connectedService
            connectedService.setLifecycleOwner(this@MainActivity)
            connectedService.setPreviewView(previewView)
            bound = true

            pendingServiceActions.drain(connectedService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            binding = false
            service = null
            updateResponse(R.string.service_disconnected)
        }
    }

    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            intent.getStringExtra(VoiceAssistantService.EXTRA_STATE)?.let { stateText.text = localizedState(it) }
            intent.getStringExtra(VoiceAssistantService.EXTRA_TRANSCRIPT)?.let { updateTranscript(it) }
            intent.getStringExtra(VoiceAssistantService.EXTRA_RESPONSE)?.let { updateResponse(it) }
            intent.getStringExtra(VoiceAssistantService.EXTRA_PERMISSION)?.let { permission ->
                when (permission) {
                    Manifest.permission.CAMERA -> showPermissionRationale(
                        R.string.permission_rationale_camera_title,
                        R.string.permission_rationale_camera_message,
                        onDecline = {
                            service?.clearPendingPermissionAction()
                            updateResponse(R.string.permission_camera_denied)
                        }
                    ) { cameraPermission.launch(permission) }

                    Manifest.permission.READ_CONTACTS -> showPermissionRationale(
                        R.string.permission_rationale_contacts_title,
                        R.string.permission_rationale_contacts_message,
                        onDecline = {
                            service?.clearPendingPermissionAction()
                            updateResponse(R.string.permission_contacts_denied)
                        }
                    ) { contactsPermission.launch(permission) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        stateText = findViewById(R.id.stateText)
        transcriptText = findViewById(R.id.transcriptText)
        responseText = findViewById(R.id.responseText)

        findViewById<Button>(R.id.speakButton).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                runWhenServiceReady { it.listenOnce() }
            } else {
                requestAudioAndStart(listenAfterGrant = true)
            }
        }
        findViewById<Button>(R.id.identifyButton).setOnClickListener { withCamera { it.captureAndIdentify() } }
        findViewById<Button>(R.id.productButton).setOnClickListener { withCamera { it.scanProduct() } }
        findViewById<Button>(R.id.readButton).setOnClickListener { withCamera { it.captureAndRead() } }
        findViewById<Button>(R.id.translateButton).setOnClickListener { withCamera { it.translateVisibleText() } }
        findViewById<Button>(R.id.readPreviousButton).setOnClickListener { runWhenServiceReady { it.readPreviousSegment() } }
        findViewById<Button>(R.id.readRepeatButton).setOnClickListener { runWhenServiceReady { it.repeatLastResponse() } }
        findViewById<Button>(R.id.readNextButton).setOnClickListener { runWhenServiceReady { it.readNextSegment() } }
        findViewById<Button>(R.id.readSlowerButton).setOnClickListener { runWhenServiceReady { it.decreaseSpeechRate() } }
        findViewById<Button>(R.id.readNormalSpeedButton).setOnClickListener { runWhenServiceReady { it.resetSpeechRate() } }
        findViewById<Button>(R.id.readFasterButton).setOnClickListener { runWhenServiceReady { it.increaseSpeechRate() } }
        findViewById<Button>(R.id.copyTextButton).setOnClickListener {
            showTextExportRationale { runWhenServiceReady { it.copyLastReadingText() } }
        }
        findViewById<Button>(R.id.shareTextButton).setOnClickListener {
            showTextExportRationale { runWhenServiceReady { it.shareLastReadingText() } }
        }
        findViewById<Button>(R.id.helpButton).setOnClickListener { runWhenServiceReady { it.speakHelp() } }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { cancelCurrentOperation() }

        requestAudioAndStart()
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                eventReceiver,
                IntentFilter(VoiceAssistantService.ACTION_EVENT),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        startAndBindService()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded()
        }
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(eventReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (bound || binding) unbindService(connection)
        bound = false
        binding = false
        pendingServiceActions.clear()
        super.onDestroy()
    }

    private fun requestAudioAndStart(listenAfterGrant: Boolean = false) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAndBindService()
            requestNotificationPermissionIfNeeded()
            if (listenAfterGrant) runWhenServiceReady { it.listenOnce() }
        } else {
            listenAfterAudioPermission = listenAfterGrant
            showPermissionRationale(
                R.string.permission_rationale_audio_title,
                R.string.permission_rationale_audio_message,
                onDecline = {
                    listenAfterAudioPermission = false
                    updateResponse(R.string.permission_audio_required)
                }
            ) { audioPermission.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        if (notificationPermissionPromptedThisSession) return

        notificationPermissionPromptedThisSession = true
        showPermissionRationale(
            R.string.permission_rationale_notifications_title,
            R.string.permission_rationale_notifications_message,
            onDecline = { updateResponse(R.string.permission_notifications_denied) }
        ) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    private fun showPermissionRationale(
        titleRes: Int,
        messageRes: Int,
        onDecline: () -> Unit = {},
        onProceed: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_dialog_continue) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton(R.string.permission_dialog_not_now) { dialog, _ ->
                dialog.dismiss()
                onDecline()
            }
            .show()
    }

    private fun showTextExportRationale(onProceed: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.text_export_privacy_title)
            .setMessage(R.string.text_export_privacy_message)
            .setCancelable(true)
            .setPositiveButton(R.string.permission_dialog_continue) { dialog, _ ->
                dialog.dismiss()
                onProceed()
            }
            .setNegativeButton(R.string.permission_dialog_not_now) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun startAndBindService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            runCatching { ContextCompat.startForegroundService(this, intent) }
        }
        if (!bound && !binding) {
            binding = bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!binding) {
                pendingServiceActions.clear()
                updateResponse(R.string.service_unavailable)
            }
        }
    }

    private fun runWhenServiceReady(action: (VoiceAssistantService) -> Unit) {
        val connectedService = service
        if (connectedService != null && bound) {
            action(connectedService)
            return
        }

        pendingServiceActions.enqueue(action)
        updateResponse(R.string.service_initializing)
        startAndBindService()
    }

    private fun withCamera(action: (VoiceAssistantService) -> Unit) {
        startAndBindService()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            previewView.visibility = View.VISIBLE
            runWhenServiceReady(action)
        } else {
            pendingCameraAction = { runWhenServiceReady(action) }
            showPermissionRationale(
                R.string.permission_rationale_camera_title,
                R.string.permission_rationale_camera_message,
                onDecline = {
                    pendingCameraAction = null
                    updateResponse(R.string.permission_camera_denied)
                }
            ) { cameraPermission.launch(Manifest.permission.CAMERA) }
        }
    }

    private fun cancelCurrentOperation() {
        pendingCameraAction = null
        pendingServiceActions.clear()
        val connectedService = service
        if (connectedService != null && bound) {
            connectedService.cancelCurrentAction()
        } else {
            updateResponse(R.string.action_cancelled_feedback)
        }
    }

    private fun localizedState(state: String): String = when (state) {
        "LISTENING" -> getString(R.string.state_listening)
        "PROCESSING" -> getString(R.string.state_processing)
        "SPEAKING" -> getString(R.string.state_speaking)
        else -> getString(R.string.state_idle)
    }

    private fun updateTranscript(text: String) {
        transcriptText.text = text
        transcriptText.contentDescription = if (text.isBlank()) {
            getString(R.string.accessibility_transcript_empty)
        } else {
            getString(R.string.accessibility_transcript_value, text)
        }
    }

    private fun updateResponse(text: String) {
        responseText.text = text
        responseText.contentDescription = if (text.isBlank()) {
            getString(R.string.accessibility_response_empty)
        } else {
            getString(R.string.accessibility_response_value, text)
        }
    }

    private fun updateResponse(resId: Int) {
        updateResponse(getString(resId))
    }
}
