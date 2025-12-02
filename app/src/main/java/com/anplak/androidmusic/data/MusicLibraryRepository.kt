package com.anplak.androidmusic.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

interface MusicLibraryRepository {
    suspend fun getAllTracks(): List<TrackInfo>
    suspend fun scanMusicDirectories()
}

class MusicLibraryRepositoryImpl(
    private val contentResolver: ContentResolver,
    private val context: Context? = null
) : MusicLibraryRepository {

    /**
     * Scans common music directories (Music, Download) to ensure MediaStore is up-to-date.
     * This is needed because files added via adb, file manager, or downloads may not be
     * immediately indexed by MediaStore.
     */
    override suspend fun scanMusicDirectories() = withContext(Dispatchers.IO) {
        val ctx = context ?: run {
            Log.w(TAG, "Context not available, skipping media scan")
            return@withContext
        }
        
        Log.d(TAG, "Starting media scan for Music and Download directories...")
        
        val directories = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )
        
        val audioExtensions = listOf(".mp3", ".m4a", ".flac", ".ogg", ".wav", ".aac")
        val filesToScan = mutableListOf<String>()
        
        directories.forEach { dir ->
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                dir.listFiles()?.filter { file ->
                    file.isFile && audioExtensions.any { ext -> 
                        file.name.endsWith(ext, ignoreCase = true) 
                    }
                }?.forEach { file ->
                    filesToScan.add(file.absolutePath)
                }
            }
        }
        
        if (filesToScan.isEmpty()) {
            Log.d(TAG, "No audio files found to scan")
            return@withContext
        }
        
        Log.d(TAG, "Found ${filesToScan.size} audio files to scan")
        
        // Scan all files and wait for completion
        suspendCancellableCoroutine { continuation ->
            var remaining = filesToScan.size
            val lock = Object()
            
            MediaScannerConnection.scanFile(
                ctx,
                filesToScan.toTypedArray(),
                null
            ) { path, uri ->
                Log.d(TAG, "Scanned: $path -> $uri")
                synchronized(lock) {
                    remaining--
                    if (remaining == 0) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
        
        Log.d(TAG, "Media scan complete")
    }
    
    override suspend fun getAllTracks(): List<TrackInfo> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackInfo>()
        var scannedCount = 0
        var errorCount = 0
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        
        // No IS_MUSIC filter - discover all audio files from MediaStore
        // The IS_MUSIC flag is unreliable: files without proper metadata, 
        // short duration files, or files in certain folders get IS_MUSIC=0
        val selection: String? = null
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        Log.d(TAG, "Starting media library scan...")
        
        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "MediaStore query returned ${cursor.count} musica entries")
                
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                
                while (cursor.moveToNext()) {
                    scannedCount++
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getStringOrDefault(titleColumn, "Unknown Title")
                        val artist = cursor.getStringOrDefault(artistColumn, "Unknown Artist")
                        val album = cursor.getStringOrDefault(albumColumn, "Unknown Album")
                        val duration = cursor.getLong(durationColumn)
                        
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        
                        val track = TrackInfo(
                            uri = contentUri,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration
                        )
                        tracks.add(track)
                        
                        Log.d(TAG, "Loaded track: id=$id, title=\"$title\", artist=\"$artist\", duration=${duration}ms")
                    } catch (e: Exception) {
                        errorCount++
                        Log.e(TAG, "Failed to parse track at position ${cursor.position}", e)
                    }
                }
            } ?: run {
                Log.w(TAG, "MediaStore query returned null cursor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed", e)
        }
        
        Log.d(TAG, "Scan complete: ${tracks.size} tracks loaded, $scannedCount scanned, $errorCount errors")
        
        tracks
    }
    
    private fun Cursor.getStringOrDefault(columnIndex: Int, default: String): String {
        return getString(columnIndex)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: default
    }
    
    companion object {
        private const val TAG = "MusicLibraryRepository"
    }
}
