package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.WatchHistory

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history WHERE username = :username")
    suspend fun getAllForUser(username: String): List<WatchHistory>

    @Query("SELECT * FROM watch_history WHERE video_id IN (:videoIds) AND username = :username")
    suspend fun getWithVideoIdForUser(videoIds: Array<String>, username: String): List<WatchHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg histories: WatchHistory): List<Long>

    @Update
    suspend fun update(vararg histories: WatchHistory): Int

    @Delete
    suspend fun delete(vararg histories: WatchHistory): Int

    @Query("DELETE FROM watch_history WHERE username = :username AND video_id IN (:videoIds)")
    suspend fun delete(username: String, videoIds: Array<String>): Int
}