package com.usbmediaexplorer.testing

import java.util.concurrent.atomic.AtomicLong

/** Injectable clock seam for deterministic timeout/position tests. */
class TestClock(startMillis: Long = 0L) {
    private val current = AtomicLong(startMillis)
    fun now(): Long = current.get()
    fun advanceBy(millis: Long) { current.addAndGet(millis) }
}

/** Minimal player seam used by unit tests without constructing Media3/ExoPlayer. */
class FakePlayerFacade {
    var isPlaying: Boolean = false
        private set
    var positionMs: Long = 0L
        private set
    var prepareCount: Int = 0
        private set
    fun prepare(positionMs: Long = this.positionMs) { this.positionMs = positionMs; prepareCount++ }
    fun play() { isPlaying = true }
    fun pause() { isPlaying = false }
    fun seekTo(positionMs: Long) { this.positionMs = positionMs }
}
