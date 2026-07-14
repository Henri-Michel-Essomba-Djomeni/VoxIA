package com.VoxIA.speech.wakeword

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineActivationException
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback

class WakeWordService(private val context: Context) {

    companion object {
        private const val TAG = "VoxIA_WakeWord"
        // Clé Picovoice — à mettre dans local.properties
        private const val ACCESS_KEY = BuildConfig.PICOVOICE_KEY
        // Nom du fichier .ppn dans assets/ (sera généré sur console Picovoice)
        private const val WAKE_WORD_FILE = "voxia_android.ppn"
    }

    private var porcupineManager: PorcupineManager? = null
    private var isActive = false
    private val listeners = mutableListOf<() -> Unit>()

    // ─── DÉMARRER LA DÉTECTION ────────────────────────
    fun start(onError: ((String) -> Unit)? = null) {
        if (isActive) return

        try {
            // Charger le fichier .ppn depuis assets
            val keywordPath = copyAssetToCache(WAKE_WORD_FILE)

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(ACCESS_KEY)
                .setKeywordPath(keywordPath)
                .setSensitivity(0.7f) // 0.0 à 1.0 — plus haut = plus sensible
                .build(context, PorcupineManagerCallback { keywordIndex ->
                    Log.d(TAG, "Wake word détecté ! index=$keywordIndex")
                    trigger()
                })

            porcupineManager?.start()
            isActive = true
            Log.d(TAG, "Porcupine actif — en attente de 'VOXIA'")

        } catch (e: PorcupineActivationException) {
            Log.e(TAG, "Clé Picovoice invalide: ${e.message}")
            onError?.invoke("Clé Picovoice invalide")
            startDevMode() // fallback simulation
        } catch (e: PorcupineException) {
            Log.e(TAG, "Erreur Porcupine: ${e.message}")
            onError?.invoke(e.message ?: "Erreur Porcupine")
            startDevMode() // fallback simulation
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue: ${e.message}")
            onError?.invoke(e.message ?: "Erreur inconnue")
            startDevMode() // fallback simulation
        }
    }

    // ─── MODE DEV : simulation ────────────────────────
    // Utilisé tant que le fichier .ppn n'est pas disponible
    private fun startDevMode() {
        Log.w(TAG, "Mode DEV — wake word simulé toutes les 30s")
        isActive = true
        Thread {
            while (isActive) {
                Thread.sleep(30000)
                if (isActive) {
                    Log.d(TAG, "Wake word simulé")
                    trigger()
                }
            }
        }.start()
    }

    // ─── DÉCLENCHER LES LISTENERS ─────────────────────
    private fun trigger() {
        listeners.forEach { it.invoke() }
    }

    // ─── S'ABONNER ────────────────────────────────────
    fun onWakeWord(callback: () -> Unit): () -> Unit {
        listeners.add(callback)
        // Retourne une fonction pour se désabonner
        return { listeners.remove(callback) }
    }

    // ─── ARRÊTER ──────────────────────────────────────
    fun stop() {
        isActive = false
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur arrêt Porcupine: ${e.message}")
        }
        Log.d(TAG, "WakeWord arrêté")
    }

    // ─── COPIER ASSET VERS CACHE ──────────────────────
    // Porcupine a besoin d'un chemin fichier, pas d'un asset stream
    private fun copyAssetToCache(fileName: String): String {
        val outFile = java.io.File(context.cacheDir, fileName)
        if (!outFile.exists()) {
            context.assets.open(fileName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    fun isRunning() = isActive
}