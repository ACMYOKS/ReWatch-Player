package com.amoscyk.android.rewatchplayer.datasource.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.amoscyk.android.rewatchplayer.datasource.vo.local.WatchHistoryVideoMeta

@Dao
interface WatchHistoryVideoMetaDao {
    @Query("SELECT vm.video_id, vm.title, vm.channel_title, vm.duration, wh.last_watch_pos_millis, wh.recent_watch_date_time_millis FROM video_metas AS vm INNER JOIN watch_history AS wh ON vm.video_id = wh.video_id WHERE wh.username = :username ORDER BY wh.recent_watch_date_time_millis DESC")
    fun getAllForUser(username: String): LiveData<List<WatchHistoryVideoMeta>>
}