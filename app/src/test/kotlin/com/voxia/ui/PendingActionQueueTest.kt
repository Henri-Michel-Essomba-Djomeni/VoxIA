package com.voxia.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingActionQueueTest {
    @Test
    fun `drain executes every action once and in order`() {
        val queue = PendingActionQueue<MutableList<String>>()
        val calls = mutableListOf<String>()

        queue.enqueue { it += "read" }
        queue.enqueue { it += "identify" }

        assertTrue(queue.isNotEmpty)
        queue.drain(calls)
        queue.drain(calls)

        assertEquals(listOf("read", "identify"), calls)
        assertFalse(queue.isNotEmpty)
    }

    @Test
    fun `clear cancels all pending actions`() {
        val queue = PendingActionQueue<MutableList<String>>()
        val calls = mutableListOf<String>()
        queue.enqueue { it += "translate" }

        queue.clear()
        queue.drain(calls)

        assertTrue(calls.isEmpty())
        assertFalse(queue.isNotEmpty)
    }
}
