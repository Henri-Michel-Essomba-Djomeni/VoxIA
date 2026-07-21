package com.voxia.assistant

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

data class VoxiaNotification(
    val app: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

object NotificationRepository {
    private const val MAX_ITEMS = 30
    private val items = CopyOnWriteArrayList<VoxiaNotification>()

    fun add(item: VoxiaNotification) {
        items.removeAll { it.app == item.app && it.title == item.title && it.text == item.text }
        items.add(0, item)
        while (items.size > MAX_ITEMS) items.removeAt(items.lastIndex)
    }

    fun latest(): List<VoxiaNotification> = items.sortedByDescending { it.timestamp }
}

class VoxiaNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val app = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(sbn.packageName)
        NotificationRepository.add(VoxiaNotification(app, title, text, sbn.postTime))
    }
}
