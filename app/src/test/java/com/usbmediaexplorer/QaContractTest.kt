package com.usbmediaexplorer

import android.net.Uri
import com.usbmediaexplorer.data.doc.DocNode
import com.usbmediaexplorer.testing.FakeDocProvider
import com.usbmediaexplorer.testing.FakePlayerFacade
import com.usbmediaexplorer.testing.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class QaContractTest {
    @Test
    fun `fake provider exposes media counts and bounded children`() = runTest {
        val root = node("root", true, 0)
        val video = node("clip.mp4", false, 10)
        val audio = node("song.mp3", false, 20)
        val provider = FakeDocProvider(listOf(root, video, audio))
        assertEquals(2, provider.mediaChildren(root, 2).size)
        assertEquals(1, provider.mediaCount(root).videos)
        assertEquals(1, provider.mediaCount(root).audios)
    }

    @Test
    fun `fake player and clock make recovery deterministic`() {
        val clock = TestClock(1000)
        val player = FakePlayerFacade()
        player.prepare(42)
        player.play()
        clock.advanceBy(2000)
        player.pause()
        assertEquals(42, player.positionMs)
        assertEquals(1, player.prepareCount)
        assertTrue(clock.now() >= 3000)
    }

    private fun node(name: String, directory: Boolean, size: Long): DocNode {
        val uri = Mockito.mock(Uri::class.java, Mockito.RETURNS_DEFAULTS)
        Mockito.`when`(uri.toString()).thenReturn("content://fake/$name")
        return DocNode(
            uri = uri,
            name = name,
            isDirectory = directory,
            size = size,
            lastModified = 1L,
            mimeType = null,
            volumeId = "fake",
            displayPath = if (directory) "root" else "root/$name",
        )
    }
}
