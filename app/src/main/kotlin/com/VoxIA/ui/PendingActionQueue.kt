package com.voxia.ui

/** Keeps UI actions until their lifecycle-bound dependency is ready. */
internal class PendingActionQueue<T> {
    private val actions = ArrayDeque<(T) -> Unit>()

    val isNotEmpty: Boolean
        get() = actions.isNotEmpty()

    fun enqueue(action: (T) -> Unit) {
        actions.addLast(action)
    }

    fun drain(target: T) {
        while (actions.isNotEmpty()) {
            actions.removeFirst().invoke(target)
        }
    }

    fun clear() {
        actions.clear()
    }
}
