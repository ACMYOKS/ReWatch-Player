package com.amoscyk.android.rewatchplayer.datasource.vo.local

import androidx.room.Embedded
import androidx.room.Relation

data class VideoMetaWithPlayerResource(
    @Embedded
    val videoMeta: VideoMeta,

    @Relation(parentColumn = "video_id", entityColumn = "video_id")
    val playerResources: List<PlayerResource> = ArrayList()
)