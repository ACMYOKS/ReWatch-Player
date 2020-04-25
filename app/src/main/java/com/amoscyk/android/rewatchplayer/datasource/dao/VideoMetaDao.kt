package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.room.*
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource

@Dao
interface VideoMetaDao {
    @Query("SELECT * FROM video_metas")
    fun getAll(): List<VideoMeta>

    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    fun getByVideoId(vararg videoIds: String): List<VideoMeta>

    @Query("SELECT * FROM video_metas WHERE bookmarked = 1")
    fun getBookmarked(): List<VideoMeta>

    @Transaction
    @Query("SELECT * FROM video_metas")
    fun getAllWithPlayerResource(): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas WHERE video_id IN (:videoIds)")
    fun getByVideoIdWithPlayerResource(vararg videoIds: String): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas INNER JOIN player_resources ON video_metas.video_id = player_resources.video_id GROUP BY video_metas.video_id")
    fun getAllExistingPlayerResource(): List<VideoMetaWithPlayerResource>

    @Transaction
    @Query("SELECT * FROM video_metas WHERE bookmarked = 1")
    fun getBookmarkedWithPlayerResource(): List<VideoMetaWithPlayerResource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg videoMetas: VideoMeta): List<Long>

    @Update
    fun update(vararg videoMetas: VideoMeta): Int

    @Delete
    fun delete(vararg videoMetas: VideoMeta): Int

    @Query("DELETE FROM video_metas WHERE video_id = :videoId")
    fun deleteByVideoId(videoId: String): Int
}