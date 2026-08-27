package com.voxia.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DelayedAnnouncementCoordinatorTest {

    @Test
    fun cancel_suppressesPendingAndAlreadyDequeuedCallback() {
        val harness = AnnouncementHarness()
        var announcements = 0

        harness.coordinator.replace(500L) { announcements++ }
        val staleTask = harness.scheduled.single()
        harness.coordinator.cancel()
        staleTask.run()

        assertEquals(0, announcements)
        assertTrue(harness.scheduled.isEmpty())
    }

    @Test
    fun replace_keepsOnlyLatestAnnouncement() {
        val harness = AnnouncementHarness()
        val announcements = mutableListOf<String>()

        harness.coordinator.replace(500L) { announcements += "first" }
        val staleTask = harness.scheduled.single()
        harness.coordinator.replace(500L) { announcements += "second" }
        val currentTask = harness.scheduled.single()
        staleTask.run()
        currentTask.run()

        assertEquals(listOf("second"), announcements)
    }

    @Test
    fun elapsedTask_announcesExactlyOnce() {
        val harness = AnnouncementHarness()
        var announcements = 0

        harness.coordinator.replace(500L) { announcements++ }
        val task = harness.scheduled.single()
        task.run()
        task.run()

        assertEquals(1, announcements)
    }

    @Test
    fun replace_passesConfiguredDelayToScheduler() {
        val harness = AnnouncementHarness()

        harness.coordinator.replace(750L) {}

        assertEquals(750L, harness.lastDelayMillis)
    }

    private class AnnouncementHarness {
        val scheduled = mutableListOf<Runnable>()
        var lastDelayMillis = -1L
        val coordinator = DelayedAnnouncementCoordinator(
            schedule = { task, delayMillis ->
                scheduled += task
                lastDelayMillis = delayMillis
            },
            unschedule = scheduled::remove
        )
    }
}
