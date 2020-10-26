package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoBookmark

@Dao
interface VideoBookmarkDao {
    @Query("SELECT * FROM video_bookmarks ORDER BY id")
    fun getAll(): LiveData<List<VideoBookmark>>

    @Query("SELECT * FROM video_bookmarks WHERE username = :username ORDER BY id")
    fun getAllForUser(username: String): LiveData<List<VideoBookmark>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg bookmark: VideoBookmark): List<Long>

    @Query("INSERT INTO video_bookmarks (username, video_id) VALUES (:username, :videoId)")
    suspend fun insert(username: String, videoId: String): Long

    @Delete
    suspend fun delete(vararg bookmark: VideoBookmark): Int

    @Query("DELETE FROM video_bookmarks WHERE username = :username AND video_id IN (:videoIds)")
    suspend fun delete(username: String, videoIds: Array<String>): Int
}