package com.amoscyk.android.rewatchplayer.ytextractor

data class PlayerResponse(
    val streamingData: StreamingData
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