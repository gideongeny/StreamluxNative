package com.streamlux.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_items WHERE isWatchlist = 1 ORDER BY timestamp DESC")
    fun getWatchlist(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items WHERE isHistory = 1 ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarked(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items WHERE isDownload = 1 ORDER BY timestamp DESC")
    fun getDownloads(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getItemByMediaId(mediaId: String): LibraryEntity?

    @Query("SELECT * FROM library_items WHERE systemDownloadId = :downloadId LIMIT 1")
    suspend fun getItemByDownloadId(downloadId: Long): LibraryEntity?

    @Query("SELECT * FROM library_items WHERE parentId = :parentId AND isDownload = 1 ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodesForShow(parentId: String): Flow<List<LibraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LibraryEntity)

    @Delete
    suspend fun deleteItem(item: LibraryEntity)

    @Query("DELETE FROM library_items WHERE id = :id AND isDownload = 1")
    suspend fun deleteDownloadById(id: String)
}
