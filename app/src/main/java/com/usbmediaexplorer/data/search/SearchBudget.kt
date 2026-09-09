package com.usbmediaexplorer.data.search

/** Deterministic limits that keep a slow USB walk bounded and cancellable. */
object SearchBudget {
    const val MAX_WALK_NODES = 15_000
    const val MAX_WALK_MILLIS = 10_000L
    fun expired(startMillis: Long, nowMillis: Long): Boolean = nowMillis - startMillis >= MAX_WALK_MILLIS
}
