package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource

@Dao
interface PlayerResourceDao {
    @Query("SELECT * FROM player_resources ORDER BY video_id, itag")
    suspend fun getAll(): List<PlayerResource>

    @Query("SELECT * FROM player_resources WHERE video_id IN (:videoIds) ORDER BY video_id, itag")
    suspend fun getByVideoId(vararg videoIds: String): List<PlayerResource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg resources: PlayerResource): List<Long>

    @Delete
    suspend fun delete(vararg resources: PlayerResource): Int

    @Query("DELETE FROM player_resources WHERE video_id IN (:videoId)")
    suspend fun deleteByVideoId(vararg videoId: String): Int

    @Query("DELETE FROM player_resources WHERE video_id = :videoId AND itag IN (:itags)")
    suspend fun deleteByVideoIdWithITag(videoId: String, vararg itags: Int): Int

    @Query("DELETE FROM player_resources WHERE download_id IN (:downloadIds)")
    suspend fun deleteByDownloadId(vararg downloadIds: Long): Int
}