package com.amoscyk.android.rewatchplayer.ytextractor

data class PlayerResponse(
    val streamingData: StreamingData = StreamingData(),
    val videoDetails: VideoDetails = VideoDetails()
)

data class StreamingData(
    val formats: List<ResourceFormat> = listOf(),
    val adaptiveFormats: List<ResourceFormat> = listOf()
)

data class ResourceFormat(
    val itag: Int = -1,
    val url: String = "",
    val mimeType: String = ""
)

data class VideoDetails(
    val videoId: String = "",
    val title: String = "",
    val channelId: String = "",
    val author: String = "",
    val shortDescription: String = "",
    val lengthSeconds: String = "",
    val keywords: List<String> = listOf()
)