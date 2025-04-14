package com.masum.musicplayer.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.masum.musicplayer.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val TAG = "MusicRepository"

    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        try {
            Log.d(TAG, "Starting to load songs from MediaStore")
            Log.d(TAG, "Content URI: ${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}")
            
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.GENRE
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            Log.d(TAG, "Querying MediaStore with selection: $selection")
            
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} songs in MediaStore")
                
                if (cursor.count == 0) {
                    Log.w(TAG, "No songs found in MediaStore")
                    return@withContext emptyList()
                }
                
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val genreColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val duration = cursor.getLong(durationColumn)
                        val data = cursor.getString(dataColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val track = cursor.getInt(trackColumn)
                        val year = cursor.getInt(yearColumn)
                        val genre = cursor.getString(genreColumn)

                        if (data != null) {
                            val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

                            songs.add(
                                Song(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    duration = duration,
                                    uri = Uri.parse(data),
                                    albumArtUri = albumArtUri,
                                    trackNumber = track,
                                    year = year,
                                    genre = genre
                                )
                            )
                            Log.d(TAG, "Added song: $title by $artist")
                        } else {
                            Log.w(TAG, "Skipping song with null data path: $title")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing song: ${e.message}", e)
                    }
                }
            } ?: run {
                Log.e(TAG, "Cursor is null, no songs found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading songs: ${e.message}", e)
            throw e
        }
        
        Log.d(TAG, "Successfully loaded ${songs.size} songs")
        return@withContext songs
    }
} 