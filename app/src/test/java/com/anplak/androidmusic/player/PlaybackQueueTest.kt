package com.anplak.androidmusic.player

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackQueueTest {
    
    @Test
    fun `empty queue has correct properties`() {
        val queue = PlaybackQueue.EMPTY
        
        assertTrue(queue.isEmpty)
        assertEquals(0, queue.size)
        assertNull(queue.currentTrack)
        assertFalse(queue.hasNext())
        assertFalse(queue.hasPrevious())
    }
    
    @Test
    fun `single track queue has no next or previous`() {
        val queue = createQueueWithTracks(1)
        
        assertFalse(queue.isEmpty)
        assertEquals(1, queue.size)
        assertEquals(0, queue.currentIndex)
        assertFalse(queue.hasNext())
        assertFalse(queue.hasPrevious())
    }
    
    @Test
    fun `queue at first position has next but no previous`() {
        val queue = createQueueWithTracks(3, startIndex = 0)
        
        assertTrue(queue.hasNext())
        assertFalse(queue.hasPrevious())
    }
    
    @Test
    fun `queue at last position has previous but no next`() {
        val queue = createQueueWithTracks(3, startIndex = 2)
        
        assertFalse(queue.hasNext())
        assertTrue(queue.hasPrevious())
    }
    
    @Test
    fun `queue at middle position has both next and previous`() {
        val queue = createQueueWithTracks(3, startIndex = 1)
        
        assertTrue(queue.hasNext())
        assertTrue(queue.hasPrevious())
    }
    
    @Test
    fun `next advances to next track`() {
        val queue = createQueueWithTracks(3, startIndex = 0)
        
        val nextQueue = queue.next()
        
        assertEquals(1, nextQueue.currentIndex)
        assertEquals("Track 2", nextQueue.currentTrack?.title)
    }
    
    @Test
    fun `next at last position returns same queue`() {
        val queue = createQueueWithTracks(3, startIndex = 2)
        
        val nextQueue = queue.next()
        
        assertEquals(2, nextQueue.currentIndex)
    }
    
    @Test
    fun `previous goes to previous track`() {
        val queue = createQueueWithTracks(3, startIndex = 2)
        
        val prevQueue = queue.previous()
        
        assertEquals(1, prevQueue.currentIndex)
        assertEquals("Track 2", prevQueue.currentTrack?.title)
    }
    
    @Test
    fun `previous at first position returns same queue`() {
        val queue = createQueueWithTracks(3, startIndex = 0)
        
        val prevQueue = queue.previous()
        
        assertEquals(0, prevQueue.currentIndex)
    }
    
    @Test
    fun `jumpTo valid index changes position`() {
        val queue = createQueueWithTracks(5, startIndex = 0)
        
        val jumpedQueue = queue.jumpTo(3)
        
        assertEquals(3, jumpedQueue.currentIndex)
        assertEquals("Track 4", jumpedQueue.currentTrack?.title)
    }
    
    @Test
    fun `jumpTo invalid index returns same queue`() {
        val queue = createQueueWithTracks(3, startIndex = 1)
        
        val jumpedQueue = queue.jumpTo(10)
        
        assertEquals(1, jumpedQueue.currentIndex)
    }
    
    @Test
    fun `jumpTo negative index returns same queue`() {
        val queue = createQueueWithTracks(3, startIndex = 1)
        
        val jumpedQueue = queue.jumpTo(-1)
        
        assertEquals(1, jumpedQueue.currentIndex)
    }
    
    @Test
    fun `fromLibrary creates queue starting at selected index`() {
        val tracks = createTestTracks(5)
        
        val queue = PlaybackQueue.fromLibrary(tracks, startIndex = 2)
        
        assertEquals(5, queue.size)
        assertEquals(2, queue.currentIndex)
        assertEquals("Track 3", queue.currentTrack?.title)
    }
    
    @Test
    fun `fromLibrary with empty list returns empty queue`() {
        val queue = PlaybackQueue.fromLibrary(emptyList(), startIndex = 0)
        
        assertTrue(queue.isEmpty)
    }
    
    @Test
    fun `fromLibrary with invalid index returns empty queue`() {
        val tracks = createTestTracks(3)
        
        val queue = PlaybackQueue.fromLibrary(tracks, startIndex = 10)
        
        assertTrue(queue.isEmpty)
    }
    
    @Test
    fun `currentTrack returns correct track`() {
        val queue = createQueueWithTracks(3, startIndex = 1)
        
        assertEquals("Track 2", queue.currentTrack?.title)
    }
    
    private fun createQueueWithTracks(count: Int, startIndex: Int = 0): PlaybackQueue {
        return PlaybackQueue(createTestTracks(count), startIndex)
    }
    
    private fun createTestTracks(count: Int): List<TrackInfo> {
        return (1..count).map { index ->
            TrackInfo(
                uri = Uri.parse("content://media/external/audio/media/$index"),
                title = "Track $index",
                artist = "Artist $index",
                album = "Album $index",
                duration = 180000L
            )
        }
    }
}

