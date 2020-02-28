package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource

@Dao
interface PlayerResourceDao {
    @Query("SELECT * FROM player_resources")
    fun getAll(): List<PlayerResource>

    @Query("SELECT * FROM player_resources WHERE video_id IN (:videoIds)")
    fun getByVideoId(vararg videoIds: String): List<PlayerResource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg resources: PlayerResource): List<Long>

    @Delete
    fun delete(resource: PlayerResource): Int

    @Query("DELETE FROM player_resources WHERE video_id in (:videoId)")
    fun deleteByVideoId(vararg videoId: String): Int

    @Query("DELETE FROM player_resources WHERE video_id = :videoId AND itag = :itag")
    fun deleteByVideoIdWithITag(videoId: String, itag: Int): Int
}