package com.amoscyk.android.rewatchplayer.datasource.vo

import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode

data class AvailableStreamFormat(
    val muxedStreamFormat: Map<Int, YouTubeStreamFormatCode.StreamFormat>,
    val videoStreamFormat: Map<Int, YouTubeStreamFormatCode.StreamFormat>,
    val audioStreamFormat: Map<Int, YouTubeStreamFormatCode.StreamFormat>
)