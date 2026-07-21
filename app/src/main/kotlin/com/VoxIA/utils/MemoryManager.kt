package com.voxia.utils

object MemoryManager {

    private const val TAG = "MemoryManager"
    private const val RAM_BUDGET_MB = 900
    private var currentUsageMb = 0
    private val modules = mutableMapOf<String, Int>()

    val RAM_BUDGETS = mapOf(
        "vision" to 180,
        "ocr" to 150,
        "android_stt" to 40,
        "intent" to 15,
        "app" to 20,
        "tts" to 20
    )

    fun canLoad(moduleName: String): Boolean {
        val moduleRam = RAM_BUDGETS[moduleName] ?: return true
        val projected = currentUsageMb + moduleRam
        if (projected > RAM_BUDGET_MB) {
            PrivacyLog.w(TAG, "Budget RAM insuffisant pour $moduleName: $projected > $RAM_BUDGET_MB Mo")
            return false
        }
        return true
    }

    fun load(moduleName: String) {
        val ram = RAM_BUDGETS[moduleName] ?: return
        currentUsageMb += ram
        modules[moduleName] = ram
        PrivacyLog.d(TAG, "[CHARGER] $moduleName -> +${ram}Mo | Total: ${currentUsageMb}Mo")
    }

    fun unload(moduleName: String) {
        val ram = modules.remove(moduleName) ?: return
        currentUsageMb -= ram
        PrivacyLog.d(TAG, "[DECHARGER] $moduleName -> -${ram}Mo | Total: ${currentUsageMb}Mo")
    }

    fun unloadAll() {
        modules.clear()
        currentUsageMb = 0
        System.gc()
        PrivacyLog.d(TAG, "[VIDE] Tous les modules déchargés")
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
