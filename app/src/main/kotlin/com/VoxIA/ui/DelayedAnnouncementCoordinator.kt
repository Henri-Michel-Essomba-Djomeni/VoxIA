package com.voxia.ui

/**
 * Owns at most one delayed accessibility announcement.
 *
 * The scheduler stays injectable so cancellation and stale-callback races can
 * be verified on the JVM without depending on an Android View or Looper.
 */
internal class DelayedAnnouncementCoordinator(
    private val schedule: (Runnable, Long) -> Unit,
    private val unschedule: (Runnable) -> Unit
) {
    private val lock = Any()
    private var pending: Runnable? = null

    fun replace(delayMillis: Long, announce: () -> Unit) {
        lateinit var task: Runnable
        task = Runnable {
            val shouldAnnounce = synchronized(lock) {
                if (pending !== task) return@synchronized false
                pending = null
                true
            }
            if (shouldAnnounce) announce()
        }
        val previous = synchronized(lock) {
            val oldTask = pending
            pending = task
            oldTask
        }
        previous?.let(unschedule)
        schedule(task, delayMillis)
    }

    fun cancel() {
        val task = synchronized(lock) {
            val currentTask = pending ?: return
            pending = null
            currentTask
        }
        unschedule(task)
    }
}
