package com.amoscyk.android.rewatchplayer.datasource.vo

import androidx.room.Entity

@Entity(
    primaryKeys = ["videoId"]
)
data class BookmarkedVideo(
    val videoId: String
)