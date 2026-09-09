package com.usbmediaexplorer

import com.usbmediaexplorer.data.ops.BulkRenameRules
import com.usbmediaexplorer.data.ops.JobProgress
import com.usbmediaexplorer.data.ops.JobState
import com.usbmediaexplorer.data.ops.OpType
import com.usbmediaexplorer.data.search.SearchEngine
import com.usbmediaexplorer.data.store.PlaybackPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM invariants for the progress / resume state machines and the search classifier.
 *
 * These are the seams the UI depends on without touching Android, Media3 or USB:
 *  - the transfer bar and the transfers screen read [JobProgress.percent] / [JobProgress.isActive],
 *  - "continue watching" and the resume prompt read [PlaybackPosition.isFinished] / [remainingMs],
 *  - the library filters rely on [SearchEngine.looksLikeEpisode] / [SearchEngine.looksLikeMovie].
 */

class JobProgressTest {

    @Test
    fun `percent uses bytes when a byte total is known`() {
        val job = job(state = JobState.RUNNING, totalBytes = 1_000L, transferredBytes = 250L)
        assertEquals(25, job.percent)
    }

    @Test
    fun `percent falls back to item count when bytes are unknown`() {
        val job = job(state = JobState.RUNNING, totalItems = 4, doneItems = 1)
        assertEquals(25, job.percent)
    }

    @Test
    fun `percent clamps to the 0-100 range`() {
        assertEquals(100, job(JobState.RUNNING, totalBytes = 100, transferredBytes = 500).percent)
        assertEquals(0, job(JobState.RUNNING, totalItems = 10, doneItems = 0).percent)
    }

    @Test
    fun `a finished job without byte or item totals reports 100 percent`() {
        assertEquals(100, job(JobState.DONE, totalBytes = 0, totalItems = 0).percent)
    }

    @Test
    fun `only queued running and paused jobs are active`() {
        assertTrue(job(JobState.QUEUED).isActive)
        assertTrue(job(JobState.RUNNING).isActive)
        assertTrue(job(JobState.PAUSED).isActive)
        assertFalse(job(JobState.CANCELED).isActive)
        assertFalse(job(JobState.FAILED).isActive)
        assertFalse(job(JobState.DONE).isActive)
    }

    private fun job(
        state: JobState,
        totalItems: Int = 0,
        doneItems: Int = 0,
        totalBytes: Long = 0L,
        transferredBytes: Long = 0L,
    ) = JobProgress(
        jobId = "id",
        type = OpType.COPY,
        state = state,
        totalItems = totalItems,
        doneItems = doneItems,
        totalBytes = totalBytes,
        transferredBytes = transferredBytes,
        currentItemName = "file.mkv",
        destinationLabel = "dest",
        speedBytesPerSec = 0.0,
        etaMs = -1L,
        startedAt = 0L,
    )
}

class PlaybackPositionTest {

    @Test
    fun `a video is finished once past 96 percent`() {
        assertTrue(position(96, 100).isFinished)
        assertFalse(position(95, 100).isFinished)
    }

    @Test
    fun `a position without a known duration is never finished`() {
        assertFalse(position(5_000, 0).isFinished)
    }

    @Test
    fun `remaining time never goes negative`() {
        assertEquals(0L, position(120, 100).remainingMs)
        assertEquals(4_000L, position(96_000, 100_000).remainingMs)
    }

    @Test
    fun `progress is clamped and zero without a duration`() {
        assertEquals(0f, position(10, 0).progress)
        assertEquals(0.5f, position(50, 100).progress, 0.0001f)
    }

    private fun position(positionMs: Long, durationMs: Long) = PlaybackPosition(
        key = "content://x",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = 0L,
    )
}

class SearchClassifierTest {

    @Test
    fun `episode markers are recognised`() {
        assertTrue(SearchEngine.looksLikeEpisode("S01E01.mkv"))
        assertTrue(SearchEngine.looksLikeEpisode("s02.e10.mkv"))
        assertTrue(SearchEngine.looksLikeEpisode("1x05.mkv"))
        assertTrue(SearchEngine.looksLikeEpisode("Episode 7.mkv"))
    }

    @Test
    fun `movie-like names have a year or resolution and no episode marker`() {
        assertTrue(SearchEngine.looksLikeMovie("The.Matrix.1999.1080p.mkv"))
        assertTrue(SearchEngine.looksLikeMovie("Dune.2021.2160p.BluRay.mkv"))
        assertFalse(SearchEngine.looksLikeMovie("S01E01.mkv"))
        assertFalse(SearchEngine.looksLikeMovie("clip.mp4"))
    }
}

class BulkRenameRulesTest {

    @Test
    fun `default rules still trim spaces so they are not considered empty`() {
        // trimSpaces defaults to true: an all-default rule set is a real "normalize names" action.
        assertFalse(BulkRenameRules().isEmpty)
    }

    @Test
    fun `a rule set with every transform disabled is empty`() {
        assertTrue(BulkRenameRules(trimSpaces = false).isEmpty)
    }

    @Test
    fun `any single transform makes the rule set non-empty`() {
        assertFalse(BulkRenameRules(prefix = "USB ").isEmpty)
        assertFalse(BulkRenameRules(numbering = true).isEmpty)
        assertFalse(BulkRenameRules(lowercase = true).isEmpty)
    }
}
