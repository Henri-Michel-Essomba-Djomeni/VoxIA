package com.voxia.utils

import android.util.Log

object MemoryManager {

    private const val TAG = "MemoryManager"
    private const val RAM_BUDGET_MB = 900
    private var currentUsageMb = 0
    private val modules = mutableMapOf<String, Int>()

    val RAM_BUDGETS = mapOf(
        "vision" to 180,
        "ocr" to 150,
        "vosk_fr" to 120,
        "vosk_en" to 120,
        "intent" to 15,
        "app" to 20,
        "tts" to 20
    )

    fun canLoad(moduleName: String): Boolean {
        val moduleRam = RAM_BUDGETS[moduleName] ?: return true
        val projected = currentUsageMb + moduleRam
        if (projected > RAM_BUDGET_MB) {
            Log.w(TAG, "Budget RAM insuffisant pour $moduleName: $projected > $RAM_BUDGET_MB Mo")
            return false
        }
        return true
    }

    fun load(moduleName: String) {
        val ram = RAM_BUDGETS[moduleName] ?: return
        currentUsageMb += ram
        modules[moduleName] = ram
        Log.d(TAG, "[CHARGER] $moduleName → +${ram}Mo | Total: ${currentUsageMb}Mo")
    }

    fun unload(moduleName: String) {
        val ram = modules.remove(moduleName) ?: return
        currentUsageMb -= ram
        Log.d(TAG, "[DÉCHARGER] $moduleName → -${ram}Mo | Total: ${currentUsageMb}Mo")
    }

    fun unloadAll() {
        modules.clear()
        currentUsageMb = 0
        System.gc()
        Log.d(TAG, "[VIDE] Tous les modules déchargés")
    }

    fun unloadExcept(moduleNames: Set<String>) {
        modules.keys
            .filter { it !in moduleNames }
            .forEach { unload(it) }
    }

    fun getCurrentUsageMb() = currentUsageMb

    fun isWithinBudget() = currentUsageMb <= RAM_BUDGET_MB

    fun getLoadedModules() = modules.keys.toList()
}
