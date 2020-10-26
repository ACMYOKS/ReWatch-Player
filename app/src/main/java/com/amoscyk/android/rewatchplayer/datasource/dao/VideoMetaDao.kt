package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource

@Dao
interface VideoMetaDao {
    @Query("SELECT * FROM video_metas")
    suspend fun getAll(): List<VideoMeta>

    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    suspend fun getByVideoId(vararg videoIds: String): List<VideoMeta>

    @Transaction
    @Query("SELECT * FROM video_metas WHERE video_id in (SELECT video_id FROM video_bookmarks WHERE username = :username)")
    suspend fun getBookmarked(username: String): List<VideoMeta>

    @Transaction
    @Query("SELECT * FROM video_metas")
    suspend fun getAllWithPlayerResource(): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    suspend fun getByVideoIdWithPlayerResource(vararg videoIds: String): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas INNER JOIN player_resources ON video_metas.video_id = player_resources.video_id GROUP BY video_metas.video_id")
    suspend fun getAllExistingPlayerResource(): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas WHERE video_id in (SELECT video_id FROM video_bookmarks WHERE username = :username)")
    fun getBookmarkedWithPlayerResource(username: String): LiveData<List<VideoMetaWithPlayerResource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg videoMetas: VideoMeta): List<Long>

    @Update
    suspend fun update(vararg videoMetas: VideoMeta): Int

    @Delete
    suspend fun delete(vararg videoMetas: VideoMeta): Int

    @Query("DELETE FROM video_metas WHERE video_id = :videoId")
    suspend fun deleteByVideoId(videoId: String): Int
}