package com.voxia.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.voxia.assistant.R
import com.voxia.assistant.VoiceAssistantService

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var previewView: PreviewView
    private var voiceService: VoiceAssistantService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VoiceAssistantService.LocalBinder
            voiceService = binder.getService()
            isServiceBound = true
            voiceService?.setLifecycleOwner(this@MainActivity)
            voiceService?.setPreviewView(previewView)
            Log.d("MainActivity", "Service VOXIA connecté")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isServiceBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startVoiceService()
        } else {
            Toast.makeText(this, "Permissions requises pour VOXIA", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (hasAllPermissions()) {
            startVoiceService()
        } else {
            requestPermissions()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(getRequiredPermissions())
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        val intent = Intent(this, VoiceAssistantService::class.java)
        stopService(intent)
        super.onDestroy()
    }
}
