package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoBookmark

@Dao
interface VideoBookmarkDao {
    @Query("SELECT * FROM video_bookmarks")
    fun getAll(): List<VideoBookmark>

    @Query("SELECT * FROM video_bookmarks WHERE username = :username")
    fun getAllForUser(username: String): List<VideoBookmark>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg bookmark: VideoBookmark): List<Long>

    @Query("INSERT INTO video_bookmarks (username, video_id) VALUES (:username, :videoId)")
    fun insert(username: String, videoId: String): Long

    @Delete
    fun delete(vararg bookmark: VideoBookmark): Int

    @Query("DELETE FROM video_bookmarks WHERE username = :username AND video_id IN (:videoIds)")
    fun delete(username: String, videoIds: Array<String>): Int
}