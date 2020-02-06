package com.amoscyk.android.rewatchplayer.ytextractor

data class PlayerResponse(
    val streamingData: StreamingData,
    val videoDetails: VideoDetails
)

data class StreamingData(
    val formats: List<ResourceFormat>,
    val adaptiveFormats: List<ResourceFormat>
)

data class ResourceFormat(
    val itag: Int,
    val url: String,
    val mimeType: String
)

data class VideoDetails(
    val videoId: String,
    val title: String,
    val channelId: String,
    val author: String,
    val shortDescription: String
)